## ADDED Requirements

### Requirement: Require API key authentication for audit-log inspection
The system SHALL require a valid `X-API-Key` header for `GET /api/v1/audit-logs`.

#### Scenario: Audit-log inspection requires API key
- **WHEN** a client sends `GET /api/v1/audit-logs` without a valid `X-API-Key`
- **THEN** the system rejects the request with `401 Unauthorized` before running the audit-log query
