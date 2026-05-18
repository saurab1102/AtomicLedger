## Context

AtomicLedger already persists wallets, transactions, ledger entries, and reconciliation results exposed through APIs, but it does not keep a dedicated audit trail describing important domain actions. The new requirement spans multiple flows: wallet creation, deposits, transfers, and reconciliation all need to emit structured audit events, and some of those events must be committed atomically with the state changes they describe.

This makes audit logging a cross-cutting concern. The design needs to preserve transactional guarantees for mutating operations while still supporting read access through `GET /api/v1/audit-logs` with simple filtering.

## Goals / Non-Goals

**Goals:**
- Persist structured audit records for the required domain actions.
- Commit audit logs in the same transaction as mutating domain operations when state changes occur.
- Record important non-mutating events such as duplicate idempotency replays and reconciliation outcomes.
- Expose audit logs through a filterable API.
- Add integration coverage that proves the required audit events are written.

**Non-Goals:**
- Emitting outbox events or integrating with external log sinks.
- Adding Redis rate limiting or any request-throttling mechanism.
- Implementing a generic actor/identity model for who triggered an action.
- Building pagination, retention, or advanced search beyond `entityType` and `entityId`.

## Decisions

### Use a dedicated `audit_logs` table with JSON metadata
Audit records will be stored in a dedicated table with a small set of indexed audit dimensions (`action`, `entityType`, `entityId`, `createdAt`) plus `metadata` for action-specific details.

Alternative considered:
- Reusing transaction or ledger tables for audit history. Rejected because many required events, such as wallet creation and failed transfer attempts, do not map cleanly to those accounting tables.

### Centralize audit writing behind an audit service
Application flows will call a single audit-writing service/component to create audit rows consistently and keep action naming and metadata shape localized.

Alternative considered:
- Building audit rows inline in each controller or service method. Rejected because it would duplicate event construction logic and make audit behavior harder to evolve.

### Write mutating-operation audit logs inside the same transaction
Wallet creation, successful deposits, successful transfers, and insufficient-balance transfer failures will write audit rows inside the same transaction scope as the domain operation, so the state change and audit trail commit or roll back together.

Alternative considered:
- Writing all audit logs after the fact in separate transactions. Rejected because it breaks the atomicity requirement for mutating operations.

### Treat duplicate idempotency replays as explicit audit events
Duplicate deposit and transfer requests will write audit records even though they do not mutate state, because the requirements call them out as important domain actions in their own right.

Alternative considered:
- Suppressing duplicate replay audits to avoid extra rows. Rejected because it would hide an important class of client behavior that the requirements explicitly want captured.

### Keep audit-log querying simple and filter-driven
`GET /api/v1/audit-logs` will support optional `entityType` and `entityId` filters and return matching audit records ordered for inspection.

Alternative considered:
- More advanced search or pagination-first design. Rejected because the current requirement only asks for basic filtering and this keeps the first version focused.

## Risks / Trade-offs

- [Audit writes increase write-path volume] → Keep the schema compact and only record the explicitly required events in this change.
- [Metadata shape can drift across events] → Centralize audit creation and define stable action names and metadata conventions in one place.
- [Duplicate replay auditing can create many rows under noisy clients] → Accept this because visibility into replay behavior is a stated requirement.
- [Audit log queries may grow over time] → Start with simple filters and ordering, and defer pagination/retention concerns until usage justifies them.
