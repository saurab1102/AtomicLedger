## ADDED Requirements

### Requirement: Create outbox events for successful and failed transfers
The system SHALL persist an outbox event for every successful transfer and for every transfer rejected due to insufficient balance.

#### Scenario: Successful transfer writes an outbox event
- **WHEN** a transfer request succeeds
- **THEN** the system stores a `PENDING` outbox event for the committed transfer in the same transaction as the transfer records

#### Scenario: Insufficient balance transfer failure writes an outbox event
- **WHEN** a transfer request is rejected because the source wallet lacks sufficient available balance
- **THEN** the system stores a `PENDING` outbox event describing the failed transfer in the same transaction as the failure handling
