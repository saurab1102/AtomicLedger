## ADDED Requirements

### Requirement: Document audit-log inspection in OpenAPI
The system SHALL document the audit-log inspection API in OpenAPI, including its optional query parameters and list response body.

#### Scenario: Audit-log filters are documented
- **WHEN** a developer inspects `GET /api/v1/audit-logs` in the OpenAPI docs
- **THEN** the documentation describes the optional `entityType` and `entityId` query parameters and the audit-log response items
