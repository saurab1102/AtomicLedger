## ADDED Requirements

### Requirement: Require API key authentication for outbox-event inspection
The system SHALL require a valid `X-API-Key` header for `GET /api/v1/outbox-events`.

#### Scenario: Outbox-event inspection requires API key
- **WHEN** a client sends `GET /api/v1/outbox-events` without a valid `X-API-Key`
- **THEN** the system rejects the request with `401 Unauthorized` before running the outbox-event query
