## Context

AtomicLedger currently persists wallet balances as cached state on the `wallets` table while also recording ledger entries and successful transactions for deposits and transfers. That gives the system two views of money movement: a fast wallet balance and an auditable event trail. Once both exist, reconciliation becomes important because bugs, manual data changes, or partial corruption can leave those views inconsistent.

The requested reconciliation behavior is read-only from the business perspective: it should inspect existing rows and report whether invariants hold, not repair them. The API also needs to surface failed checks in a structured way that tests and operators can interpret easily.

## Goals / Non-Goals

**Goals:**
- Add a reconciliation endpoint that inspects wallets, transactions, and ledger entries and returns `PASS` or `FAIL`.
- Detect wallet balance mismatches by comparing cached `availableBalance` against ledger-derived balance.
- Detect broken accounting structure for successful deposits and transfers.
- Keep reconciliation read-only and deterministic.
- Add integration tests that prove both healthy and corrupted accounting states are detected correctly.

**Non-Goals:**
- Automatically fixing corrupted data.
- Adding audit-log persistence or outbox event publishing.
- Introducing background scheduling; this change is only for an explicit API-triggered run.
- Reconciling failed or pending transactions beyond the successful-transaction checks explicitly required.

## Decisions

### Expose reconciliation as an explicit API run
The system will provide `POST /api/v1/reconciliation/run` so callers can trigger a fresh integrity scan on demand and receive a structured report immediately.

Alternative considered:
- Background-only reconciliation jobs. Rejected because the requirement explicitly asks for an API endpoint and immediate result reporting.

### Model reconciliation as a report with overall status and failed checks
The response will contain an overall status (`PASS` or `FAIL`) plus a list of failed check details describing what invariant failed and which wallet or transaction was affected.

Alternative considered:
- Returning only a boolean health result. Rejected because failures would be hard to diagnose and tests could not assert the exact broken invariant.

### Derive wallet balance from ledger entries using grouped aggregation
Wallet reconciliation will compute each wallet's ledger-derived balance as total `CREDIT` amount minus total `DEBIT` amount using database aggregation, then compare that against cached `availableBalance`.

Alternative considered:
- Reconstruct balances in Java by loading all ledger entries into memory. Rejected because it is less efficient and more complex than letting Postgres aggregate by wallet.

### Reconcile successful transaction shapes separately by transaction type
Successful transfers and deposits will be checked with transaction-type-specific invariants:
- `TRANSFER`: exactly one `DEBIT`, exactly one `CREDIT`, and equal total debit/credit amounts
- `DEPOSIT`: exactly one `CREDIT`

Alternative considered:
- One generic ledger-count rule for all transaction types. Rejected because deposit and transfer accounting shapes are intentionally different.

### Keep corrupted-state tests at the integration level
The test suite will create healthy flows using the public API, then intentionally corrupt persisted rows directly through repositories or SQL-facing persistence helpers before running reconciliation.

Alternative considered:
- Unit tests with mocked repositories. Rejected because reconciliation depends on actual persistence behavior and aggregate queries over Postgres-backed state.

## Risks / Trade-offs

- [Full reconciliation can become heavier as data grows] → Keep queries aggregated in the database and scope the first version to the required invariants only.
- [Structured failure output can become noisy] → Use concise per-check detail records that identify the failed invariant and affected entity.
- [Tests that corrupt state directly may couple to persistence details] → Limit corruption to the smallest possible changes and assert only required reconciliation outcomes.
- [Read-only reconciliation could still observe changing data under concurrent writes] → Accept this for the on-demand first version and rely on the current transaction boundaries of writes; this change does not introduce snapshot-based reconciliation.
