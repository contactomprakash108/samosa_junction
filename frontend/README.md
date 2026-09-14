# Samosa Junction frontend


React 19 + TypeScript + Vite. UX only. Java remains the source of truth.

## Working

Auth, menu/search, recommendations, cart, checkout, wallet, orders (pay/cancel), complaints + image upload.

## Pending

AI ordering chat — Python agent is not built. `/assistant` says so.

## Run

```bash
bash scripts/start-local-postgres.sh
source scripts/dev-env.sh
cd backend && ./mvnw spring-boot:run

cd frontend && npm install && npm run dev
```

Open http://localhost:5173. `VITE_API_BASE_URL` defaults to `http://localhost:8080`.
