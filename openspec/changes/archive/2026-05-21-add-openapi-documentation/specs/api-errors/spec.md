## ADDED Requirements

### Requirement: Document the shared API error schema in OpenAPI
The system SHALL document the standardized API error response shape as a reusable OpenAPI schema including `errorCode`, `message`, `details`, and `timestamp`.

#### Scenario: Standard error schema appears in docs
- **WHEN** a developer inspects an endpoint that can return a handled API error
- **THEN** the OpenAPI documentation references the shared error schema instead of an endpoint-specific undocumented error body
