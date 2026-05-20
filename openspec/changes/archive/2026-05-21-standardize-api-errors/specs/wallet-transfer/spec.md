## ADDED Requirements

### Requirement: Return standardized transfer API errors
The system SHALL return wallet-transfer API failures using the shared API error response contract with `errorCode`, `message`, `details`, and `timestamp`.

#### Scenario: Missing idempotency key uses a stable error code
- **WHEN** a client sends `POST /api/v1/transfers` without an `Idempotency-Key` header
- **THEN** the system rejects the request with `400` and `errorCode = MISSING_IDEMPOTENCY_KEY`

#### Scenario: Unsupported transfer currency uses a stable error code
- **WHEN** a client sends `POST /api/v1/transfers` with `currency` set to a value other than `INR`
- **THEN** the system rejects the request with `400` and `errorCode = UNSUPPORTED_CURRENCY`

#### Scenario: Same-wallet transfer target uses a stable error code
- **WHEN** a client sends `POST /api/v1/transfers` with the same wallet ID for `sourceWalletId` and `destinationWalletId`
- **THEN** the system rejects the request with `400` and `errorCode = INVALID_TRANSFER_TARGET`

#### Scenario: Missing transfer wallet uses a stable error code
- **WHEN** a client sends `POST /api/v1/transfers` using a wallet ID that is not stored for either side of the transfer
- **THEN** the system rejects the request with `404` and `errorCode = WALLET_NOT_FOUND`

#### Scenario: Insufficient balance uses a stable error code and conflict status
- **WHEN** a client sends `POST /api/v1/transfers` with an amount greater than the source wallet `availableBalance`
- **THEN** the system rejects the request with `409` and `errorCode = INSUFFICIENT_BALANCE`

### Requirement: Preserve duplicate transfer idempotency success replay
The system SHALL continue to return the original successful transfer response when a client reuses the same `Idempotency-Key` for the same successful transfer request.

#### Scenario: Duplicate transfer idempotency replay is not converted into an error
- **WHEN** a client repeats a previously successful transfer request using the same `Idempotency-Key`
- **THEN** the system returns the original successful transfer response instead of an API error
