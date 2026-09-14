
CREATE TABLE complaint_images (
    id            UUID PRIMARY KEY,
    complaint_id  UUID          NOT NULL REFERENCES complaints (id) ON DELETE CASCADE,
    s3_object_key VARCHAR(512)  NOT NULL UNIQUE,
    file_name     VARCHAR(200)  NOT NULL,
    content_type  VARCHAR(64)   NOT NULL,
    file_size     INTEGER       NOT NULL,
    uploaded_at   TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_complaint_images_size CHECK (file_size > 0)
);

CREATE INDEX idx_complaint_images_complaint_id ON complaint_images (complaint_id);

CREATE TABLE product_images (
    id            UUID PRIMARY KEY,
    product_id    UUID          NOT NULL UNIQUE REFERENCES products (id) ON DELETE CASCADE,
    s3_object_key VARCHAR(512)  NOT NULL UNIQUE,
    file_name     VARCHAR(200)  NOT NULL,
    content_type  VARCHAR(64)   NOT NULL,
    file_size     INTEGER       NOT NULL,
    uploaded_at   TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_product_images_size CHECK (file_size > 0)
);
