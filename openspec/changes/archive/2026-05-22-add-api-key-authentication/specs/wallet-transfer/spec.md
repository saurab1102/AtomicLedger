## ADDED Requirements

### Requirement: Require API key authentication for transfers
The system SHALL require a valid `X-API-Key` header for `POST /api/v1/transfers`.

#### Scenario: Transfer request requires API key
- **WHEN** a client sends `POST /api/v1/transfers` without a valid `X-API-Key`
- **THEN** the system rejects the request with `401 Unauthorized` before applying transfer validation or domain processing
