## ADDED Requirements

### Requirement: Accept deposits only for existing active wallets
The system SHALL allow wallet deposits only when the `walletId` identifies an existing wallet whose status is `ACTIVE`.

#### Scenario: Wallet does not exist
- **WHEN** a client sends `POST /api/v1/wallets/{walletId}/deposit` for a wallet ID that is not stored
- **THEN** the system rejects the request with a wallet-not-found error

#### Scenario: Wallet is not active
- **WHEN** a client sends `POST /api/v1/wallets/{walletId}/deposit` for a wallet whose status is not `ACTIVE`
- **THEN** the system rejects the request and does not create any deposit transaction, ledger entry, or balance update
