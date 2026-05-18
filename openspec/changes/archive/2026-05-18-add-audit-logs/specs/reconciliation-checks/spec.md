## ADDED Requirements

### Requirement: Audit reconciliation runs and failures
The system SHALL record an audit log for every reconciliation run and SHALL record a reconciliation failure audit log when a run returns failed checks.

#### Scenario: Reconciliation run is audited
- **WHEN** a client sends `POST /api/v1/reconciliation/run`
- **THEN** the system stores an audit log describing the reconciliation run

#### Scenario: Reconciliation failure is audited
- **WHEN** a reconciliation run returns status `FAIL`
- **THEN** the system stores an audit log describing the reconciliation failure
