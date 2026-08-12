# ADR 0014: JWKS-based resource-server authentication

## Status

Accepted.

## Context

Catalog must authorize Auth-issued identities without sharing Auth persistence or private signing material.

## Decision

Auth publishes its public RSA key as JWKS with a stable `kid`. Catalog resolves that JWKS and validates RS256 signature, expiry, issuer, and audience locally, then maps the `roles` claim to Spring authorities. Cookie-carried access tokens retain CSRF protection for mutations.

## Alternatives Considered

Calling `/me` for every request would add latency and runtime coupling. Sharing a private key or the Auth database would violate service ownership.

## Consequences

Catalog can authenticate independently and tolerates temporary Auth outages after key retrieval, while key publication and rotation require a stable JWKS contract.
