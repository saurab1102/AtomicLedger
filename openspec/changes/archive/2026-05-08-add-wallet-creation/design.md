## Context

AtomicLedger is currently a minimal Spring Boot service with JPA, Flyway, Bean Validation, and PostgreSQL dependencies configured but no domain endpoints beyond the application bootstrap. The wallet creation feature establishes the first persisted business entity and the first externally consumable API under `/api/v1`, while staying aligned with the project invariant that wallet state changes must remain transaction-safe and backed by the database.

This change introduces a new data model, a database migration, request validation, and integration tests. Because later transfer and ledger capabilities will depend on wallets, the design should create a stable baseline without prematurely adding money-movement behavior.

## Goals / Non-Goals

**Goals:**
- Create wallets through `POST /api/v1/wallets`.
- Enforce `ownerReference` presence and restrict `currency` to `INR`.
- Persist wallets in PostgreSQL with UUID identifiers.
- Initialize new wallets with `availableBalance = 0` and `status = ACTIVE`.
- Return predictable validation errors suitable for integration testing.

**Non-Goals:**
- Deposits, withdrawals, transfers, or ledger entries.
- Balance mutation after wallet creation.
- Multi-currency support beyond validating `INR`.
- Lifecycle transitions beyond the initial `ACTIVE` status.

## Decisions

### Expose wallet creation as a versioned REST endpoint
The system will add a controller for `POST /api/v1/wallets` that accepts a JSON request and returns the created wallet resource.

Rationale:
- Establishes the project’s first public API using a versioned path that can evolve safely.
- Fits naturally with Spring Web MVC request validation and JSON serialization.

Alternatives considered:
- Exposing an unversioned `/wallets` route was rejected because the project is at the start of its API lifecycle and versioning is inexpensive now.
- Deferring the API until transfers exist was rejected because downstream features need a persisted wallet primitive first.

### Model wallet currency and status as enums in the application
The wallet domain will represent currency and status as constrained application enums, with `INR` as the only accepted currency value and `ACTIVE` as the default status.

Rationale:
- Keeps allowed values explicit in the domain model and prevents arbitrary strings from entering persistence.
- Makes unsupported currency validation straightforward and readable.

Alternatives considered:
- Using free-form strings with service-layer checks was rejected because it weakens type safety.
- Adding multi-currency support immediately was rejected because the current requirement is intentionally narrow.

### Store balances as a zero-initialized persisted field
The wallet table will include an `available_balance` column persisted with a zero value for every newly created wallet.

Rationale:
- Makes the initial state explicit and queryable, which is useful for later reconciliation and transfer logic.
- Aligns with the invariant that failed operations must not mutate wallet balances and that balance changes should be database-backed.

Alternatives considered:
- Deriving balance from ledger entries only was rejected for now because ledger functionality does not exist yet and the requirement explicitly calls for an available balance on creation.

### Use Flyway-managed PostgreSQL schema with UUID primary key
The migration will create a `wallets` table with a UUID primary key and required fields for owner reference, currency, balance, status, and audit timestamps if the implementation chooses to include them.

Rationale:
- Flyway is already part of the stack and is the expected migration mechanism for persistent schema evolution.
- UUID identifiers support externally visible, non-sequential wallet IDs without requiring coordination across services.

Alternatives considered:
- Numeric auto-increment IDs were rejected because the requirement explicitly calls for UUID and because opaque identifiers are a better fit for public APIs.

### Handle invalid input through framework-backed validation and a consistent error response
Bean Validation will enforce required fields and the application will map validation and domain input errors into a stable client-facing error shape.

Rationale:
- Keeps common validation close to the request DTO while allowing business validation, such as unsupported currency, to remain explicit.
- Enables the required integration tests to verify both status codes and meaningful validation failures.

Alternatives considered:
- Returning default framework error payloads without application mapping was rejected because they can vary and are less stable for API consumers and tests.

## Risks / Trade-offs

- Unsupported currency validation spans enum/domain concerns and request parsing behavior → Mitigation: accept a simple request payload and centralize currency validation so unsupported values return a controlled client error rather than an opaque server failure.
- Introducing a persisted balance before ledger support can create future synchronization responsibilities → Mitigation: codify the zero-initialization rule now and preserve room for later transactional balance updates tied to ledger entries.
- The first API endpoint sets conventions for future resources → Mitigation: keep versioning, validation, and error handling deliberate so later endpoints can reuse the same patterns.
- UUID handling and PostgreSQL column types can be inconsistent if left implicit → Mitigation: define the ID column type clearly in both entity mapping and Flyway migration.

## Migration Plan

1. Add a Flyway migration that creates the `wallets` table in PostgreSQL.
2. Deploy the application with the new controller, domain, repository, and validation handling.
3. Verify migrations apply successfully in test and runtime environments before enabling clients to call the endpoint.
4. Roll back by removing the application deployment and reverting the migration only in non-production environments; in production, prefer a follow-up migration instead of destructive rollback once data exists.

## Open Questions

- No blocking open questions for this first capability. Response payload shape can follow the application’s existing JSON defaults as long as it includes the created wallet state needed by clients and tests.
