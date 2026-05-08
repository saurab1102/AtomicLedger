# AtomicLedger

AtomicLedger is a production-style backend system for wallet transfers using idempotency keys, database transactions, row-level locking, immutable ledger entries, reconciliation checks, and audit logs.

The goal is to model money movement safely, where duplicate requests, concurrent transfers, failed transactions, and inconsistent balances are handled deliberately.

## Local development

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
./mvnw spring-boot:run
```

Run the tests:

```bash
./mvnw test
```
