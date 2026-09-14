
CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY,
    event_id     UUID         NOT NULL,
    event_type   VARCHAR(64)  NOT NULL,
    aggregate_id UUID         NOT NULL,
    user_id      UUID         NOT NULL,
    status       VARCHAR(16)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ,
    CONSTRAINT uk_outbox_events_event_id UNIQUE (event_id)
);

CREATE INDEX idx_outbox_events_status_created ON outbox_events (status, created_at);

CREATE TABLE support_messages (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id),
    subject     VARCHAR(160) NOT NULL,
    body        VARCHAR(2000) NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_support_messages_user_created ON support_messages (user_id, created_at DESC);
CREATE INDEX idx_support_messages_status ON support_messages (status);
