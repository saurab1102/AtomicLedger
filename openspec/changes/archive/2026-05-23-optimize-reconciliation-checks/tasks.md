## 1. Schema And Query Foundations

- [x] 1.1 Add a Flyway migration for reconciliation-supporting indexes on `ledger_entries(wallet_id)`, `ledger_entries(transaction_id)`, `transactions(status)`, and `transactions(transaction_type, status)`.
- [x] 1.2 Add aggregate repository queries or projections for transfer reconciliation mismatches so ledger-entry counts and debit/credit totals are summarized by transaction in SQL.
- [x] 1.3 Add aggregate repository queries or projections for deposit reconciliation mismatches so credit-entry shape is summarized by transaction in SQL.

## 2. Reconciliation Service Refactor

- [x] 2.1 Refactor `ReconciliationService` to keep wallet balance checks on the existing balance summary query while replacing transfer and deposit N+1 loops with the new aggregate query results.
- [x] 2.2 Cap reconciliation mismatch details to 100 rows per failed-check type while preserving the existing `PASS` and `FAIL` behavior.
- [x] 2.3 Add timing logs around wallet, transfer, and deposit reconciliation phases.

## 3. Verification

- [x] 3.1 Update reconciliation integration tests to verify wallet balance mismatches are still detected.
- [x] 3.2 Update reconciliation integration tests to verify transfer ledger mismatches are still detected.
- [x] 3.3 Update reconciliation integration tests to verify deposit ledger mismatches are still detected.
- [x] 3.4 Validate the OpenSpec change after the artifacts are complete.
