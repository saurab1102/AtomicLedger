## Purpose
Describe wallet creation rules and the preconditions for wallet-based operations.
## Requirements
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

### Requirement: Accept deposits only for existing active wallets
The system SHALL allow wallet deposits only when the `walletId` identifies an existing wallet whose status is `ACTIVE`.

#### Scenario: Wallet does not exist
- **WHEN** a client sends `POST /api/v1/wallets/{walletId}/deposit` for a wallet ID that is not stored
- **THEN** the system rejects the request with a wallet-not-found error

#### Scenario: Wallet is not active
- **WHEN** a client sends `POST /api/v1/wallets/{walletId}/deposit` for a wallet whose status is not `ACTIVE`
- **THEN** the system rejects the request and does not create any deposit transaction, ledger entry, or balance update

### Requirement: Allow transfers only between existing active wallets
The system SHALL allow transfers only when both `sourceWalletId` and `destinationWalletId` identify existing wallets whose status is `ACTIVE`.

#### Scenario: Source wallet does not exist
- **WHEN** a client sends `POST /api/v1/transfers` with a source wallet ID that is not stored
- **THEN** the system rejects the request with a wallet-not-found error

#### Scenario: Destination wallet does not exist
- **WHEN** a client sends `POST /api/v1/transfers` with a destination wallet ID that is not stored
- **THEN** the system rejects the request with a wallet-not-found error

#### Scenario: One wallet is not active
- **WHEN** a client sends `POST /api/v1/transfers` where either the source wallet or destination wallet is not `ACTIVE`
- **THEN** the system rejects the request and does not create any transfer transaction, ledger entry, or balance update

### Requirement: Audit wallet creation
The system SHALL record an audit log when a wallet is created successfully.

#### Scenario: Wallet creation is audited
- **WHEN** a wallet creation request succeeds
- **THEN** the system stores an audit log describing the wallet creation action for the created wallet

### Requirement: Create an outbox event for wallet creation
The system SHALL persist an outbox event when a wallet is created successfully.

#### Scenario: Wallet creation writes an outbox event
- **WHEN** a wallet creation request succeeds
- **THEN** the system stores a `PENDING` outbox event for the created wallet in the same transaction as the wallet record

### Requirement: Return paginated wallet transaction history
The system SHALL provide `GET /api/v1/wallets/{walletId}/transactions` to return paginated transaction history for an existing wallet, including deposit transactions for that wallet and transfer transactions where that wallet participated through ledger entries.

#### Scenario: Wallet transaction history returns deposit and transfer activity
- **WHEN** a client sends `GET /api/v1/wallets/{walletId}/transactions` for an existing wallet that has deposits and transfers
- **THEN** the system returns wallet-relative history items for the wallet's deposit, outgoing transfer, and incoming transfer activity

#### Scenario: Wallet is not found
- **WHEN** a client sends `GET /api/v1/wallets/{walletId}/transactions` for a wallet ID that is not stored
- **THEN** the system rejects the request with a wallet-not-found error

### Requirement: Paginate and sort wallet transaction history
The system SHALL support `page`, `size`, and `sort` query parameters for wallet transaction history, with defaults of `page=0`, `size=20`, and `sort=createdAt,desc`.

#### Scenario: Default pagination is applied
- **WHEN** a client sends `GET /api/v1/wallets/{walletId}/transactions` without pagination query parameters
- **THEN** the system returns the first page of at most 20 history items ordered by `createdAt` descending

#### Scenario: Requested page and size are applied
- **WHEN** a client sends `GET /api/v1/wallets/{walletId}/transactions` with explicit `page` and `size` query parameters
- **THEN** the system returns the requested slice of wallet history using the requested page size

### Requirement: Return wallet-relative history fields
Each wallet transaction history item SHALL include `transactionId`, `type`, `status`, `direction`, `amount`, `currency`, `counterpartyWalletId`, and `createdAt`, where `direction` is `CREDIT` for deposits and incoming transfers, `DEBIT` for outgoing transfers, transfer `counterpartyWalletId` is the other wallet, and deposit `counterpartyWalletId` is `null`.

#### Scenario: Deposit appears as credit history
- **WHEN** a deposit transaction is returned in wallet history
- **THEN** the history item uses `direction = CREDIT` and `counterpartyWalletId = null`

#### Scenario: Outgoing transfer appears as debit history
- **WHEN** a transfer is returned in history for the source wallet
- **THEN** the history item uses `direction = DEBIT` and `counterpartyWalletId` equal to the destination wallet ID

#### Scenario: Incoming transfer appears as credit history
- **WHEN** a transfer is returned in history for the destination wallet
- **THEN** the history item uses `direction = CREDIT` and `counterpartyWalletId` equal to the source wallet ID
