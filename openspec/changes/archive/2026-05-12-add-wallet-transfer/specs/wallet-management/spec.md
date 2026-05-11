## ADDED Requirements

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
