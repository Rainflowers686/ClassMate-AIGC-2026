> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# Red-team v2 Official Runtime Review

Date: 2026-06-20

## Finding

Claude v2 found a P0 credibility issue in the v1.6 runtime wiring: the gateway plumbing existed, but production `AppViewModel` still defaulted to a no-argument `ProviderBackedOfficialRuntimeGateway()`. That constructor uses ConfigMissing retrieval providers, so Query Rewrite, Embedding, and Text Similarity could never reach the real Vivo runtime in the production App even when demo config was present.

## v1.7 Fix

- Production `AppViewModel` now uses `OfficialRuntimeGatewayFactory.production()`.
- The factory injects:
  - `VivoQueryRewriteProvider -> VivoQueryRewriteLearningProvider`
  - `VivoEmbeddingProvider -> VivoEmbeddingLearningProvider`
  - `VivoTextSimilarityProvider -> VivoTextSimilarityLearningProvider`
- Missing config or runtime failure still returns explicit not-configured/failure status and falls back locally.
- `OFFICIAL_RUNTIME_USED` is only valid when the provider call succeeds.
- No live provider network smoke was run for this fix.

## Remaining Validation

- Cloud/demo device must prove official runtime configured and used for Query Rewrite, Embedding, and Text Similarity.
- If config is missing, diagnostics must show validation pending or local fallback, not official success.
- Translation, official TTS, official Function Calling, and ASR Long are not upgraded by this fix and must keep honest seam/not-configured wording.

## Claude v3 Ask

Claude v3 should verify:

- The factory injection point cannot regress to the no-argument ConfigMissing gateway.
- Docs no longer overclaim smoke PASS as live App runtime success.
- The demo script separates stable demo items from validation-pending official runtime items.
- Security boundaries around config, endpoints, Authorization, and local model files remain intact.
