# ADR 0016: PostgreSQL concurrency for inventory reservations

## Status
Accepted

## Context
Multiple instances must prevent overselling and partial multi-item holds.

## Decision
Inventory owns a dedicated PostgreSQL database. Operations sort variant UUIDs, take row-level write locks in that order, and use database checks as the final invariant boundary. Available quantity is derived.

## Alternatives Considered
Java locks, Redis locks, optimistic-only retries, and persisted available quantity.

## Consequences
Correctness spans instances and deadlock risk is reduced. Contention queues on rows and PostgreSQL-specific behavior needs real integration tests.
