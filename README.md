# Samosa Junction

Smart Indian snack ordering platform. Java 21 Spring Boot modular monolith and a React app (customer + staff).

This repository is a learning and portfolio kitchen. Built incrementally. **How to study it:** [docs/learning.md](docs/learning.md).

## Problem

Customers need to browse snacks, pay from a wallet, track orders, and ask an assistant without leaking money-moving logic to an LLM. The Java backend is the source of truth. Samosa AI only calls controlled Java APIs.

## Current status (demo ship)

- Auth (JWT), catalog, cart, inventory, wallet, checkout, kitchen, complaints, support
- One **default delivery address** on Profile (`PATCH /api/users/me`)
- Recommendations from **paid** orders (TF-IDF + cosine). Cold start is honest.
- **Samosa AI** (`POST /api/assistant/chat`): OpenRouter tool-calling when `OPENROUTER_API_KEY` is set; Java still validates stock, wallet, and the Profile address
- CI: `.github/workflows/ci.yml`

**Not this ship** (next kitchen, already noted in the design docs): Razorpay, real email/SMS, Redis required-up, many saved addresses.

See [docs/architecture.md](docs/architecture.md) and [docs/frontend.md](docs/frontend.md).

## Architecture

```text
Client
  → Spring Boot API (Java 21)
       → PostgreSQL
       → Redis (cart)
       → Kafka (optional, after commit)
       → S3 / MinIO (optional; local disk when off)
       → Recommendations (Java, from paid orders)
```

```text
React → Java Spring Boot → PostgreSQL / Redis / Kafka / S3
              ↓
       OpenRouter (optional) → tool calls only
              ↓
       Same Java services (never direct DB from the LLM)
```

See [docs/architecture.md](docs/architecture.md).

## Technology stack (implemented)

| Layer | Choice |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Security, Validation, Actuator, Flyway) |
| Auth | JWT (HS256) + BCrypt |
| Build | Maven |
| Database | PostgreSQL 16 |
| Cache | Redis 7 (cart working copy) |
| Events | Kafka (optional; default off) |
| Objects | AWS SDK S3 (MinIO or AWS); local disk + HMAC GET when off |
| Tests | JUnit 5, Mockito, MockMvc, Testcontainers (Postgres + Redis) |
| Local infra | Docker Compose (API image + Postgres + Redis + Kafka + MinIO) |

## Prerequisites

- Docker Desktop / Docker Engine (full stack **or** infra for a host JVM)
- JDK 21 and Maven 3.9+ only if you run `./mvnw` on the host

This WSL environment can use a user-local JDK and Maven:

```bash
source scripts/dev-env.sh
```

## Local development

1. Copy environment defaults if you want a local `.env` (optional; Compose uses the values in `docker-compose.yml`):

   ```bash
   cp .env.example .env
   ```

2. Start the stack (API image + infra). First build downloads Maven and JDK layers:

   ```bash
   docker compose up -d --build
   docker compose ps
   curl -s http://localhost:8080/actuator/health
   ```

   Infra only (then run the JVM on the host): `docker compose up -d postgres redis kafka minio`. See [docs/docker.md](docs/docker.md). Without Docker Redis, `bash scripts/start-local-redis.sh` (cart still works on Postgres if Redis is missing).

   The **API container** enables Kafka and MinIO. Host `spring-boot:run` still defaults those off unless you export the flags.

5. UI (API must already be on `:8080`):

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   Open http://localhost:5173. See [docs/frontend.md](docs/frontend.md).

3. Or build and run the API on the host (infra must already be up):

   ```bash
   source scripts/dev-env.sh
   cd backend
   mvn spring-boot:run
   ```

4. Verify health, then auth:

   ```bash
   curl -s http://localhost:8080/actuator/health

   curl -s -X POST http://localhost:8080/api/auth/register \
     -H 'Content-Type: application/json' \
     -d '{"email":"ada@samosa.test","password":"password1","fullName":"Ada Lovelace"}'

   TOKEN='<accessToken from register>'

   curl -s http://localhost:8080/api/users/me \
     -H "Authorization: Bearer $TOKEN"
   ```

See [docs/authentication.md](docs/authentication.md), [docs/product-catalog.md](docs/product-catalog.md), [docs/redis-design.md](docs/redis-design.md), [docs/inventory.md](docs/inventory.md), [docs/wallet-design.md](docs/wallet-design.md), [docs/order-flow.md](docs/order-flow.md), [docs/payment-design.md](docs/payment-design.md), [docs/kafka-events.md](docs/kafka-events.md), [docs/complaints.md](docs/complaints.md), [docs/s3-images.md](docs/s3-images.md), [docs/testing.md](docs/testing.md), and [docs/docker.md](docs/docker.md).

Browse the catalog (no token):

```bash
curl -s 'http://localhost:8080/api/products?search=paneer&sort=price,asc'
curl -s http://localhost:8080/api/products/11111111-1111-4111-8111-111111111112/inventory
```

Cart (customer JWT; Paneer Samosa id from seed):

