## 1. Locked Wallet Loading

- [x] 1.1 Extend wallet repository access so transfer flow can load and lock multiple wallet rows with Postgres row-level write locks.
- [x] 1.2 Ensure locked wallet loading acquires the source and destination rows in deterministic wallet ID order inside the transfer transaction.

## 2. Transfer Concurrency Safety

- [x] 2.1 Update transfer service flow to acquire both wallet locks before validating source balance or mutating either wallet.
- [x] 2.2 Keep transfer transaction creation, paired ledger entry creation, and both wallet balance updates inside the same locked database transaction.
- [x] 2.3 Preserve the existing transfer API behavior and idempotent replay behavior while rejecting overspending attempts under contention.

## 3. Integration Testing

- [x] 3.1 Add an integration test that seeds a `1000 INR` source wallet and runs concurrent `800 INR` and `700 INR` transfer attempts.
- [x] 3.2 Verify the concurrent transfer test proves exactly one transfer succeeds and the final source balance is never negative.
- [x] 3.3 Verify the concurrent transfer test proves ledger entries and transfer records exist only for the successful transfer.
