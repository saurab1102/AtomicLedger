ALTER TABLE transactions
    ADD COLUMN counterparty_wallet_id UUID REFERENCES wallets(id),
    ADD COLUMN counterparty_resulting_available_balance NUMERIC(19, 2);

ALTER TABLE ledger_entries
    DROP CONSTRAINT IF EXISTS ledger_entries_transaction_id_key;
