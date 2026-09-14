# Redis and the cart

The catalog lives in PostgreSQL. The cart is **per-user, short-lived, write-heavy** working state. That is a good Redis use. We did **not** put products in Redis yet — a seven-row menu is not a cache problem.

```text
JWT user
  → CartController
  → CartService (quantities, availability, line totals)
  → DualCartStore
       ├─ Redis   key cart:user:{userId}   TTL 7 days   (hot copy)
       └─ PostgreSQL table carts           JSONB        (durable fallback)
  → ProductRepository (current name/price; cart does not trust client prices)
```

## Why Redis

| Need | Redis | PostgreSQL |
| --- | --- | --- |
| Get/update one user's bag many times | O(1) GET/SET | row + JSON, still fine at small scale |
| Expire abandoned working copies | TTL | extra job |
| Survive process restart | yes | yes |

At our size Postgres alone would work. Redis is here because carts are the first *legitimate* hot-key workload, and because interviews expect you to explain TTL and failure, not because the menu required a cache.

## TTL

`SET cart:user:{id} ... EX 604800` (7 days) on every write. TTL is **memory hygiene for Redis**, not “delete the customer's cart forever.” PostgreSQL still has the payload. After Redis eviction, the next `GET /api/cart` is a miss → load Postgres → `SET` Redis again (cache-aside).

Explicit `DELETE /api/cart` removes both.

## Serialization

Values are JSON (`CartPayload`) via Jackson. The key is a plain string. We do not store Java serialization (`ObjectOutputStream`) — it is brittle across deploys.

## Cache invalidation

Cart is not a cache of products. Product price changes show up on the next `GET /api/cart` because we re-read `products` and recompute totals. Redis/Postgres only store `{ productId, quantity }`.

Invalidation of the Redis copy: overwrite on write, delete on clear, expire via TTL.

## Redis down

`DualCartStore` catches Spring `DataAccessException` from Redis:

- **Read:** use PostgreSQL; do not fail the request
- **Write:** PostgreSQL first, Redis best-effort; log a warning if Redis fails
- **Miss in Redis, hit in Postgres:** restore Redis

The API stays up. Actuator `/actuator/health` may show Redis `DOWN` while carts still work.

Without Docker, try `bash scripts/start-local-redis.sh`. If Redis is not installed, the script exits with install hints and the API keeps using the Postgres JSONB cart.

## What we are not doing

- Caching the product list in Redis (no stampede, no invalidation story yet)
- Treating Redis as the only source of truth (TTL would silently empty carts)
- Two-phase commit between Redis and Postgres (we accept a brief Redis lag after a failed SET)

Checkout reads this cart, then inventory/wallet/payment in a **Postgres transaction**. The cart is cleared only `afterCommit`. Redis is not the money boundary.

## Next kitchen: Redis always up

Today the API stays up if Redis is down. For a production demo, run Redis (Compose or `scripts/start-local-redis.sh`) so `/actuator/health` is green and the hot path hits Redis. Keep Postgres JSONB as the durable copy — do not make Redis the only truth.
