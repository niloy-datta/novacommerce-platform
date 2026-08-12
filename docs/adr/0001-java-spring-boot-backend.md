# ADR 0001: Java and Spring Boot Backend

## Status

Accepted.

## Context

NovaCommerce needs a stable, modern JVM baseline that supports production-style service development and is suitable for full-stack and backend interview preparation.

## Decision

Use Java 21 LTS and Spring Boot 4.1.x with Maven. Services use the `com.novacommerce` package root and remain individually buildable modules.

## Alternatives Considered

Java 17 is supported but omits Java 21 language and concurrency capabilities. A newer JDK baseline is not selected because Java 21 is the agreed portfolio and local-development standard.

## Consequences

Contributors need Java 21. The platform can use Java 21 features where they improve clarity, but it does not mandate them before a concrete use case exists.