```bash
curl -s -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"productId":"11111111-1111-4111-8111-111111111112","quantity":2}'

curl -s http://localhost:8080/api/cart -H "Authorization: Bearer $TOKEN"
```

Wallet (same customer JWT; simulated top-up, not a card charge):

```bash
curl -s http://localhost:8080/api/wallet -H "Authorization: Bearer $TOKEN"

curl -s -X POST http://localhost:8080/api/wallet/add-money \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: add-100-ada" \
  -H 'Content-Type: application/json' \
  -d '{"amount":100.00}'

curl -s http://localhost:8080/api/wallet/transactions -H "Authorization: Bearer $TOKEN"
```

Place an order (wallet must cover the cart; delivery is a snapshot. Profile can store one default address — not a full address book):

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: ada-order-1" \
  -H 'Content-Type: application/json' \
  -d '{"delivery":{"recipientName":"Ada Lovelace","line1":"42 Baker Street","city":"Mumbai","state":"MH","pincode":"400001"}}'

curl -s http://localhost:8080/api/orders -H "Authorization: Bearer $TOKEN"

curl -s "http://localhost:8080/api/recommendations?limit=5" -H "Authorization: Bearer $TOKEN"
```

Two-step pay (unpaid order, then simulated wallet capture):

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: ada-order-unpaid" \
  -H 'Content-Type: application/json' \
  -d '{"payNow":false,"delivery":{"recipientName":"Ada Lovelace","line1":"42 Baker Street","city":"Mumbai","state":"MH","pincode":"400001"}}'

curl -s -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: ada-pay-1" \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"<order id>","method":"WALLET"}'
```

File a complaint (customer JWT; order must be paid — not `CREATED`):

```bash
curl -s -X POST http://localhost:8080/api/complaints \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: ada-complaint-1" \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"<order id>","category":"DAMAGED","description":"My samosas arrived damaged.","priority":"HIGH"}'
```

Staff mutations use the seeded admin (`admin@samosa.test` / `password1`):

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@samosa.test","password":"password1"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

curl -s -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Test Samosa","description":"Temp","category":"CLASSIC","price":25.00,"ingredients":["potato"],"calories":200,"protein":5.00,"spiceLevel":"MILD","dietaryTags":["VEGETARIAN"],"available":true}'

curl -s -X PUT http://localhost:8080/api/inventory/11111111-1111-4111-8111-111111111112 \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"quantity":50}'

curl -s -X PATCH http://localhost:8080/api/complaints/<complaint-id> \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS"}'

curl -s -X POST http://localhost:8080/api/products/11111111-1111-4111-8111-111111111112/image \
  -H "Authorization: Bearer $TOKEN" \
  -F 'file=@/path/to/samosa.jpg;type=image/jpeg'
```

Customer JWT, after a complaint exists:

```bash
curl -s -X POST http://localhost:8080/api/complaints/<complaint-id>/images \
  -H "Authorization: Bearer $TOKEN" \
  -F 'file=@/path/to/damage.jpg;type=image/jpeg'
```

Stop with `docker compose down`. Data persists in the `postgres_data` and `minio_data` volumes unless you pass `-v`.

## Project structure

```text
samosa_junction/
├── backend/                 Java modular monolith + Dockerfile
├── frontend/                React 19 + Vite (increment F1: auth + catalog)
├── docker-compose.yml       API + Postgres + Redis + Kafka + MinIO
├── docs/                    Start at docs/learning.md
├── .github/workflows/ci.yml Maven tests + frontend build
├── scripts/dev-env.sh
├── scripts/start-local-postgres.sh
└── scripts/start-local-redis.sh
```

## Features / later documentation

See [docs/learning.md](docs/learning.md) for the study order. Recs: [docs/recommendation-system.md](docs/recommendation-system.md). UI: [docs/frontend.md](docs/frontend.md).

## Design decisions (Phase 1)

- **Modular monolith, not 10 services.** One deployable Java app; packages map to future extraction boundaries.
- **PostgreSQL from day one.** No H2 in the main runtime. Local DB matches production dialect.
- **`ddl-auto: none`.** Schema will be explicit (Flyway) when entities exist. Hibernate will not invent production tables.
- **`open-in-view: false`.** Prevents lazy-loading surprises after the transaction closes. We will confront the N+1 problem honestly later.
- **Secrets via environment variables.** Compose uses local-only defaults; never commit real AWS or DB passwords.
- **Samosa AI calls Java.** The model never updates wallets or inventory itself.
- **One default address for this ship.** A real address book is the next kitchen.
- **JWT is stateless.** Any API instance can verify the signature. We still load the user on each request so a disabled account cannot keep calling forever with an old token.
- **Passwords are BCrypt hashes.** Plaintext never hits PostgreSQL.
- **Flyway owns schema.** Hibernate `ddl-auto` stays `none`.

## How to run (short)

```bash
docker compose up -d --build
curl http://localhost:8080/actuator/health
```

## License

Private learning project.
