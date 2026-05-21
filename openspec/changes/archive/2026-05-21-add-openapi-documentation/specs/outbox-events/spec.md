## ADDED Requirements

### Requirement: Document outbox-event inspection in OpenAPI
The system SHALL document the outbox-event inspection API in OpenAPI, including its list response body and the outbox-event fields exposed to clients.

#### Scenario: Outbox-event response is documented
- **WHEN** a developer inspects `GET /api/v1/outbox-events` in the OpenAPI docs
- **THEN** the documentation describes the outbox-event response items returned by the endpoint
