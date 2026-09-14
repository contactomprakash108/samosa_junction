CREATE TABLE categories (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(32)  NOT NULL,
    name       VARCHAR(80)  NOT NULL,
    CONSTRAINT uk_categories_code UNIQUE (code)
);

CREATE TABLE products (
    id           UUID PRIMARY KEY,
    name         VARCHAR(120)   NOT NULL,
    description  VARCHAR(1000)  NOT NULL,
    category_id  BIGINT         NOT NULL REFERENCES categories (id),
    price_paise  INTEGER        NOT NULL,
    calories     INTEGER        NOT NULL,
    protein_grams NUMERIC(6, 2) NOT NULL,
    spice_level  VARCHAR(16)    NOT NULL,
    available    BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ    NOT NULL,
    updated_at   TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uk_products_name UNIQUE (name),
    CONSTRAINT ck_products_price_paise CHECK (price_paise >= 0),
    CONSTRAINT ck_products_calories CHECK (calories >= 0),
    CONSTRAINT ck_products_protein CHECK (protein_grams >= 0)
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_available ON products (available);
CREATE INDEX idx_products_spice_level ON products (spice_level);
CREATE INDEX idx_products_name_lower ON products (LOWER(name));

CREATE TABLE product_ingredients (
    product_id UUID        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    ingredient VARCHAR(80) NOT NULL,
    CONSTRAINT pk_product_ingredients PRIMARY KEY (product_id, ingredient)
);

CREATE TABLE product_dietary_tags (
    product_id UUID        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    tag        VARCHAR(40) NOT NULL,
    CONSTRAINT pk_product_dietary_tags PRIMARY KEY (product_id, tag)
);

CREATE INDEX idx_product_dietary_tags_tag ON product_dietary_tags (tag);

INSERT INTO categories (code, name) VALUES
    ('CLASSIC', 'Classic'),
    ('HEALTHY', 'Healthy'),
    ('PROTEIN', 'High Protein'),
    ('BAKED', 'Baked');
