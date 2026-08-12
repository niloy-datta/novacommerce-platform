# Authentication Architecture

## Model

The auth service owns identity data in `novacommerce_auth`. Passwords use Argon2id; access tokens are 15-minute RS256 JWTs with issuer, audience, UUID subject, expiry, token ID, and role claims. Refresh tokens are random 256-bit opaque values; only their SHA-256 hashes are stored.

Browser authentication uses HttpOnly `NC_ACCESS` and `NC_REFRESH` cookies. `NC_REFRESH` is restricted to `/api/v1/auth`. Browser mutations require Spring Security CSRF tokens, exposed by `GET /api/v1/auth/csrf`; the frontend keeps only that non-authentication CSRF value in memory. HTTPS and `AUTH_COOKIE_SECURE=true` are mandatory outside local HTTP development.

## Registration and Login

```mermaid
sequenceDiagram
    participant B as Browser
    participant A as Auth service
    participant D as Auth database
    B->>A: GET /csrf
    A-->>B: CSRF token
    B->>A: POST /register or /login + CSRF header
    A->>D: Store Argon2 hash / refresh-token hash
    A-->>B: Safe user JSON and, on login, HttpOnly cookies
```

Public registration normalizes email and assigns `CUSTOMER` only. Login returns safe user data but never token values.

## Authenticated Requests, Refresh, and Logout

```mermaid
sequenceDiagram
    participant B as Browser
    participant A as Auth service
    participant D as Auth database
    B->>A: GET /me with NC_ACCESS cookie
    A-->>B: Current user after JWT validation
    B->>A: POST /refresh + CSRF + NC_REFRESH
    A->>D: Lock token, revoke old, persist hashed successor
    A-->>B: Replaced HttpOnly cookies
    B->>A: POST /logout + CSRF
    A->>D: Revoke presented refresh token
    A-->>B: Clear both cookies
```

Refresh rotation is transactional and locks the token row. Reuse of a revoked/rotated token revokes all active tokens in its family and returns a controlled authentication error. This may sign out a legitimate client if a stolen old token is replayed; that trade-off contains credential replay promptly.

## Key Management and Boundaries

RSA keys come from configured PEM paths. The key-generation helper creates local files under ignored `.local/keys/`; automated tests generate an in-memory RSA pair instead. Other services must not query auth tables. They will validate access JWTs through the auth public-key distribution design introduced in a later phase.
