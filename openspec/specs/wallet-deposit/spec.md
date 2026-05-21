## Purpose
Describe how deposits are validated, recorded, and applied to wallet balances.
## Requirements
### Requirement: Deposit funds through the wallet API
The system SHALL provide `POST /api/v1/wallets/{walletId}/deposit` to deposit funds into a wallet using a JSON request body containing `amount` and `currency`, plus a required `Idempotency-Key` header.

#### Scenario: Deposit succeeds for an active INR wallet
- **WHEN** a client sends `POST /api/v1/wallets/{walletId}/deposit` with a positive `amount`, `currency` set to `INR`, and an `Idempotency-Key` header for an existing `ACTIVE` wallet
- **THEN** the system records a successful deposit and returns the created transaction result

#### Scenario: Idempotency key header is missing
- **WHEN** a client sends `POST /api/v1/wallets/{walletId}/deposit` without an `Idempotency-Key` header
- **THEN** the system rejects the request with a `400` validation error

### Requirement: Validate wallet deposit input
The system SHALL reject wallet deposit requests with non-positive amounts or currencies other than `INR`.

#### Scenario: Amount is zero or negative
- **WHEN** a client sends `POST /api/v1/wallets/{walletId}/deposit` with `amount` less than or equal to `0`
- **THEN** the system rejects the request with a validation error indicating that `amount` must be positive

#### Scenario: Currency is unsupported
- **WHEN** a client sends `POST /api/v1/wallets/{walletId}/deposit` with `currency` set to a value other than `INR`
- **THEN** the system rejects the request with a validation error indicating that the currency is unsupported

### Requirement: Persist successful deposits as transaction and ledger records
The system SHALL persist each successful wallet deposit as exactly one `DEPOSIT` transaction with status `SUCCEEDED` and exactly one `CREDIT` ledger entry for the wallet.

#### Scenario: Accounting records are created
- **WHEN** a deposit request succeeds
- **THEN** the system stores one transaction record of type `DEPOSIT` with status `SUCCEEDED` and one `CREDIT` ledger entry associated with the wallet

### Requirement: Apply deposit balance updates atomically
The system SHALL increase the wallet `availableBalance` by the deposit amount in the same database transaction that persists the successful deposit transaction and ledger entry.

#### Scenario: Deposit updates wallet balance
- **WHEN** a deposit request succeeds
- **THEN** the wallet `availableBalance` increases by exactly the deposited amount and the change is committed atomically with the deposit records

### Requirement: Replay the original result for duplicate idempotency keys
The system SHALL not create another transaction, ledger entry, or balance update when a client reuses the same `Idempotency-Key` for the same deposit request, and SHALL return the original transaction result instead.

#### Scenario: Duplicate idempotency key is retried
- **WHEN** a client repeats a previously successful deposit request using the same `Idempotency-Key`
- **THEN** the system returns the original deposit result and does not create additional transaction records, ledger entries, or balance changes

### Requirement: Audit successful and duplicate deposit handling
The system SHALL record an audit log for every successful deposit and for every duplicate deposit request replayed through idempotency handling.

#### Scenario: Successful deposit is audited
- **WHEN** a deposit request succeeds
- **THEN** the system stores an audit log describing the successful deposit action

#### Scenario: Duplicate deposit replay is audited
- **WHEN** a client repeats a previously successful deposit request using the same `Idempotency-Key`
- **THEN** the system stores an audit log describing the duplicate deposit replay

### Requirement: Create an outbox event for successful deposits
The system SHALL persist an outbox event when a deposit request succeeds.

#### Scenario: Successful deposit writes an outbox event
- **WHEN** a deposit request succeeds
- **THEN** the system stores a `PENDING` outbox event for the committed deposit in the same transaction as the deposit records

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

### Requirement: Document the deposit API in OpenAPI
The system SHALL document `POST /api/v1/wallets/{walletId}/deposit` in OpenAPI, including its request body, success response, standardized error responses, and the required `Idempotency-Key` header.

#### Scenario: Deposit header requirement is documented
- **WHEN** a developer inspects the deposit operation in the OpenAPI docs
- **THEN** the documentation marks `Idempotency-Key` as a required header and describes the deposit request and response bodies
