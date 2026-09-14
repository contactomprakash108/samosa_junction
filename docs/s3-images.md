# Object storage (S3 / MinIO / local disk)


PostgreSQL stores **metadata only** (`complaint_images`, `product_images`). Bytes live in object storage. We never put JPEG/PNG/WebP into a `BYTEA` column.

## Why not BYTEA

A 3 MB photo in the same row as a complaint description:

- bloats WAL and backups
- makes `SELECT * FROM complaints` expensive
- couples OLTP to blob I/O
- is awkward to CDN or expire

S3 (or MinIO) is the blob store. The database answers “which keys belong to this complaint?”

## Key layout

```text
complaints/{complaintId}/{imageId}-{fileName}
products/{productId}/{fileName}
```

Prefix-by-owner keeps listing and IAM prefix policies simple. One product has **one** image row (replace overwrites). A complaint may have up to `samosa.s3.max-complaint-images` (default 5).

## Private bucket + presigned GET

Objects are **private**. The API never returns AWS access keys to a browser.

A **presigned URL** is a time-limited GET (default 15 minutes) signed with credentials the **server** holds. The client uses the URL as a normal HTTPS GET. After expiry the signature is rejected.

Local mode (`S3_ENABLED=false`) does the same idea with HMAC-SHA256 on `GET /api/objects?key=&exp=&sig=`. That is **not** AWS SigV4. It exists so `spring-boot:run` works without Docker/MinIO.

The product catalog is public, so returning a short-lived `imageUrl` is fine. The object itself stays private; leaking the signing key or IAM user would not be.

## IAM (when you use real AWS)

Create an IAM user or role for the API only:

- `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject` on `arn:aws:s3:::samosa-junction/complaints/*` and `.../products/*`
- `s3:ListBucket` only if you need it (this app does not list the bucket)
- no console login, no `s3:*` on `*`

Access key + secret live in env (`S3_ACCESS_KEY`, `S3_SECRET_KEY`) or the instance role (`DefaultCredentialsProvider` when those are blank). Never ship them in a JWT or a mobile APK.

## MinIO vs AWS

MinIO speaks the S3 API. Point the AWS SDK at `http://localhost:9000` with **path-style** addressing (`S3_PATH_STYLE=true`). Same `put` / `delete` / `presignGet` code. Compose credentials `minioadmin` / `minioadmin` are **local-only**.

```bash
docker compose up -d postgres redis kafka minio
export S3_ENABLED=true
export S3_ENDPOINT=http://localhost:9000
export S3_ACCESS_KEY=minioadmin
export S3_SECRET_KEY=minioadmin
```

The API **container** uses `S3_ENDPOINT=http://minio:9000` and `S3_PRESIGN_ENDPOINT=http://localhost:9000`. Put/delete stay on the Compose network; the browser opens `localhost:9000`.

Console: http://localhost:9001

## Dual-write gap

Upload order:

1. `put` the object
2. insert the metadata row
3. if the row fails, `delete` the object (compensate)

If the process dies after `put` and before the row, you have an **orphan object**. The opposite order (row then put) leaves a row that 404s. We chose put-then-row so a client never receives a URL for a missing object.

Delete is row-then-object. If S3 delete fails after the row is gone, you have an orphan object. Acceptable for V1; a sweeper can delete unreferenced keys later.

This is the same family of problem as Kafka-after-commit. A transactional outbox does not apply to S3 bytes.

## Validation

JPEG / PNG / WebP only. Content-Type **and** magic bytes must match. Max 5 MB (`spring.servlet.multipart` and `samosa.s3.max-file-bytes`). Filenames are sanitized; `../` cannot escape the key prefix.

## APIs

```text
POST   /api/complaints/{id}/images     JWT owner or STAFF/ADMIN   multipart file
GET    /api/complaints/{id}/images     JWT owner or STAFF/ADMIN
DELETE /api/complaints/{id}/images/{imageId}
GET    /api/complaints/{id}            includes images[] with presigned urls
GET    /api/complaints                 images[] is empty (no N+1)

POST   /api/products/{id}/image        STAFF, ADMIN
DELETE /api/products/{id}/image        STAFF, ADMIN
GET    /api/products                   imageUrl may be a presigned GET
GET    /api/objects                    permitAll; HMAC local mode only
```

Other users’ complaints still **404**, not 403.
