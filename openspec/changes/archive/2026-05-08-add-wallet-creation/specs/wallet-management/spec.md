## ADDED Requirements

### Requirement: Create wallet through API
The system SHALL provide `POST /api/v1/wallets` to create a wallet from a JSON request containing `ownerReference` and `currency`.

#### Scenario: Wallet is created successfully
- **WHEN** a client sends `POST /api/v1/wallets` with a non-blank `ownerReference` and `currency` set to `INR`
- **THEN** the system creates a wallet with a UUID identifier and returns the created wallet representation

### Requirement: Enforce wallet creation input validation
The system SHALL reject wallet creation requests that omit required fields, provide a blank `ownerReference`, or specify a currency other than `INR`.

#### Scenario: Owner reference is missing or blank
- **WHEN** a client sends `POST /api/v1/wallets` without `ownerReference` or with `ownerReference` as blank text
- **THEN** the system rejects the request with a validation error indicating that `ownerReference` is required

#### Scenario: Currency is unsupported
- **WHEN** a client sends `POST /api/v1/wallets` with `currency` set to a value other than `INR`
- **THEN** the system rejects the request with a validation error indicating that the currency is unsupported

### Requirement: Initialize wallet state on creation
The system SHALL initialize every newly created wallet with `availableBalance = 0` and `status = ACTIVE`.

#### Scenario: Initial wallet state is applied
- **WHEN** a wallet is created successfully
- **THEN** the persisted wallet and the API response both show `availableBalance` equal to `0` and `status` equal to `ACTIVE`

### Requirement: Persist created wallets in PostgreSQL
The system SHALL persist created wallets in PostgreSQL using a Flyway-managed schema.

#### Scenario: Created wallet is durable
- **WHEN** a wallet creation request succeeds
- **THEN** the wallet record is stored in PostgreSQL with its UUID, owner reference, currency, available balance, and status
