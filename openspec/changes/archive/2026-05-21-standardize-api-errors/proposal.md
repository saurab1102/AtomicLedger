## Why

AtomicLedger currently returns several different error payload shapes depending on which validation or domain exception was raised. Standardizing API errors now will make the API easier to integrate with, easier to document, and easier to extend as more endpoints and failure cases are added.

## What Changes

- Add a common API error response contract with `errorCode`, `message`, `details`, and `timestamp`.
- Replace ad hoc controller error handling with a single global `@RestControllerAdvice`.
- Standardize validation failures so they include field-level detail entries in the common response shape.
- Assign explicit error codes and HTTP statuses for missing idempotency keys, wallet-not-found cases, unsupported currency, insufficient balance, and invalid transfer targets.
- Preserve existing successful response payloads and duplicate-idempotency replay behavior.

## Capabilities

### New Capabilities
- `api-errors`: Defines the shared JSON error contract and the rules for translating validation and domain exceptions into consistent API responses.

### Modified Capabilities
- `wallet-management`: Wallet creation and wallet lookup failures will use the shared API error contract.
- `wallet-deposit`: Deposit validation, missing idempotency-key handling, unsupported currency handling, and wallet-not-found failures will use the shared API error contract.
- `wallet-transfer`: Transfer validation, invalid-target handling, missing idempotency-key handling, unsupported currency handling, wallet-not-found failures, and insufficient-balance failures will use the shared API error contract.

## Impact

- Affected code will include controller advice, error DTOs, and integration tests for API failure responses.
- Error response bodies for unsuccessful requests will change to a standardized contract, while successful API responses remain unchanged.
- No new infrastructure or external dependencies are required.
