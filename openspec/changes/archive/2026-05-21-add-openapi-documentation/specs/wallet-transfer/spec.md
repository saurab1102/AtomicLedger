## ADDED Requirements

### Requirement: Document the transfer API in OpenAPI
The system SHALL document `POST /api/v1/transfers` in OpenAPI, including its request body, success response, standardized error responses, the required `Idempotency-Key` header, and an insufficient-balance error example.

#### Scenario: Transfer header and error example are documented
- **WHEN** a developer inspects the transfer operation in the OpenAPI docs
- **THEN** the documentation marks `Idempotency-Key` as a required header and includes an example of the insufficient-balance error response
