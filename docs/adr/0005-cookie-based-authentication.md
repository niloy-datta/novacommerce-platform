# ADR 0005: Cookie-Based Browser Authentication

## Status

Accepted.

## Context

The web client needs browser authentication without exposing reusable credentials to JavaScript storage.

## Decision

Use HttpOnly `NC_ACCESS` and `NC_REFRESH` cookies. Access tokens are short lived; refresh cookies are limited to the auth API path.

## Alternatives Considered

Returning bearer tokens to JavaScript would simplify API clients but increases token-exfiltration exposure. HTTP sessions would make cross-service authentication stateful.

## Consequences

Cookie mutations require CSRF protection and explicit credentialed CORS configuration.
