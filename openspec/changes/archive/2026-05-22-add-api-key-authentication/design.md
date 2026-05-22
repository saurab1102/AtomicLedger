## Context

AtomicLedger currently exposes every `/api/v1/**` endpoint without request authentication. The project already has a standardized API error envelope, OpenAPI/Swagger support for local development, and Actuator endpoints for operational visibility, so the new authentication layer needs to fit into those existing cross-cutting concerns without changing successful API behavior.

This is also a local-development and portfolio-oriented security boundary rather than a full identity system. The design therefore needs to stay intentionally small: one configured key, request validation at the HTTP boundary, explicit exemptions for local docs and health checks, and test coverage that proves protected endpoints and public exceptions behave as expected.

## Goals / Non-Goals

**Goals:**
- Require a valid `X-API-Key` header for all `/api/v1/**` endpoints.
- Keep `/actuator/health`, `/swagger-ui/**`, and `/v3/api-docs/**` accessible without authentication.
- Source the accepted local API key from application configuration.
- Return the existing standardized error response shape for missing and invalid API keys with `401 Unauthorized`.
- Preserve all current successful request and response contracts for valid authenticated calls.

**Non-Goals:**
- Introduce users, sessions, login flows, JWTs, OAuth, RBAC, or per-client authorization.
- Protect non-API local developer surfaces such as Swagger UI and the OpenAPI docs in this change.
- Redesign current controllers, services, or domain flows beyond adding an authentication boundary.

## Decisions

### Use Spring Security for request boundary enforcement
AtomicLedger should enforce API-key authentication through Spring Security rather than ad hoc controller checks or service-level guards. This keeps authentication at the HTTP boundary, applies consistently across all `/api/v1/**` endpoints, and makes it straightforward to declare public exceptions for health and docs.

Alternative considered:
- Servlet interceptor or filter without Spring Security: workable, but it would re-create path matching, unauthorized handling, and public-path policy outside the framework’s main security configuration model.

### Validate a single configured local API key from application properties
The accepted API key should come from configuration properties such as `application.yml` or environment overrides. This keeps secrets out of controllers/services, supports local overrides without code changes, and matches the requirement that the key not be hardcoded in application logic.

Alternative considered:
- Hardcode a default key in Java code: rejected because it leaks configuration into implementation and makes environment-specific overrides awkward.

### Return standardized unauthorized errors from the security layer
Missing and invalid API keys occur before a controller method runs, so the security layer should emit the same `errorCode` / `message` / `details` / `timestamp` shape as the existing global exception handling contract. The simplest way is to centralize error-body creation in a reusable component and have the authentication entry point use it directly.

Alternative considered:
- Let Spring Security return its default `401` response: rejected because it would break the standardized API error contract.
- Throw controller-visible exceptions and rely on `@RestControllerAdvice`: rejected because requests should be rejected before controller dispatch.

### Keep authentication path-scoped and explicitly permit local docs and health
The security rule set should protect `/api/v1/**` and permit `/actuator/health`, `/swagger-ui/**`, `/swagger-ui.html`, and `/v3/api-docs/**`. This satisfies the requirement to keep health public for monitoring and Swagger/OpenAPI public for local exploration without widening access to the application APIs themselves.

Alternative considered:
- Protect every request except a short allowlist: possible, but broader than required and more likely to produce accidental friction on non-API local surfaces.

### Verify behavior with integration tests at the HTTP layer
Authentication is a boundary concern, so the main verification should be integration tests that exercise real HTTP requests against protected and public paths. These tests should cover missing key, invalid key, successful authenticated wallet creation/deposit, public health, and public docs.

Alternative considered:
- Unit-test only the filter or matcher logic: useful but not sufficient to prove the full request path, error serialization, and public exclusions.

## Risks / Trade-offs

- [A single shared local API key is coarse-grained] → Accept for now because the goal is a basic boundary, not identity or authorization; future auth schemes can build on the same HTTP security boundary.
- [Public Swagger/OpenAPI surfaces expose the contract locally] → This is intentional for development ergonomics; a future environment-specific profile can lock docs down outside local use.
- [Security-layer error responses can drift from controller-layer errors] → Mitigate by reusing the same error envelope builder or mapping component for both.
- [Path-based exemptions can regress as new endpoints are added] → Mitigate with integration tests that cover protected API routes and public exceptions explicitly.
