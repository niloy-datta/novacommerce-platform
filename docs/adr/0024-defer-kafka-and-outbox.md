# ADR 0024: Defer Kafka and Transactional Outbox

- **Status:** Accepted
- **Context:** Phase 4 has no implemented downstream event consumer and adding event infrastructure would create unverified guarantees.
- **Decision:** Keep checkout synchronous and do not publish order events yet.
- **Alternatives considered:** Add placeholder producers/outbox rows now.
- **Consequences:** Current reliability boundaries are explicit and smaller. Phase 5 must introduce event contracts and outbox delivery together with real consumers.
