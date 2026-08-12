# ADR 0009: Argon2 Password Hashing

## Status

Accepted.

## Context

Passwords need a modern adaptive, salted password-storage algorithm.

## Decision

Use Spring Security's Argon2id encoder with 19 MiB memory, two iterations, and parallelism one.

## Alternatives Considered

BCrypt is supported but is not selected for this new service. Manual cryptographic implementations are excluded.

## Consequences

Password hashing has deliberate CPU and memory cost. Password data is never logged or returned.
