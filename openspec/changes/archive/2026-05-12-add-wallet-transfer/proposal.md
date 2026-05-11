## Why

AtomicLedger now supports wallet creation and idempotent deposits, but money movement between wallets is still missing. Adding wallet-to-wallet transfers now completes the first two-sided ledger flow and extends the backend from single-wallet balance changes to atomic value movement between accounts.

## What Changes

- Add a `POST /api/v1/transfers` API for moving funds from one wallet to another.
- Require an `Idempotency-Key` header and reject requests that omit it.
- Require `sourceWalletId`, `destinationWalletId`, `amount`, and `currency` in the request body.
- Validate positive amounts, support only `INR`, require distinct source and destination wallets, and require both wallets to exist and be `ACTIVE`.
- Reject transfers when the source wallet has insufficient `availableBalance` or when wallet currencies do not match the requested transfer currency.
- Persist a successful transfer as one transaction record with type `TRANSFER` and status `SUCCEEDED`.
- Persist exactly two ledger entries for each successful transfer: one `DEBIT` for the source wallet and one `CREDIT` for the destination wallet.
- Decrease the source wallet `availableBalance` and increase the destination wallet `availableBalance` inside the same database transaction that persists the transaction and ledger entries.
- Return the original transaction result when the same `Idempotency-Key` is reused, without creating duplicate transaction, ledger, or balance changes.
- Add integration tests for successful transfer, insufficient balance, same source/destination wallet, missing idempotency key, unsupported currency, wallet not found, and duplicate idempotency key reuse.
- Explicitly defer concurrency locking improvements and reconciliation to later changes.

## Capabilities

### New Capabilities
- `wallet-transfer`: Idempotent wallet-to-wallet transfer processing, including atomic balance movement, transfer transaction persistence, and paired ledger entry creation.

### Modified Capabilities
- `wallet-management`: Wallet requirements now include transfer eligibility rules for active wallets, distinct source and destination wallets, sufficient source balance, and currency matching.

## Impact

- Adds a new versioned transfer API under `/api/v1/transfers`.
- Extends the transaction and ledger persistence model to support transfer semantics.
- Expands service-layer business rules for transfer validation, idempotent replay, and atomic multi-wallet balance updates.
- Adds PostgreSQL-backed integration coverage for transfer success, validation failures, and duplicate-request handling.
