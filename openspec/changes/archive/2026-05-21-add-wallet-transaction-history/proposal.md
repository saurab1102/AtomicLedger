## Why

AtomicLedger can currently create wallets, accept deposits, and move funds, but it does not provide a wallet-centric view of transaction history. Adding paginated wallet transaction history now makes the ledger-backed activity of a wallet inspectable through the API without requiring direct database access.

## What Changes

- Add `GET /api/v1/wallets/{walletId}/transactions` to return paginated transaction history for a wallet.
- Include transactions where the wallet participated through ledger entries, covering deposits plus incoming and outgoing transfers.
- Return wallet-specific history records with direction, amount, currency, optional counterparty wallet, status, and creation time.
- Support pagination query parameters `page`, `size`, and `sort` with defaults of `0`, `20`, and `createdAt,desc`.
- Add integration coverage for deposit history, outgoing transfer history, incoming transfer history, pagination, and wallet-not-found behavior.
- Do not add date filtering, CSV export, or other search features in this change.

## Capabilities

### New Capabilities

### Modified Capabilities
- `wallet-management`: wallets now expose paginated transaction history through the wallet API for existing wallets

## Impact

- Adds a new wallet API endpoint, response DTOs, and pagination/query logic over transaction and ledger data.
- Requires repository support for retrieving wallet-participation history and mapping it into wallet-specific directions and counterparties.
- Expands integration tests to verify history semantics and paging behavior.
