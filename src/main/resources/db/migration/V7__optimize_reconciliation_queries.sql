CREATE INDEX IF NOT EXISTS idx_ledger_entries_wallet_id
    ON ledger_entries (wallet_id);

CREATE INDEX IF NOT EXISTS idx_ledger_entries_transaction_id
    ON ledger_entries (transaction_id);

CREATE INDEX IF NOT EXISTS idx_transactions_status
    ON transactions (status);

CREATE INDEX IF NOT EXISTS idx_transactions_transaction_type_status
    ON transactions (transaction_type, status);
