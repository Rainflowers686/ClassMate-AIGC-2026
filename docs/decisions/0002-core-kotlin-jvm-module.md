# ADR-0002: `core` is a pure Kotlin/JVM module

- Status: Accepted (2026-05-31)

## Decision

Put the entire domain (models, providers, prompt, parser, validation, evidence, feedback, review,
logging) in a pure Kotlin/JVM library `core`, with **no Android dependency**. `app` is the only
Android module.

## Why

- Fast, deterministic unit tests with plain JUnit via `:core:test` (the project's verification
  command), with no Robolectric/instrumentation.
- Clear boundary: the model/business logic can be reused (CLI, server, finals features) without
  dragging in Android.
- Keeps `core` dependency-light — networking is behind `HttpTransport`, so no HTTP client is
  compiled into the domain.

## Consequences

- The two verification commands map cleanly: `:core:test` (JVM) and `:app:testDebugUnitTest`
  (Android unit tests).
- A real HTTP transport / BlueLM signer is implemented in the app/data layer and injected.
