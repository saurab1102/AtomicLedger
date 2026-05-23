# AtomicLedger

[![CI](https://github.com/saurab1102/AtomicLedger/actions/workflows/ci.yml/badge.svg)](https://github.com/saurab1102/AtomicLedger/actions/workflows/ci.yml)

AtomicLedger is a small backend service for wallet operations, but it is built around the parts that usually make these systems harder than they look: retries, concurrent balance updates, ledger correctness, auditability, and post-commit event delivery.

## Problem Statement

Wallet APIs are easy to sketch and easy to get wrong. The trouble starts when requests are retried, two transfers touch the same wallet at once, or the stored balance drifts from the ledger that is supposed to justify it.

- clients retry requests after timeouts
- transfers race against each other
- balances can drift from ledger truth if writes are not modeled carefully
- integrations often need durable events after a commit
- failed operations still need auditability

This project is meant to be a clean implementation of those concerns. Deposits and transfers run inside transactions, ledger entries are immutable, duplicate requests replay the first committed result, reconciliation checks the accounting rules, and committed changes leave behind outbox events for downstream consumers.

## What It Does

- Wallet creation with fixed currency and active status.
- Idempotent deposits.
- Concurrency-safe wallet-to-wallet transfers.
- Immutable ledger entries for every successful money movement.
- Paginated transaction history per wallet.
- Reconciliation checks for stored balances and ledger structure.
- Audit logs for business events and duplicate replays.
- Transactional outbox events plus a scheduled publisher.
- OpenAPI docs and Swagger UI for local inspection.

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Flyway
- Bean Validation
- springdoc-openapi / Swagger UI
- Testcontainers
- GitHub Actions

## Architecture Overview

The runtime shape is simple:

- controllers expose REST APIs under `/api/v1/...`
- `WalletService` handles wallet creation, deposits, transfers, and history
- `ReconciliationService` runs accounting checks
- `AuditLogService` persists audit records
- `OutboxEventService` writes outbox rows inside the active transaction
- `OutboxEventWorker` polls pending events and marks them published after a successful publish step
- JPA repositories persist everything to PostgreSQL

What matters more than the layering is the transaction boundary. A successful write can update wallet state, create a business transaction, append ledger entries, record audit data, and enqueue an outbox event in one commit.

## Domain Model Overview

- `Wallet`: current wallet state, including currency, status, and cached available balance.
- `WalletTransaction`: business transaction record for deposits and transfers.
- `LedgerEntry`: immutable accounting entry tied to a transaction and wallet.
- `AuditLog`: durable operational and domain audit trail.
- `OutboxEvent`: durable integration event waiting to be published or already published.

A successful deposit creates one transaction row and one `CREDIT` ledger entry. A successful transfer creates one transaction row plus two ledger entries: `DEBIT` for the source wallet and `CREDIT` for the destination wallet.

## API Overview

Public APIs:

- `POST /api/v1/wallets`
- `POST /api/v1/wallets/{walletId}/deposit`
- `GET /api/v1/wallets/{walletId}/transactions`
- `POST /api/v1/transfers`
- `POST /api/v1/reconciliation/run`
- `GET /api/v1/audit-logs`
- `GET /api/v1/outbox-events`

OpenAPI endpoints for local development:

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`

Deposit and transfer requests require the `Idempotency-Key` header. Error responses use one JSON shape with `errorCode`, `message`, `details`, and `timestamp`.

## Core Invariants

The code leans on a few invariants:

- Wallet balances must match the balance derived from ledger entries.
- A successful transfer must produce exactly one `DEBIT` entry and one `CREDIT` entry.
- The total debits and credits of a successful transfer must be equal.
- A successful deposit must produce exactly one `CREDIT` entry.
- Duplicate idempotent requests must not create duplicate state changes.
- Outbox events for successful domain changes must commit with the same database transaction as the change they describe.

These are enforced by the write path and then checked again by reconciliation against persisted data.

## Idempotency Design

Deposits and transfers accept a caller-provided `Idempotency-Key`. The service normalizes the key, looks up any existing transaction with that key, and returns the original committed response if it finds one.

That prevents duplicate balance changes when a client retries after a timeout. It also covers races: if two requests arrive with the same key, the database uniqueness check decides the winner and the losing request replays the stored result instead of creating a second effect.

Duplicate replays are still auditable. They return the original success response, and the service writes a separate audit record for the replay.

## Transfer Concurrency Design

Transfers are where concurrency control matters most. The service:

- locks the participating wallets with `SELECT ... FOR UPDATE`
- acquires both locks in a stable sorted order
- checks balance and applies both updates inside one transaction

The sorted lock order is there to reduce deadlock risk when opposing transfers race each other.

## Reconciliation Design

Reconciliation is there to answer a narrow question: does the stored state still agree with the ledger model?

The reconciliation flow:

- derives balances from ledger entries and compares them to stored wallet balances
- verifies the structure of successful transfer ledger entries
- verifies debit and credit totals for successful transfers
- verifies the structure of successful deposit ledger entries

If reconciliation fails, the API returns `FAIL`, records audit data, and writes a failure outbox event so the failure can be observed outside the request itself.

## Outbox Pattern

The project uses a transactional outbox so database state and integration events do not drift apart.

For wallet creation, successful deposits, successful transfers, and selected failure outcomes such as insufficient-balance transfers and failed reconciliation:

- the domain operation commits
- an `outbox_events` row is written in the same transaction
- the row starts in `PENDING`
- a scheduled worker picks up pending rows
- publishing currently means logging the payload
- on success the row is marked `PUBLISHED`
- on failure the worker increments `attemptCount` and records `lastError`

Right now publishing just logs the payload. That is intentional. The point here is the transaction and retry shape, not the broker integration itself.

## Local Setup

Prerequisites:

- Java 21
- Docker

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
./mvnw spring-boot:run
```

Inspect the database if needed:

```bash
docker compose exec postgres psql -U atomicledger -d atomicledger
```

## Running Tests

Run the full test suite with:

```bash
./mvnw test
```

The suite uses Testcontainers with PostgreSQL, so Docker needs to be available. GitHub Actions runs the same command on GitHub-hosted Ubuntu runners.

## Load Testing

There is a local k6 script at `load-tests/atomicledger-transfer-load.js` for exercising authenticated wallet, deposit, and transfer traffic against a running AtomicLedger instance.

Install k6, start the app on `localhost:8080`, and run:

```bash
k6 run load-tests/atomicledger-transfer-load.js
```

Useful runtime inputs:

```bash
API_KEY=atomicledger-local-api-key \
VUS=10 \
DURATION=60s \
k6 run load-tests/atomicledger-transfer-load.js
```

The script:

- targets `http://localhost:8080` by default
- sends `X-API-Key` on protected API requests
- creates source and destination wallets automatically unless you provide both `SOURCE_WALLET_ID` and `DESTINATION_WALLET_ID`
- seeds the source wallet with an initial deposit
- mixes repeated transfer attempts with deliberate duplicate idempotency-key replays

Useful environment variables:

- `BASE_URL`
- `API_KEY`
- `SOURCE_WALLET_ID`
- `DESTINATION_WALLET_ID`
- `INITIAL_DEPOSIT_AMOUNT`
- `DEPOSIT_AMOUNT`
- `TRANSFER_AMOUNT`
- `DEPOSIT_EVERY`
- `VUS`
- `DURATION`

This script is for local behavior exercise and rough load exploration, not production-grade benchmarking.

## Tradeoffs And Future Improvements

- Wallet balance is cached for fast reads. That keeps the API simple, but it means reconciliation has to keep proving the cached value still matches ledger truth.
- The outbox publisher only logs events today. A production version would send them to Kafka, SQS, or another broker and would need backoff, dead-letter handling, and better operational metrics.
- Reconciliation is synchronous and API-triggered. In a real deployment it would probably run on a schedule and emit alerts.
- Transaction history supports pagination and sorting, but not date filters, exports, or richer statement views yet.
- The system stays in one service and one relational database on purpose. That keeps the failure model legible while still showing the patterns that matter before a system is split into separate components.
