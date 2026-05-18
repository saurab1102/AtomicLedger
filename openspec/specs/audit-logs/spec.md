# audit-logs Specification

## Purpose
Describe how audit records are persisted and queried for important domain actions.
## Requirements
### Requirement: Persist structured audit logs
The system SHALL persist audit records in PostgreSQL for important domain actions using an `audit_logs` table containing `id`, `action`, `entityType`, `entityId`, `metadata`, and `createdAt`.

#### Scenario: Audit record is stored for a domain action
- **WHEN** the system records an auditable domain action
- **THEN** it stores an audit log row with the required audit fields

### Requirement: Query audit logs through the API
The system SHALL provide `GET /api/v1/audit-logs` and SHALL support filtering audit records by `entityType` and `entityId`.

#### Scenario: Audit logs are filtered by entity
- **WHEN** a client sends `GET /api/v1/audit-logs` with `entityType` and `entityId` filters
- **THEN** the system returns only matching audit records

### Requirement: Record audit logs for required wallet, transfer, deposit, and reconciliation actions
The system SHALL record audit logs for wallet creation, successful deposits, duplicate deposit requests, successful transfers, failed transfers due to insufficient balance, duplicate transfer requests, reconciliation runs, and reconciliation failures.

#### Scenario: Duplicate idempotency replay is audited
- **WHEN** a client repeats a previously processed deposit or transfer request with the same idempotency key
- **THEN** the system records an audit log describing the duplicate replay action

#### Scenario: Reconciliation failure is audited
- **WHEN** a reconciliation run detects one or more failed checks
- **THEN** the system records an audit log describing the reconciliation failure

### Requirement: Commit audit logs atomically with mutating domain operations
The system SHALL persist audit logs in the same database transaction as the corresponding domain operation whenever that operation mutates state.

#### Scenario: Successful transfer and audit log commit together
- **WHEN** a transfer request succeeds
- **THEN** the transfer state changes and its audit log are committed atomically
