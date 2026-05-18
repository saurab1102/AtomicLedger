## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: Enforce transfer wallet balance and currency rules
The system SHALL reject transfers when the source wallet lacks sufficient `availableBalance` or when the requested currency does not match both wallets, and SHALL perform the source balance check after acquiring row-level locks for both wallets involved in the transfer.

#### Scenario: Source wallet has insufficient balance
- **WHEN** a client sends `POST /api/v1/transfers` with an amount greater than the source wallet `availableBalance`
- **THEN** the system rejects the request and does not create any transfer transaction, ledger entry, or balance update

#### Scenario: Wallet currency does not match transfer currency
- **WHEN** a client sends `POST /api/v1/transfers` with a currency that does not match one or both wallets
- **THEN** the system rejects the request and does not create any transfer transaction, ledger entry, or balance update

### Requirement: Apply both wallet balance updates atomically
The system SHALL decrease the source wallet `availableBalance` and increase the destination wallet `availableBalance` by the transfer amount in the same database transaction that persists the transfer transaction and both ledger entries, after acquiring row-level locks for both wallet rows.

#### Scenario: Transfer updates both wallets
- **WHEN** a transfer request succeeds
- **THEN** the source wallet balance decreases by exactly the transfer amount, the destination wallet balance increases by exactly the transfer amount, and the changes are committed atomically with the transfer records
