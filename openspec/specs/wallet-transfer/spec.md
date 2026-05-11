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
The system SHALL reject transfers when the source wallet lacks sufficient `availableBalance` or when the requested currency does not match both wallets.

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
The system SHALL decrease the source wallet `availableBalance` and increase the destination wallet `availableBalance` by the transfer amount in the same database transaction that persists the transfer transaction and both ledger entries.

#### Scenario: Transfer updates both wallets
- **WHEN** a transfer request succeeds
- **THEN** the source wallet balance decreases by exactly the transfer amount, the destination wallet balance increases by exactly the transfer amount, and the changes are committed atomically with the transfer records

### Requirement: Replay the original result for duplicate transfer idempotency keys
The system SHALL not create another transaction, ledger entry, or balance update when a client reuses the same `Idempotency-Key` for the same transfer request, and SHALL return the original transfer result instead.

#### Scenario: Duplicate idempotency key is retried for a transfer
- **WHEN** a client repeats a previously successful transfer request using the same `Idempotency-Key`
- **THEN** the system returns the original transfer result and does not create additional transaction records, ledger entries, or balance changes
