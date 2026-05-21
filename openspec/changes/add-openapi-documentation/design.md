## Context

AtomicLedger exposes multiple public REST APIs across wallet management, deposits, transfers, transaction history, reconciliation, audit logs, and outbox inspection. The project now also has a standardized API error shape, but discovery is still code-first: consumers need to read controllers, DTOs, and tests to understand request bodies, headers, success responses, and failure responses.

This change is documentation-focused and cross-cutting. It adds generated OpenAPI documentation and local Swagger UI access without altering runtime business behavior or persistence.

## Goals / Non-Goals

**Goals:**
- Add generated OpenAPI docs and Swagger UI using `springdoc-openapi`.
- Publish stable API metadata for title, description, and version.
- Document every public endpoint currently exposed by the wallet, transfer, reconciliation, audit log, and outbox APIs.
- Reuse the shared API error contract in generated docs.
- Document the required `Idempotency-Key` header on deposit and transfer endpoints.
- Include representative examples for key success flows and insufficient-balance failures.
- Add a smoke test or equivalent verification that the OpenAPI docs endpoint is reachable.

**Non-Goals:**
- Changing any API behavior, validation rules, or response payloads.
- Introducing authentication/authorization documentation that does not exist in the current system.
- Replacing the existing code-first controllers with a spec-first workflow.
- Adding client SDK generation or external API publishing workflows.

## Decisions

### Use `springdoc-openapi` with annotation-driven documentation
The implementation will use `springdoc-openapi` on top of the existing Spring MVC controllers and DTOs.

Rationale:
- It fits the current code-first Spring Boot architecture with minimal disruption.
- It can generate both the OpenAPI JSON and local Swagger UI from the existing controllers.

Alternative considered:
- Maintain a handwritten OpenAPI YAML file. Rejected because it is more likely to drift from the actual controllers and DTOs over time.

### Centralize top-level API metadata in OpenAPI configuration
API title, description, and version should live in one OpenAPI configuration class instead of being repeated on individual controllers.

Rationale:
- The metadata is global to the service.
- It keeps controller annotations focused on endpoint-level documentation.

Alternative considered:
- Configure only through properties. Rejected because examples and reusable schema registration are likely easier to manage in code for this project size.

### Reuse DTO classes and annotate public controllers for request/response docs
Request DTOs, response DTOs, and the standardized error DTOs should be the source of schema documentation, while controller methods carry operation summaries, header parameter docs, and examples.

Rationale:
- The existing DTO layer already expresses the API contract clearly.
- Reusing actual DTOs avoids creating documentation-only types.

Alternative considered:
- Create separate documentation DTO wrappers. Rejected because it duplicates the public contract and adds maintenance overhead.

### Document `Idempotency-Key` explicitly as a required header on deposit and transfer operations
The OpenAPI contract should show `Idempotency-Key` as a required header parameter for deposit and transfer endpoints, even though the runtime code currently reads it via controller parameters.

Rationale:
- The header is a critical client requirement and easy to miss without explicit documentation.
- This preserves current behavior while making the contract visible in Swagger UI.

### Verify docs availability with a lightweight smoke test
The implementation should include a smoke-level test of the OpenAPI endpoint instead of deep schema snapshot testing.

Rationale:
- The requirement is availability-oriented, not schema-freezing.
- Smoke verification is less brittle while still confirming the docs are wired and exposed.

Alternative considered:
- Assert the full generated OpenAPI document structure. Rejected because it is likely to be noisy and tightly coupled to library output details.

## Risks / Trade-offs

- [Risk] OpenAPI annotations can become verbose across controllers and DTOs. → Mitigation: keep metadata focused on public contract essentials and rely on reusable DTO schemas where possible.
- [Risk] Generated docs may drift if future endpoints are added without annotations. → Mitigation: create a dedicated `api-documentation` capability and include endpoint documentation expectations in specs/tasks going forward.
- [Risk] Swagger UI should be available locally without accidentally implying a production-only support guarantee. → Mitigation: phrase the requirement specifically around local development exposure and smoke verification of docs availability.

## Migration Plan

1. Add the `springdoc-openapi` dependency and basic OpenAPI metadata configuration.
2. Annotate controllers and DTOs to document public endpoints, request bodies, response bodies, the standardized error schema, and required headers.
3. Add examples for representative wallet, deposit, transfer, and insufficient-balance responses.
4. Add a smoke test that verifies the OpenAPI docs endpoint is reachable.

Rollback strategy:
- Remove the dependency and OpenAPI annotations/configuration if the generated docs create unexpected startup or compatibility issues.

## Open Questions

- No open questions for this scope. The required endpoints, metadata, error shape, examples, and smoke verification target are sufficiently defined to proceed directly.
