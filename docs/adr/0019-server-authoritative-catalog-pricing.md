# ADR 0019: Server-Authoritative Catalog Pricing

- **Status:** Accepted
- **Context:** Browser totals are untrusted and cart prices become stale.
- **Decision:** Checkout resolves all variants in one Catalog call and snapshots the current Catalog prices. Requests contain cart identity, never authoritative money.
- **Alternatives considered:** Trusting submitted totals; persisting cart prices as authority.
- **Consequences:** Orders are tamper-resistant and historical totals remain stable. Checkout depends synchronously on Catalog availability.
