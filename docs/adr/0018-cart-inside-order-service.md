# ADR 0018: Keep Cart Inside Order Service

- **Status:** Accepted
- **Context:** Cart and checkout share ownership, locking, and lifecycle invariants; a separate deployment would add a distributed transition before it provides independent scaling value.
- **Decision:** Order Service owns carts and orders in one database with separate domain packages.
- **Alternatives considered:** A Cart microservice; browser-only carts.
- **Consequences:** Checkout can lock and transition a cart atomically. Cart cannot yet scale independently, and guest carts remain out of scope.
