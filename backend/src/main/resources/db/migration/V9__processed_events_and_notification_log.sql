
CREATE TABLE processed_domain_events (
    event_id       UUID         NOT NULL,
    consumer_name  VARCHAR(64)  NOT NULL,
    processed_at   TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (event_id, consumer_name)
);

CREATE TABLE notification_log (
    id             UUID         PRIMARY KEY,
    event_id       UUID         NOT NULL,
    event_type     VARCHAR(64)  NOT NULL,
    channel        VARCHAR(16)  NOT NULL,
    recipient_key  VARCHAR(128) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_notification_log_event_channel UNIQUE (event_id, channel)
);
