# Testing


Three layers. They answer different questions. Do not replace one with another.

```text
JUnit + Mockito     "Does this class enforce the rule?"
MockMvc             "Does HTTP + Security map that rule to the right status?"
Testcontainers      "Does Flyway + Postgres + Redis still do it when the process is real?"
```

## Unit tests (`*Test`)

JUnit 5 + Mockito. No Spring context (except tiny helpers). Fast. These already cover the money and ownership rules:

| Rule | Where |
| --- | --- |
| Cannot order an unavailable product | `OrderServiceTest` |
| Cannot exceed inventory | `InventoryServiceTest`, `OrderServiceTest` |
| Cannot cancel a delivered order | `OrderServiceTest` |
| Cannot debit below zero | `WalletServiceTest` |
| Duplicate add-money / debit does not double-apply | `WalletServiceTest` |
| Duplicate payment does not double-charge | `PaymentServiceTest` |
| Other user’s order / payment / complaint → not found | service tests |
| Paneer / high-protein taste ranks High Protein Samosa | `RecommendationServiceTest` |
| Cold start does not invent popularity scores | `RecommendationServiceTest` |

Mockito is a **stand-in**. `when(walletRepository.findByUserId(...))` is not PostgreSQL. A unique constraint, a Flyway checksum, or a Redis timeout will not show up here.

## MockMvc (`web/*WebMvcTest`)

`@WebMvcTest` loads **controllers + Security + JSON**, not JPA. Services are mocked.

Use it to prove:

- anonymous `GET /api/products` is **200**
- anonymous `POST /api/products` is **401** with `{ code: UNAUTHORIZED }`
- `CUSTOMER` write is **403**, `STAFF` is **201**
- `PATCH /api/complaints/{id}` is staff-only
- `@Valid` register password < 8 → **400**
- anonymous `GET /api/recommendations` is **401**

Controllers take `@AuthenticationPrincipal UserPrincipal`. `@WithMockUser` puts a **username string** in the context and NPEs. Tests use `@WithSamosaUser`, which builds a real `UserPrincipal`.

## Testcontainers (`it/*IT`)

Real **PostgreSQL 16** and **Redis 7** in Docker. Flyway runs. Seeded paneer id is live.

```text
@SpringBootTest (RANDOM_PORT)
  → TestRestTemplate
  → Tomcat
  → Security + services + JPA
  → Postgres / Redis containers
```

Kafka and S3 stay **off** (`application-test.yml`), same as local `spring-boot:run`. After-commit events are unit-tested. A Kafka container would prove the broker wire format; it would not prove checkout.

`@EnabledIf(DockerAvailability)` + `@Testcontainers(disabledWithoutDocker = true)`: if Docker is missing (this WSL today), ITs are **skipped**, not failed. `./mvnw test` stays green.

```bash
# after Docker Desktop / Engine is available
source scripts/dev-env.sh
cd backend && ./mvnw test
```

## Why not H2

H2 is not PostgreSQL. JSONB, `TIMESTAMPTZ`, and Flyway `$` in BCrypt hashes already forced us off “fake SQL.” Testcontainers is the cheapest way to keep the dialect honest.

## Why not `@DataJpaTest` on H2

Same reason. When Docker is here, a `@DataJpaTest` + Postgres container is a good next slice (unique `(wallet_id, idempotency_key)`). Not required for this phase.

## What we still do not claim

- Coverage percentage
- Load / soak numbers (including the in-memory recommender)
- CI (Phase 18)
