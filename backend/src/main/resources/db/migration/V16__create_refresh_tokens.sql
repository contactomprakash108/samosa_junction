CREATE TABLE refresh_tokens (
    id                    UUID PRIMARY KEY,
    user_id               UUID         NOT NULL REFERENCES users (id),
    token_hash            VARCHAR(64)  NOT NULL,
    expires_at            TIMESTAMPTZ  NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL,
    revoked_at            TIMESTAMPTZ,
    replaced_by_token_id  UUID         REFERENCES refresh_tokens (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);
