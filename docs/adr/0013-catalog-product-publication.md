# ADR 0013: Explicit product publication state

Status: Accepted

## Context

Operators need to prepare product data before shoppers can see it.

## Decision

Products start as `DRAFT`; activation requires at least one active variant. `ARCHIVED` products are excluded from public discovery.

## Alternatives Considered

A boolean published flag was rejected because it cannot represent the lifecycle states needed for safe operations.

## Consequences

Publication is a domain rule with testable transitions, and later moderation or scheduling can extend the state machine without changing public semantics.
