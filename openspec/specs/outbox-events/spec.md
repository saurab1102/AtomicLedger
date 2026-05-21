# outbox-events Specification

## Purpose
TBD - created by archiving change add-outbox-events. Update Purpose after archive.
## Requirements
### Requirement: Persist transactional outbox events
The system SHALL persist publishable domain events in PostgreSQL using an `outbox_events` table containing `id`, `eventType`, `aggregateType`, `aggregateId`, `payload`, `status`, `attemptCount`, `createdAt`, `publishedAt`, and `lastError`.

#### Scenario: Outbox event is stored with pending status
- **WHEN** the system records a new outbox event for a domain operation
- **THEN** it stores exactly one outbox row with the required fields and `status` set to `PENDING`

### Requirement: Commit outbox events atomically with domain operations
The system SHALL persist each required outbox event in the same database transaction as the corresponding domain operation that produced it.

#### Scenario: Successful transfer commits with its outbox event
- **WHEN** a transfer request succeeds
- **THEN** the transfer state changes and its outbox event are committed atomically in one database transaction

#### Scenario: Failed transfer commits failure outcome with its outbox event
- **WHEN** a transfer request is rejected due to insufficient balance
- **THEN** the failure outcome and its outbox event are persisted in the same database transaction

### Requirement: Publish pending outbox events with a scheduled worker
The system SHALL run a scheduled worker that selects `PENDING` outbox events, attempts publication for each event, marks successfully published events as `PUBLISHED`, and sets `publishedAt` when publication succeeds.

#### Scenario: Pending outbox event is published
- **WHEN** the scheduled worker processes a `PENDING` outbox event and publication succeeds
- **THEN** the system logs the event payload, updates the row status to `PUBLISHED`, and stores the publish timestamp

### Requirement: Track publication failures for retry
The system SHALL increment `attemptCount` and store `lastError` whenever a publish attempt fails, while keeping the outbox event eligible for later retry.

#### Scenario: Publish attempt fails
- **WHEN** the scheduled worker attempts to publish a `PENDING` outbox event and the publish step throws an error
- **THEN** the system increments `attemptCount`, stores the error detail in `lastError`, and leaves the event in a retryable state

### Requirement: Expose outbox events for inspection
The system SHALL provide `GET /api/v1/outbox-events` to return stored outbox events for inspection.

#### Scenario: Outbox events are listed
- **WHEN** a client sends `GET /api/v1/outbox-events`
- **THEN** the system returns stored outbox events including their aggregate metadata, payload, status, attempt count, and timestamps

### Requirement: Document outbox-event inspection in OpenAPI
The system SHALL document the outbox-event inspection API in OpenAPI, including its list response body and the outbox-event fields exposed to clients.

#### Scenario: Outbox-event response is documented
- **WHEN** a developer inspects `GET /api/v1/outbox-events` in the OpenAPI docs
- **THEN** the documentation describes the outbox-event response items returned by the endpoint
