## Context

AtomicLedger already exposes the main ingredients that make wallet backends interesting under load: authenticated API access, idempotent deposits and transfers, row-level locking for concurrent transfer safety, and durable audit/outbox side effects. What the repo does not yet have is a lightweight, repository-native way to drive those behaviors repeatedly from a local developer machine.

This change is intentionally about local evaluation rather than performance certification. The load script should be easy to run against `localhost:8080`, should work with the existing `X-API-Key` requirement, and should let an engineer choose between bootstrapping fresh wallets or pointing the script at pre-created wallet IDs. It also needs to demonstrate duplicate idempotency-key replays in a controlled way because that is one of the more interesting parts of AtomicLedger’s behavior under retry pressure.

## Goals / Non-Goals

**Goals:**
- Add a k6 script at `load-tests/atomicledger-transfer-load.js`.
- Target a locally running AtomicLedger instance on `http://localhost:8080` by default.
- Send authenticated requests using `X-API-Key`.
- Support either creating wallets dynamically or using configured wallet IDs.
- Exercise deposit and transfer requests, including repeated transfer attempts and practical duplicate idempotency replays.
- Document how to run the script from the project README.

**Non-Goals:**
- Introduce Gatling, JMeter, Dockerized k6, or any new server-side performance feature.
- Change application behavior, API contracts, or production runtime settings.
- Guarantee production-grade benchmarking accuracy or automated pass/fail performance thresholds.

## Decisions

### Use a single k6 script with environment-driven setup
The load test should live in one script and rely on environment variables for configuration such as base URL, API key, wallet IDs, and whether wallets should be created automatically. That keeps the entry point simple and makes the script usable both for quick local runs and for more deliberate experiments with pre-seeded data.

Alternative considered:
- Separate bootstrap and load scripts: rejected because it adds operational friction for a repo that only needs one local load-testing path right now.

### Default to localhost and configuration compatible with current auth
The script should default to `http://localhost:8080` and require an `X-API-Key` value from either a default local setting or explicit environment overrides. This matches the current local app configuration and avoids forcing script edits just to get started.

Alternative considered:
- Hardcode wallet IDs or a fixed API key in the script: rejected because it makes the script brittle across machines and hides the fact that auth is part of the exercised request path.

### Cover both steady-state transfers and idempotency replay behavior
The script should not only fire nominal deposits and transfers, but also include a practical path for repeated transfer attempts and duplicate idempotency-key replays. That produces traffic that reflects the project’s main correctness concerns rather than measuring only the “happy path.”

Alternative considered:
- Only send unique transfer requests: rejected because it misses an important part of AtomicLedger’s intended behavior under retries.

### Keep documentation in the main README
How to run the k6 script should be documented in the main README rather than only in script comments. This keeps the portfolio-oriented entry point self-contained and makes the new capability discoverable without opening the load-test source first.

Alternative considered:
- Add a separate load-testing README only: rejected because it is easier for users to miss and duplicates setup context already present in the main README.

## Risks / Trade-offs

- [Local load tests can be misread as authoritative benchmarks] → Mitigate by documenting that the script is for local exercise and behavior inspection, not production capacity certification.
- [Auto-created wallets can leave behind test data across repeated runs] → Mitigate by making wallet creation configurable so engineers can reuse pre-created wallets when they want tighter control.
- [Duplicate idempotency replay traffic can skew naive “success rate” interpretation] → Mitigate by documenting that some repeated requests intentionally exercise replay behavior rather than distinct business operations.
- [The script depends on k6 being installed locally] → Mitigate by keeping the run instructions short and explicit in the README.
