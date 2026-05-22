## Purpose
Describe how reconciliation is run and which accounting invariants it checks.
## Requirements
### Requirement: Run reconciliation through the API
The system SHALL provide `POST /api/v1/reconciliation/run` to execute reconciliation checks over wallet balances, successful deposit transactions, successful transfer transactions, and their ledger entries.

#### Scenario: Healthy accounting data passes reconciliation
- **WHEN** a client sends `POST /api/v1/reconciliation/run` and all wallet balances and successful transaction ledger invariants are valid
- **THEN** the system returns a structured reconciliation result with status `PASS` and no failed checks

### Requirement: Reconcile wallet balances against ledger-derived balances
The system SHALL compare each wallet's cached `availableBalance` against a ledger-derived balance computed as total `CREDIT` ledger-entry amount minus total `DEBIT` ledger-entry amount for that wallet, and SHALL detect mismatches.

#### Scenario: Wallet balance mismatch is detected
- **WHEN** a wallet's cached `availableBalance` differs from its ledger-derived balance
- **THEN** the reconciliation result status is `FAIL` and includes failed-check details identifying the wallet balance mismatch

### Requirement: Reconcile successful transfer accounting shape
The system SHALL verify that every successful `TRANSFER` transaction has exactly one `DEBIT` ledger entry, exactly one `CREDIT` ledger entry, and equal total debit and credit amounts.

#### Scenario: Transfer is missing a ledger entry
- **WHEN** a successful `TRANSFER` transaction does not have exactly one `DEBIT` ledger entry and exactly one `CREDIT` ledger entry
- **THEN** the reconciliation result status is `FAIL` and includes failed-check details identifying the broken transfer ledger structure

#### Scenario: Transfer ledger amounts are unbalanced
- **WHEN** a successful `TRANSFER` transaction has total `DEBIT` amount different from total `CREDIT` amount
- **THEN** the reconciliation result status is `FAIL` and includes failed-check details identifying the unbalanced transfer amounts

### Requirement: Reconcile successful deposit accounting shape
The system SHALL verify that every successful `DEPOSIT` transaction has exactly one `CREDIT` ledger entry.

#### Scenario: Deposit is missing its credit ledger entry
- **WHEN** a successful `DEPOSIT` transaction does not have exactly one `CREDIT` ledger entry
- **THEN** the reconciliation result status is `FAIL` and includes failed-check details identifying the broken deposit ledger structure

### Requirement: Return structured failed-check details
The system SHALL return reconciliation results with overall status `PASS` or `FAIL` and SHALL include failed-check details for every detected mismatch or broken accounting invariant.

#### Scenario: Multiple reconciliation failures are reported
- **WHEN** reconciliation detects one or more wallet or transaction integrity failures
- **THEN** the response contains status `FAIL` and structured failed-check detail records for each detected failure

### Requirement: Audit reconciliation runs and failures
The system SHALL record an audit log for every reconciliation run and SHALL record a reconciliation failure audit log when a run returns failed checks.

#### Scenario: Reconciliation run is audited
- **WHEN** a client sends `POST /api/v1/reconciliation/run`
- **THEN** the system stores an audit log describing the reconciliation run

#### Scenario: Reconciliation failure is audited
- **WHEN** a reconciliation run returns status `FAIL`
- **THEN** the system stores an audit log describing the reconciliation failure

### Requirement: Create an outbox event for failed reconciliation runs
The system SHALL persist an outbox event when a reconciliation run returns status `FAIL`.

#### Scenario: Failed reconciliation writes an outbox event
- **WHEN** a reconciliation run returns status `FAIL`
- **THEN** the system stores a `PENDING` outbox event for the failed reconciliation in the same transaction as the reconciliation result

### Requirement: Document reconciliation execution in OpenAPI
The system SHALL document the reconciliation API in OpenAPI, including the operation for triggering a run and the reconciliation response body.

#### Scenario: Reconciliation endpoint is documented
- **WHEN** a developer inspects `POST /api/v1/reconciliation/run` in the OpenAPI docs
- **THEN** the documentation describes the reconciliation response contract returned by the endpoint

### Requirement: Require API key authentication for reconciliation execution
The system SHALL require a valid `X-API-Key` header for `POST /api/v1/reconciliation/run`.

#### Scenario: Reconciliation run requires API key
- **WHEN** a client sends `POST /api/v1/reconciliation/run` without a valid `X-API-Key`
- **THEN** the system rejects the request with `401 Unauthorized` before starting reconciliation
