# ADR 0008: CSRF Protection for Browser Clients

## Status

Accepted.

## Context

Authentication cookies accompany browser requests automatically.

## Decision

Keep Spring Security CSRF protection enabled. The frontend obtains a CSRF token from the auth API and supplies its expected header for unsafe requests.

## Alternatives Considered

Disabling CSRF or using wildcard credentialed CORS would weaken browser protections.

## Consequences

The auth client must fetch CSRF state before registration, login, refresh, and logout.
