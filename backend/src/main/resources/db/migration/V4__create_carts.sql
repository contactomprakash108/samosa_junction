CREATE TABLE carts (
    user_id    UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    payload    JSONB        NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);
