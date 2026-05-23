# Load Testing and Reconciliation Optimization

## Context

AtomicLedger keeps a cached wallet balance on the wallet row. That is fine as long as we keep proving the cached value still matches the ledger.

Reconciliation does three checks:

- compares each wallet's cached balance with a balance derived from ledger entries
- verifies that successful transfers still have the expected `DEBIT` / `CREDIT` ledger structure
- verifies that successful deposits still have the expected `CREDIT` ledger structure

In v1 this is still a synchronous API call through `POST /api/v1/reconciliation/run`.

## Dataset Used

The issue showed up during local load testing against a database that contained approximately:

- 25 wallets
- 15,675 transactions
- 30,656 ledger entries
- 17,208 audit logs
- 15,684 outbox events

This was just the state of the database after running the service under local load and then trying reconciliation on that data.

## Problem Found

On that dataset, the reconciliation endpoint still had not returned after 600 seconds.

At that point the request had to be stopped manually, and the service had to be shut down. The endpoint was not usable.

## Investigation

The first guess was that PostgreSQL was stuck on one bad query or waiting on a lock.

That was not what the database was showing:

- PostgreSQL was not blocked
- `pg_stat_activity` did not show a long-running reconciliation query
- there was no obvious lock-waiting reconciliation statement to blame

That pushed the investigation back into the application code. The problem was not one bad query. It was the number of queries being issued.

## Root Cause

The original transfer and deposit checks had N+1 query behavior.

The flow looked roughly like this:

- load successful transactions
- for each transaction, fetch its ledger entries
- inspect the entries in Java to decide whether the structure or amounts were wrong

That is easy to miss on a small dataset. Once the tables grew, it turned into thousands of repository calls for work that should have been grouped in SQL.

Wallet balance reconciliation was already fine. It used a grouped ledger summary query. The slow part was the per-transaction ledger lookup pattern in the transfer and deposit phases.

## Fix

The fix was straightforward:

- keep wallet balance reconciliation on the existing aggregate ledger balance summary
- replace per-transaction ledger entry lookups with aggregate SQL queries
- add the indexes needed to support those access paths

The replacement queries summarize mismatches at the transaction level instead of pulling ledger rows through JPA one transaction at a time.

Indexes were added for:

- `ledger_entries(wallet_id)`
- `ledger_entries(transaction_id)`
- `wallet_transactions(status)` in domain terms, implemented on the `transactions(status)` table
- `wallet_transactions(transaction_type, status)` in domain terms, implemented on the `transactions(transaction_type, status)` table

The response also caps mismatch details per check type so one noisy failure mode does not blow up the payload.

## Result

On the same dataset, reconciliation dropped from over 600 seconds to milliseconds.

That is the important part. The original problem was structural, and the fix removed that structure.

## Design Notes

The original implementation was readable and the invariants were right, but it pushed a set-based problem into an object-by-object access pattern; easy to miss until the data volume gets large enough.

It also does not change the basic design choice. Keeping a cached wallet balance is still a reasonable tradeoff here. It just means reconciliation has to stay cheap enough that we can keep running it.

## Future Production Improvement

For larger production datasets, full reconciliation should not stay as one synchronous request.

A more realistic shape is:

- trigger an async background reconciliation job
- persist job status and summary results
- run checks in batches instead of one all-or-nothing request
- cap returned mismatch details while keeping enough context for investigation

That would make it easier to schedule, observe, and retry. For now, v1 stays synchronous because the simpler shape is easier to reason about, and the optimized queries make it fast enough on the current dataset.
