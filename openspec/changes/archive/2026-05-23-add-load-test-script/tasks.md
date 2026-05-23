## 1. Load Test Script

- [x] 1.1 Create `load-tests/atomicledger-transfer-load.js` with k6 defaults targeting local AtomicLedger on `localhost:8080`.
- [x] 1.2 Add script configuration for `X-API-Key`, optional preconfigured wallet IDs, and automatic wallet bootstrap when IDs are not supplied.
- [x] 1.3 Implement deposit, repeated transfer, and duplicate idempotency replay request paths in the k6 script.

## 2. Documentation

- [x] 2.1 Update the main README with concise instructions for running the k6 load test locally.
- [x] 2.2 Document the key runtime inputs for the script, including API key usage and wallet-ID configuration.

## 3. Verification

- [x] 3.1 Review the script and README together to confirm they match the current authenticated API surface and local defaults.
- [x] 3.2 Validate the OpenSpec change before marking it ready for implementation.
