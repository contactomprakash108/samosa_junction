CREATE TABLE wallets (
    id            UUID PRIMARY KEY,
    user_id       UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    balance_paise INTEGER      NOT NULL,
    currency      VARCHAR(3)   NOT NULL,
    version       BIGINT       NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_wallets_user_id UNIQUE (user_id),
    CONSTRAINT ck_wallets_balance CHECK (balance_paise >= 0)
);

CREATE TABLE wallet_transactions (
    id               UUID PRIMARY KEY,
    wallet_id        UUID         NOT NULL REFERENCES wallets (id) ON DELETE CASCADE,
    type             VARCHAR(16)  NOT NULL,
    amount_paise     INTEGER      NOT NULL,
    idempotency_key  VARCHAR(128) NOT NULL,
    reference_id     VARCHAR(64),
    status           VARCHAR(16)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_wallet_transactions_amount CHECK (amount_paise > 0),
    CONSTRAINT uk_wallet_transactions_idempotency UNIQUE (wallet_id, idempotency_key)
);

CREATE INDEX idx_wallet_transactions_wallet_created
    ON wallet_transactions (wallet_id, created_at DESC);

INSERT INTO wallets (id, user_id, balance_paise, currency, version, created_at, updated_at)
VALUES (
    '00000000-0000-4000-8000-000000000010',
    '00000000-0000-4000-8000-000000000001',
    0,
    'INR',
    0,
    NOW(),
    NOW()
);
