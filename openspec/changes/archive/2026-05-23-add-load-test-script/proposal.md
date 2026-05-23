## Why

AtomicLedger already demonstrates correctness-oriented backend concerns such as idempotency, transfer locking, reconciliation, and outbox publishing, but it does not yet include a lightweight way to exercise those paths under concurrent request pressure. A small k6 script makes the project easier to evaluate locally by giving engineers a repeatable way to drive deposits, transfers, duplicate idempotency replays, and repeated transfer attempts against the running service.

## What Changes

- Add a k6 load test script at `load-tests/atomicledger-transfer-load.js` targeting local AtomicLedger on `localhost:8080`.
- Make the script send authenticated requests with `X-API-Key` and support either creating wallets dynamically or using configured wallet IDs.
- Exercise deposit and transfer flows, including repeated transfer attempts and duplicate idempotency-key replays where practical.
- Document how to run the load test from the project README without changing application behavior or introducing heavier load-testing tooling.

## Capabilities

### New Capabilities
- `load-testing`: Covers the local k6 load test asset, its configurable execution model, authenticated request setup, and the runbook for exercising transfer-heavy AtomicLedger behavior.

### Modified Capabilities

## Impact

- Affected code: new `load-tests/` script and README documentation only.
- Affected APIs: none; the script drives existing wallet, deposit, and transfer endpoints as a client.
- Dependencies/systems: k6 as an external local tool, plus the existing `X-API-Key` protected API surface.
