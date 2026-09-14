# Architecture

Samosa Junction is a **modular monolith**: one Spring Boot process, packages split by business capability.

```text
React (customer + staff)
  → Spring Boot API (Java 21)
       → Auth / User profile / Product / Cart / Inventory
       → Wallet / Order / Payment / Complaint / Support
       → Kitchen + staff ops
       → Samosa AI (OpenRouter tools → Java services)
       → Recommendations (TF-IDF + cosine, from paid orders)
       → PostgreSQL
       → Redis (cart hot copy; Postgres JSONB fallback)
       → Kafka (optional)  --afterCommit--> notification / analytics / recs observer
       → Object storage (S3 / MinIO, or local disk when S3 is off)
```

Java is the source of truth. The LLM does not write wallets, inventory, or orders except by calling those services.

## Shipped in this demo

JWT, catalog, cart, inventory, wallet, checkout (WALLET + simulated CARD + COD), default delivery on Profile, kitchen/staff UI, complaints, support inbox, outbox, private images, test pyramid, Compose + CI, content-based recs, Samosa AI with stock/wallet/address checks.

## Next kitchen (not this ship)

| Topic | Why it is leftover | Design doc |
| --- | --- | --- |
| Razorpay (or any card gateway) | CARD is a simulator | [payment-design.md](payment-design.md) |
| Real email/SMS | Password reset is local-dev | [authentication.md](authentication.md) |
| Redis always up | Cart works if Redis is down | [redis-design.md](redis-design.md) |
| Full address book | One default address on the user | [order-flow.md](order-flow.md), [learning.md](learning.md) |

How to study the repo: [learning.md](learning.md).
