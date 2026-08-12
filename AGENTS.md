# NovaCommerce contributor guidance

## Engineering principles

- Prefer correctness and maintainability over cleverness; do not over-engineer.
- Add dependencies only when they have a clear purpose. Keep services independently deployable.
- Keep business logic out of controllers and preserve explicit domain boundaries.
- A service owns its data: never access another service's database directly.
- Validate external input, never commit secrets, and never fabricate benchmarks or completed functionality.
- Add or update tests when behavior changes. Keep documentation and the README synchronized with implemented behavior.

## Java conventions

- Use the `com.novacommerce` package root.
- When useful, organize code as `api`, `application`, `domain`, `infrastructure`, and `config`; do not add empty layers just to satisfy a pattern.
- Use DTOs at REST boundaries. Do not expose JPA entities through REST APIs.
- Use constructor injection, meaningful exception types, and immutable data where reasonable.
- Introduce centralized API error handling when APIs are added.

## Frontend conventions

- Use strict TypeScript, functional React components, and the Next.js App Router.
- Keep UI components separate from data-access code.
- Use accessible semantic HTML with explicit loading, error, and empty states.
- Do not use `any` without a documented reason.

## Testing

Future tests should be clearly categorized as unit, integration, or end-to-end tests. Critical business behavior must be tested.
