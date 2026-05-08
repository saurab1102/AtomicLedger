## 1. Persistence Setup

- [x] 1.1 Add a Flyway migration that creates the `wallets` table with a UUID primary key and columns for owner reference, currency, available balance, and status.
- [x] 1.2 Implement the wallet persistence model and repository mappings for PostgreSQL storage.

## 2. Wallet Creation API

- [x] 2.1 Add request and response models for `POST /api/v1/wallets`, including validation for required `ownerReference` and `currency` fields.
- [x] 2.2 Implement wallet creation service logic that accepts only `INR`, generates a UUID, and initializes `availableBalance` to `0` with `status` set to `ACTIVE`.
- [x] 2.3 Expose the `POST /api/v1/wallets` controller endpoint and persist newly created wallets in a transaction-safe flow.
- [x] 2.4 Add application error handling so invalid input returns stable validation errors for blank owner reference and unsupported currency.

## 3. Integration Testing

- [x] 3.1 Add an integration test for successful wallet creation that verifies the API response and persisted wallet record.
- [x] 3.2 Add an integration test for missing or blank `ownerReference` validation failure.
- [x] 3.3 Add an integration test for unsupported currency validation failure.
- [x] 3.4 Add an integration test that verifies every created wallet starts with `availableBalance = 0` and `status = ACTIVE`.
