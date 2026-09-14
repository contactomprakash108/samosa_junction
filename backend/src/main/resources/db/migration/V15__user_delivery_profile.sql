
ALTER TABLE users
    ADD COLUMN phone VARCHAR(20),
    ADD COLUMN recipient_name VARCHAR(120),
    ADD COLUMN address_line1 VARCHAR(200),
    ADD COLUMN city VARCHAR(80),
    ADD COLUMN state VARCHAR(80),
    ADD COLUMN pincode VARCHAR(16);
