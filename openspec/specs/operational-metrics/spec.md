## Purpose
Describe how AtomicLedger emits operational metrics and structured logs for its core business and background-worker flows.
## Requirements
### Requirement: Record operational counters for core business outcomes
The system SHALL register and increment Micrometer counters for wallet creation, deposit success, deposit duplicate replay, transfer success, transfer failure, transfer duplicate replay, reconciliation runs, reconciliation failures, outbox publish success, and outbox publish failure outcomes.

#### Scenario: Wallet creation increments metric
- **WHEN** a wallet is created successfully
- **THEN** the system increments `wallets_created_total`

#### Scenario: Deposit success increments metric
- **WHEN** a deposit completes successfully
- **THEN** the system increments `deposits_succeeded_total`

#### Scenario: Deposit replay increments metric
- **WHEN** a duplicate deposit request replays a previously committed result
- **THEN** the system increments `deposit_duplicate_replays_total`

#### Scenario: Transfer success increments metric
- **WHEN** a transfer completes successfully
- **THEN** the system increments `transfers_succeeded_total`

#### Scenario: Transfer failure increments metric
- **WHEN** a transfer fails due to a business failure outcome handled by the application
- **THEN** the system increments `transfers_failed_total`

#### Scenario: Transfer replay increments metric
- **WHEN** a duplicate transfer request replays a previously committed result
- **THEN** the system increments `transfer_duplicate_replays_total`

#### Scenario: Reconciliation run increments metric
- **WHEN** reconciliation executes
- **THEN** the system increments `reconciliation_runs_total`

#### Scenario: Reconciliation failure increments metric
- **WHEN** reconciliation completes with a `FAIL` status
- **THEN** the system increments `reconciliation_failures_total`

#### Scenario: Outbox publish success increments metric
- **WHEN** an outbox event is published successfully
- **THEN** the system increments `outbox_events_published_total`

#### Scenario: Outbox publish failure increments metric
- **WHEN** outbox event publishing fails for an event
- **THEN** the system increments `outbox_events_failed_total`

### Requirement: Record transfer processing duration
The system SHALL register a Micrometer timer for transfer processing duration.

#### Scenario: Successful transfer records duration
- **WHEN** a transfer request completes successfully
- **THEN** the system records the processing duration in the transfer timer metric

#### Scenario: Failed transfer records duration
- **WHEN** a transfer request completes with a handled failure outcome
- **THEN** the system records the processing duration in the transfer timer metric

### Requirement: Emit structured operational logs for key flows
The system SHALL emit structured logs for deposit, transfer, reconciliation, and outbox publishing flows, including the relevant identifiers when they are available in that flow.

#### Scenario: Deposit log includes identifiers
- **WHEN** a deposit flow logs a business outcome
- **THEN** the log includes `walletId`
- **AND** the log includes `transactionId` when a transaction exists
- **AND** the log includes `idempotencyKey` when one is available

#### Scenario: Transfer log includes identifiers
- **WHEN** a transfer flow logs a business outcome
- **THEN** the log includes `transactionId` when a transaction exists
- **AND** the log includes `idempotencyKey` when one is available

#### Scenario: Reconciliation log includes status
- **WHEN** reconciliation logs its outcome
- **THEN** the log includes `reconciliationStatus`

#### Scenario: Outbox publish log includes identifiers
- **WHEN** the outbox publishing flow logs a publish attempt or outcome
- **THEN** the log includes `outboxEventId`

### Requirement: Expose metrics through actuator
The system SHALL expose the actuator metrics endpoint for operational inspection.

#### Scenario: Metrics endpoint is reachable
- **WHEN** the application is running
- **THEN** the actuator metrics endpoint is available for inspection

### Requirement: Verify observability instrumentation
The system SHALL include practical automated verification for metric registration or increment behavior where feasible.

#### Scenario: Metrics can be verified in tests
- **WHEN** the automated test suite exercises representative instrumented flows
- **THEN** the suite verifies that the relevant metrics are registered or incremented as expected
