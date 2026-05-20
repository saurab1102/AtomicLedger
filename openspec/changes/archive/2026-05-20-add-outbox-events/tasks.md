## 1. Persistence Model

- [x] 1.1 Add the Flyway migration and JPA entity for `outbox_events` with event metadata, payload, status, retry fields, and timestamps.
- [x] 1.2 Add outbox enums and repository methods for listing events and selecting `PENDING` events for publication.

## 2. Domain Event Recording

- [x] 2.1 Add a shared outbox event service that writes `PENDING` events inside the current transaction with serialized payload data.
- [x] 2.2 Record outbox events for successful wallet creation and successful deposits in the same transaction as the state mutation.
- [x] 2.3 Record outbox events for successful transfers and refactor insufficient-balance transfer handling so the failure event is persisted in the same transaction as the failure outcome.
- [x] 2.4 Record an outbox event for failed reconciliation runs in the reconciliation transaction.

## 3. Publication Worker And API

- [x] 3.1 Add a scheduled outbox worker that reads `PENDING` events, logs payloads as the publish step, marks successful events as `PUBLISHED`, and sets `publishedAt`.
- [x] 3.2 Update failed publish attempts to increment `attemptCount` and store `lastError` while keeping the event retryable.
- [x] 3.3 Add `GET /api/v1/outbox-events` response models and controller/service support for inspecting stored outbox events.

## 4. Verification

- [x] 4.1 Add integration tests verifying wallet creation, deposit, and transfer each create the expected outbox event.
- [x] 4.2 Add an integration test verifying an insufficient-balance transfer creates the expected failure outbox event.
- [x] 4.3 Add an integration test verifying the outbox worker publishes a pending event and marks it as `PUBLISHED`.
