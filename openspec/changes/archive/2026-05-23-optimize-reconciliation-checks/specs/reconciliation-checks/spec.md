## ADDED Requirements

### Requirement: Execute transfer and deposit reconciliation with set-based aggregation
The system SHALL evaluate successful transfer and successful deposit reconciliation invariants through set-based aggregate queries over ledger entries and transactions rather than per-transaction ledger-entry lookups.

#### Scenario: Transfer mismatches are found without per-transaction ledger lookups
- **WHEN** reconciliation validates successful `TRANSFER` transactions
- **THEN** the system detects broken transfer ledger structure and amount mismatches through aggregate transaction-level summaries

#### Scenario: Deposit mismatches are found without per-transaction ledger lookups
- **WHEN** reconciliation validates successful `DEPOSIT` transactions
- **THEN** the system detects broken deposit ledger structure through aggregate transaction-level summaries

### Requirement: Support reconciliation queries with dedicated indexes
The system SHALL maintain database indexes that support reconciliation access paths on ledger-entry wallet and transaction references and on successful transaction status/type filters.

#### Scenario: Reconciliation indexes are present
- **WHEN** the database schema is migrated for reconciliation optimization
- **THEN** the schema includes indexes for `ledger_entries(wallet_id)`, `ledger_entries(transaction_id)`, `transactions(status)`, and `transactions(transaction_type, status)`

### Requirement: Log reconciliation phase durations
The system SHALL log timing information for the wallet balance, transfer, and deposit reconciliation phases.

#### Scenario: Wallet phase duration is logged
- **WHEN** reconciliation completes the wallet balance phase
- **THEN** the application logs the elapsed time for the wallet balance check

#### Scenario: Transfer phase duration is logged
- **WHEN** reconciliation completes the transfer phase
- **THEN** the application logs the elapsed time for the transfer check

#### Scenario: Deposit phase duration is logged
- **WHEN** reconciliation completes the deposit phase
- **THEN** the application logs the elapsed time for the deposit check

## MODIFIED Requirements

### Requirement: Return structured failed-check details
The system SHALL return reconciliation results with overall status `PASS` or `FAIL` and SHALL include structured failed-check details for detected mismatches or broken accounting invariants, limited to at most 100 rows per failed-check type.

#### Scenario: Multiple reconciliation failures are reported
- **WHEN** reconciliation detects one or more wallet or transaction integrity failures
- **THEN** the response contains status `FAIL`
- **AND** the response contains structured failed-check detail records for the detected failure categories

#### Scenario: Excess mismatches are capped per check type
- **WHEN** reconciliation detects more than 100 failures for the same failed-check type
- **THEN** the response includes only the first 100 detail rows for that failed-check type
- **AND** the reconciliation result status remains `FAIL`
