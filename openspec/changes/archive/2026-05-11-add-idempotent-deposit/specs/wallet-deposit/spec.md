## ADDED Requirements

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
