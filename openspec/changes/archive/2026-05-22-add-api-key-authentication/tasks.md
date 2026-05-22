## 1. Security Boundary

- [x] 1.1 Add the API-key authentication dependency/configuration needed to secure HTTP requests at the application boundary.
- [x] 1.2 Implement configuration-backed API key validation for `X-API-Key` and apply it to `/api/v1/**` while permitting `/actuator/health`, Swagger UI, and OpenAPI docs.
- [x] 1.3 Ensure authentication failures return the standardized `401` API error response for missing and invalid API keys.

## 2. API Integration

- [x] 2.1 Wire the protected API routes so valid API-key requests continue to reach the existing wallet, deposit, transfer, reconciliation, audit-log, and outbox-event flows unchanged.
- [x] 2.2 Add or update OpenAPI/Swagger configuration so protected `/api/v1/**` operations document the required `X-API-Key` header while docs themselves remain publicly reachable in local development.

## 3. Test Coverage

- [x] 3.1 Add integration tests proving missing and invalid API keys return standardized `401` responses on protected API routes.
- [x] 3.2 Add integration tests proving valid API-key requests still allow wallet creation and deposit flows to succeed.
- [x] 3.3 Add integration tests proving `/actuator/health`, Swagger UI, and OpenAPI docs remain publicly accessible without an API key.

## 4. Configuration and Verification

- [x] 4.1 Add local configuration for the accepted API key without hardcoding it in controllers or services.
- [x] 4.2 Run the relevant test suite and validate the OpenSpec change before marking the change complete.
