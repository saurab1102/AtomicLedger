## ADDED Requirements

### Requirement: Return standardized wallet-management API errors
The system SHALL return wallet-management API failures using the shared API error response contract with `errorCode`, `message`, `details`, and `timestamp`.

#### Scenario: Wallet creation validation uses the shared error contract
- **WHEN** a client sends `POST /api/v1/wallets` with invalid wallet-creation input
- **THEN** the system rejects the request using the shared API error response contract and includes field-level detail entries for the invalid request fields

#### Scenario: Unsupported wallet currency uses a stable error code
- **WHEN** a client sends `POST /api/v1/wallets` with `currency` set to a value other than `INR`
- **THEN** the system rejects the request with `400` and `errorCode = UNSUPPORTED_CURRENCY`

#### Scenario: Wallet history lookup for a missing wallet uses a stable error code
- **WHEN** a client sends `GET /api/v1/wallets/{walletId}/transactions` for a wallet ID that is not stored
- **THEN** the system rejects the request with `404` and `errorCode = WALLET_NOT_FOUND`
