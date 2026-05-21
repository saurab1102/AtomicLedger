## ADDED Requirements

### Requirement: Document wallet-management public APIs in OpenAPI
The system SHALL document wallet creation and wallet transaction history in OpenAPI, including path/query parameters, request bodies, success responses, and standardized error responses.

#### Scenario: Wallet creation is documented
- **WHEN** a developer inspects `POST /api/v1/wallets` in the OpenAPI docs
- **THEN** the documentation describes the wallet-creation request body, success response, and supported error response shape

#### Scenario: Wallet history is documented
- **WHEN** a developer inspects `GET /api/v1/wallets/{walletId}/transactions` in the OpenAPI docs
- **THEN** the documentation describes the wallet ID path parameter, pagination query parameters, history response body, and supported error response shape
