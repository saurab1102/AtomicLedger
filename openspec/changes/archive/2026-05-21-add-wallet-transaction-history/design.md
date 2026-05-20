## Context

AtomicLedger already stores deposits and transfers as transaction rows plus wallet-specific ledger entries. Deposits create one `CREDIT` ledger entry for the target wallet, while transfers create one `DEBIT` entry for the source wallet and one `CREDIT` entry for the destination wallet. That existing accounting shape makes wallet transaction history naturally wallet-relative: the history endpoint should be driven by the ledger participation of a wallet rather than by raw transaction ownership alone.

The new endpoint also introduces pagination semantics to the wallet API for the first time. We need stable sorting, sensible defaults, and response records that describe a transaction from the perspective of a single wallet, including direction and counterparty.

## Goals / Non-Goals

**Goals:**
- Add `GET /api/v1/wallets/{walletId}/transactions` and require that `walletId` refers to an existing wallet.
- Return deposit and transfer transactions in which the wallet participated through ledger entries.
- Map each history item into wallet-relative fields: `direction`, `counterpartyWalletId`, amount, currency, status, type, and `createdAt`.
- Support pagination query params `page`, `size`, and `sort` with defaults of `0`, `20`, and `createdAt,desc`.
- Add integration tests covering deposits, incoming and outgoing transfers, pagination, and wallet-not-found behavior.

**Non-Goals:**
- Adding date-based filters, free-text search, or CSV export.
- Redesigning the transaction schema beyond what is needed to expose created timestamps.
- Returning unrelated ledger-entry detail beyond the wallet-facing history projection.

## Decisions

### Drive history from ledger-entry participation
The endpoint will query transactions that have ledger entries for the requested wallet, rather than treating `transactions.wallet_id` alone as the source of truth.

Why:
- Deposits and incoming transfers both appear because the wallet has a ledger entry.
- It reflects the accounting model already used by reconciliation and balance derivation.
- It keeps the history definition consistent with the requirement that the wallet “participated through ledger entries.”

Alternative considered:
- Querying only `transactions.wallet_id` plus transfer counterparty joins. Rejected because it would miss the destination-side participation model and diverge from ledger truth.

### Add a transaction creation timestamp
To support the required default sort of `createdAt,desc`, add a `created_at` field to persisted wallet transactions and expose it through the history response.

Why:
- The endpoint contract explicitly requires `createdAt`.
- Sorting by transaction ID or relying on insertion order would be unstable and implicit.

Alternative considered:
- Sorting by ledger-entry creation time. Rejected because ledger entries do not currently expose a separate created timestamp and the API is returning transaction history, not ledger-entry history.

### Compute direction from the wallet’s ledger entry
The endpoint will derive `direction` from the ledger entry tied to the requested wallet:
- `CREDIT` for deposits and incoming transfers
- `DEBIT` for outgoing transfers

Why:
- This avoids transaction-type-specific branching as the primary source of truth.
- The ledger entry already encodes the wallet-relative movement.

Alternative considered:
- Infer direction from `transaction.wallet` vs `transaction.counterpartyWallet`. Rejected because ledger entry type is the most direct representation of wallet-relative movement.

### Compute counterparty from the transaction shape
For `TRANSFER` history items, `counterpartyWalletId` will be the other wallet in the transaction. For `DEPOSIT`, it will be `null`.

Why:
- This matches the API contract and gives clients a simple wallet-facing representation.
- Deposits do not have a second wallet participant in the current model.

Alternative considered:
- Return the deposit wallet itself as counterparty. Rejected because it would be misleading and redundant.

### Use Spring Data pagination at the repository layer
Add repository support that returns a `Page` ordered by transaction creation time and ID, with the service/controller translating `page`, `size`, and `sort` defaults into a `Pageable`.

Why:
- It keeps paging in the database rather than slicing in memory.
- It aligns with standard Spring MVC pagination conventions and the requested query params.

Alternative considered:
- Load all history rows and paginate in Java. Rejected because it would not scale and would make sorting less trustworthy.

## Risks / Trade-offs

- [History is wallet-relative, not globally canonical per transaction] -> Make `direction` and `counterpartyWalletId` explicit in the response so clients understand the viewpoint.
- [Joining ledger entries to transactions may duplicate results if the query is not constrained carefully] -> Query exactly one ledger entry per requested wallet participation and page distinct transaction-participation rows.
- [Adding `created_at` to transactions is a schema change] -> Use an additive Flyway migration and initialize existing rows safely if needed.

## Migration Plan

1. Add a Flyway migration for `transactions.created_at` and update the JPA entity.
2. Add repository/query support for wallet-participation history with pagination and stable sorting.
3. Add response DTOs plus `GET /api/v1/wallets/{walletId}/transactions`.
4. Add integration tests for history direction, pagination, and wallet-not-found behavior.

Rollback:
- The change is additive.
- If rolled back, the new endpoint disappears while the added timestamp column can remain unused.

## Open Questions

- Should the response include total pages/elements using a custom page envelope or rely on Spring’s page serialization conventions? Current scope assumes an explicit paginated response object is preferable for a stable API contract.
