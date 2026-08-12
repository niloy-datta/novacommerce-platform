# ADR 0004: Initial Service Boundaries

## Status

Accepted.

## Context

The platform must demonstrate distributed-system boundaries without starting with a fragile collection of overly small services.

## Decision

Begin with six planned service boundaries: auth, catalog, inventory, order, payment, and notification. They share build conventions through Maven parent POMs but have no shared runtime database or business module.

## Alternatives Considered

A single modular monolith would reduce operations but would not exercise independent deployment boundaries. Splitting into dozens of services now would add coordination and deployment complexity without real workflows.

## Consequences

The boundaries may be refined when domain behavior is implemented. Kafka will be introduced for concrete asynchronous workflows rather than as a general-purpose coupling mechanism.
