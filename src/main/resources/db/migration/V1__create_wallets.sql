CREATE TABLE wallets (
    id UUID PRIMARY KEY,

    owner_reference VARCHAR(255) NOT NULL,

    currency VARCHAR(3) NOT NULL,

    available_balance NUMERIC(19, 2) NOT NULL,

    status VARCHAR(32) NOT NULL
);
