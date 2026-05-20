## Context

AtomicLedger already persists wallets, transactions, ledger entries, and audit logs inside Spring-managed transactions. Wallet creation uses `@Transactional`, deposit and transfer use a `TransactionTemplate` to contain idempotent write flows, and reconciliation executes in a single transactional service method. That makes the project a good fit for the transactional outbox pattern: the same places that commit domain state can also commit an outbox record without introducing external infrastructure yet.

The requested scope is intentionally narrow. We need durable outbox persistence, a simple scheduled publisher that logs payloads instead of talking to Kafka/SQS/Redis, an inspection endpoint, and integration coverage for event creation plus worker publication. We do not need distributed delivery guarantees beyond retry metadata and status tracking inside the database.

## Goals / Non-Goals

**Goals:**
- Persist outbox events in PostgreSQL with the full required schema and `PENDING` as the initial status.
- Write outbox events inside the same transaction as wallet creation, successful deposits, successful transfers, failed transfers, and failed reconciliations.
- Provide a scheduled worker that reads pending events, logs them as the current publish mechanism, marks successful publishes as `PUBLISHED`, and records retry metadata on failure.
- Expose `GET /api/v1/outbox-events` so stored outbox rows can be inspected during development and testing.
- Keep the design compatible with a future real publisher by separating event creation from event publication.

**Non-Goals:**
- Integrating Kafka, SQS, Redis, or any other external broker.
- Building exactly-once delivery semantics across process crashes or multi-node coordination.
- Publishing duplicate deposit replay or duplicate transfer replay events unless a later change explicitly requires them.
- Replacing existing audit logs; outbox events and audit logs will coexist.

## Decisions

### Store outbox records as a first-class persisted model
Add a Flyway migration for `outbox_events` and introduce an `OutboxEvent` JPA entity plus repository. The table will include `id`, `event_type`, `aggregate_type`, `aggregate_id`, `payload`, `status`, `attempt_count`, `created_at`, `published_at`, and `last_error`.

Why:
- It mirrors the existing Flyway plus JPA pattern used by wallets, transactions, and audit logs.
- It keeps event state queryable for the inspection endpoint and tests.
- It creates a clean seam for later swapping the publisher implementation without changing domain services.

Alternative considered:
- Reusing the audit log table for publishable events. Rejected because audit logs and outbox events serve different consumers, lifecycle fields, and retry semantics.

### Centralize event creation in an `OutboxEventService`
Create a dedicated service with a method equivalent to `recordInCurrentTransaction(...)` that serializes the payload map and persists a `PENDING` outbox record. Domain services will call it directly inside their existing transactions.

Why:
- It keeps outbox event shape consistent across wallet, transfer, and reconciliation flows.
- It matches the project’s existing `AuditLogService` pattern, making the implementation easier to reason about.
- It localizes defaults such as status initialization, timestamps, and payload serialization.

Alternative considered:
- Constructing and saving outbox entities inline in every domain service. Rejected because it would duplicate payload, status, and timestamp logic and make later publisher changes harder.

### Emit events only for the requested domain outcomes
Map outbox creation to the specific outcomes in scope:
- wallet created
- deposit succeeded
- transfer succeeded
- transfer failed due to insufficient balance
- reconciliation failed

Why:
- It aligns the implementation with the user’s explicit requirements.
- It avoids broadening the event contract prematurely, especially around duplicate idempotency replays and successful reconciliation runs.

Alternative considered:
- Emitting events for every audit-loggable action. Rejected because the current request is intentionally narrower and should not silently expand behavior.

### Keep failure events transactional by recording them before throwing
For insufficient-balance transfers and failed reconciliations, persist the outbox event inside the same active transaction before the service returns or throws. For transfers, that means recording the failure event in the transaction block immediately before raising the domain exception. For reconciliation, record the failure event in the same transactional method once failed checks are known.

Why:
- This satisfies the requirement that the failure outbox entry shares the domain transaction boundary.
- It avoids the current audit-log pattern of recording some failures after the transaction has unwound.

Alternative considered:
- Recording failure events in a separate transaction after the exception is caught. Rejected because it violates the transactional outbox requirement.

### Use a scheduled poller with row-level status updates
Add a scheduled worker that periodically queries pending outbox rows in created-time order, attempts publication by logging the payload, then updates the row to `PUBLISHED` with `publishedAt`. If publish logic throws, increment `attemptCount` and store `lastError` while leaving the row in `PENDING`.

Why:
- It is the simplest useful outbox worker for the current scope.
- It provides visible state transitions for tests and manual inspection.
- Leaving failed rows as `PENDING` keeps retry behavior straightforward until a richer state machine is needed.

Alternative considered:
- Introducing a separate `FAILED` terminal status. Rejected for now because the current requirement only asks for retry counting on failure, not dead-letter handling.

### Expose a read-only inspection endpoint
Add `GET /api/v1/outbox-events` with a response model that surfaces the persisted outbox fields. Ordering should favor newest events first to match the inspection style used by audit logs.

Why:
- The endpoint is explicitly requested.
- It makes integration tests and local debugging simpler without needing direct database access.

Alternative considered:
- Omitting the endpoint and relying on repository assertions in tests. Rejected because inspectability is a product requirement, not just a test convenience.

## Risks / Trade-offs

- [Transactional failure events require refactoring current control flow] -> Move insufficient-balance event recording into the transactional transfer path instead of the current post-exception audit pattern.
- [A simple polling worker can republish the same event under concurrent schedulers] -> Keep the first version scoped to the current single-app setup and design repository methods so row locking or bounded batches can be added later.
- [Logging as the publisher does not validate external delivery semantics] -> Treat the logged publish step as a temporary adapter behind a service boundary so Kafka/SQS integration can replace it later.
- [Storing arbitrary payloads may create schema drift across event types] -> Keep payloads structured and minimal, and define eventType plus aggregate metadata clearly in specs and tests.

## Migration Plan

1. Add the Flyway migration and JPA model for `outbox_events`.
2. Introduce outbox enums, repository access, response DTOs, and the inspection endpoint.
3. Add `OutboxEventService` and wire event creation into wallet creation, deposit, transfer, and reconciliation flows.
4. Refactor the insufficient-balance transfer path so the failure outbox event is persisted inside the transaction before the exception exits the service.
5. Add the scheduled worker and publisher abstraction that logs payloads and updates status fields.
6. Add integration tests for event creation and worker publication.

Rollback:
- Code rollback is straightforward because all changes are additive.
- The new table can remain unused if the code is rolled back; no destructive migration is required.

## Open Questions

- Should the inspection endpoint support filtering by `status`, `aggregateType`, or `aggregateId` now, or only return all rows? Current scope assumes a simple list endpoint unless implementation convenience suggests adding lightweight filters.
- Should failed reconciliation emit one event per failed check or one event per run? Current scope assumes one event per failed run with summary payload because the requirement asks for failed reconciliation events, not per-check fanout.
