# How to learn this kitchen

Read **one doc, then the matching Java/React code**, then hit the API or UI. Do not read every file first.

Java is the source of truth. The LLM in Samosa AI only calls Java tools. Money, stock, and addresses never live in the model.

This build is a **portfolio ship**. After GitHub, the leftover list at the bottom is the next kitchen.

## Setup (once)

```bash
source scripts/dev-env.sh
bash scripts/start-local-postgres.sh
bash scripts/start-local-redis.sh   # optional; cart still works on Postgres
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

UI: http://localhost:5173 · API: http://localhost:8080

Seeded admin: `admin@samosa.test` / `password1`

Samosa AI (optional): put `OPENROUTER_API_KEY` in gitignored `.env`, never in git. Restart the API after setting it.

## Learning path (in order)

Do the UI click, then open the code, then the design doc.

| Step | What to do | Code | Doc |
| --- | --- | --- | --- |
| 1 | Register, log in, see JWT in Network tab | `AuthService`, `JwtAuthenticationFilter`, `AuthContext.tsx` | [authentication.md](authentication.md) |
| 2 | Browse menu, product page | `ProductService`, `HomePage.tsx` | [product-catalog.md](product-catalog.md) |
| 3 | Add to cart, change qty | `CartService`, `DualCartStore`, `CartPage.tsx` | [redis-design.md](redis-design.md) |
| 4 | Staff inventory (admin) | `InventoryService`, `StaffInventoryPage.tsx` | [inventory.md](inventory.md) |
| 5 | Wallet add-money | `WalletService`, `WalletPage.tsx` | [wallet-design.md](wallet-design.md) |
| 6 | Save default address on Profile | `UserService`, `ProfilePage.tsx` | this file, Profile section |
| 7 | Checkout WALLET / CARD / COD | `OrderService`, `CheckoutPage.tsx` | [order-flow.md](order-flow.md), [payment-design.md](payment-design.md) |
| 8 | Kitchen status (staff) | `KitchenService`, `StaffKitchenPage.tsx` | [order-flow.md](order-flow.md) |
| 9 | Complaint + image | `ComplaintService`, `ComplaintsPage.tsx` | [complaints.md](complaints.md), [s3-images.md](s3-images.md) |
| 10 | Recommendations after a **paid** order | `RecommendationService` | [recommendation-system.md](recommendation-system.md) |
| 11 | Samosa AI: recommend, stock refuse, pay | `AssistantService`, `AssistantPage.tsx` | this file, AI section |
| 12 | Tests: why three layers | `*Test`, `*IT` | [testing.md](testing.md) |
| 13 | Compose / CI | `docker-compose.yml`, `.github/workflows/ci.yml` | [docker.md](docker.md) |

Map of the whole process: [architecture.md](architecture.md). React routes: [frontend.md](frontend.md).

## What “one default address” means (shipped)

There is **no address book** of many homes. `users` holds one default delivery: recipient, line, city, state, pincode, plus phone.

- Profile `PATCH /api/users/me` writes it
- Checkout prefills it and, after a successful order, remembers it
- Samosa AI `place_order` **only** uses that Profile row. If it is empty, AI will not invent an address

Learn: `User.java`, `UserService.java`, `V15__user_delivery_profile.sql`, `ProfilePage.tsx`.

## What Samosa AI actually does (shipped)

```text
You type in /assistant
  → POST /api/assistant/chat  { message, history }
  → OpenRouter (if OPENROUTER_API_KEY set)
  → tool calls: search_menu, add_to_cart, place_order, ...
  → Java runs the tool (stock, wallet, address)
  → model writes the sentence
```

Without a key, the same Java tools still run via a local heuristic. Checkout is never “the model said so.”

Read `AssistantService.java` (tools + validation), `OpenRouterClient.java`, `SemanticMenuSearch.java`.

## Leftover (next kitchen, after you ship this)

Merge these into the existing design docs when you implement them. Until then they stay **out of the demo claim**.

| Leftover | Today | Next |
| --- | --- | --- |
| Real card gateway | `CARD` writes SUCCESS, no bank | Razorpay (or similar) order + webhook; Java still owns `payments` |
| Real email / SMS | Forgot-password may return `resetPath` locally | SMTP/SMS; never return the reset token in JSON in prod |
| Redis always up | `DualCartStore` falls back to Postgres JSONB | Run Redis in Compose/prod; health green; keep Postgres as durable copy |
| Full address book | One default on `users` | `user_addresses` table, `addressId` on checkout, AI picks default or named home |

Do **not** put Razorpay keys or SMTP passwords in git. Same rule as OpenRouter.

## Upload this kitchen to GitHub

1. Confirm `.env` is gitignored (it is). Never add OpenRouter, JWT, or DB passwords.
2. Rotate the OpenRouter key if it was ever pasted in chat.
3. In the project folder:

```bash
cd /home/say2omprakash01/samosa_junction
git status
git add -A
git status   # look for .env — if listed, unstage it
```

4. Commit (only when you want a snapshot):

```bash
git commit -m "$(cat <<'EOF'
Ship customer, staff, and Samosa AI demo kitchen.

EOF
)"
```

5. Create an empty GitHub repo (no README if this folder already has one). Then:

```bash
git remote add origin https://github.com/<your-user>/<repo>.git
git branch -M main
git push -u origin main
```

If GitHub already has commits: `git pull origin main --rebase` then `git push`.

Private repo is safer while the OpenRouter key might still be in old chat logs (it should never be in git history).
