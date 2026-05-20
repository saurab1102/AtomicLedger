## ADDED Requirements

### Requirement: Create an outbox event for successful deposits
The system SHALL persist an outbox event when a deposit request succeeds.

#### Scenario: Successful deposit writes an outbox event
- **WHEN** a deposit request succeeds
- **THEN** the system stores a `PENDING` outbox event for the committed deposit in the same transaction as the deposit records
