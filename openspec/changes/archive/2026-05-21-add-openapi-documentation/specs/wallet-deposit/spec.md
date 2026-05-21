## ADDED Requirements

### Requirement: Document the deposit API in OpenAPI
The system SHALL document `POST /api/v1/wallets/{walletId}/deposit` in OpenAPI, including its request body, success response, standardized error responses, and the required `Idempotency-Key` header.

#### Scenario: Deposit header requirement is documented
- **WHEN** a developer inspects the deposit operation in the OpenAPI docs
- **THEN** the documentation marks `Idempotency-Key` as a required header and describes the deposit request and response bodies
