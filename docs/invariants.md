# AtomicLedger Invariants

1. Ledger entries are immutable after creation.
2. Every successful money movement must be represented by ledger entries.
3. Every successful transfer must create exactly one DEBIT ledger entry for the source wallet and one CREDIT ledger entry for the destination wallet.
4. For every successful transaction, total debit amount must equal total credit amount.
5. Wallet balance mutations and their corresponding ledger entries must be committed in the same database transaction.
6. A failed transaction must not mutate wallet balances.
7. A duplicate request with the same idempotency key must not create duplicate money movement.
8. Concurrent transfers from the same wallet must not allow overspending.
9. Wallet balances must never become negative.
10. Reconciliation must detect mismatch between cached wallet balance and ledger-derived balance.