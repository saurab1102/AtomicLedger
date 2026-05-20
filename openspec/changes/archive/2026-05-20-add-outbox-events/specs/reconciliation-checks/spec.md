## ADDED Requirements

### Requirement: Create an outbox event for failed reconciliation runs
The system SHALL persist an outbox event when a reconciliation run returns status `FAIL`.

#### Scenario: Failed reconciliation writes an outbox event
- **WHEN** a reconciliation run returns status `FAIL`
- **THEN** the system stores a `PENDING` outbox event for the failed reconciliation in the same transaction as the reconciliation result
