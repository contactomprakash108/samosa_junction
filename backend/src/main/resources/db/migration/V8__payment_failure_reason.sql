
ALTER TABLE payments
    ADD COLUMN failure_reason VARCHAR(200);

CREATE INDEX idx_payments_user_created ON payments (user_id, created_at DESC);
