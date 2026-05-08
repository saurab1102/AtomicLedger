## Why

AtomicLedger needs a foundational wallet resource before it can safely support deposits, transfers, and ledger-backed balance changes. Adding wallet creation now establishes the first domain object, validates core API and persistence patterns, and gives later money-movement features a stable starting point.

## What Changes

- Add a `POST /api/v1/wallets` API to create wallets.
- Require `ownerReference` and `currency` in the request body, with validation errors for blank or unsupported values.
- Support only `INR` as the accepted wallet currency in this first version.
- Initialize every newly created wallet with `availableBalance = 0` and `status = ACTIVE`.
- Persist wallets in PostgreSQL using a Flyway migration and UUID primary keys.
- Add integration tests covering successful creation, missing `ownerReference`, unsupported `currency`, and the zero initial balance contract.
- Explicitly defer deposit and transfer behavior to later changes.

## Capabilities

### New Capabilities
- `wallet-management`: Creation and persistence of wallets, including API validation and wallet initialization rules.

### Modified Capabilities
- None.

## Impact

- Adds the first versioned wallet API under `/api/v1/wallets`.
- Introduces wallet domain, persistence, and migration code in the Spring Boot application.
- Requires PostgreSQL schema changes managed through Flyway.
- Expands integration test coverage for API validation and persisted wallet defaults.
