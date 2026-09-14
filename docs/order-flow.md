# Order checkout

Checkout is one Postgres transaction. Cart, catalog, inventory, wallet, and a wallet payment row either all succeed or all roll back. Redis is **not** in that transaction.

Delivery on the order is a **snapshot** of name + line + city + state + pincode. The customer also has **one default** of those fields on `users` (Profile). Checkout and Samosa AI use that default. There is still no list of many homes.

We persist **CONFIRMED** on a successful pay-now checkout. `CREATED` exists so unpaid / COD-style flows can use it.

## Flow

```text
POST /api/orders
  Idempotency-Key: ada-order-1
  { "delivery": { recipientName, line1, city, state, pincode } }
        ↓
  1. Same (user_id, key) already exists → return that order (200)
  2. Load cart lines (empty → 400). Prices are not taken from Redis.
  3. Load products. Missing or available=false → 400.
  4. Snapshot name + catalog unitPricePaise + qty on order_items.
  5. INSERT order (CREATED) + items
  6. InventoryService.reserve per line
  7. WalletService.debit  key = order-debit:{idempotencyKey}
  8. PaymentService.captureWallet (method WALLET, SUCCESS)
  9. status = CONFIRMED
 10. afterCommit → cartService.clear
 11. Commit
```

If step 6–8 fail, the transaction rolls back. The cart stays. The customer can retry.

## Why after-commit cart clear

Redis and the `carts` JSONB row are updated by `CartService.clear`, not by the order INSERT. If we deleted the cart and then the wallet debit failed, the customer would lose the cart **and** have no order.

`TransactionSynchronization.afterCommit` clears the cart only after Postgres commits. On rollback the cart is unchanged.

`DualCartStore` still has a Postgres copy. If Redis were deleted and Postgres rolled back, the next `GET /api/cart` would restore from Postgres. Clearing after commit avoids that window on the success path.

## Money and stock

| Concern | Rule |
| --- | --- |
| Price | Catalog `price_paise` at checkout, not the client body or cart JSON |
| Stock | `reserve` inside the same TX as the order |
| Money | `debit` inside the same TX; ledger + `@Version` |
| Payment row | One `payments` row per order (`uk_payments_order_id`). Not a card gateway |
| Kafka | After commit only. See [kafka-events.md](kafka-events.md). Not exactly-once. |

Do not retry `reserve` or `debit` inside a failed `@Transactional` method. Spring will mark the transaction rollback-only. Retry the **HTTP request**.

## Cancel

Allowed for `CREATED`, `CONFIRMED`, `PREPARING`. Not `READY`, `OUT_FOR_DELIVERY`, `DELIVERED`.

```text
POST /api/orders/{id}/cancel
  1. Load order; other user's id → 404 (not 403)
  2. Already CANCELLED → 409
  3. Not cancellable → 400
  4. inventory.release each line
  5. wallet.refund  key = order-refund:{orderId}
  6. payment status REFUNDED
  7. status = CANCELLED
```

Refund uses the order id as the idempotency key, so a retried cancel cannot double-credit.

## Idempotency

Unique `(user_id, idempotency_key)` on `orders`. A mobile retry or AI double-tool-call returns the same order. Wallet debit uses `order-debit:{key}` so a partial replay cannot debit twice if we ever re-enter apply.

Replay of a successful checkout returns **200**. A new checkout returns **201**.

## Isolation from the AI

Samosa AI may call `OrderService.create` through `place_order`. It must not `UPDATE orders` or `UPDATE wallets` itself. Java re-reads the cart, catalog, stock, Profile default address, and wallet. A hallucinated “order already placed” is not a row.

## APIs

```text
POST /api/orders                 JWT + Idempotency-Key
GET  /api/orders                 JWT, own orders, newest first
GET  /api/orders/{id}            JWT, own order or 404
POST /api/orders/{id}/cancel     JWT
```

Public payment APIs are in [docs/payment-design.md](payment-design.md). `payNow: false` leaves a `CREATED` order for `POST /api/payments`. Kitchen status is staff (`/staff/kitchen`).

## Address (this ship vs next)

Delivery fields are a **snapshot on the order**. Profile stores **one default** (`users` columns). Checkout and AI use that default.

**Next kitchen — full address book:** `user_addresses` table, `addressId` on checkout, named homes (Home / Work), AI picks default or a named address. Do not invent addresses in the LLM.

## N+1

List does not fetch-join items. `@BatchSize(16)` on `Order.items` loads lines in batches inside the service transaction (`open-in-view` is still false). Detail uses `@EntityGraph`.
