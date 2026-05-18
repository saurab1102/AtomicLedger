## ADDED Requirements

### Requirement: Audit successful and duplicate deposit handling
The system SHALL record an audit log for every successful deposit and for every duplicate deposit request replayed through idempotency handling.

#### Scenario: Successful deposit is audited
- **WHEN** a deposit request succeeds
- **THEN** the system stores an audit log describing the successful deposit action

#### Scenario: Duplicate deposit replay is audited
- **WHEN** a client repeats a previously successful deposit request using the same `Idempotency-Key`
- **THEN** the system stores an audit log describing the duplicate deposit replay
