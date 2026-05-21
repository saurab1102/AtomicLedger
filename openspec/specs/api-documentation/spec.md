## Purpose
Describe how AtomicLedger publishes generated OpenAPI documentation and local Swagger UI for its public REST APIs.
## Requirements
### Requirement: Publish generated OpenAPI documentation
The system SHALL generate OpenAPI documentation for AtomicLedger's public REST APIs and expose the documentation endpoint for local development.

#### Scenario: OpenAPI document is available
- **WHEN** the application is running in local development
- **THEN** the OpenAPI docs endpoint is reachable and returns the generated API description

### Requirement: Expose Swagger UI for local API exploration
The system SHALL expose Swagger UI for local development so engineers can inspect and try the documented public APIs.

#### Scenario: Swagger UI is available locally
- **WHEN** the application is running in local development
- **THEN** a developer can open Swagger UI and browse the documented AtomicLedger endpoints

### Requirement: Publish API metadata and examples
The generated OpenAPI contract SHALL include the API title `AtomicLedger API`, the description `Transaction-safe wallet and double-entry ledger backend`, the version `v1`, and examples for successful wallet creation, deposit, transfer, and insufficient balance errors.

#### Scenario: OpenAPI metadata and examples are present
- **WHEN** a client or developer reads the generated OpenAPI document
- **THEN** the document includes the configured title, description, version, and the required representative examples
