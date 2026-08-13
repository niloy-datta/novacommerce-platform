# ADR 0017: Reservation idempotency and distributed expiration

## Status
Accepted

## Context
Retries must not reserve twice, and multiple schedulers must not double-release.

## Decision
Store a unique caller key plus canonical SHA-256 request hash, serialized by a transaction advisory lock. Claim bounded expiry batches with `FOR UPDATE SKIP LOCKED` and release transactionally.

## Alternatives Considered
In-memory deduplication, Redis authority, unbounded polling, and single-instance scheduling.

## Consequences
Retries are deterministic and workers coordinate safely. This deliberately depends on PostgreSQL.
