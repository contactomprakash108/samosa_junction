
CREATE TABLE staff_audit_events (
    id          UUID PRIMARY KEY,
    actor_id    UUID         NOT NULL REFERENCES users (id),
    action      VARCHAR(64)  NOT NULL,
    entity_type VARCHAR(32)  NOT NULL,
    entity_id   UUID         NOT NULL,
    detail      VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_staff_audit_created ON staff_audit_events (created_at DESC);
CREATE INDEX idx_staff_audit_entity ON staff_audit_events (entity_type, entity_id);
