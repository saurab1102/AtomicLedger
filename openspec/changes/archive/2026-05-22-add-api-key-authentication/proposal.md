## Why

AtomicLedger currently exposes its `/api/v1/**` endpoints without any request authentication, which is fine for local iteration but not for demonstrating a backend that has even a minimal boundary around state-changing and data-reading APIs. Adding a simple API key check raises the baseline for local and portfolio use while preserving the current API contracts and keeping operational endpoints that support local development available.

## What Changes

- Add request authentication based on the `X-API-Key` header for all `/api/v1/**` endpoints.
- Keep `/actuator/health`, Swagger UI, and OpenAPI docs publicly accessible for local development.
- Load the accepted local API key from configuration rather than hardcoding it in controllers or services.
- Return standardized `401 Unauthorized` API errors for missing and invalid API keys without changing successful response bodies.
- Add automated coverage for authenticated API access, unauthorized failures, and the public health/docs exceptions.

## Capabilities

### New Capabilities
- `api-key-authentication`: Covers API key validation, protected-path matching, public-path exclusions, configuration-backed local key storage, and unauthorized error handling for missing or invalid `X-API-Key` requests.

### Modified Capabilities
- `api-errors`: Add stable standardized unauthorized error responses for missing and invalid API keys.
- `api-documentation`: Document which public APIs require `X-API-Key` and preserve public access to Swagger UI and OpenAPI docs for local development.
- `wallet-management`: Require a valid API key for wallet creation and wallet transaction history requests.
- `wallet-deposit`: Require a valid API key for deposit requests while preserving current idempotent success behavior.
- `wallet-transfer`: Require a valid API key for transfer requests while preserving current idempotent success behavior.
- `reconciliation-checks`: Require a valid API key for reconciliation execution.
- `audit-logs`: Require a valid API key for audit-log inspection.
- `outbox-events`: Require a valid API key for outbox-event inspection.

## Impact

- Affected code: Spring MVC/security boundary for API request filtering, shared error handling, API configuration, controller integration tests, and API documentation annotations/configuration.
- Affected APIs: All `/api/v1/**` endpoints gain an `X-API-Key` precondition; `/actuator/health`, Swagger UI, and OpenAPI docs remain public.
- Dependencies/systems: Likely Spring Security or an equivalent servlet filter/interceptor-based auth layer, plus configuration properties for the local API key.
