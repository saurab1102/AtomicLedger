## 1. Reconciliation Read Model

- [x] 1.1 Add repository queries needed to derive wallet balances from ledger entries and inspect successful deposit and transfer ledger composition.
- [x] 1.2 Define reconciliation result models that capture overall status plus failed-check details.

## 2. Reconciliation API and Service Flow

- [x] 2.1 Add `POST /api/v1/reconciliation/run` and wire it to a reconciliation service flow.
- [x] 2.2 Implement wallet balance reconciliation by comparing cached `availableBalance` with ledger-derived `CREDIT - DEBIT` totals.
- [x] 2.3 Implement successful transfer checks for exactly one `DEBIT`, exactly one `CREDIT`, and equal total debit/credit amounts.
- [x] 2.4 Implement successful deposit checks for exactly one `CREDIT` ledger entry.
- [x] 2.5 Return structured `PASS` or `FAIL` responses with failed-check details for every detected reconciliation problem.

## 3. Integration Testing

- [x] 3.1 Add an integration test for healthy accounting data that returns reconciliation status `PASS`.
- [x] 3.2 Add an integration test for a corrupted wallet balance that returns `FAIL` with wallet mismatch details.
- [x] 3.3 Add an integration test for a missing ledger entry that returns `FAIL` with broken transaction-ledger details.
- [x] 3.4 Add an integration test for unbalanced transfer ledger amounts that returns `FAIL` with unbalanced-transfer details.
