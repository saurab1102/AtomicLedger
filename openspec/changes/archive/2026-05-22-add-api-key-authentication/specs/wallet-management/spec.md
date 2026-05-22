## ADDED Requirements

### Requirement: Require API key authentication for wallet-management APIs
The system SHALL require a valid `X-API-Key` header for wallet-management API operations, including wallet creation and wallet transaction history.

#### Scenario: Wallet creation requires API key
- **WHEN** a client sends `POST /api/v1/wallets` without a valid `X-API-Key`
- **THEN** the system rejects the request with `401 Unauthorized` before attempting wallet creation

#### Scenario: Wallet history requires API key
- **WHEN** a client sends `GET /api/v1/wallets/{walletId}/transactions` without a valid `X-API-Key`
- **THEN** the system rejects the request with `401 Unauthorized` before attempting the wallet-history lookup
