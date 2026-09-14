
CREATE TABLE orders (
    id               UUID PRIMARY KEY,
    user_id          UUID         NOT NULL REFERENCES users (id),
    status           VARCHAR(32)  NOT NULL,
    recipient_name   VARCHAR(120) NOT NULL,
    address_line1    VARCHAR(200) NOT NULL,
    city             VARCHAR(80)  NOT NULL,
    state            VARCHAR(80)  NOT NULL,
    pincode          VARCHAR(16)  NOT NULL,
    total_paise      INTEGER      NOT NULL,
    idempotency_key  VARCHAR(128) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_orders_total CHECK (total_paise >= 0),
    CONSTRAINT uk_orders_user_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_orders_user_created ON orders (user_id, created_at DESC);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_items (
    id               UUID PRIMARY KEY,
    order_id         UUID         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id       UUID         NOT NULL REFERENCES products (id),
    product_name     VARCHAR(120) NOT NULL,
    unit_price_paise INTEGER      NOT NULL,
    quantity         INTEGER      NOT NULL,
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_unit_price CHECK (unit_price_paise >= 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE TABLE payments (
    id               UUID PRIMARY KEY,
    order_id         UUID         NOT NULL REFERENCES orders (id),
    user_id          UUID         NOT NULL REFERENCES users (id),
    amount_paise     INTEGER      NOT NULL,
    method           VARCHAR(16)  NOT NULL,
    status           VARCHAR(16)  NOT NULL,
    idempotency_key  VARCHAR(128) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_payments_order_id UNIQUE (order_id),
    CONSTRAINT uk_payments_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_payments_amount CHECK (amount_paise > 0)
);
