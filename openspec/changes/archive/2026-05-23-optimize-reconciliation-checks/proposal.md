## Why

AtomicLedger reconciliation currently does the right accounting checks, but the transfer and deposit phases degrade into N+1 query patterns as data grows. Optimizing those checks now keeps the existing reconciliation API intact while making the service safer to run against larger ledgers.

## What Changes

- Replace per-transaction ledger entry lookups in transfer and deposit reconciliation with aggregate SQL queries that summarize mismatches in bulk.
- Keep wallet balance reconciliation on the existing aggregate ledger balance summary approach.
- Add supporting database indexes for reconciliation-heavy access paths on ledger entries and wallet transactions.
- Add timing logs around the wallet, transfer, and deposit reconciliation phases so slow sections are visible in production logs.
- Limit reconciliation mismatch details to the first 100 rows per check type while preserving overall pass/fail behavior.
- Update automated verification so wallet, transfer, and deposit mismatches are still detected after the query changes.

## Capabilities

### New Capabilities

### Modified Capabilities
- `reconciliation-checks`: Tighten reconciliation performance and observability requirements while keeping the existing run API and mismatch detection behavior compatible.

## Impact

- Affected code: reconciliation service, reconciliation-related repositories/queries, Flyway migrations, integration tests
- Dependencies/systems: PostgreSQL query plans and indexes, application logging output
- APIs: no endpoint contract changes; reconciliation responses remain compatible apart from capped mismatch detail lists
