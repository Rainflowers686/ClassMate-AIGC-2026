# Official Tool Productization Matrix

Date: 2026-06-20

No secrets, endpoint URLs, or Authorization values are recorded here.

## ClassMate Effective Capability Matrix

ClassMate product capability matrix has exactly 18 learning-loop capabilities:

1. Large model
2. Function calling
3. Image generation
4. Video generation
5. General OCR
6. Text translation
7. Text embedding
8. Text similarity
9. Query rewrite
10. Realtime short ASR
11. Long audio dictation
12. Long audio transcription
13. Dialect free speech
14. Simultaneous interpretation
15. Audio generation
16. Edge 3B large model
17. Edge text audit
18. Edge capability files

Image generation, video generation, and simultaneous interpretation are experimental and hidden by default. They produce prompts, storyboards, scripts, or bilingual transcript drafts unless a real configured runtime succeeds.

## Runtime Status

| Capability | Smoke status | App-level status | Product path |
| --- | --- | --- | --- |
| OCR | PASS | `OFFICIAL_RUNTIME_USED` for OCR evidence / config-gated | Image/photo OCR text and PDF page OCR seam can enter LessonSource and Evidence with provider provenance; manual OCR text fallback remains. |
| QUERY_REWRITE | PASS | `OFFICIAL_RUNTIME_READY / VALIDATION_PENDING` after v1.7 production adapter injection | L3 publish path now asks the production runtime gateway for study/retrieval query normalization. Official success records `OFFICIAL_RUNTIME_USED`; missing config/runtime failure falls back to local query planning. |
| EMBEDDING | PASS | `OFFICIAL_RUNTIME_READY / VALIDATION_PENDING` after v1.7 production adapter injection | Semantic records store `officialVector`, `localVector`, and `vectorSource`; official success saves `vectorSource=OFFICIAL`; missing config/runtime failure persists local lexical vectors. |
| TEXT_SIMILARITY | PASS | `OFFICIAL_RUNTIME_READY / VALIDATION_PENDING` after v1.7 production adapter injection | Evidence matching and similar-question recommendations record `scoreSource`; official success saves `scoreSource=OFFICIAL`; missing config/runtime failure uses local similarity fallback. |
| TRANSLATION | not product-smoked in app | runtime gateway wired / usually NOT_CONFIGURED | Lesson/evidence translation first checks official runtime; original evidence remains unchanged when not configured or failed. |
| TTS | not product-smoked in app | runtime gateway wired / LOCAL_FALLBACK | Listen-review checks official runtime first, then uses Android local TextToSpeech or script text. No voice identity feature is part of ClassMate. |
| FUNCTION_CALLING | not product-smoked in app | runtime gateway wired / LOCAL_ORCHESTRATOR fallback | Tool plan proposal can use official Function Calling when adapter succeeds; local ToolOrchestrator remains active. |
| ASR_LONG | not product-smoked in app | CORE_CONTRACT_PRESENT / APP_WIRING_PENDING / manual fallback | Recording/audio artifacts create ASR Long jobs. Core VivoAsrProvider 1739 create/upload/run/progress/result contract exists, but app-level upload/poll/result validation is pending. |
| Edge model | local availability dependent | EDGE_MODEL_USED when available / LOCAL_RULE_FALLBACK | On-device availability now feeds the runtime fallback strategy for offline summary/practice/review; unavailable devices use local rules. |

## v1.3 Additions

- `SemanticIndexChunk` records provide a local lightweight semantic index without claiming real provider vectors.
- `ToolOrchestrationPlan` records planned OCR / ASR / Query Rewrite / Embedding / Similarity / LLM / Question Generation / Review Update chains.
- `ImportReport` and `PdfPageArtifact` make PDF/manual page fallback visible in the input and course diagnostics.
- `ReviewDailyStats` and `ExamResultReport` make review/exam outcomes visible without requiring device-level validation.

## v1.4 Additions

- `LocalSemanticIndexRecord` persists local lexical vectors to app-private storage and supports top-k search.
- `ToolStepRecord` explains provider mode for OCR / ASR / PDF OCR / Query Rewrite / Embedding / Similarity / LLM / Question Generation / Review Update / TTS / Translation.
- `AsrLongJob` now exposes provider/upload/polling/result status and maps configured-but-unvalidated app ASR to `CORE_CONTRACT_PRESENT_APP_WIRING_PENDING`.
- `TtsPlaybackState` makes Android local TTS fallback visible without claiming official TTS network success.
- `TranslationResultRecord` keeps translation as a derived artifact and preserves original evidence.
- `MasteryHistoryEvent`, `MasteryTrendStats`, and enriched `ExamResultReport` make long-term review and exam diagnostics concrete.

## v1.6 Additions

- `OfficialRuntimeGateway` and `OfficialRuntimeIntegrator` run in the L3 publish path.
- Query Rewrite, Embedding, and Text Similarity gained official app-runtime success paths, but v1.6 still allowed the production default to be ConfigMissing-only if no adapter was injected.
- `LocalSemanticIndexRecord` now stores official/local vectors plus `vectorSource`.
- `TextSimilarityMatch` now stores `scoreSource`.
- OCR evidence carries provider provenance.
- Runtime diagnostics include configured/used/fallback/blocker/redaction fields.

## v1.7 / v1.8 Status Freeze

- v1.7 fixed the production injection gap found by Claude v2: `AppViewModel` now uses `OfficialRuntimeGatewayFactory.production()` instead of directly constructing a no-argument `ProviderBackedOfficialRuntimeGateway()`.
- Production injection now uses `VivoQueryRewriteProvider -> VivoQueryRewriteLearningProvider`, `VivoEmbeddingProvider -> VivoEmbeddingLearningProvider`, and `VivoTextSimilarityProvider -> VivoTextSimilarityLearningProvider`.
- The retrieval trio is now `OFFICIAL_RUNTIME_READY / VALIDATION_PENDING`, not `OFFICIAL_RUNTIME_USED` until a demo/cloud device proves the runtime call succeeds.
- Translation, official TTS, official Function Calling, and ASR Long remain honest seam/not-configured/validation-pending paths.

## Diagnostics Contract

The app may show capability and status labels such as `OFFICIAL_RUNTIME_USED`, `OFFICIAL_RUNTIME_READY`, `OFFICIAL_APP_WIRING_PENDING`, `LOCAL_FALLBACK_USED`, `OFFICIAL_RUNTIME_NOT_CONFIGURED`, `LOCAL_ORCHESTRATOR`, `CORE_CONTRACT_PRESENT_APP_WIRING_PENDING`, `LOCAL_TTS_AVAILABLE`, or `MANUAL_TRANSCRIPT_FALLBACK`.

Diagnostics must not display:

- AppKey
- Authorization
- full endpoint URL
- request body
- local `config.local.json` contents
