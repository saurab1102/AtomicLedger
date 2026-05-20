## ADDED Requirements

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
