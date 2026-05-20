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
