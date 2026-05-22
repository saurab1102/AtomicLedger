## Purpose
Describe how wallet-to-wallet transfers are validated, recorded, and applied safely.
## Requirements
### Requirement: Transfer funds through the transfer API
The system SHALL provide `POST /api/v1/transfers` to move funds from one wallet to another using a JSON request body containing `sourceWalletId`, `destinationWalletId`, `amount`, and `currency`, plus a required `Idempotency-Key` header.

#### Scenario: Transfer succeeds between two active INR wallets
- **WHEN** a client sends `POST /api/v1/transfers` with distinct source and destination wallet IDs, a positive `amount`, `currency` set to `INR`, and an `Idempotency-Key` for existing `ACTIVE` wallets
- **THEN** the system records a successful transfer and returns the created transfer transaction result

#### Scenario: Idempotency key header is missing
- **WHEN** a client sends `POST /api/v1/transfers` without an `Idempotency-Key` header
- **THEN** the system rejects the request with a `400` validation error

### Requirement: Validate transfer request input
The system SHALL reject transfer requests with non-positive amounts, unsupported currencies, or the same source and destination wallet.

#### Scenario: Amount is zero or negative
- **WHEN** a client sends `POST /api/v1/transfers` with `amount` less than or equal to `0`
- **THEN** the system rejects the request with a validation error indicating that `amount` must be positive

#### Scenario: Currency is unsupported
- **WHEN** a client sends `POST /api/v1/transfers` with `currency` set to a value other than `INR`
- **THEN** the system rejects the request with a validation error indicating that the currency is unsupported

#### Scenario: Source and destination wallets are the same
- **WHEN** a client sends `POST /api/v1/transfers` with the same wallet ID for `sourceWalletId` and `destinationWalletId`
- **THEN** the system rejects the request and does not create any transaction, ledger entry, or balance update

### Requirement: Enforce transfer wallet balance and currency rules
The system SHALL reject transfers when the source wallet lacks sufficient `availableBalance` or when the requested currency does not match both wallets, and SHALL perform the source balance check after acquiring row-level locks for both wallets involved in the transfer.

#### Scenario: Source wallet has insufficient balance
- **WHEN** a client sends `POST /api/v1/transfers` with an amount greater than the source wallet `availableBalance`
- **THEN** the system rejects the request and does not create any transfer transaction, ledger entry, or balance update

#### Scenario: Wallet currency does not match transfer currency
- **WHEN** a client sends `POST /api/v1/transfers` with a currency that does not match one or both wallets
- **THEN** the system rejects the request and does not create any transfer transaction, ledger entry, or balance update

### Requirement: Persist successful transfers as one transaction and two ledger entries
The system SHALL persist each successful transfer as exactly one `TRANSFER` transaction with status `SUCCEEDED`, one `DEBIT` ledger entry for the source wallet, and one `CREDIT` ledger entry for the destination wallet.

#### Scenario: Accounting records are created for a transfer
- **WHEN** a transfer request succeeds
- **THEN** the system stores one `TRANSFER` transaction record, one `DEBIT` ledger entry for the source wallet, and one `CREDIT` ledger entry for the destination wallet

### Requirement: Apply both wallet balance updates atomically
The system SHALL decrease the source wallet `availableBalance` and increase the destination wallet `availableBalance` by the transfer amount in the same database transaction that persists the transfer transaction and both ledger entries, after acquiring row-level locks for both wallet rows.

#### Scenario: Transfer updates both wallets
- **WHEN** a transfer request succeeds
- **THEN** the source wallet balance decreases by exactly the transfer amount, the destination wallet balance increases by exactly the transfer amount, and the changes are committed atomically with the transfer records

### Requirement: Prevent overspending during concurrent transfers
The system SHALL make wallet-to-wallet transfers safe under concurrent requests by locking the involved wallet rows in Postgres before checking the source balance and mutating either wallet balance.

#### Scenario: Only one conflicting transfer can succeed
- **WHEN** two transfer requests run concurrently against the same source wallet with amounts whose combined total exceeds the source wallet `availableBalance`
- **THEN** the system allows at most one transfer to succeed and rejects the other without creating extra transfer records, ledger entries, or balance updates

### Requirement: Lock transfer wallets in deterministic order
The system SHALL lock both the source wallet row and destination wallet row inside the same database transaction, and SHALL acquire those locks in deterministic wallet ID order to reduce deadlock risk.

#### Scenario: Opposing concurrent transfers use the same lock order
- **WHEN** concurrent transfer requests involve the same pair of wallets
- **THEN** the system acquires row-level locks for both wallets in the same deterministic order before evaluating balances or mutating wallet state

### Requirement: Replay the original result for duplicate transfer idempotency keys
The system SHALL not create another transaction, ledger entry, or balance update when a client reuses the same `Idempotency-Key` for the same transfer request, and SHALL return the original transfer result instead.

#### Scenario: Duplicate idempotency key is retried for a transfer
- **WHEN** a client repeats a previously successful transfer request using the same `Idempotency-Key`
- **THEN** the system returns the original transfer result and does not create additional transaction records, ledger entries, or balance changes

### Requirement: Audit successful, failed, and duplicate transfer handling
The system SHALL record an audit log for every successful transfer, every transfer rejected due to insufficient balance, and every duplicate transfer request replayed through idempotency handling.

#### Scenario: Successful transfer is audited
- **WHEN** a transfer request succeeds
- **THEN** the system stores an audit log describing the successful transfer action

#### Scenario: Insufficient balance transfer failure is audited
- **WHEN** a transfer request is rejected because the source wallet lacks sufficient available balance
- **THEN** the system stores an audit log describing the failed transfer action

#### Scenario: Duplicate transfer replay is audited
- **WHEN** a client repeats a previously successful transfer request using the same `Idempotency-Key`
- **THEN** the system stores an audit log describing the duplicate transfer replay

### Requirement: Create outbox events for successful and failed transfers
The system SHALL persist an outbox event for every successful transfer and for every transfer rejected due to insufficient balance.

#### Scenario: Successful transfer writes an outbox event
- **WHEN** a transfer request succeeds
- **THEN** the system stores a `PENDING` outbox event for the committed transfer in the same transaction as the transfer records

#### Scenario: Insufficient balance transfer failure writes an outbox event
- **WHEN** a transfer request is rejected because the source wallet lacks sufficient available balance
- **THEN** the system stores a `PENDING` outbox event describing the failed transfer in the same transaction as the failure handling

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

### Requirement: Document the transfer API in OpenAPI
The system SHALL document `POST /api/v1/transfers` in OpenAPI, including its request body, success response, standardized error responses, the required `Idempotency-Key` header, and an insufficient-balance error example.

#### Scenario: Transfer header and error example are documented
- **WHEN** a developer inspects the transfer operation in the OpenAPI docs
- **THEN** the documentation marks `Idempotency-Key` as a required header and includes an example of the insufficient-balance error response

### Requirement: Require API key authentication for transfers
The system SHALL require a valid `X-API-Key` header for `POST /api/v1/transfers`.

#### Scenario: Transfer request requires API key
- **WHEN** a client sends `POST /api/v1/transfers` without a valid `X-API-Key`
- **THEN** the system rejects the request with `401 Unauthorized` before applying transfer validation or domain processing
