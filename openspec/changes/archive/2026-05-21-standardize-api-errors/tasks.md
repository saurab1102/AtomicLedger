## 1. Error Contract And Exception Handling

- [x] 1.1 Add a shared API error response DTO and supporting detail model for field/header-level errors.
- [x] 1.2 Replace the current specialized wallet exception response handling with a global `@RestControllerAdvice` that returns the shared error contract.
- [x] 1.3 Map required exceptions to the specified HTTP statuses and error codes, including `MISSING_IDEMPOTENCY_KEY`, `WALLET_NOT_FOUND`, `UNSUPPORTED_CURRENCY`, `INSUFFICIENT_BALANCE`, and `INVALID_TRANSFER_TARGET`.
- [x] 1.4 Ensure bean-validation failures populate field-level `details` and include a response `timestamp`.

## 2. Endpoint Behavior Preservation

- [x] 2.1 Keep successful wallet, deposit, transfer, and duplicate-idempotency replay responses unchanged while routing their failure cases through the shared error contract.
- [x] 2.2 Verify wallet-history lookup failures and wallet creation validation failures also use the standardized error shape.

## 3. Verification

- [x] 3.1 Add an integration test verifying validation failures use the standardized error response shape with field-level details.
- [x] 3.2 Add an integration test verifying wallet-not-found failures return `404` with `errorCode = WALLET_NOT_FOUND`.
- [x] 3.3 Add an integration test verifying insufficient-balance transfer failures return `409` with `errorCode = INSUFFICIENT_BALANCE`.
- [x] 3.4 Add integration tests verifying missing idempotency-key and unsupported-currency failures return the required `400` error codes.
