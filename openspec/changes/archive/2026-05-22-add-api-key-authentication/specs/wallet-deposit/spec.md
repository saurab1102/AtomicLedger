## ADDED Requirements

### Requirement: Require API key authentication for deposits
The system SHALL require a valid `X-API-Key` header for `POST /api/v1/wallets/{walletId}/deposit`.

#### Scenario: Deposit request requires API key
- **WHEN** a client sends `POST /api/v1/wallets/{walletId}/deposit` without a valid `X-API-Key`
- **THEN** the system rejects the request with `401 Unauthorized` before applying deposit validation or domain processing
