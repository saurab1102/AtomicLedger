## Why

AtomicLedger needs wallet deposits to be safe against retries, client timeouts, and duplicate submissions. Adding idempotent deposits now establishes the first money-movement flow and proves the transaction and ledger model before transfers are introduced.

## What Changes

- Add a `POST /api/v1/wallets/{walletId}/deposit` API for depositing funds into an existing wallet.
- Require an `Idempotency-Key` header and reject requests that omit it.
- Require `amount` and `currency` in the request body, validating positive amounts and supporting only `INR`.
- Validate that the target wallet exists and is currently `ACTIVE` before processing the deposit.
- Persist a deposit transaction record with type `DEPOSIT` and status `SUCCEEDED` for each successful deposit.
- Persist exactly one `CREDIT` ledger entry per successful deposit and increase wallet `availableBalance` by the deposited amount.
- Ensure transaction creation, ledger entry creation, and wallet balance update occur inside one database transaction.
- Return the original transaction result when the same `Idempotency-Key` is reused, without creating duplicate transaction, ledger, or balance changes.
- Add Flyway migrations and integration tests covering success, missing idempotency key, invalid amount, unsupported currency, wallet not found, and duplicate idempotency key reuse.
- Explicitly defer transfers to a later change.

## Capabilities

### New Capabilities
- `wallet-deposit`: Idempotent wallet deposit processing, including transaction persistence, ledger entry creation, and retry-safe API behavior.

### Modified Capabilities
- `wallet-management`: Wallet requirements now include accepting deposits only for existing `ACTIVE` wallets and returning updated balances after successful deposits.

## Impact

- Adds a new versioned wallet deposit API under `/api/v1/wallets/{walletId}/deposit`.
- Introduces transaction and ledger persistence models, repository logic, and supporting Flyway schema changes.
- Expands the service layer to enforce idempotency and atomic balance updates in PostgreSQL.
- Adds integration tests for deposit validation, duplicate request handling, and persisted accounting records.
