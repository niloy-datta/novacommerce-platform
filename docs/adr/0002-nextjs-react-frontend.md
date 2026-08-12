# ADR 0002: Next.js and React Frontend

## Status

Accepted.

## Context

The platform needs a modern TypeScript web application while retaining Java services as the eventual business-logic boundary.

## Decision

Use Next.js 16, React, strict TypeScript, and the App Router. Keep the frontend focused on presentation and data-access clients; commerce rules remain in Spring Boot services.

## Alternatives Considered

A standalone React single-page application would require assembling routing and rendering concerns independently. Implementing commerce behavior in Next.js would blur backend ownership.

## Consequences

The frontend can evolve independently and will need explicit API contracts as service APIs are introduced. No UI framework is introduced during Phase 0.
