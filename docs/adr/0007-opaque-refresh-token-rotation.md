# ADR 0007: Opaque Refresh Token Rotation

## Status

Accepted.

## Context

Browser sessions need revocation and replay containment beyond a short access-token lifetime.

## Decision

Use random 256-bit opaque refresh tokens, store only SHA-256 hashes, rotate them transactionally, and revoke a token family after replay.

## Alternatives Considered

JWT refresh tokens are harder to revoke safely. Storing raw opaque tokens increases database-exposure impact.

## Consequences

Refresh persistence and row locking are part of the auth service's correctness boundary.
