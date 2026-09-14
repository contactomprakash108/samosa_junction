# Inventory and locking

Cart quantity is a wish. Inventory is **how many samosas we can actually sell**. Without locking, this happens:

```text
Stock = 10
Thread A reads 10, decides to sell 8
Thread B reads 10, decides to sell 8
Both write 2
Sold 16, stock shows 2  → overselling
```

That is a lost update / race. Checkout calls `InventoryService.reserve` **inside the same database transaction** as creating the order.


## Choice: optimistic locking

`inventory.version` is a Hibernate `@Version` column.

```text
UPDATE inventory
SET quantity = 2, version = 1, ...
WHERE product_id = ? AND version = 0
```

If another transaction already committed `version = 1`, this update hits **0 rows**. Hibernate throws `OptimisticLockingFailureException`. We map that to HTTP **409 `CONCURRENT_UPDATE`**. The caller retries the **whole checkout**, not a nested retry inside a failed transaction (Spring would mark the transaction rollback-only).

Why optimistic first:

- Decrement is a short transaction (milliseconds), not a user holding a row while choosing chutney
- No extra row locks under read-committed for the common uncontended path
- The conflict is visible and testable

**Pessimistic** `SELECT … FOR UPDATE` would serialize A and B on the row. Better when many checkouts hit the last 10 units of one SKU. We will switch if we measure contention, not before.

## Isolation

Default Spring/Postgres is typically **READ COMMITTED**. That does **not** by itself prevent the lost update above. `@Version` (or `FOR UPDATE`) does. Repeatable read / SSI can help some anomalies; we are not claiming serializable checkout yet.

## APIs

```text
GET  /api/products/{productId}/inventory     public
PUT  /api/inventory/{productId}              STAFF, ADMIN   { "quantity": 50 }
```

There is **no** public “reserve” HTTP API. Customers must not decrement stock except through checkout. `reserve` / `release` are Java methods for `OrderService`.

New products get inventory **0**. Seeded catalog products have **50**.

## Cart vs inventory

Adding to the cart does **not** reserve stock. Two carts can both hold 8 of 10. Only checkout reserves. That is a product decision (Amazon-style) vs “hold for 10 minutes” (ticket-style). We start with checkout-time reservation.
