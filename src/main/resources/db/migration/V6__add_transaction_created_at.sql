ALTER TABLE transactions
ADD COLUMN created_at TIMESTAMPTZ;

UPDATE transactions
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

ALTER TABLE transactions
ALTER COLUMN created_at SET NOT NULL;

CREATE INDEX transactions_created_at_idx ON transactions (created_at);
