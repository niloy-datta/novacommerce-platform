# ADR 0022: Synchronous Inventory Reservation

- **Status:** Accepted
- **Context:** Phase 4 must tell the customer whether stock was reserved without adding an event workflow with no consumer requirement.
- **Decision:** Order synchronously calls Inventory after committing a pending order, then finalizes in a second short transaction.
- **Alternatives considered:** Kafka choreography; a long database transaction around the remote call.
- **Consequences:** The flow is understandable and recoverable through idempotency. Inventory latency affects checkout and unknown outcomes require retry.
