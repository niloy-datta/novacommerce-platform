# NovaCommerce

### Distributed Commerce & Payment Platform

NovaCommerce is a production-style distributed commerce platform designed to demonstrate full-stack engineering, transactional correctness, concurrency control, event-driven workflows, reliability, observability, and cloud deployment.

## Project Overview

This repository contains independently deployable Spring Boot services, a Next.js storefront shell, local infrastructure definitions, design documentation, and implemented Authentication, Catalog, and Inventory foundations. Ordering, payment, and event workflows are **planned**, not implemented.

## Engineering Goals

- Model explicit service ownership and independently deployable boundaries.
- Build transactionally correct, observable, and well-tested workflows in future phases.
- Demonstrate pragmatic distributed-systems, performance, cloud, and full-stack engineering.

## Current Status

**Phase 3 — Inventory & Reservation Service (implementation complete; Docker validation pending).** Inventory now owns auditable stock, atomic multi-item reservations, deterministic PostgreSQL row locking, idempotent lifecycle operations, bounded expiration, and public availability. The real PostgreSQL concurrency suite is present but has not run in this Docker-unavailable environment. Ordering, payments, and event processing remain **planned**.

## Planned Architecture

The Next.js application owns user experience, while Spring Boot services own commerce rules and data. Auth owns `novacommerce_auth`; Catalog owns `novacommerce_catalog`. Catalog currently connects to PostgreSQL and can use Redis for cache-aside reads; Kafka remains a local foundation only. Read the [system overview](docs/architecture/system-overview.md) and [catalog architecture](docs/architecture/catalog.md).

## Technology Stack

- Backend: Java 21 LTS, Spring Boot 4.1, Maven, Spring Web, Validation, Actuator, Spring Security, Spring Data JPA, Flyway, PostgreSQL full-text search, Redis product caching, JUnit, and Testcontainers
- Authentication integration: RS256 JWTs, public JWKS distribution, and independent issuer/audience validation in resource services
- Frontend: Next.js 16, React 19, strict TypeScript, App Router, ESLint, and catalog-backed product discovery
- Local infrastructure: PostgreSQL 17, Redis 7.4, Apache Kafka 4.0 in KRaft mode
- Planned when justified: Kafka domain workflows, AWS, OpenTelemetry, Prometheus, Grafana, and GitHub Actions

## Authentication

The browser receives an HttpOnly `NC_ACCESS` cookie containing a 15-minute RS256 JWT and an HttpOnly `NC_REFRESH` cookie containing a seven-day opaque refresh token. The refresh token is SHA-256 hashed before persistence, rotates on every successful use, and revokes its remaining token family if a rotated token is replayed. Cookie-authenticated mutations use CSRF protection; the frontend requests `/api/v1/auth/csrf` before sending its CSRF header.

New public registrations receive only the `CUSTOMER` role. `ADMIN` exists for controlled future provisioning. The auth service owns the separate local-development `novacommerce_auth` PostgreSQL database. Production requires HTTPS, `AUTH_COOKIE_SECURE=true`, and externally supplied RSA key paths. See [authentication architecture](docs/architecture/authentication.md).

## Services

| Service | Responsibility | Status |
| --- | --- | --- |
| `auth-service` | Identity and access management | Implemented |
| `catalog-service` | Product and merchandising data | Implemented |
| `inventory-service` | Availability and reservation workflows | Planned |
| `order-service` | Order lifecycle coordination | Planned |
| `payment-service` | Payment authorization and capture coordination | Planned |
| `notification-service` | Customer and operational communications | Planned |

## Repository Structure

```text
frontend/web/             Next.js technical shell
services/                 Spring Boot service modules
infrastructure/docker/    Local PostgreSQL, Redis, and Kafka foundation
docs/                     Architecture and decision records
scripts/                  Future repository automation
```

## Local Development

1. Install Java 21 and Node.js 20.9 or later.
2. Copy `.env.example` to `.env` and choose a local PostgreSQL password.
3. Start infrastructure with `docker compose --env-file .env -f infrastructure/docker/compose.yaml up -d`.
4. Run backend tests with `./mvnw test` on macOS/Linux or `mvnw.cmd test` on Windows. A running service exposes `GET /actuator/health` on its configured port.
5. In `frontend/web`, run `npm install` and `npm run dev`, then visit `http://localhost:3000`.

The PostgreSQL-specific integration suites use Testcontainers and skip when Docker is unavailable; a skipped suite is not equivalent to a passing PostgreSQL execution.

## Engineering Roadmap

1. **Phase 0 (complete):** architecture, repository, runtime, and local infrastructure foundation.
2. **Phase 1 (complete):** Authentication & Identity Service.
3. **Phase 2 (complete):** Catalog & Product Discovery.
4. **Phase 3 (validation pending):** Inventory & Reservation Service; run the PostgreSQL/Testcontainers concurrency suite before marking complete.
5. **Phase 4 (planned):** Cart, Order & Checkout.
6. **Later (planned):** payment reliability, event-driven integration, observability, performance engineering, and cloud deployment.

## Security

Never commit `.env` files, access tokens, private keys, or production credentials. The local Compose stack takes its PostgreSQL password from the environment. Auth JWT keys are supplied through configured file paths; payment settings remain placeholders until their capabilities are implemented.

## Contributing

Read [AGENTS.md](AGENTS.md) before contributing. Keep changes focused, tested, and reflected in the relevant documentation.

## License

NovaCommerce is available under the [MIT License](LICENSE).
