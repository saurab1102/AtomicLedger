## ADDED Requirements

### Requirement: Require API key authentication for reconciliation execution
The system SHALL require a valid `X-API-Key` header for `POST /api/v1/reconciliation/run`.

#### Scenario: Reconciliation run requires API key
- **WHEN** a client sends `POST /api/v1/reconciliation/run` without a valid `X-API-Key`
- **THEN** the system rejects the request with `401 Unauthorized` before starting reconciliation
