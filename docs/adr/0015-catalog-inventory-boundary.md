# ADR 0015: Catalog and inventory ownership boundary

## Status

Accepted.

## Context

Product merchandising and stock correctness change for different reasons and require different concurrency models.

## Decision

Catalog owns product identity, SKU identity, descriptive data, merchandising price, images, publication state, and discovery. The planned Inventory Service will exclusively own available, reserved, and sold quantities and reservation behavior.

## Alternatives Considered

Adding stock columns to Catalog would simplify early reads but couple product editing to high-contention inventory transactions and blur ownership.

## Consequences

Catalog never presents stock as its own fact. Later storefront availability will require an explicit Inventory API or event-derived view.
