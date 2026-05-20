## Why

AtomicLedger currently persists domain state and audit logs, but it has no durable handoff mechanism for downstream event publication. Adding an outbox now gives the system a reliable, transactionally consistent event stream foundation before external brokers are introduced.

## What Changes

- Add an `outbox_events` table through Flyway with event identity, aggregate metadata, payload, publication status, retry fields, and timestamps.
- Create outbox events for successful wallet creation, successful deposit, successful transfer, failed transfer, and failed reconciliation.
- Persist each outbox event in the same database transaction as the corresponding domain operation so emitted events stay consistent with committed state.
- Add a scheduled outbox worker that selects `PENDING` events, attempts publication by logging the payload, marks successful publishes as `PUBLISHED`, and increments `attemptCount` plus `lastError` on failure.
- Add `GET /api/v1/outbox-events` for inspection of stored outbox events.
- Add integration coverage for outbox event creation on wallet creation, deposit, transfer, failed transfer, and worker-driven publication.

## Capabilities

### New Capabilities
- `outbox-events`: persist, inspect, and publish transactional outbox events for important domain operations

### Modified Capabilities
- `wallet-management`: wallet creation now guarantees an outbox event is persisted with the created wallet action
- `wallet-deposit`: successful deposits now guarantee an outbox event is persisted with the committed deposit
- `wallet-transfer`: successful transfers and insufficient-balance transfer failures now guarantee outbox events are persisted
- `reconciliation-checks`: failed reconciliation runs now guarantee an outbox event is persisted

## Impact

- Adds a new Flyway migration, outbox persistence model, repository, service, scheduled worker, and inspection API.
- Threads outbox event creation through wallet, deposit, transfer, and reconciliation service flows alongside existing transactional logic.
- Expands the integration suite to verify both outbox creation and publication state transitions without introducing Kafka, SQS, or Redis.
