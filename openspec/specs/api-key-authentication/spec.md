## Purpose
Describe how AtomicLedger protects its public application APIs with a configured API key while keeping local health and documentation surfaces accessible.
## Requirements
### Requirement: Authenticate API requests with X-API-Key
The system SHALL require a valid `X-API-Key` header for every request whose path matches `/api/v1/**`.

#### Scenario: Protected API request includes a valid API key
- **WHEN** a client sends a request to a `/api/v1/**` endpoint with the configured valid `X-API-Key`
- **THEN** the request is allowed to continue to the existing controller and service flow without changing the successful API contract

### Requirement: Reject protected API requests missing the API key
The system SHALL reject requests to `/api/v1/**` that omit the `X-API-Key` header with `401 Unauthorized` using the standardized API error response shape.

#### Scenario: Protected request omits X-API-Key
- **WHEN** a client sends a request to a `/api/v1/**` endpoint without an `X-API-Key` header
- **THEN** the system responds with `401` and a standardized error body describing the missing API key

### Requirement: Reject protected API requests with an invalid API key
The system SHALL reject requests to `/api/v1/**` whose `X-API-Key` value does not match the configured accepted key with `401 Unauthorized` using the standardized API error response shape.

#### Scenario: Protected request uses the wrong API key
- **WHEN** a client sends a request to a `/api/v1/**` endpoint with an `X-API-Key` value that does not match the configured accepted key
- **THEN** the system responds with `401` and a standardized error body describing the invalid API key

### Requirement: Keep health and local documentation endpoints public
The system SHALL allow unauthenticated access to `/actuator/health`, Swagger UI, and OpenAPI docs for local development.

#### Scenario: Actuator health remains public
- **WHEN** a client sends a request to `/actuator/health` without an `X-API-Key`
- **THEN** the system returns the health response instead of an unauthorized error

#### Scenario: Swagger UI remains public
- **WHEN** a client opens Swagger UI without an `X-API-Key`
- **THEN** the system serves the Swagger UI resources instead of an unauthorized error

#### Scenario: OpenAPI docs remain public
- **WHEN** a client requests the OpenAPI docs endpoint without an `X-API-Key`
- **THEN** the system returns the generated OpenAPI document instead of an unauthorized error

### Requirement: Read the accepted API key from configuration
The system SHALL source the accepted local API key from application configuration rather than hardcoding it in controllers or services.

#### Scenario: Local API key is configured externally
- **WHEN** the application starts
- **THEN** the authentication layer reads the accepted API key from configuration and uses it to validate protected API requests
