# ADR-0001: BlueLM-first, fallback only

- Status: Accepted (2026-05-31)

## Decision

vivo BlueLM (蓝心大模型) is the **primary** intelligence. `BlueLMProvider` is the main path.
`CompatibleProvider` (OpenAI-compatible) is a secondary backup. `LocalFallbackProvider` is a
bounded, deterministic, offline safety net — never the headline.

`ProviderResolver` orders them `BlueLM → Compatible → LocalFallback`. `CourseAnalyzer` falls back
on non-2xx, parse failure, or validation failure.

## Why

The competition value is *real* model understanding. The fallback exists only so a demo never
shows an empty screen; it must not masquerade as the model.

## Consequences

- Round 1 ships BlueLM **wired but inert** (no real keys, default `NoNetworkTransport` /
  `UnconfiguredBlueLmSigner`), so it cleanly reports `CONFIG_MISSING` and falls back. It never
  fabricates a model response.
- Going live = inject a real `HttpTransport`, a real `BlueLmSigner`, and `Credential.BlueLm`.
- The bundled sample uses curated, clearly-labelled demo data (not a faked live call).
