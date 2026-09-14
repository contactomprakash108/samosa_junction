# Complaints


Customers file a text complaint against **their** order, then optionally attach photos. Staff move the ticket through a whitelist. Image **bytes** are not in PostgreSQL; see [s3-images.md](s3-images.md).

## APIs

```text
POST   /api/complaints                    JWT + Idempotency-Key
GET    /api/complaints                    JWT (own) or STAFF/ADMIN (all); images[] empty
GET    /api/complaints/{id}               own or STAFF/ADMIN; includes images[]
PATCH  /api/complaints/{id}               STAFF, ADMIN   { "status", "priority" }
POST   /api/complaints/{id}/images        JWT owner or STAFF/ADMIN   multipart file
GET    /api/complaints/{id}/images        JWT owner or STAFF/ADMIN
DELETE /api/complaints/{id}/images/{imageId}
```

Create body:

```json
{
  "orderId": "...",
  "category": "DAMAGED",
  "description": "My samosas arrived damaged.",
  "priority": "HIGH"
}
```

`priority` is optional (default `MEDIUM`). Categories: `DAMAGED`, `MISSING_ITEM`, `WRONG_ITEM`, `LATE`, `QUALITY`, `OTHER`.

Images: JPEG, PNG, or WebP, max 5 MB, max 5 per complaint. Response `url` is a time-limited GET (presigned S3 or local HMAC).

## Rules

| Rule | Why |
| --- | --- |
| Order must belong to the caller | 404, not 403 — same as orders/payments |
| Unpaid `CREATED` order rejected | Nothing was fulfilled |
| Unique `(user_id, idempotency_key)` | AI/mobile retry must not open two tickets |
| Customer cannot PATCH status | Support owns the lifecycle |
| Terminal `RESOLVED` / `REJECTED` | No reopen in V1 |
| Other user’s complaint or image | 404, including image upload/list/delete |
| List endpoint skips image URLs | Avoid N+1 presigns on every page |

Transitions:

```text
OPEN → IN_PROGRESS | RESOLVED | REJECTED
IN_PROGRESS → RESOLVED | REJECTED
```

## Events

After commit: `COMPLAINT_CREATED`. Each real status change: `COMPLAINT_UPDATED` with event id `complaintId:STATUS` so IN_PROGRESS and RESOLVED are both delivered.

The Samosa AI may call `createComplaint`. It must not `UPDATE complaints` and must not write S3. A hallucinated “I already filed this” is not a row.

## Not in this phase

- Comment thread on a complaint
- Transactional outbox for orphan object cleanup
