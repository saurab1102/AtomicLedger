## 1. Metrics And Management Setup

- [x] 1.1 Add explicit management configuration so the actuator metrics endpoint is exposed for inspection.
- [x] 1.2 Introduce a shared observability component that owns the required metric names, counters, and transfer timer registration through Micrometer.

## 2. Business Flow Instrumentation

- [x] 2.1 Instrument wallet creation, deposit success, deposit duplicate replay, transfer success, transfer failure, and transfer duplicate replay flows with the required counters.
- [x] 2.2 Record transfer processing duration with the shared timer across the handled transfer paths.
- [x] 2.3 Add structured logs for deposit and transfer outcomes with `walletId`, `transactionId`, and `idempotencyKey` where available.

## 3. Reconciliation And Outbox Instrumentation

- [x] 3.1 Instrument reconciliation runs and reconciliation failures with the required counters and structured logs including `reconciliationStatus`.
- [x] 3.2 Instrument outbox publish success and failure with the required counters and structured logs including `outboxEventId`.

## 4. Verification

- [x] 4.1 Add automated verification for representative metric registration or increment behavior in integration or smoke tests.
- [x] 4.2 Add a practical check that the actuator metrics endpoint is reachable without changing business API behavior.
