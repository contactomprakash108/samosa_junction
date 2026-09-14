# Frontend

```text
React page
  → api/*.ts (Axios + JWT)
  → Spring Boot controller
  → JSON
  → TanStack Query
  → UI
```

The browser never decides availability, wallet balance, cancel rules, or the charged total.

## Screens

| Route | API |
| --- | --- |
| `/` `/menu` `/products/:id` | `GET /api/categories`, `/api/products`, `/api/recommendations` |
| `/login` `/register` `/forgot-password` `/reset-password` | `POST /api/auth/login|register|forgot-password|reset-password` |
| `/profile` | `GET/PATCH /api/users/me` (phone + one default delivery) |
| `/cart` | `GET/POST/PUT/DELETE /api/cart` |
| `/checkout` | `POST /api/orders` with `{ delivery, payNow, paymentMethod }` + `Idempotency-Key`. Prefills Profile default address. |
| `/wallet` | `GET /api/wallet`, `POST /api/wallet/add-money`, `GET /api/wallet/transactions` |
| `/orders` `/orders/:id` | `GET /api/orders`, cancel, `POST /api/payments` for unpaid (`WALLET` or `CARD`) |
| `/assistant` | `POST /api/assistant/chat`. Java validates stock, wallet, Profile address. |
| `/help` | Hub: guides, support inbox, or complaints |
| `/help/guides` | FAQs from live product rules |
| `/support` | `POST/GET /api/support` (not live chat) |
| `/staff/*` | Staff/Admin ops (kitchen, inventory, products, complaints, support) |
| `/about` `/story` `/careers` `/privacy` `/terms` `/contact` | Brand and legal pages |
| `/complaints` | `POST/GET /api/complaints`, images via multipart to Java |

Checkout body is a **delivery snapshot**, not `addressId`. A full address book is leftover (see [learning.md](learning.md)). Default `payNow: true` is one Postgres transaction.

`POST /api/payments` **409** can be a `PaymentResponse` with `status: FAILED`, not `{ code, message }`.

## State

| Kind | Tool |
| --- | --- |
| JWT | `localStorage` (`samosa.accessToken`) |
| User / cart / wallet / orders | TanStack Query |
| Address form | `react-hook-form` + zod |

Idempotency keys are generated once per checkout submit (`crypto.randomUUID()`). A retry of a failed two-step pay uses a **new** key so Spring does not replay `FAILED`.

## If the API is down

```bash
bash scripts/start-local-postgres.sh
source scripts/dev-env.sh
cd backend && ./mvnw spring-boot:run
cd frontend && npm run dev
```
