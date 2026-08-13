# ADR 0021: Idempotent Checkout

- **Status:** Accepted
- **Context:** Retries and concurrent clicks can otherwise create duplicate orders or reservations.
- **Decision:** Scope checkout keys to the owner, bind them to a canonical request hash, lock the cart, and derive the Inventory key from the order ID.
- **Alternatives considered:** Client-side click suppression; random key per downstream attempt.
- **Consequences:** Same-intent retries converge on one logical order. Reusing a key for changed intent returns a conflict.
