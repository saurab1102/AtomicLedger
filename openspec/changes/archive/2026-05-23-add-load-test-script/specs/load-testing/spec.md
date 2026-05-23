## ADDED Requirements

### Requirement: Provide a local k6 load test script
The repository SHALL include a k6 load test script at `load-tests/atomicledger-transfer-load.js` for exercising AtomicLedger locally.

#### Scenario: Load test script exists in the repository
- **WHEN** a developer inspects the repository for local load-testing assets
- **THEN** the repository contains `load-tests/atomicledger-transfer-load.js`

### Requirement: Target local AtomicLedger with API key authentication
The load test script SHALL target AtomicLedger on `http://localhost:8080` by default and SHALL send the `X-API-Key` header on protected API requests.

#### Scenario: Authenticated local request configuration is used
- **WHEN** a developer runs the load test script with default local settings
- **THEN** the script sends requests to the local AtomicLedger base URL and includes `X-API-Key` on the protected API calls it performs

### Requirement: Support wallet bootstrap or configured wallet IDs
The load test script SHALL support creating wallets as part of setup or reusing wallet IDs provided through configuration.

#### Scenario: Script bootstraps wallets dynamically
- **WHEN** the developer runs the script without preconfigured wallet IDs
- **THEN** the script creates the wallets it needs before exercising deposit and transfer traffic

#### Scenario: Script reuses configured wallet IDs
- **WHEN** the developer provides source and destination wallet IDs through configuration
- **THEN** the script skips wallet creation and drives load against the configured wallets

### Requirement: Exercise deposit, transfer, and replay-oriented traffic
The load test script SHALL exercise deposit and transfer requests, SHALL include repeated transfer attempts, and SHALL include duplicate idempotency-key replay checks where practical.

#### Scenario: Transfer-heavy traffic is generated
- **WHEN** the developer runs the load test
- **THEN** the script performs deposits and repeated transfer requests against AtomicLedger

#### Scenario: Duplicate idempotency replay is exercised
- **WHEN** the script reaches its replay-oriented request path
- **THEN** it reuses one or more idempotency keys intentionally to exercise AtomicLedger’s duplicate replay behavior

### Requirement: Document how to run the load test
The repository SHALL document how to run the k6 load test from the main README.

#### Scenario: README explains local load-test execution
- **WHEN** a developer reads the project README
- **THEN** the README includes concise instructions for running `load-tests/atomicledger-transfer-load.js` locally, including any required configuration inputs
