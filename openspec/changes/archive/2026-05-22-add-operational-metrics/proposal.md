## Why

AtomicLedger already models the hard parts of wallet operations correctly, but it does not yet expose enough operational signal to understand how those flows behave in production. Adding metrics and structured logs now makes the system easier to observe, debug, and reason about without changing API behavior.

## What Changes

- Add Micrometer-backed counters for key wallet, transfer, reconciliation, and outbox outcomes.
- Add a transfer processing duration timer.
- Add structured logs for deposit, transfer, reconciliation, and outbox publishing flows with the relevant IDs where available.
- Expose the actuator metrics endpoint so the new metrics can be inspected and scraped.
- Add practical verification for metric registration and increment behavior.

## Capabilities

### New Capabilities
- `operational-metrics`: Cross-cutting operational metrics and structured logging for core AtomicLedger flows, including actuator metrics exposure.

### Modified Capabilities

## Impact

- Affected code: wallet, reconciliation, and outbox services; actuator configuration; tests
- Dependencies/systems: Micrometer through Spring Boot Actuator, application logging output
- APIs: no business API contract changes; actuator metrics exposure becomes an explicit operational surface
