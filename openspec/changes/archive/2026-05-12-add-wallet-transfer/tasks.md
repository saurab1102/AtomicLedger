## 1. Persistence and Schema

- [x] 1.1 Add Flyway changes needed for transfer transaction data and any persisted result snapshot fields required for idempotent replay.
- [x] 1.2 Extend transaction and ledger persistence types to support `TRANSFER` transactions and paired `DEBIT` / `CREDIT` ledger entries.
- [x] 1.3 Extend repository access patterns needed to load source and destination wallets and to look up an existing transfer by `Idempotency-Key`.

## 2. Transfer API and Service Flow

- [x] 2.1 Add request and response models for `POST /api/v1/transfers`, including validation for required `Idempotency-Key`, positive `amount`, required wallet IDs, and supported `currency`.
- [x] 2.2 Implement transfer service logic that validates wallet eligibility, rejects same-wallet transfers, checks currency matching and sufficient source balance, creates one `TRANSFER` transaction, creates two ledger entries, and updates both wallet balances in one database transaction.
- [x] 2.3 Expose the `POST /api/v1/transfers` controller endpoint and return the original transfer result when the same idempotency key is reused.
- [x] 2.4 Add application error handling for missing idempotency key, unsupported currency, wallet not found, inactive wallet, same source and destination wallet, and insufficient balance.

## 3. Integration Testing

- [x] 3.1 Add an integration test for successful transfer that verifies the API response, persisted `TRANSFER` transaction, one `DEBIT` ledger entry, one `CREDIT` ledger entry, and both updated wallet balances.
- [x] 3.2 Add integration tests for insufficient balance, same source and destination wallet, missing idempotency key, unsupported currency, and wallet not found.
- [x] 3.3 Add an integration test for duplicate idempotency key reuse that verifies the original transfer result is returned and no duplicate transaction, ledger entries, or balance updates are created.
