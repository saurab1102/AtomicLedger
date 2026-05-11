# AtomicLedger Invariants

1. A successful transfer must create one debit ledger entry and one credit ledger entry.
2. Total debit amount must equal total credit amount for every successful transaction.
3. A duplicate idempotency key must not create duplicate money movement
4. Wallet balance and ledger entries must be update in the same database transaction.
5. Concurrent transfers from the same wallet must not allow overspending.
6. Failed transactions must not mutate wallet balances.
7. Ledger entries are immutable after creation.
8. Reconciliation must detect mismatch between cached balance and ledger-dervied balance.
9. A successful deposit must create one CREDIT ledger entry.
10. A duplicate deposit request with the same idempotency key must not increase balance twice.