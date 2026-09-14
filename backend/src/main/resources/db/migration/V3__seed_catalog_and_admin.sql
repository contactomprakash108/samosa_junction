INSERT INTO products (
    id, name, description, category_id, price_paise, calories, protein_grams, spice_level, available, created_at, updated_at
)
SELECT
    '11111111-1111-4111-8111-111111111111',
    'Classic Samosa',
    'The standard potato-and-pea samosa, fried until crisp.',
    id, 3000, 260, 6.00, 'MEDIUM', TRUE, NOW(), NOW()
FROM categories WHERE code = 'CLASSIC';

INSERT INTO products (
    id, name, description, category_id, price_paise, calories, protein_grams, spice_level, available, created_at, updated_at
)
SELECT
    '11111111-1111-4111-8111-111111111112',
    'Paneer Samosa',
    'Crumbled paneer with mild spices in a crisp shell.',
    id, 4000, 310, 12.00, 'MILD', TRUE, NOW(), NOW()
FROM categories WHERE code = 'PROTEIN';

INSERT INTO products (
    id, name, description, category_id, price_paise, calories, protein_grams, spice_level, available, created_at, updated_at
)
SELECT
    '11111111-1111-4111-8111-111111111113',
    'Corn Samosa',
    'Sweet corn, capsicum and spices.',
    id, 3500, 240, 7.00, 'MILD', TRUE, NOW(), NOW()
FROM categories WHERE code = 'CLASSIC';

INSERT INTO products (
    id, name, description, category_id, price_paise, calories, protein_grams, spice_level, available, created_at, updated_at
)
SELECT
    '11111111-1111-4111-8111-111111111114',
    'Baked Samosa',
    'Oven-baked shell with a lighter potato filling.',
    id, 3500, 180, 6.00, 'MILD', TRUE, NOW(), NOW()
FROM categories WHERE code = 'BAKED';

INSERT INTO products (
    id, name, description, category_id, price_paise, calories, protein_grams, spice_level, available, created_at, updated_at
)
SELECT
    '11111111-1111-4111-8111-111111111115',
    'High Protein Samosa',
    'Paneer and lentil filling aimed at a higher protein snack.',
    id, 4500, 290, 18.00, 'MEDIUM', TRUE, NOW(), NOW()
FROM categories WHERE code = 'PROTEIN';

INSERT INTO products (
    id, name, description, category_id, price_paise, calories, protein_grams, spice_level, available, created_at, updated_at
)
SELECT
    '11111111-1111-4111-8111-111111111116',
    'Millet Samosa',
    'Millet flour shell with mixed vegetable filling.',
    id, 4000, 210, 8.00, 'MEDIUM', TRUE, NOW(), NOW()
FROM categories WHERE code = 'HEALTHY';

INSERT INTO products (
    id, name, description, category_id, price_paise, calories, protein_grams, spice_level, available, created_at, updated_at
)
SELECT
    '11111111-1111-4111-8111-111111111117',
    'Chole Samosa',
    'Chickpea masala stuffed samosa, a heavier classic.',
    id, 3800, 330, 11.00, 'HOT', TRUE, NOW(), NOW()
FROM categories WHERE code = 'CLASSIC';

INSERT INTO product_ingredients (product_id, ingredient) VALUES
    ('11111111-1111-4111-8111-111111111111', 'potato'),
    ('11111111-1111-4111-8111-111111111111', 'peas'),
    ('11111111-1111-4111-8111-111111111111', 'wheat flour'),
    ('11111111-1111-4111-8111-111111111112', 'paneer'),
    ('11111111-1111-4111-8111-111111111112', 'onion'),
    ('11111111-1111-4111-8111-111111111112', 'wheat flour'),
    ('11111111-1111-4111-8111-111111111113', 'corn'),
    ('11111111-1111-4111-8111-111111111113', 'capsicum'),
    ('11111111-1111-4111-8111-111111111114', 'potato'),
    ('11111111-1111-4111-8111-111111111114', 'wheat flour'),
    ('11111111-1111-4111-8111-111111111115', 'paneer'),
    ('11111111-1111-4111-8111-111111111115', 'lentils'),
    ('11111111-1111-4111-8111-111111111116', 'millet flour'),
    ('11111111-1111-4111-8111-111111111116', 'mixed vegetables'),
    ('11111111-1111-4111-8111-111111111117', 'chickpeas'),
    ('11111111-1111-4111-8111-111111111117', 'onion');

INSERT INTO product_dietary_tags (product_id, tag) VALUES
    ('11111111-1111-4111-8111-111111111111', 'VEGETARIAN'),
    ('11111111-1111-4111-8111-111111111112', 'VEGETARIAN'),
    ('11111111-1111-4111-8111-111111111112', 'HIGH_PROTEIN'),
    ('11111111-1111-4111-8111-111111111113', 'VEGETARIAN'),
    ('11111111-1111-4111-8111-111111111114', 'VEGETARIAN'),
    ('11111111-1111-4111-8111-111111111114', 'BAKED'),
    ('11111111-1111-4111-8111-111111111115', 'VEGETARIAN'),
    ('11111111-1111-4111-8111-111111111115', 'HIGH_PROTEIN'),
    ('11111111-1111-4111-8111-111111111116', 'VEGETARIAN'),
    ('11111111-1111-4111-8111-111111111116', 'MILLET'),
    ('11111111-1111-4111-8111-111111111117', 'VEGETARIAN'),
    ('11111111-1111-4111-8111-111111111117', 'HIGH_PROTEIN');

INSERT INTO users (id, email, password_hash, full_name, enabled, created_at, updated_at)
VALUES (
    '00000000-0000-4000-8000-000000000001',
    'admin@samosa.test',
    '$2a$10$aMZaJqRkKhEvKVXLpUavS./cURdThXdiIXY5wFMC1FMLsxyqzYfMK',
    'Samosa Admin',
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO user_roles (user_id, role_id)
SELECT '00000000-0000-4000-8000-000000000001', id FROM roles WHERE name = 'ADMIN';
