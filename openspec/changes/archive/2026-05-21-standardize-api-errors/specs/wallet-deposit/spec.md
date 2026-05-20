## ADDED Requirements

### Requirement: Return standardized deposit API errors
The system SHALL return wallet-deposit API failures using the shared API error response contract with `errorCode`, `message`, `details`, and `timestamp`.

#### Scenario: Missing idempotency key uses a stable error code
- **WHEN** a client sends `POST /api/v1/wallets/{walletId}/deposit` without an `Idempotency-Key` header
- **THEN** the system rejects the request with `400` and `errorCode = MISSING_IDEMPOTENCY_KEY`

#### Scenario: Deposit wallet lookup uses a stable error code
- **WHEN** a client sends `POST /api/v1/wallets/{walletId}/deposit` for a wallet ID that is not stored
- **THEN** the system rejects the request with `404` and `errorCode = WALLET_NOT_FOUND`

#### Scenario: Unsupported deposit currency uses a stable error code
- **WHEN** a client sends `POST /api/v1/wallets/{walletId}/deposit` with `currency` set to a value other than `INR`
- **THEN** the system rejects the request with `400` and `errorCode = UNSUPPORTED_CURRENCY`

### Requirement: Preserve duplicate idempotency success replay
The system SHALL continue to return the original successful deposit response when a client reuses the same `Idempotency-Key` for the same successful deposit request.

#### Scenario: Duplicate deposit idempotency replay is not converted into an error
- **WHEN** a client repeats a previously successful deposit request using the same `Idempotency-Key`
- **THEN** the system returns the original successful deposit response instead of an API error
