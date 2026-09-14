# Docker


```text
docker compose up -d --build
        │
        ▼
   api (Java 21, port 8080)
        │  Compose DNS
        ├── postgres:5432
        ├── redis:6379
        ├── kafka:19092
        └── minio:9000
```

The host still reaches the same processes on localhost (5432, 6379, 9092, 9000, 8080). That is **two names for one broker**: a container cannot use `localhost:9092` (that is itself), and a laptop browser cannot use `kafka:19092` (that name is only on the Compose network).

## Two ways to run

| Command | What starts | When |
| --- | --- | --- |
| `docker compose up -d --build` | API + Postgres + Redis + Kafka + MinIO | One-command local stack |
| `docker compose up -d postgres redis kafka minio` | Infra only | You run `./mvnw spring-boot:run` on the host |

Do **not** run the API container and `spring-boot:run` at the same time — both bind **8080**.

Host-run API env (defaults already match localhost):

```bash
export KAFKA_ENABLED=true
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export S3_ENABLED=true
export S3_ENDPOINT=http://localhost:9000
```

## Dockerfile

Multi-stage:

1. **`maven:3.9.9-eclipse-temurin-21`** — `mvn -DskipTests package`
2. **`eclipse-temurin:21-jre-alpine`** — copy the fat jar, run as user `samosa`

Why skip tests in the image build: Testcontainers needs a Docker daemon. A build container does not get your host socket unless you add Docker-in-Docker. Tests stay `./mvnw test` / CI.

Why a JRE image, not JDK: the runtime does not compile. Smaller attack surface.

Why non-root: a RCE in the JVM should not be root in the container. This is not a security *proof*; it is the default you should be able to explain.

`-XX:MaxRAMPercentage=75.0` lets the heap follow the container memory limit (`docker compose` / later ECS). A hardcoded `-Xmx2g` ignores the cgroup.

`.dockerignore` drops `target/` so we do not send a host-built jar into the context.

## Kafka listeners

```text
Host app  ----localhost:9092---->  EXTERNAL
API container ----kafka:19092---->  INTERNAL
```

`KAFKA_ADVERTISED_LISTENERS` is what the **client** is told to reconnect to after metadata. If the API container received `localhost:9092`, it would talk to itself and hang.

## MinIO endpoints

| Setting | Value in the API container | Who uses it |
| --- | --- | --- |
| `S3_ENDPOINT` | `http://minio:9000` | `S3Client` put/delete |
| `S3_PRESIGN_ENDPOINT` | `http://localhost:9000` | `S3Presigner` URL host |

A browser on your laptop cannot resolve `minio`. Signing with the internal name would produce a dead link.

## Health

Compose starts the API only after Postgres, Redis, Kafka, and MinIO report healthy. The API image then curls `/actuator/health` until Flyway has finished.

That is **readiness**, not a substitute for a lock on first request. The first HTTP call can still race a slow migration if you drop the start period too low.

## What this is not

- Not AWS ECS/Fargate (Phase 17)
- Not GitHub Actions (Phase 18)
- Not a production image scan or distroless hardening pass
- MinIO `minioadmin` / `minioadmin` is local-only
