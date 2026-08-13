# ADR 0020: Immutable Order Item Snapshots

- **Status:** Accepted
- **Context:** Catalog names, SKUs, attributes, and prices change while order history must not.
- **Decision:** Order items snapshot checkout-time descriptive and monetary fields and are never hydrated from Catalog for historical reads.
- **Alternatives considered:** Store only variant IDs and join remotely during reads.
- **Consequences:** History is durable and reads are isolated from Catalog changes, at the cost of intentional data duplication.
