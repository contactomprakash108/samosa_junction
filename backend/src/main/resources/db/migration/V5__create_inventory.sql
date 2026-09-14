CREATE TABLE inventory (
    product_id UUID PRIMARY KEY REFERENCES products (id) ON DELETE CASCADE,
    quantity   INTEGER     NOT NULL,
    version    BIGINT      NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_inventory_quantity CHECK (quantity >= 0)
);

INSERT INTO inventory (product_id, quantity, version, updated_at)
SELECT id, 50, 0, NOW() FROM products;
