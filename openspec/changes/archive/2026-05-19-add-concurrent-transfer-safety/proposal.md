## Why

Concurrent transfer requests can currently observe the same source wallet balance before either request updates it, which creates a risk of overspending. We need to harden transfers now that money can move between wallets and correctness under contention matters more than raw throughput.

## What Changes

- Add row-level locking to wallet-to-wallet transfer processing so concurrent requests cannot overspend the same source wallet.
- Lock both source and destination wallet rows within the same database transaction before balance validation and mutation.
- Acquire wallet locks in deterministic wallet ID order to reduce deadlock risk while preserving the existing transfer API contract.
- Add integration coverage that proves concurrent transfers of `800 INR` and `700 INR` from a `1000 INR` wallet allow only one success and never produce a negative source balance.

## Capabilities

### New Capabilities

### Modified Capabilities
- `wallet-transfer`: strengthen transfer behavior to guarantee balance safety under concurrent requests without changing the external API contract

## Impact

- Affects transfer orchestration in the wallet service and repository access patterns for wallet loading.
- Adds concurrency-focused integration testing using the existing Postgres-backed test stack.
- Uses Postgres row-level locking semantics already available through the persistence layer; no API or schema contract changes are required.
