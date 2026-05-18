## Why

AtomicLedger now performs meaningful money and integrity operations, but it has no durable audit trail that explains which domain actions happened and which entity they affected. Adding audit logs now gives us an internal history for successful operations, important retries, and key failures before the system grows harder to inspect.

## What Changes

- Add an `audit_logs` table through Flyway with `id`, `action`, `entityType`, `entityId`, `metadata`, and `createdAt`.
- Record audit logs for wallet creation, successful deposits, duplicate deposit requests, successful transfers, failed transfers due to insufficient balance, duplicate transfer requests, reconciliation runs, and reconciliation failures.
- Ensure audit logging happens in the same database transaction as state-mutating domain operations.
- Add `GET /api/v1/audit-logs` with filtering by `entityType` and `entityId`.
- Add integration coverage for wallet creation, deposit, transfer, duplicate idempotency requests, failed transfer, and reconciliation audit records.

## Capabilities

### New Capabilities
- `audit-logs`: persist and query structured audit records for important domain actions

### Modified Capabilities
- `wallet-management`: wallet creation now guarantees an audit record is persisted with the created wallet action
- `wallet-deposit`: successful deposits and duplicate deposit replays now guarantee audit records are persisted
- `wallet-transfer`: successful transfers, duplicate transfer replays, and insufficient-balance failures now guarantee audit records are persisted
- `reconciliation-checks`: reconciliation runs and reconciliation failures now guarantee audit records are persisted

## Impact

- Adds a new persisted audit log model, Flyway migration, repository, and query API.
- Threads audit logging through wallet, deposit, transfer, and reconciliation service flows.
- Expands the API surface with an audit-log listing endpoint and the integration suite with audit assertions.
