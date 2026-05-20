## ADDED Requirements

### Requirement: Create an outbox event for wallet creation
The system SHALL persist an outbox event when a wallet is created successfully.

#### Scenario: Wallet creation writes an outbox event
- **WHEN** a wallet creation request succeeds
- **THEN** the system stores a `PENDING` outbox event for the created wallet in the same transaction as the wallet record
