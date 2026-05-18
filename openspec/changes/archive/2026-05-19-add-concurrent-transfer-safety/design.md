## Context

AtomicLedger already supports idempotent wallet-to-wallet transfers that create one transfer transaction, two ledger entries, and two wallet balance updates in a single database transaction. The current transfer flow validates balances before any database row locks are acquired, which leaves a race where two concurrent transfers from the same source wallet can both observe enough balance and overspend it.

Transfers already run against Postgres and Spring Data JPA, so we can rely on database row-level locking rather than introducing distributed locks or optimistic retry loops. The external `POST /api/v1/transfers` contract is already established and must remain unchanged.

## Goals / Non-Goals

**Goals:**
- Prevent overspending when concurrent transfers target the same source wallet.
- Lock both source and destination wallets before checking balances and mutating balances.
- Acquire locks in deterministic order by wallet ID to reduce deadlock risk.
- Keep transfer transaction creation, ledger entry creation, and wallet balance updates in the same database transaction.
- Add a realistic integration test that proves only one of two conflicting transfers succeeds.

**Non-Goals:**
- Changing the transfer request or response contract.
- Adding reconciliation, audit logging, or new asynchronous processing.
- Solving concurrency for deposits in this change.
- Eliminating all deadlock possibilities beyond deterministic wallet lock ordering.

## Decisions

### Use Postgres row-level locking through JPA pessimistic write locks
The transfer service will load both wallets with `FOR UPDATE` semantics inside the existing transaction boundary. This lets Postgres serialize conflicting mutations on the same wallet rows and prevents another transfer from passing the balance check until the first one commits or rolls back.

Alternative considered:
- Optimistic locking with entity versions and retries. Rejected because the service would still need retry orchestration and conflict handling, while the requirement explicitly asks for database row-level locking.

### Lock both wallets in deterministic UUID order
The service will sort the two wallet IDs before loading locked rows. Both concurrent requests will therefore attempt to lock the same rows in the same order, which reduces the classic deadlock pattern where request A locks source then destination while request B locks destination then source.

Alternative considered:
- Lock source first and destination second based on request shape. Rejected because opposite-direction transfers between the same wallets would have a higher deadlock risk.

### Perform balance validation only after locks are acquired
The source wallet `availableBalance` check will happen after both wallet rows are locked and materialized in memory. This ensures the decision is based on the latest committed balance and that no concurrent transfer can change it before mutation.

Alternative considered:
- Pre-check balance before locking for faster failure. Rejected because it preserves the overspending race the change is meant to fix.

### Keep the persistence model and API unchanged
No new schema or HTTP contract is required. The safety improvement can be implemented by extending repository access patterns for locked wallet reads and by updating transfer orchestration in the service layer.

Alternative considered:
- Persist extra transfer lock state or queue transfer execution. Rejected because it would add complexity without improving correctness beyond what row-level locking already guarantees.

### Prove the behavior with a concurrent integration test
The test suite will seed a `1000 INR` source wallet, run two transfer requests concurrently for `800 INR` and `700 INR`, and assert exactly one succeeds. The test will verify the source balance never becomes negative and that transfer accounting rows exist only for the successful request.

Alternative considered:
- Unit-test the locking logic in isolation. Rejected because row-level locking behavior is meaningful only against a real Postgres transaction boundary.

## Risks / Trade-offs

- [Longer lock hold time under contention] → Keep the locked section limited to validation, transaction creation, ledger creation, and wallet updates already required for correctness.
- [Deadlocks are reduced but not impossible] → Lock wallets in sorted ID order and keep the transaction scope narrow.
- [Concurrent integration tests can be flaky if they rely on timing] → Coordinate request start with synchronization primitives so both transfers contend predictably.
- [Pessimistic locks reduce throughput for hot wallets] → Accept the trade-off because preventing financial inconsistency is more important than maximizing concurrent transfer throughput.
