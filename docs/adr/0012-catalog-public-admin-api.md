# ADR 0012: Separate public and admin catalog APIs

Status: Accepted

## Context

Storefront readers and catalog operators have different authorization, validation, and response needs.

## Decision

Expose public read-only endpoints under `/api/v1` and authenticated admin writes under `/api/v1/admin/catalog`, with DTOs at both boundaries.

## Alternatives Considered

One controller surface with implicit role checks was rejected because it obscures exposure and makes accidental publication easier.

## Consequences

The contract is explicit and easier to secure and evolve, at the cost of a small amount of endpoint duplication.
