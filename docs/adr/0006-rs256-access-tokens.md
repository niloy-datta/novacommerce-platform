# ADR 0006: RS256 Access Tokens

## Status

Accepted.

## Context

Future services need verifiable, short-lived identity assertions without receiving the signing secret.

## Decision

Issue 15-minute JWT access tokens signed with RS256. Validate signature, issuer, audience, and expiry with Spring Security JWT infrastructure.

## Alternatives Considered

HS256 would share signing material with validators. A full authorization server is unnecessary for this phase.

## Consequences

Private keys must be external configuration; public-key distribution is a later integration concern.
