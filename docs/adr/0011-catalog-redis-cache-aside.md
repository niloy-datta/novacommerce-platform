# ADR 0011: Redis cache-aside for product details

Status: Accepted

## Context

Product detail reads are a likely hot path, while PostgreSQL remains the source of truth.

## Decision

Cache serialized public product details in Redis with a bounded TTL and post-commit invalidation. Cache errors fall back to PostgreSQL.

## Alternatives Considered

Write-through caching and Redis as the primary store were rejected because they complicate correctness and ownership.

## Consequences

Reads can be faster, but stale data is possible within the TTL and Redis is an optional runtime dependency for correctness.
