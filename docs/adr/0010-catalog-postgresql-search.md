# ADR 0010: PostgreSQL-backed catalog search

Status: Accepted

## Context

Catalog discovery needs filtering, pagination, and text search without introducing a second search platform during the foundation phase.

## Decision

Use PostgreSQL relational queries with an English `tsvector`/GIN index and a small native-query adapter with whitelisted sort expressions.

## Alternatives Considered

Elasticsearch/OpenSearch and an application-side in-memory search were rejected as premature or operationally weak.

## Consequences

Catalog has one authoritative store and a clear migration path to a projection later. Search syntax and database-specific SQL require integration coverage.
