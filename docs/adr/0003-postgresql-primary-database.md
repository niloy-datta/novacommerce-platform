# ADR 0003: PostgreSQL as the Primary Database

## Status

Accepted.

## Context

Commerce workflows need durable, transactional relational storage. The local development foundation also needs a database with broad production support.

## Decision

Use PostgreSQL as the planned primary database. Each service will own its schema, migrations, and data access; no service may use another service's database as an integration mechanism.

## Alternatives Considered

MongoDB and search databases are not needed for Phase 0. A shared database would be simpler initially but would erase service ownership boundaries.

## Consequences

Future persistence work will add Flyway and a PostgreSQL driver only to services that need them. Cross-service data requirements will use APIs or events rather than cross-schema reads.
