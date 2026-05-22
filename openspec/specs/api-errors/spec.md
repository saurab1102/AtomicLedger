## Purpose
Define the shared API error contract and the rules for translating handled exceptions into consistent HTTP error responses.
## Requirements
### Requirement: Return a common API error response shape
The system SHALL serialize handled API failures as JSON objects containing `errorCode`, `message`, `details`, and `timestamp`.

#### Scenario: Domain error uses the common envelope
- **WHEN** a handled domain exception is returned from an API endpoint
- **THEN** the response body contains `errorCode`, `message`, `details`, and `timestamp` in a consistent JSON shape

### Requirement: Include field-level details for validation-style failures
The system SHALL include field-level or header-level detail entries inside `details` for validation errors and targeted request-validation failures.

#### Scenario: Bean validation failure includes field details
- **WHEN** a request body fails bean validation for one or more fields
- **THEN** the error response includes one or more detail entries identifying the invalid field names and their validation messages

#### Scenario: Missing header validation includes header detail
- **WHEN** a request is rejected because `Idempotency-Key` is missing
- **THEN** the error response includes a detail entry identifying the `Idempotency-Key` header and the reason it is required

### Requirement: Global exception handling standardizes API failures
The system SHALL translate handled API exceptions through a global Spring `@RestControllerAdvice` so controllers do not need endpoint-specific error serialization.

#### Scenario: Controller-raised exception is handled globally
- **WHEN** a controller or service raises a handled request or domain exception
- **THEN** the global exception handler returns the standardized error response with the mapped HTTP status

### Requirement: Document the shared API error schema in OpenAPI
The system SHALL document the standardized API error response shape as a reusable OpenAPI schema including `errorCode`, `message`, `details`, and `timestamp`.

#### Scenario: Standard error schema appears in docs
- **WHEN** a developer inspects an endpoint that can return a handled API error
- **THEN** the OpenAPI documentation references the shared error schema instead of an endpoint-specific undocumented error body

### Requirement: Return standardized unauthorized errors for API key authentication
The system SHALL return missing and invalid API key failures using the shared API error response contract with `errorCode`, `message`, `details`, and `timestamp`.

#### Scenario: Missing API key uses a stable error code
- **WHEN** a client sends a request to a protected `/api/v1/**` endpoint without an `X-API-Key` header
- **THEN** the system rejects the request with `401` and `errorCode = MISSING_API_KEY`

#### Scenario: Invalid API key uses a stable error code
- **WHEN** a client sends a request to a protected `/api/v1/**` endpoint with an `X-API-Key` value that does not match the configured accepted key
- **THEN** the system rejects the request with `401` and `errorCode = INVALID_API_KEY`

#### Scenario: Missing API key identifies the header in details
- **WHEN** a client sends a request to a protected `/api/v1/**` endpoint without an `X-API-Key` header
- **THEN** the error response includes a detail entry identifying the `X-API-Key` header and the reason it is required
