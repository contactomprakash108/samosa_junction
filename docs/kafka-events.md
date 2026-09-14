# Kafka domain events


Checkout, pay, cancel, and kitchen status still commit in **PostgreSQL first**. Domain events are written to `outbox_events` in that same transaction, then published after commit (and again by a poller if the process died). If the transaction rolls back, Kafka is not written. Redis cart clear still uses `afterCommit`.

This is **at-least-once** delivery. We do **not** claim exactly-once.

## Why not publish inside `@Transactional`

If we `kafkaTemplate.send` before commit, kitchen/notification can see `ORDER_CONFIRMED` for an order that then rolls back (wallet debit failed). That is worse than a missing email.

`afterCommit` has the opposite gap: the order is real, then the process dies before the send. Kitchen never hears. That is the **dual-write problem**. We now insert an `outbox_events` row in the **same Postgres transaction**, then a poller publishes to Kafka (or the log sender). A crash after commit leaves PENDING rows for the next poll.

## Topic and key

One topic: `samosa.domain-events` (override with `KAFKA_TOPIC`).

Message key = `aggregateId` (order id, or user id for `WALLET_DEBITED`). Same order stays on the same partition, so consumers see that order’s events in publish order.

## Events we publish

| Type | When |
| --- | --- |
| `ORDER_CREATED` | New order persisted (`payNow` true or false). Not on HTTP replay. |
| `ORDER_CONFIRMED` | Pay succeeded (wallet or simulated card; one-step or two-step). |
| `ORDER_PREPARING` | Kitchen advanced from `CONFIRMED`. |
| `ORDER_READY` | Kitchen advanced from `PREPARING`. |
| `ORDER_OUT_FOR_DELIVERY` | Kitchen advanced from `READY`. |
| `ORDER_DELIVERED` | Kitchen advanced from `OUT_FOR_DELIVERY`. |
| `PAYMENT_SUCCESS` | Same commit as confirm. |
| `PAYMENT_FAILED` | Two-step decline committed (`FAILED` row). |
| `WALLET_DEBITED` | New wallet debit (not replay, not credit/refund). |
| `ORDER_CANCELLED` | Cancel/refund committed. |
| `COMPLAINT_CREATED` | New complaint persisted. Not on HTTP replay. |
| `COMPLAINT_UPDATED` | Staff status change (`eventId` includes the new status). |

`ORDER_DELIVERED` (and preparing / ready / out for delivery) is published from `PATCH /api/kitchen/orders/{id}`.


Event ids are **deterministic** (`type + aggregate`). A retry publish of the same confirm is the same `eventId`. Consumers claim `(event_id, consumer_name)` in `processed_domain_events`.

## Consumer groups

Same topic, three groups. Each group gets every message (fan-out):

```text
Order / Payment / Wallet  --afterCommit-->  samosa.domain-events
                                              |
                    +-------------------------+-------------------------+
                    |                         |                         |
           samosa-notification         samosa-analytics        samosa-recommendation
                    |                         |                         |
           mock EMAIL/SMS/PUSH              log                       log (observe only)
```

Notification is **Strategy**: `NotificationChannel` with logging email/SMS/push. No SES, no Twilio.

`samosa-recommendation` does **not** write a preference table. `GET /api/recommendations` rebuilds taste from paid orders. The listener exists so a later collaborative model can hook the same group without a new topic.

If Kafka is down after commit, we **log the error** and still return 201. The HTTP request must not fail because the broker blipped.

## Local run

Compose includes Kafka (KRaft). Host `spring-boot:run` defaults to `KAFKA_ENABLED=false`. The **API container** sets `KAFKA_ENABLED=true` and `KAFKA_BOOTSTRAP_SERVERS=kafka:19092`.

The broker advertises **two** listeners: `localhost:9092` for a process on the host, `kafka:19092` for containers. See [docker.md](docker.md).

```bash
docker compose up -d --build
# or infra + host JVM:
docker compose up -d postgres redis kafka minio
export KAFKA_ENABLED=true
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
cd backend && ./mvnw spring-boot:run
```

## Isolation from the AI

The agent must not produce Kafka records. Java publishes after a successful commit. A hallucinated “I notified the kitchen” is not a consumer offset.
