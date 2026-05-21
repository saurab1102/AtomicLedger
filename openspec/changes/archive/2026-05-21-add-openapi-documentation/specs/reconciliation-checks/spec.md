## ADDED Requirements

### Requirement: Document reconciliation execution in OpenAPI
The system SHALL document the reconciliation API in OpenAPI, including the operation for triggering a run and the reconciliation response body.

#### Scenario: Reconciliation endpoint is documented
- **WHEN** a developer inspects `POST /api/v1/reconciliation/run` in the OpenAPI docs
- **THEN** the documentation describes the reconciliation response contract returned by the endpoint
