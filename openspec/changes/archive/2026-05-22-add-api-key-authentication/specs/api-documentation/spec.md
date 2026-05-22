## ADDED Requirements

### Requirement: Document API key authentication for protected APIs
The system SHALL document that `/api/v1/**` operations require the `X-API-Key` header while preserving public access to Swagger UI and OpenAPI docs for local development.

#### Scenario: Protected operation documents X-API-Key
- **WHEN** a developer inspects a protected `/api/v1/**` operation in the OpenAPI docs
- **THEN** the documentation indicates that the operation requires the `X-API-Key` header

#### Scenario: Local documentation remains accessible without authentication
- **WHEN** a developer opens Swagger UI or requests the OpenAPI docs endpoint without an `X-API-Key`
- **THEN** the documentation surfaces remain reachable for local development
