## Context

AtomicLedger already supports wallet creation and idempotent deposits, with transaction records, ledger entries, and wallet balances persisted in PostgreSQL. Transfers are the next step because they move value between two wallets instead of only increasing one wallet’s balance, which means the system now has to validate two resources and apply a debit/credit pair atomically.

This change keeps the same core constraints as deposits: idempotent retries, stable validation errors, and one database transaction covering all persisted effects. Unlike the deposit change, this one explicitly does not add concurrency locking yet, so the design needs to stay correct in single-request execution while acknowledging that stronger concurrent safety is deferred.

## Goals / Non-Goals

**Goals:**
- Add `POST /api/v1/transfers` with required `Idempotency-Key`.
- Validate source and destination wallet existence, `ACTIVE` status, wallet distinctness, positive amount, supported `INR` currency, wallet currency matching, and sufficient source balance.
- Persist a successful transfer as one `TRANSFER` transaction with status `SUCCEEDED`.
- Persist exactly two ledger entries for each successful transfer: one `DEBIT` for the source wallet and one `CREDIT` for the destination wallet.
- Decrease the source wallet balance and increase the destination wallet balance in the same database transaction as the transaction and ledger entries.
- Return the original transaction result for duplicate idempotency-key reuse.
- Cover the behavior with PostgreSQL-backed integration tests.

**Non-Goals:**
- Concurrency locking improvements for simultaneous transfers.
- Reconciliation jobs or balance-vs-ledger verification workflows.
- Failed transfer recovery, reversals, or dispute handling.
- Multi-currency support beyond enforcing `INR`.

## Decisions

### Reuse the existing transaction and ledger model for transfers
Transfers will reuse the existing `transactions` and `ledger_entries` tables, extending the transaction type enum to include `TRANSFER` and the ledger entry type enum to include `DEBIT`.

Rationale:
- Transfers are the same class of business event as deposits: they create a transaction record and one or more ledger entries.
- Reusing the same persistence model keeps the accounting surface consistent and avoids introducing a parallel schema for another kind of money movement.

Alternatives considered:
- Creating dedicated transfer tables was rejected because the existing transaction/ledger structure already represents the right concepts.
- Recording transfer effects on wallets only was rejected because it would drop auditability and traceability.

### Model a transfer as one transaction with two wallet-linked ledger entries
Each successful transfer will produce one `TRANSFER` transaction and exactly two ledger entries: a `DEBIT` tied to the source wallet and a `CREDIT` tied to the destination wallet.

Rationale:
- One transaction row captures the single business event, while two ledger rows capture the dual accounting effect.
- This is the first place where the project expresses paired movement rather than single-wallet balance increase.

Alternatives considered:
- Creating separate transaction rows for debit and credit was rejected because the user action is one transfer, not two independent operations.

### Persist source and destination balance snapshots on the transfer transaction result
The transfer response needs to be replayable under idempotency, so the implementation should persist the resulting balances needed to reconstruct the original transfer result even if later activity changes either wallet.

Rationale:
- Returning current wallet balances on replay would violate the requirement to return the original transaction result.
- A persisted result snapshot keeps duplicate responses deterministic.

Alternatives considered:
- Recomputing the response from live wallet state was rejected because it becomes wrong after later deposits or transfers.

### Validate transfer eligibility before applying wallet mutations
The transfer service will load both wallets, confirm they exist and are `ACTIVE`, reject same-wallet transfers, verify the requested currency matches both wallets, and ensure the source wallet has sufficient balance before creating the transfer transaction.

Rationale:
- Transfers involve more preconditions than deposits, and centralizing them in the service keeps the controller thin and the failure modes explicit.
- This mirrors the validation style already established for wallet creation and deposits.

Alternatives considered:
- Splitting these checks across controller and repository layers was rejected because business rules become harder to follow and test.

### Keep transfer persistence atomic without adding concurrency locking yet
The transfer service will still wrap transaction creation, both ledger entries, and both wallet balance updates in one database transaction, but it will not add explicit row locking in this change.

Rationale:
- Atomic commit is required by the feature even without concurrency enhancements.
- The user explicitly asked to defer locking, so the design should avoid introducing pessimistic or optimistic lock management here.

Alternatives considered:
- Adding row-level locking now was rejected because it conflicts with the requested scope.
- Deferring atomic updates was rejected because that would break the core transfer invariant immediately.

### Keep idempotency-key enforcement aligned with the current transaction model
Transfer requests will keep using the existing transaction-backed idempotency pattern: look up by `Idempotency-Key`, reuse the original transaction result if present, otherwise attempt creation and recover from duplicate-key conflicts by reading back the stored transaction.

Rationale:
- This matches the deposit implementation pattern and keeps retry behavior consistent across money-movement endpoints.
- It minimizes the number of new architectural ideas introduced in one change.

Alternatives considered:
- Introducing a separate idempotency registry just for transfers was rejected as unnecessary complexity for the current stage.

## Risks / Trade-offs

- Without explicit locking, concurrent transfers can still race on the same source balance → Mitigation: document the limitation in the design and defer locking to a later change as requested.
- Reusing the current global idempotency-key uniqueness scope may be stricter than ideal for future operations → Mitigation: keep the implementation consistent with current behavior and revisit key scoping in a dedicated follow-up if needed.
- Persisting transfer result snapshots adds denormalized historical data → Mitigation: treat those fields as immutable replay metadata, not live sources of truth.
- Paired ledger entries increase the number of invariants per operation → Mitigation: verify counts and debit/credit roles explicitly in integration tests.

## Migration Plan

1. Add a Flyway migration that extends the transaction and ledger schema as needed for transfer transaction types and any additional replay/result fields.
2. Deploy repository, service, controller, and exception-handling changes together so transfer writes are only possible once the schema and logic agree.
3. Run PostgreSQL-backed integration tests covering successful transfer, replay, and validation failures before allowing client use.
4. Follow up with a later change for concurrency locking and reconciliation instead of expanding this release’s scope.

## Open Questions

- No blocking open questions for this change. The exact transfer response shape can be finalized during implementation as long as it supports deterministic idempotent replay of the original transfer result.
