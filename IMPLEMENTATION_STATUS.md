# NovaCommerce implementation status

Audit basis: repository source, migrations, tests, configuration, Docker Compose, and frontend files inspected on 2026-08-25.

## Services and feature status

| Area | Status | Evidence / notes |
| --- | --- | --- |
| Auth service | COMPLETE | RS256 JWTs, JWKS, refresh-token rotation, cookies, CSRF, Flyway migrations, integration tests. |
| Catalog service | COMPLETE | Product/category/brand administration, public search, PostgreSQL search migration, Redis cache, JWT/CSRF security, integration tests. |
| Inventory service | COMPLETE | PostgreSQL-owned stock, ordered pessimistic locking, reservations, expiration, idempotency-key advisory lock, release/commit, concurrency-oriented tests. |
| Order/cart service | PARTIAL | Cart, authoritative catalog snapshots, checkout idempotency, reservation coordination, recovery, cancellation, and order outbox exist. Payment/event integration and outbox publishing are absent. |
| Payment service | PARTIAL | Persistence, basic mock/placeholder Stripe adapters, authorize/capture/refund endpoints, and an outbox table exist. State transitions, operation idempotency, ownership, cancellation, partial refunds, webhook verification/deduplication, and publishing are incomplete. |
| Notification service | PARTIAL | Notification entity and migration exist. No consumer, dispatcher, provider abstraction, deduplication, retry worker, or API is implemented. |
| Frontend | PARTIAL | Next.js storefront, auth, catalog, cart/order pages, and payment client exist. Checkout does not initiate payment, there is no payment UX/status stream, and no notification surface. |

## Persistence

| Database/service | Tables | Status |
| --- | --- | --- |
| Auth | `users`, `user_roles`, `refresh_tokens` | COMPLETE |
| Catalog | brands, categories, products, product_images, product_variants | COMPLETE |
| Inventory | `inventory_items`, `inventory_reservations`, reservation items, `inventory_movements` | COMPLETE |
| Order | carts, cart_items, orders, order_items, `outbox_events` | PARTIAL: no publisher/claiming metadata |
| Payment | `payments`, `outbox_events` | PARTIAL: missing operation/webhook dedupe and monetary accounting fields |
| Notification | `notifications` | PARTIAL: missing event dedupe and delivery metadata |

## Platform status

- Kafka: `apache/kafka:4.0.2` is declared in Compose, but no producer, consumer, topic contract, retry, or dead-letter implementation exists.
- Redis: used for catalog cache-aside reads; not used for transactional inventory/order/payment correctness.
- Security: RS256 issuer/audience validation and cookie bearer resolution exist in resource services; payment ownership/admin authorization needs tightening.
- Docker: PostgreSQL, Redis, and Kafka Compose services exist. The init bind mount requires SELinux relabeling on this host; payment/notification databases are present in init SQL.
- Testing: unit and fast integration tests exist across services; PostgreSQL Testcontainers concurrency tests are present but require Docker. Payment/notification critical-path tests are insufficient.
- CI/CD: no GitHub Actions workflow is present.
- Observability: Actuator health/info and a small number of Micrometer counters exist; no outbox/Kafka/payment latency metrics or correlation propagation exists.

## Prioritized checklist

1. COMPLETE — preserve and extend verified inventory/order correctness tests; add missing duplicate-checkout coverage.
2. IN PROGRESS — make payment state transitions, operation idempotency, ownership, gateway abstraction, webhook verification/deduplication, and migrations production-safe.
3. IN PROGRESS — add a transactional outbox contract and bounded publisher/consumer seams without XA or remote calls inside DB transactions.
4. PENDING — connect payment outcomes and compensation to order/inventory using idempotent APIs/events.
5. PENDING — implement notification consumption, deduplication, retries, and non-blocking delivery.
6. PENDING — add minimal payment/order status frontend UX with retry-safe keys and no interval polling.
7. PENDING — add CI, performance/latency measurement hooks, metrics, and architecture documentation.

## Known environment constraints

- The checked-in Maven wrapper is incomplete because `.mvn/wrapper/maven-wrapper.jar` is missing; system Maven is used locally.
- This workspace uses Java 25 while the project targets Java 21; compilation and runtime compatibility must be verified rather than assumed.
- Local Docker PostgreSQL/Redis ports 5432/6379 were already occupied, so the ignored local `.env` uses 55432/56379 for the Compose stack.
