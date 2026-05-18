## ADDED Requirements

### Requirement: Audit wallet creation
The system SHALL record an audit log when a wallet is created successfully.

#### Scenario: Wallet creation is audited
- **WHEN** a wallet creation request succeeds
- **THEN** the system stores an audit log describing the wallet creation action for the created wallet
