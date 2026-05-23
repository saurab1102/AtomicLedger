## Context

`ReconciliationService` already checks the right invariants, but the transfer and deposit phases currently fetch every successful transaction and then load ledger entries one transaction at a time. That N+1 pattern makes reconciliation cost grow with both transaction count and round trips, even though the invariants being checked are set-based and can be summarized directly in SQL.

This change keeps the reconciliation API contract intact while tightening the internals around three concerns:

- set-based mismatch detection for transfers and deposits
- index support for the reconciliation-heavy access paths
- better operator visibility through timing logs for each reconciliation phase

The current wallet balance check already uses an aggregate ledger balance query and should remain on that approach.

## Goals / Non-Goals

**Goals:**
- Replace per-transaction transfer and deposit reconciliation lookups with aggregate SQL queries
- Keep wallet balance reconciliation on the current aggregate balance summary pattern
- Add indexes that support the transaction and ledger-entry lookups reconciliation depends on
- Emit timing logs for wallet, transfer, and deposit reconciliation phases
- Cap failed-check detail rows at 100 per mismatch type without changing the overall `PASS` or `FAIL` semantics
- Preserve automated coverage for wallet, transfer, and deposit mismatch detection

**Non-Goals:**
- Changing the reconciliation endpoint shape or authentication behavior
- Introducing asynchronous reconciliation or background batching
- Replacing audit or outbox behavior for failed reconciliations
- Adding new reconciliation metrics beyond the existing operational metrics work

## Decisions

### Use set-based aggregate queries for transfer and deposit mismatches
Transfer and deposit reconciliation should move from loading transactions plus per-transaction ledger entries to queries that group ledger entries by transaction and filter to mismatches in SQL.

Rationale:
- the invariants are naturally aggregate checks
- one set-based query per phase scales better than fetching ledger entries transaction by transaction
- it keeps the mismatch detection logic close to the database, where grouping and counting are cheapest

Alternatives considered:
- keeping the current repository methods and adding caching. Rejected because the query pattern would still be N+1 and would remain sensitive to large result sets.
- loading all candidate ledger entries into memory and grouping in Java. Rejected because it still moves more data than needed and makes the application layer do work the database can summarize directly.

### Keep wallet balance reconciliation on the existing aggregate summary query
The wallet balance phase already uses a ledger-derived grouped balance summary and should continue doing so.

Rationale:
- it already expresses the invariant as one grouped query
- the requirement explicitly keeps this approach
- changing that phase would add churn without addressing the current hotspot

Alternative considered:
- unifying all reconciliation phases into one large multi-purpose SQL statement. Rejected because wallet balance and transaction-shape checks are different enough that the resulting query would be harder to read, test, and tune.

### Limit mismatch detail rows per check type, not globally
The reconciliation response should cap details to 100 rows for each mismatch code, such as wallet balance mismatches or transfer amount mismatches, rather than stopping after 100 total failures.

Rationale:
- preserves representative diagnostics across multiple failure categories
- avoids one noisy mismatch type starving all others out of the response
- keeps the response bounded without changing pass/fail semantics

Alternative considered:
- cap the entire failed-check list at 100 rows. Rejected because one dominant failure mode could hide unrelated categories that operators also need to see.

### Add targeted indexes aligned with reconciliation access paths
The migration should add indexes for `ledger_entries(wallet_id)`, `ledger_entries(transaction_id)`, `transactions(status)`, and `transactions(transaction_type, status)`.

Rationale:
- wallet balance summaries and history queries benefit from the wallet lookup index
- transfer and deposit mismatch grouping depends on efficient transaction-to-ledger joins
- reconciliation filters successful transactions by status and type, so those columns need supporting indexes

Alternative considered:
- rely on existing primary keys and foreign keys only. Rejected because the reconciliation queries filter by status/type and group over foreign-key columns that are explicitly called out as optimization targets.

### Log phase durations in the reconciliation service
`ReconciliationService` should measure and log elapsed time for wallet, transfer, and deposit checks independently.

Rationale:
- operators need to know which phase is slow before reaching for deeper profiling
- timing by phase is more actionable than only logging the final reconciliation outcome
- this adds observability without changing metrics contracts

Alternative considered:
- log only the total reconciliation duration. Rejected because it would not identify which phase regressed.

## Risks / Trade-offs

- [Risk] Aggregate SQL can become harder to read than row-by-row Java logic. → Mitigation: keep separate repository projections or query methods per reconciliation phase and name them after the invariant they summarize.
- [Risk] Limiting mismatch detail rows means a severely corrupted dataset will not return every failure record. → Mitigation: preserve the overall `FAIL` status and make the cap explicit in the spec and implementation comments.
- [Risk] Additional indexes increase write cost slightly. → Mitigation: use only the four targeted indexes requested for known reconciliation access paths.
- [Risk] SQL summaries may miss edge cases if the grouping logic is not aligned with the existing invariants. → Mitigation: keep or extend integration tests that exercise wallet, transfer, and deposit mismatch scenarios against the new query path.

## Migration Plan

1. Add a Flyway migration that creates the reconciliation-supporting indexes.
2. Introduce aggregate repository queries or projections for transfer and deposit mismatch detection.
3. Refactor `ReconciliationService` to use the aggregate queries, retain wallet balance summary logic, cap mismatch details per check type, and log phase durations.
4. Update reconciliation integration tests to prove wallet, transfer, and deposit mismatches are still detected.
5. Deploy with no client migration because the endpoint and response contract remain compatible.

Rollback strategy:
- revert the reconciliation query refactor and remove the new indexes if the aggregate SQL behaves unexpectedly in production
- response compatibility remains intact, so rollback is operational rather than contract-driven

## Open Questions

- No open product questions. The remaining implementation choices are local code-structure details, such as projection naming and whether the timing helper lives inline or as a small private utility.
