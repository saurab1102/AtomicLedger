## 1. Transaction History Persistence And Querying

- [x] 1.1 Add the transaction timestamp persistence needed to support `createdAt` responses and default `createdAt,desc` sorting.
- [x] 1.2 Add repository/query support for paginated wallet transaction history based on ledger-entry participation for a wallet.

## 2. Wallet History API

- [x] 2.1 Add wallet-history response models that expose transaction ID, type, status, direction, amount, currency, optional counterparty wallet, and creation time.
- [x] 2.2 Add `GET /api/v1/wallets/{walletId}/transactions` with `page`, `size`, and `sort` support plus defaults of `0`, `20`, and `createdAt,desc`.
- [x] 2.3 Map deposits to `CREDIT` with `counterpartyWalletId = null`, outgoing transfers to `DEBIT`, and incoming transfers to `CREDIT` with the other wallet as counterparty.
- [x] 2.4 Return `404` when wallet transaction history is requested for a wallet that does not exist.

## 3. Verification

- [x] 3.1 Add an integration test verifying a deposit appears in wallet history as `CREDIT`.
- [x] 3.2 Add integration tests verifying outgoing transfers appear as `DEBIT` and incoming transfers appear as `CREDIT`.
- [x] 3.3 Add an integration test verifying pagination works with the wallet history endpoint.
- [x] 3.4 Add an integration test verifying wallet-not-found history requests return `404`.
