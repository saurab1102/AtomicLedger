## 1. Persistence and Schema

- [x] 1.1 Add Flyway migrations for deposit transaction and ledger entry tables, including foreign keys to wallets and a unique constraint for deposit idempotency.
- [x] 1.2 Implement JPA entities, enums, and repositories for deposit transactions and ledger entries, including any persisted balance snapshot needed for idempotent replay.
- [x] 1.3 Extend wallet repository access to support loading an `ACTIVE` wallet for deposit processing with the locking strategy chosen in the design.

## 2. Deposit API and Service Flow

- [x] 2.1 Add request and response models for `POST /api/v1/wallets/{walletId}/deposit`, including validation for required `Idempotency-Key`, positive `amount`, and supported `currency`.
- [x] 2.2 Implement deposit service logic that validates wallet eligibility, enforces idempotency, creates a `DEPOSIT` transaction, creates one `CREDIT` ledger entry, and updates wallet `availableBalance` in one database transaction.
- [x] 2.3 Expose the `POST /api/v1/wallets/{walletId}/deposit` controller endpoint and return the original transaction result when the same idempotency key is reused.
- [x] 2.4 Add application error handling for missing idempotency key, unsupported currency, invalid amount, wallet not found, and wallet not active.

## 3. Integration Testing

- [x] 3.1 Add an integration test for successful deposit that verifies the API response, persisted `DEPOSIT` transaction, single `CREDIT` ledger entry, and updated wallet balance.
- [x] 3.2 Add integration tests for missing idempotency key, invalid amount, unsupported currency, and wallet not found.
- [x] 3.3 Add an integration test for duplicate idempotency key reuse that verifies the original result is returned and no duplicate transaction, ledger, or balance update is created.
