## Why

AtomicLedger now caches wallet balances while also storing ledger entries and transactions as accounting history, so we need a way to detect drift or broken accounting invariants before they silently compound. A reconciliation endpoint gives us an explicit integrity check over wallet balances and successful deposit/transfer records.

## What Changes

- Add `POST /api/v1/reconciliation/run` to execute reconciliation checks across wallets, transactions, and ledger entries.
- Compare each wallet's cached `availableBalance` against a ledger-derived balance computed as total `CREDIT` minus total `DEBIT` for that wallet.
- Verify successful `TRANSFER` transactions have exactly one `DEBIT` ledger entry, exactly one `CREDIT` ledger entry, and equal total debit/credit amounts.
- Verify successful `DEPOSIT` transactions have exactly one `CREDIT` ledger entry.
- Return a structured reconciliation result with overall `PASS` or `FAIL` status plus failed-check details.
- Add integration coverage for healthy data, corrupted wallet balance, missing ledger entry, and unbalanced transfer ledger entries.

## Capabilities

### New Capabilities
- `reconciliation-checks`: run reconciliation over wallets, transactions, and ledger entries and report integrity failures in a structured API response

### Modified Capabilities

## Impact

- Adds a new reconciliation API, service flow, and response model.
- Introduces read-side repository queries that aggregate wallet ledger balances and inspect ledger composition per successful transaction.
- Expands the integration test suite to cover intentionally corrupted accounting states and failure reporting.
