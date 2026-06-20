# Official Runtime Wiring v1.6

Date: 2026-06-20

Superseded note: v1.6 introduced the runtime gateway and integrator, but Claude v2 later found that production `AppViewModel` still defaulted to a no-argument ConfigMissing-only retrieval gateway. v1.7 fixes that with `OfficialRuntimeGatewayFactory.production()`. Keep this file as v1.6 history; use `docs/current/official_runtime_injection_v1_7.md` and `docs/current/project_current_status_v1_8.md` for current retrieval status.

No AppKey, Authorization value, full endpoint URL, request body, or local `config.local.json` content is recorded here.

> v1.7 current-status note: v1.6 established gateway plumbing, but production `AppViewModel` still defaulted to ConfigMissing retrieval providers. v1.7 fixes that production injection gap with `OfficialRuntimeGatewayFactory.production()`, which injects the existing Vivo Query Rewrite / Embedding / Text Similarity adapters. This v1.6 report remains useful as the gateway design record.

## Scope

This sprint moves official AI capabilities from smoke/seam/local-only status toward the real L3 App learning pipeline. It does not run provider network smoke. Official runtime success is recorded only when an injected app adapter returns success. Otherwise the App keeps the learning pipeline alive through explicit local fallback.

## Runtime Layer

Code-level additions:

- `OfficialRuntimeGateway`
- `OfficialRuntimeResult`
- `OfficialRuntimeStatus`
- `ProviderBackedOfficialRuntimeGateway`
- `OfficialRuntimeIntegrator`

Every runtime result records:

- capability
- runtime status
- output summary
- error code/message when relevant
- `fallbackUsed`
- `sensitiveFieldsRedacted = true`

The gateway is called from the single L3 publish path in `AppViewModel`, so text, question bank, OCR image text, PDF manual page text, and transcript fallback all share the same provenance model.

## Capability Status

| Capability | v1.6 App pipeline status | What changed | Fallback |
| --- | --- | --- | --- |
| OCR | `OFFICIAL_RUNTIME_USED` when OCR evidence enters L3 and OCR config is present; otherwise `OFFICIAL_RUNTIME_READY` or not configured | OCR evidence now carries provider provenance; diagnostics show used/ready/fallback. | Manual OCR text fallback remains. |
| Query Rewrite | Official runtime gateway is now in the L3 path. Fake/runtime-adapter success is tested as `OFFICIAL_RUNTIME_USED`; default missing adapter becomes `OFFICIAL_APP_WIRING_PENDING` or not configured. | Rewritten query can drive semantic search and pipeline step logs. | Local safe rewrite/query planning. |
| Embedding | Official vector path is wired through `EmbeddingProvider`; success saves `officialVector` and `vectorSource=OFFICIAL`. | `LocalSemanticIndexRecord` now stores official/local vectors and source. | Local lexical vector persists and search continues. |
| Text Similarity | Official rerank path is wired through `TextSimilarityProvider`; success writes `scoreSource=OFFICIAL`. | Similarity matches and similar-question recommendations expose score provenance. | Local token/vector similarity. |
| ASR Long | Core `VivoAsrProvider` 1739 contract is recognized; App runtime status remains `OFFICIAL_APP_WIRING_PENDING` until non-sensitive upload/poll/result validation. | Runtime diagnostics distinguish core-present/app-pending from schema-missing. | Manual transcript fallback enters L3. |
| Translation | `TranslationProvider` runtime path is wired for lesson/evidence translation. | Official success would persist translated derived artifact; default missing adapter remains not configured/app-pending. | Original evidence remains unchanged. |
| TTS | `TtsProvider` runtime path is wired before local playback. | Official success can produce an audio artifact; Android local TTS remains the usable fallback. | Android local TTS or script text. |
| Function Calling | `FunctionCallingProvider` runtime path is wired for tool-plan proposal. | Official success can override/prove a tool proposal; default uses local orchestrator. | Local ToolOrchestrator remains active. |
| Edge model | Edge availability is checked in the runtime integrator. | Available edge model can be marked used for study fallback; unavailable state records local-rule fallback. | Local rule fallback. |

## Diagnostics Contract

Each capability now reports:

- official runtime configured or not
- official runtime used or not
- fallback used or not
- exact blocker code
- redaction flag

Typical current demo-device statuses:

- `OCR`: `OFFICIAL_RUNTIME_READY` or `OFFICIAL_RUNTIME_USED` for OCR evidence
- `QUERY_REWRITE`: `OFFICIAL_APP_WIRING_PENDING` or `LOCAL_FALLBACK_USED`
- `EMBEDDING`: `LOCAL_FALLBACK_USED` unless an embedding adapter returns vectors
- `TEXT_SIMILARITY`: `LOCAL_FALLBACK_USED` unless a rerank adapter returns scores
- `ASR_LONG`: `OFFICIAL_APP_WIRING_PENDING` or `OFFICIAL_RUNTIME_NOT_CONFIGURED`
- `TRANSLATION`: `OFFICIAL_RUNTIME_NOT_CONFIGURED` unless provider config/adapter are injected
- `TTS`: `LOCAL_FALLBACK_USED` when Android local TTS is available
- `FUNCTION_CALLING`: `LOCAL_FALLBACK_USED` unless official tool proposal succeeds
- `EDGE_MODEL`: `OFFICIAL_RUNTIME_USED` only when the on-device model reports available

## Tests

New coverage proves:

- Query Rewrite official success path and fallback path.
- Embedding official vector save and local fallback save.
- Text Similarity official score source and local fallback source.
- OCR evidence provider provenance.
- ASR core-present/app-pending status.
- Translation/TTS/Function Calling official gateway hooks with local fallback.
- No secret fields in runtime diagnostics.

## Hard Limits

- No live provider network call was run in this task.
- No local config content was read.
- Provider smoke scripts were not changed.
- Query Rewrite / Embedding / Text Similarity runtime success requires the v1.7 production adapter injection plus demo/cloud validation.
- Official ASR Long remains pending non-sensitive upload/poll/result validation.
