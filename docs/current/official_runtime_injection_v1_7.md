# Official Runtime Injection v1.7

Date: 2026-06-20

No AppKey, Authorization value, full endpoint URL, request body, or `config.local.json` content is recorded here.

## P0 Root Cause

v1.6 added `OfficialRuntimeGateway`, `ProviderBackedOfficialRuntimeGateway`, and `OfficialRuntimeIntegrator`, but production `AppViewModel` still created `ProviderBackedOfficialRuntimeGateway()` directly. The no-argument gateway uses ConfigMissing retrieval providers, so Query Rewrite, Embedding, and Text Similarity could only fall back locally in production even if demo config existed.

## v1.7 Fix

- Added `OfficialRuntimeGatewayFactory.production()`.
- `AppViewModel` default runtime gateway now comes from that factory.
- The factory injects existing Vivo retrieval contracts:
  - `VivoQueryRewriteProvider -> VivoQueryRewriteLearningProvider`
  - `VivoEmbeddingProvider -> VivoEmbeddingLearningProvider`
  - `VivoTextSimilarityProvider -> VivoTextSimilarityLearningProvider`
- `VivoQueryRewriteProvider` uses the official docId 2061 `prompts` request-body schema instead of the old `{ "query": ... }` shape.
- Missing config and runtime failures still return honest status and fall back locally.

## Current Status

| Capability | Current state | Demo/cloud requirement |
| --- | --- | --- |
| Query Rewrite | OFFICIAL_RUNTIME_READY / VALIDATION_PENDING | Requires configured official provider and successful runtime call before showing `OFFICIAL_RUNTIME_USED`. |
| Embedding | OFFICIAL_RUNTIME_READY / VALIDATION_PENDING | Requires configured official provider and returned vector before saving `vectorSource=OFFICIAL`. |
| Text Similarity | OFFICIAL_RUNTIME_READY / VALIDATION_PENDING | Requires configured official provider and returned scores before saving `scoreSource=OFFICIAL`. |

## Non-goals

- No real provider network smoke was run.
- No local config content was read or recorded.
- Provider smoke scripts were not changed.
- Translation, official TTS, Function Calling, and ASR Long are not claimed complete by this injection fix.

## Validation Criteria

- Production code must not directly instantiate a no-argument ConfigMissing-only retrieval gateway as the default.
- Missing config must still preserve local fallback.
- Diagnostics must distinguish ready, used, failed, not configured, and fallback states.
