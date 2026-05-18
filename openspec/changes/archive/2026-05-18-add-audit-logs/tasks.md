## 1. Audit Persistence

- [x] 1.1 Add the Flyway migration and persistence model for `audit_logs` with the required fields.
- [x] 1.2 Add repository access patterns for writing audit logs and filtering them by `entityType` and `entityId`.

## 2. Audit Logging Integration

- [x] 2.1 Add a shared audit logging component/service to create structured audit records.
- [x] 2.2 Record audit logs for wallet creation and successful deposits inside the same transaction as the state mutation.
- [x] 2.3 Record audit logs for duplicate deposit replays, successful transfers, insufficient-balance transfer failures, and duplicate transfer replays.
- [x] 2.4 Record audit logs for reconciliation runs and reconciliation failures.

## 3. Audit Log API

- [x] 3.1 Add `GET /api/v1/audit-logs` and return audit log records.
- [x] 3.2 Support optional filtering by `entityType` and `entityId`.

## 4. Integration Testing

- [x] 4.1 Add integration tests verifying audit logs are created for wallet creation, deposit, and transfer.
- [x] 4.2 Add integration tests verifying audit logs are created for duplicate idempotency requests and failed transfer due to insufficient balance.
- [x] 4.3 Add an integration test verifying audit logs are created for reconciliation runs and reconciliation failures.
