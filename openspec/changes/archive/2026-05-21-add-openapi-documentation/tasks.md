## 1. OpenAPI Setup

- [x] 1.1 Add `springdoc-openapi` support and configure OpenAPI metadata with title `AtomicLedger API`, description `Transaction-safe wallet and double-entry ledger backend`, and version `v1`.
- [x] 1.2 Expose the generated OpenAPI docs endpoint and Swagger UI for local development without changing existing API behavior.

## 2. Public API Documentation

- [x] 2.1 Annotate the wallet creation, deposit, transfer, wallet transaction history, reconciliation, audit log, and outbox event endpoints for OpenAPI generation.
- [x] 2.2 Document request DTOs, success response DTOs, and the shared API error response DTOs as reusable OpenAPI schemas.
- [x] 2.3 Document the required `Idempotency-Key` header on deposit and transfer endpoints.
- [x] 2.4 Add representative examples for successful wallet creation, deposit, transfer, and insufficient balance errors.

## 3. Verification

- [x] 3.1 Add a smoke test or equivalent verification that the OpenAPI docs endpoint is available.
- [x] 3.2 Verify the generated docs include the standardized error response shape for public APIs.
