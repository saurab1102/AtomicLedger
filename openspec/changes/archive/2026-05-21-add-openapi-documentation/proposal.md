## Why

AtomicLedger now has a broader public API surface, but it still relies on source code and tests as the primary way to discover request/response contracts. Adding OpenAPI documentation now will make the API easier to explore locally, easier to integrate with, and easier to keep aligned with the standardized success and error contracts already in the codebase.

## What Changes

- Add OpenAPI generation and Swagger UI support using `springdoc-openapi`.
- Publish API metadata with the title `AtomicLedger API`, description `Transaction-safe wallet and double-entry ledger backend`, and version `v1`.
- Document every public API endpoint, including wallet creation, deposit, transfer, wallet transaction history, reconciliation, audit logs, and outbox events.
- Document request bodies, success responses, and the shared API error response shape.
- Document the required `Idempotency-Key` header for deposit and transfer endpoints.
- Add examples for successful wallet creation, deposit, transfer, and insufficient balance errors.
- Add a smoke test or equivalent verification that the OpenAPI docs endpoint is available.

## Capabilities

### New Capabilities
- `api-documentation`: Defines the generated OpenAPI contract, local Swagger UI exposure, and the requirement to document shared request/response/error shapes for public APIs.

### Modified Capabilities
- `wallet-management`: Wallet creation and wallet transaction history will gain explicit OpenAPI documentation for request parameters, responses, and errors.
- `wallet-deposit`: Deposit behavior will gain OpenAPI documentation, including the required `Idempotency-Key` header and standard error responses.
- `wallet-transfer`: Transfer behavior will gain OpenAPI documentation, including the required `Idempotency-Key` header and insufficient-balance error examples.
- `reconciliation-checks`: Reconciliation execution will gain OpenAPI documentation for its response contract.
- `audit-logs`: Audit log inspection will gain OpenAPI documentation for query parameters and responses.
- `outbox-events`: Outbox event inspection will gain OpenAPI documentation for responses and endpoint exposure.
- `api-errors`: The shared API error contract will be documented as a reusable OpenAPI schema across public endpoints.

## Impact

- Affected code will include build dependencies, OpenAPI configuration, controller/DTO annotations, and a smoke test for docs availability.
- Public API behavior will remain unchanged; the change only adds generated documentation and local API exploration support.
- Swagger UI and the OpenAPI JSON endpoint will be available during local development.
