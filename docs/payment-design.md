# Simulated payments

This is **not** a card gateway. There is no Razorpay/Stripe charge. `method` is `WALLET`, `CARD`, or `COD`.

- **WALLET:** money moves in `WalletService`.
- **CARD:** writes a `SUCCESS` payment row and reserves stock. No bank.
- **COD:** kitchen starts now; staff marks collected after delivery.

The `payments` row is the customer-visible receipt and retry handle.

## Next kitchen: Razorpay

Keep Java as the source of truth: create a gateway order, store `razorpay_order_id` on `payments`, verify the webhook signature, then mark SUCCESS. The UI must not trust a client-only “paid” flag. Until then, demo CARD is the simulator only.

## Two ways to pay

**One-step (default).** `POST /api/orders` with `payNow` omitted or `true` and `paymentMethod` `WALLET` (default) or `CARD` reserves, captures, writes `SUCCESS`, persists `CONFIRMED` in one transaction.

**Two-step.** `POST /api/orders` with `"payNow": false` snapshots the cart into a `CREATED` order and does **not** reserve or debit. Then:

```text
POST /api/payments
  Idempotency-Key: ada-pay-1
  { "orderId": "...", "method": "WALLET" }   // or "CARD"
```

```text
1. Own order or 404
2. Same payment key SUCCESS/REFUNDED → 200 replay
3. Existing SUCCESS for the order → 200 replay
4. FAILED (or new row) → attempt capture
5. simulateFailure=true or wallet short (WALLET only) → commit FAILED, HTTP 409, order stays CREATED
6. Else capture + reserve + SUCCESS + CONFIRMED → 201
   WALLET: debit ledger. CARD: no bank charge and no wallet movement.
```

We **do not throw** after `markFailed`. An unchecked exception would roll the FAILED row back, and retry/GET would have nothing to see.

`simulateFailure` is a **simulator switch** for learning decline + retry. It is not a bank.

## Why FAILED is committed

Checkout-in-one-TX has no FAILED row: the whole order disappears on debit failure. Two-step pay needs a durable decline so the customer can `GET /api/payments/{id}` and retry.

Same `Idempotency-Key` after `FAILED` **retries**. Same key after `SUCCESS` **replays**. That is the usual payment-gateway rule.

Wallet debit uses `pay:{idempotencyKey}`. A failed attempt never wrote a ledger row, so retry can debit once.

## Refund

```text
POST /api/payments/{id}/refund
```

Same rules as cancel: `CREATED` / `CONFIRMED` / `PREPARING` only. Stock is released only if it was reserved (`CONFIRMED` / `PREPARING`). Wallet refund uses `order-refund:{orderId}` so cancel and refund cannot double-credit. CARD cancel marks the payment `REFUNDED` and does **not** credit the wallet (nothing was taken from it). Already `REFUNDED` → 200.

Staff/Admin may refund any payment. Customers only see their own (`404` otherwise, not `403`).

## APIs

```text
POST /api/payments              JWT + Idempotency-Key
GET  /api/payments              JWT, own payments
GET  /api/payments/{id}         JWT, own or STAFF/ADMIN
POST /api/payments/{id}/refund  JWT
```

409 on decline returns the **PaymentResponse** (`status=FAILED`), not only the generic error envelope, so the client has a payment id to poll.

## Isolation from the AI

The agent may call `createPayment` / `getPayment`. It must not `UPDATE payments` or `UPDATE wallets`. A hallucinated “payment succeeded” is not a `SUCCESS` row.
