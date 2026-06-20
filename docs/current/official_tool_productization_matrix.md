# Official Tool Productization Matrix

Date: 2026-06-20

No secrets, endpoint URLs, or Authorization values are recorded here.

| Capability | Smoke status | App-level status | Product path |
| --- | --- | --- | --- |
| OCR | PASS | `OFFICIAL_RUNTIME_USED` for OCR evidence / config-gated | Image/photo OCR text and PDF page OCR seam can enter LessonSource and Evidence with provider provenance; manual OCR text fallback remains. |
| QUERY_REWRITE | PASS | runtime gateway wired; `OFFICIAL_RUNTIME_USED` only when app adapter succeeds | L3 publish path now asks the runtime gateway for study/retrieval query normalization. Missing adapter/config falls back to local query planning. |
| EMBEDDING | PASS | runtime gateway wired; official vector path tested with fake adapter | Semantic records store `officialVector`, `localVector`, and `vectorSource`; missing adapter/config persists local lexical vectors. |
| TEXT_SIMILARITY | PASS | runtime gateway wired; official score path tested with fake adapter | Evidence matching and similar-question recommendations record `scoreSource`; missing adapter/config uses local similarity fallback. |
| TRANSLATION | not product-smoked in app | runtime gateway wired / usually NOT_CONFIGURED | Lesson/evidence translation first checks official runtime; original evidence remains unchanged when not configured or failed. |
| TTS | not product-smoked in app | runtime gateway wired / LOCAL_FALLBACK | Listen-review checks official runtime first, then uses Android local TextToSpeech or script text. No voice clone. |
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
- Query Rewrite, Embedding, and Text Similarity have official app-runtime success paths with injected adapters and tests; default demo path still falls back when adapters/config are absent.
- `LocalSemanticIndexRecord` now stores official/local vectors plus `vectorSource`.
- `TextSimilarityMatch` now stores `scoreSource`.
- OCR evidence carries provider provenance.
- Runtime diagnostics include configured/used/fallback/blocker/redaction fields.

## Diagnostics Contract

The app may show capability and status labels such as `OFFICIAL_RUNTIME_USED`, `OFFICIAL_RUNTIME_READY`, `OFFICIAL_APP_WIRING_PENDING`, `LOCAL_FALLBACK_USED`, `OFFICIAL_RUNTIME_NOT_CONFIGURED`, `LOCAL_ORCHESTRATOR`, `CORE_CONTRACT_PRESENT_APP_WIRING_PENDING`, `LOCAL_TTS_AVAILABLE`, or `MANUAL_TRANSCRIPT_FALLBACK`.

Diagnostics must not display:

- AppKey
- Authorization
- full endpoint URL
- request body
- local `config.local.json` contents
