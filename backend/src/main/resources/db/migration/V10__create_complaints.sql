
CREATE TABLE complaints (
    id               UUID PRIMARY KEY,
    user_id          UUID         NOT NULL REFERENCES users (id),
    order_id         UUID         NOT NULL REFERENCES orders (id),
    category         VARCHAR(32)  NOT NULL,
    description      VARCHAR(1000) NOT NULL,
    status           VARCHAR(16)  NOT NULL,
    priority         VARCHAR(16)  NOT NULL,
    idempotency_key  VARCHAR(128) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_complaints_user_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_complaints_user_created ON complaints (user_id, created_at DESC);
CREATE INDEX idx_complaints_order_id ON complaints (order_id);
CREATE INDEX idx_complaints_status ON complaints (status);
