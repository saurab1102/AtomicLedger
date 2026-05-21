## Context

AtomicLedger already includes Spring Boot Actuator and logs useful domain outcomes indirectly through persisted audit and outbox records, but it does not yet emit lightweight operational telemetry for live observation. This change adds cross-cutting observability for the main business flows without altering API contracts, persistence rules, or domain behavior.

The observability surface needs to cover:

- counters for high-value success, failure, and replay outcomes
- a transfer duration timer
- structured logs with IDs that make request and background-worker flows traceable
- actuator metrics exposure for local inspection and external scraping

Because the metrics span wallet creation, deposits, transfers, reconciliation, and outbox publishing, this is best treated as one cross-cutting operational capability rather than a series of endpoint-specific changes.

## Goals / Non-Goals

**Goals:**
- Register and increment Micrometer counters for the required business and worker outcomes
- Record transfer processing duration with a timer
- Emit structured logs for deposit, transfer, reconciliation, and outbox publishing flows
- Include the relevant IDs in those logs whenever the flow has them available
- Expose the actuator metrics endpoint as the operational read surface
- Add practical tests or smoke checks proving the new metrics exist and increment

**Non-Goals:**
- Changing success or error API payloads
- Introducing distributed tracing, log shipping, dashboards, or alerting infrastructure
- Replacing audit logs with application logs
- Adding broker integrations beyond the current outbox logging publisher

## Decisions

### Use a dedicated observability component around Micrometer `MeterRegistry`
The implementation should centralize metric names and increment/timing calls in one application component instead of scattering raw `MeterRegistry` usage across services.

Rationale:
- keeps metric naming consistent
- lowers the chance of typos or duplicate meter registration
- makes tests more direct because they can inspect one abstraction

Alternative considered:
- inject `MeterRegistry` into every service and update counters inline. Rejected because the change touches several flows and would spread metric concerns broadly through business logic.

### Increment counters at committed business outcome boundaries
Counters should reflect business outcomes that the application has decided to treat as final for that flow, not intermediate steps.

Rationale:
- `wallets_created_total`, `deposits_succeeded_total`, and `transfers_succeeded_total` should reflect committed success outcomes
- duplicate replay counters should increment only when the system actually serves a replay path
- `transfers_failed_total` should reflect the failed transfer outcome that the application surfaces and audits
- reconciliation and outbox counters should align with the final result of each run/publication attempt

Alternative considered:
- counting at method entry. Rejected because retries, validation failures, and exceptions would make the numbers less meaningful.

### Time the full transfer service path rather than only the persistence sub-block
The transfer duration timer should measure the service-level processing path that includes idempotency lookup, locking, validation, transaction work, and response construction.

Rationale:
- this is the operator-facing latency shape the application owns
- it captures both normal success and failure paths when they matter operationally

Alternative considered:
- timing only the transactional lambda or only repository calls. Rejected because it would under-report the real application cost of handling a transfer request.

### Emit structured key-value logs from the service and worker layers
Structured logs should be added at the business-service level and outbox worker level, using stable event messages plus key-value fields such as `walletId`, `transactionId`, `idempotencyKey`, `reconciliationStatus`, and `outboxEventId`.

Rationale:
- these layers have the domain context needed to log meaningful identifiers
- logs stay close to the business outcome they describe
- structured key-value logging is easier to query later than free-form prose logs

Alternative considered:
- only logging in controllers. Rejected because not all interesting flows are request scoped, especially outbox publishing and reconciliation internals.

### Expose the actuator metrics endpoint through Spring Boot management configuration
The implementation should explicitly expose the metrics actuator endpoint rather than relying on defaults.

Rationale:
- makes the operational surface intentional and testable
- avoids ambiguity across local and deployed environments

Alternative considered:
- leaving actuator exposure implicit. Rejected because the requirement is explicit and the endpoint should remain verifiable.

## Risks / Trade-offs

- [Risk] Metric calls could become inconsistent across success, failure, and replay branches. → Mitigation: centralize the metric API and map each required metric to a small number of explicit call sites.
- [Risk] Structured logs can become noisy if emitted at the wrong level or too frequently. → Mitigation: log only meaningful business outcomes and worker publish attempts, not every intermediate step.
- [Risk] Operators may read metrics and audit logs as interchangeable. → Mitigation: keep the design clear that metrics are aggregate operational signals while audit logs remain durable domain records.
- [Risk] Timer coverage on both success and failure paths can be easy to miss. → Mitigation: wrap the public transfer service path in a timing helper that records duration in `finally` semantics.

## Migration Plan

1. Add management configuration for metrics endpoint exposure if it is not already explicit.
2. Introduce the shared observability component and wire it into wallet, reconciliation, and outbox flows.
3. Add structured log statements at the selected business outcome boundaries.
4. Add tests or smoke checks for metric registration and representative counter increments.
5. Deploy without API migration because this is an operational-only change.

Rollback strategy:
- remove or disable the new observability component and management exposure changes if they create unacceptable noise or overhead
- business behavior remains unchanged, so rollback is operational rather than data-migratory

## Open Questions

- No open product questions for this scope. The metric set, timer, structured log identifiers, and actuator exposure are sufficiently defined for implementation.
