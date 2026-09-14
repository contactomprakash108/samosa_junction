# Product catalog

Customers browse before they authenticate. Staff and admins change the menu.

```text
GET    /api/categories
GET    /api/products?search=&category=&spiceLevel=&available=&dietaryTag=&page=&size=&sort=
GET    /api/products/{id}          public (imageUrl is a short-lived GET)
POST   /api/products               STAFF, ADMIN
PUT    /api/products/{id}          STAFF, ADMIN
DELETE /api/products/{id}          STAFF, ADMIN
POST   /api/products/{id}/image    STAFF, ADMIN   multipart file
DELETE /api/products/{id}/image    STAFF, ADMIN
```

One image per product. Bytes are not in PostgreSQL; see [s3-images.md](s3-images.md).

List flow:

```text
Controller (query params + Pageable)
  → ProductService.search
       → whitelist sort fields (price → pricePaise)
       → JPA Specification (dynamic WHERE)
       → fetch join category on the data query only
       → Page<Product> → PageResponse<ProductResponse>
```

## Why DTOs

`Product` has `pricePaise` and a lazy `Category`. The JSON contract is rupees (`price`) and a category code. Clients must not see persistence details.

## Why price is stored as paise

`double` and even some `BigDecimal` arithmetic round money incorrectly. Integers of the smallest currency unit (paise) keep checkout math exact. The API still speaks rupees.

## Search and indexes

`search=paneer` is `LOWER(name) LIKE '%paneer%' OR LOWER(description) LIKE ...`. The `LOWER(name)` index helps prefix-like filters more than leading-wildcard LIKE. Leading `%` still often sequential-scans — we would add `pg_trgm` when search volume justifies it. Elasticsearch is not needed for seven samosas.

## Pagination + collections

We **do not** `JOIN FETCH` ingredients and tags on the list query. Fetching collections with `LIMIT/OFFSET` duplicates rows and breaks `Page` totals. List loads category in one query; tags/ingredients use Hibernate `@BatchSize(16)` (N/16 extra queries, not N+1 per row if batched). Detail uses an `@EntityGraph`.

## Authorization

Anonymous `GET` is a product decision: a menu is public. Mutations require `ROLE_STAFF` or `ROLE_ADMIN`. A customer JWT receives **403**, not 404.

Seeded local admin (password `password1`): `admin@samosa.test`.


