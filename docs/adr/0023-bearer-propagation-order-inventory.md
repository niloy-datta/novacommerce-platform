# ADR 0023: Bearer Propagation from Order to Inventory

- **Status:** Accepted
- **Context:** Inventory owns reservation authorization and requires caller identity, while browser cookies should not become a service-to-service convention.
- **Decision:** Order forwards the validated caller JWT in an Authorization bearer header. Inventory accepts header bearer first and narrows CSRF exemption to explicit bearer mutations.
- **Alternatives considered:** Forward browser cookies; introduce client credentials before a machine-identity design exists.
- **Consequences:** Ownership checks retain end-user context and browser CSRF stays intact. Token propagation will be revisited with production service identity.
