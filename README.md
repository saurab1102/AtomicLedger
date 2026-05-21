# AtomicLedger

[![CI](https://github.com/saurab1102/AtomicLedger/actions/workflows/ci.yml/badge.svg)](https://github.com/saurab1102/AtomicLedger/actions/workflows/ci.yml)

AtomicLedger is a production-style backend system for wallet transfers using idempotency keys, database transactions, row-level locking, immutable ledger entries, reconciliation checks, and audit logs.

The goal is to model money movement safely, where duplicate requests, concurrent transfers, failed transactions, and inconsistent balances are handled deliberately.

## Local development

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
./mvnw spring-boot:run
```

Run the tests:

```bash
./mvnw test
```

The GitHub Actions CI workflow runs the same `./mvnw test` command on GitHub-hosted Ubuntu runners, which provide the Docker support needed by the Testcontainers-based integration suite.

## Inspecting PostgreSQL

```bash
docker compose exec postgres psql -U atomicledger -d atomicledger
```
