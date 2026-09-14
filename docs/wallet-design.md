# Wallet ledger

The AI agent will eventually pay from the wallet. That means **Java**, not the LLM, must be the only place money moves.

We do **not** do this:

```text
wallet.balance = wallet.balance - amount
```

as a standalone assignment with no ledger and no concurrency control.

## Flow

```text
POST /api/wallet/add-money
  Idempotency-Key: add-100-ada
  { "amount": 100.00 }
        ↓
  WalletService.apply
        ↓
  1. Require idempotency key
  2. If (wallet_id, key) exists → replay same result or 409 if payload differs
  3. If DEBIT and balance < amount → 409 INSUFFICIENT_FUNDS (no ledger row)
  4. INSERT wallet_transactions (COMPLETED)
  5. Update wallets.balance_paise  (+ @Version)
  6. Commit
```

`debit` / `refund` are Java methods for checkout, `POST /api/payments`, cancel, and refund. There is **no** public “subtract money” HTTP API.


Add-money is **simulated**. We did not integrate a payment gateway.

## Why a ledger

| Without ledger | With ledger |
| --- | --- |
| Balance is the only history | Every CREDIT/DEBIT/REFUND is a row |
| Duplicate POST doubles money | Unique `(wallet_id, idempotency_key)` |
| Support cannot explain a balance | Audit trail |

`balance_paise` is a cached total updated **in the same transaction** as the ledger insert. The ledger is the audit; the balance is for fast reads. Rebuilding balance from the ledger is a future reconciliation job, not required for V1.

Amounts are **paise**, same as catalog prices.

## Concurrent debits

Same idea as inventory:

```text
UPDATE wallets SET balance_paise = 200, version = 2
WHERE id = ? AND version = 1
```

Two checkouts spending ₹8 from ₹10: one commits; the other gets `OptimisticLockingFailureException` → **409 `CONCURRENT_UPDATE`** (or `INSUFFICIENT_FUNDS` if the loser reloads and sees ₹2). The **order** request retries as a whole.

We do not retry money movement inside a failed Spring transaction.

## Idempotency

A mobile retry or AI double-tool-call must not credit twice.

- Header `Idempotency-Key` is required on add-money
- Unique constraint plus a pre-check
- Same key + same amount/type → return current wallet (no second credit)
- Same key + different amount → 409

Two concurrent identical add-money calls: one insert wins; the other hits the unique constraint and we reload the first row.

## Isolation from the AI

The Samosa AI may call `getWalletBalance` and later `createOrder`. It must never `UPDATE wallets`. Java validates again at debit time. A hallucinated “I already paid” is not a ledger row.

## APIs

```text
GET  /api/wallet
POST /api/wallet/add-money     Idempotency-Key required
GET  /api/wallet/transactions
```

All JWT-scoped to the caller. Simulated top-up cap: ₹1–₹50,000.
