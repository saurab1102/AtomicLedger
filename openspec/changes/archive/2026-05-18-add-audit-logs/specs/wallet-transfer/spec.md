## ADDED Requirements

### Requirement: Audit successful, failed, and duplicate transfer handling
The system SHALL record an audit log for every successful transfer, every transfer rejected due to insufficient balance, and every duplicate transfer request replayed through idempotency handling.

#### Scenario: Successful transfer is audited
- **WHEN** a transfer request succeeds
- **THEN** the system stores an audit log describing the successful transfer action

#### Scenario: Insufficient balance transfer failure is audited
- **WHEN** a transfer request is rejected because the source wallet lacks sufficient available balance
- **THEN** the system stores an audit log describing the failed transfer action

#### Scenario: Duplicate transfer replay is audited
- **WHEN** a client repeats a previously successful transfer request using the same `Idempotency-Key`
- **THEN** the system stores an audit log describing the duplicate transfer replay
