## ADDED Requirements

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
