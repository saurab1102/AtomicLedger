## Context

AtomicLedger already supports wallet creation and persists wallets in PostgreSQL with Flyway-managed schema changes. This deposit change is the first money-movement capability, so it needs to establish the project’s accounting shape: transaction records, immutable ledger entries, balance mutation, and retry-safe request handling.

The key constraint is idempotency. Clients may retry `POST /api/v1/wallets/{walletId}/deposit` after timeouts or network failures, and the system must return the original successful result without creating duplicate accounting artifacts. The change also has to preserve the project invariant that wallet balance updates and ledger persistence happen inside one database transaction.

## Goals / Non-Goals

**Goals:**
- Add `POST /api/v1/wallets/{walletId}/deposit` with required `Idempotency-Key` header.
- Validate positive deposit amounts, `INR` currency, and that the target wallet exists and is `ACTIVE`.
- Persist a successful deposit as one `DEPOSIT` transaction and exactly one `CREDIT` ledger entry.
- Increase wallet `availableBalance` atomically with transaction and ledger creation.
- Make repeated use of the same idempotency key return the original deposit result without duplicate writes.
- Cover the behavior with PostgreSQL-backed integration tests.

**Non-Goals:**
- Transfers, withdrawals, reversals, or failed transaction recovery flows.
- Multi-currency support beyond enforcing `INR`.
- Cross-wallet or cross-endpoint idempotency policies beyond this deposit endpoint.
- Ledger balancing across multiple accounts; this change only records the wallet-side credit entry required by the current scope.

## Decisions

### Model deposits with explicit transaction and ledger tables
The design will introduce a `transactions` table and a `ledger_entries` table in addition to the existing `wallets` table.

Rationale:
- A transaction record captures the business event (`DEPOSIT`, `SUCCEEDED`) and becomes the durable object returned by duplicate idempotent requests.
- A ledger entry captures the accounting mutation separately from the wallet balance, which keeps the path open for later reconciliation and transfer features.

Alternatives considered:
- Storing deposit history only on the wallet row was rejected because it cannot express immutable accounting events or idempotent replay cleanly.
- Combining transaction and ledger details into one table was rejected because the concepts serve different purposes and will evolve differently once transfers exist.

### Enforce idempotency through a persisted unique deposit key
Each successful deposit transaction will store the supplied `Idempotency-Key`, and the database will enforce uniqueness for deposit requests using a unique constraint scoped to the deposit transaction record.

Rationale:
- Database-enforced uniqueness is the most reliable guard against duplicate inserts under concurrent retries.
- Reusing the stored transaction lets the API return the same logical result without re-running business side effects.

Alternatives considered:
- In-memory idempotency caches were rejected because they do not survive restarts and are unsafe in multi-instance deployments.
- A separate idempotency table was considered, but the transaction record already represents the durable result we need to replay, so adding a third persistence surface is unnecessary for this phase.

### Lock the wallet row during deposit processing
The deposit service will load the wallet with a write lock and perform validation, transaction creation, ledger entry creation, and balance update inside one Spring-managed database transaction.

Rationale:
- Row-level locking prevents concurrent deposits from reading the same starting balance and producing lost updates.
- A single database transaction preserves the invariant that wallet balance, transaction record, and ledger entry stay in sync.

Alternatives considered:
- Optimistic locking alone was rejected for the first money-movement flow because retry orchestration would become more complex and less deterministic.
- Updating balance before writing the transaction and ledger was rejected because partial success would break accounting traceability.

### Persist an after-balance snapshot for replaying the original result
The ledger entry and/or transaction record will store the wallet balance after the deposit is applied so duplicate requests can return the original result even if later deposits change the wallet again.

Rationale:
- The requirement is to return the original transaction result, not the wallet’s balance at the time of the duplicate replay.
- Persisting an after-balance snapshot makes duplicate responses deterministic and testable.

Alternatives considered:
- Rebuilding the response from the wallet’s current balance was rejected because subsequent activity would make the replay incorrect.

### Treat missing header and invalid business input as stable client errors
The controller layer will explicitly require the `Idempotency-Key` header and continue using stable validation error payloads for missing header, invalid amount, unsupported currency, and wallet lookup failures.

Rationale:
- This keeps the API contract predictable for clients and integration tests.
- It preserves the validation approach already established by wallet creation.

Alternatives considered:
- Relying on default framework exceptions without application mapping was rejected because the payload shape is harder to control.

## Risks / Trade-offs

- Concurrent reuse of the same idempotency key can race before the original transaction is fully committed → Mitigation: rely on a database unique constraint and handle duplicate-key recovery by reading back the stored transaction result.
- Storing only one wallet-side `CREDIT` entry is intentionally narrower than full double-entry bookkeeping → Mitigation: keep the transaction and ledger schema extensible so a later transfer/change can add the counter-entry model without breaking deposit history.
- Introducing a balance snapshot creates denormalized data alongside the wallet balance → Mitigation: treat the snapshot as immutable historical output used for replay and audit rather than as the live source of truth.
- Wallet-not-found and wallet-inactive behavior can leak into multiple layers if not centralized → Mitigation: perform wallet eligibility validation in the deposit service and map domain exceptions consistently at the API boundary.

## Migration Plan

1. Add a Flyway migration for `transactions` and `ledger_entries`, including foreign keys to `wallets` and a unique constraint covering deposit idempotency.
2. Deploy repository, service, controller, and exception-handling updates together so the endpoint and schema are introduced atomically.
3. Run PostgreSQL-backed integration tests to verify duplicate retries do not create extra transaction, ledger, or balance mutations.
4. Roll forward with follow-up migrations if schema refinement is needed; avoid destructive rollback once deposit records exist.

## Open Questions

- No blocking open questions for this change. The response payload can be finalized during implementation as long as it contains the original deposit transaction result needed for idempotent replay.
