## Context

AtomicLedger already centralizes many wallet-related exceptions in `WalletExceptionHandler`, but the current responses are specialized around `ValidationErrorResponse` and do not expose stable machine-readable error codes or a uniform top-level JSON contract. As the API surface grows across wallet creation, deposits, transfers, audit inspection, reconciliation, and outbox inspection, clients need a consistent way to interpret errors without reverse-engineering endpoint-specific payloads.

This change is cross-cutting but intentionally lightweight: it standardizes HTTP error serialization and exception-to-response mapping without changing successful responses, persistence, or business workflows.

## Goals / Non-Goals

**Goals:**
- Define one common error response DTO used for API failures.
- Centralize exception translation in a global `@RestControllerAdvice`.
- Preserve current business behavior while making failure responses consistent across wallet-related endpoints.
- Include structured detail entries for validation-style failures so clients can bind errors to specific fields or headers.
- Assign stable error codes for the explicitly required domain and request-validation cases.

**Non-Goals:**
- Changing any successful API response bodies.
- Changing duplicate-idempotency handling from success replay to an error.
- Introducing i18n/localized error messages.
- Redesigning every possible exception in the system beyond the currently documented wallet/deposit/transfer failure cases.

## Decisions

### Use a single top-level API error response contract
All handled API failures will serialize as a shared DTO with:
- `errorCode`: stable machine-readable identifier
- `message`: human-readable summary
- `details`: optional list for field/header-specific issues
- `timestamp`: server-side `Instant`

Rationale:
- Clients need a stable discriminator that is safer than matching free-form messages.
- A shared contract reduces controller-level duplication and makes future endpoints consistent by default.

Alternative considered:
- Keep separate DTOs for validation vs domain errors. Rejected because it preserves inconsistency and forces clients to branch on shape before interpreting content.

### Represent field-level validation issues inside `details`
Validation failures and targeted request problems like missing `Idempotency-Key` will populate `details` with field-aware entries instead of creating special top-level response types.

Rationale:
- This keeps one envelope for all API errors while still preserving granular validation information.
- The current code already models field-level issues, so this can be migrated with minimal conceptual change.

Alternative considered:
- Flatten validation metadata into the top-level object. Rejected because multiple simultaneous validation failures need a collection.

### Keep exception-to-status mapping explicit in controller advice
The global advice will map domain/request exceptions directly to status codes and error codes, including:
- `MISSING_IDEMPOTENCY_KEY` → `400`
- `WALLET_NOT_FOUND` → `404`
- `UNSUPPORTED_CURRENCY` → `400`
- `INSUFFICIENT_BALANCE` → `409`
- `INVALID_TRANSFER_TARGET` → `400`

Rationale:
- The requested contract is part of the public API and should be visible in one place.
- Explicit mappings are easier to review and extend than implicit conventions derived from exception names.

Alternative considered:
- Annotate exceptions with `@ResponseStatus`. Rejected because it handles status but not the standardized body shape or stable error code mapping cleanly.

### Preserve existing business semantics and only standardize serialization
Duplicate idempotency key replays remain successful responses, and domain workflows like failed-transfer auditing/outbox behavior remain unchanged.

Rationale:
- The change is about API error consistency, not business logic redesign.
- Constraining scope reduces regression risk and keeps integration tests focused on response contracts.

## Risks / Trade-offs

- [Risk] Existing clients may parse current error payload fields such as `errors` directly. → Mitigation: document the new response contract in specs and cover required cases with integration tests before further API expansion.
- [Risk] Some exceptions outside the currently enumerated list may still fall through to Spring defaults. → Mitigation: include a generic fallback handler during implementation so unhandled server errors still use the common shape.
- [Risk] Field names in validation `details` must stay stable enough for clients to rely on. → Mitigation: reuse existing request field names and the `Idempotency-Key` header label as-is.

## Migration Plan

1. Introduce the new shared error DTO and detail DTOs.
2. Replace the current specialized handler output with the standardized contract under `@RestControllerAdvice`.
3. Update integration tests to assert the new error shape and required error codes/statuses.
4. Keep successful response tests unchanged to confirm no regressions in normal flows.

Rollback strategy:
- Revert the advice/DTO changes and restore the prior specialized error responses if a compatibility issue is discovered before release.

## Open Questions

- No open questions for this scope. The requested error-code set, statuses, and exclusion of success-contract changes are sufficiently specific to implement directly.
