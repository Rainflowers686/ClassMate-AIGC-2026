# Official Tool Productization Matrix

Date: 2026-06-20

No secrets, endpoint URLs, or Authorization values are recorded here.

| Capability | Smoke status | App-level status | Product path |
| --- | --- | --- | --- |
| OCR | PASS | COMPLETE path / config-gated | Image/photo OCR text and PDF page OCR seam can enter LessonSource and Evidence; manual OCR text fallback remains. |
| QUERY_REWRITE | PASS | READY_SEAM_USED / local plan | Pipeline step logs and local tool orchestration standardize learning questions, retrieval query planning, and wrong-question follow-up intent. |
| EMBEDDING | PASS | LOCAL_INDEX / PROVIDER_READY_SEAM | Embedding records and lightweight semantic index chunks are created for lesson, evidence, knowledge points, and questions. |
| TEXT_SIMILARITY | PASS | LOCAL_FALLBACK / TEXT_SIMILARITY_READY_SEAM | Local similarity supports evidence matching and similar-question recommendation; official provider execution remains app-validation work. |
| TRANSLATION | not product-smoked in app | SEAM_ONLY / NOT_CONFIGURED | Translation request/result seam is reachable for multilingual material aid; original evidence remains unchanged when not configured. |
| TTS | not product-smoked in app | LOCAL_FALLBACK / OFFICIAL_TTS_NOT_CONFIGURED | Listen-review uses an Android local TextToSpeech port for summary, wrong-answer explanation, and review card when available. Official TTS remains unclaimed unless configured. No voice clone. |
| FUNCTION_CALLING | not product-smoked in app | LOCAL_ORCHESTRATOR | Structured tool step records now explain each local/official/seam-only tool in the L3 plan; official Function Calling is not claimed. |
| ASR_LONG | not product-smoked in app | HARD_BLOCKED / NOT_CONFIGURED / MANUAL_FALLBACK | Recording/audio artifacts create ASR Long jobs. Upload/poll/result schema is missing in current app mapping, so manual transcript fallback is the usable path. |
| Edge model | local availability dependent | LOCAL_RULE_FALLBACK / PARTIAL | Offline summary/practice/review fallback output is recorded when edge model is unavailable. |

## v1.3 Additions

- `SemanticIndexChunk` records provide a local lightweight semantic index without claiming real provider vectors.
- `ToolOrchestrationPlan` records planned OCR / ASR / Query Rewrite / Embedding / Similarity / LLM / Question Generation / Review Update chains.
- `ImportReport` and `PdfPageArtifact` make PDF/manual page fallback visible in the input and course diagnostics.
- `ReviewDailyStats` and `ExamResultReport` make review/exam outcomes visible without requiring device-level validation.

## v1.4 Additions

- `LocalSemanticIndexRecord` persists local lexical vectors to app-private storage and supports top-k search.
- `ToolStepRecord` explains provider mode for OCR / ASR / PDF OCR / Query Rewrite / Embedding / Similarity / LLM / Question Generation / Review Update / TTS / Translation.
- `AsrLongJob` now exposes provider/upload/polling/result status and maps missing official schema to `HARD_BLOCKED_MISSING_SCHEMA`.
- `TtsPlaybackState` makes Android local TTS fallback visible without claiming official TTS network success.
- `TranslationResultRecord` keeps translation as a derived artifact and preserves original evidence.
- `MasteryHistoryEvent`, `MasteryTrendStats`, and enriched `ExamResultReport` make long-term review and exam diagnostics concrete.

## Diagnostics Contract

The app may show capability and status labels such as `READY_SEAM_USED`, `RECORD_CREATED`, `MATCH_CREATED`, `LOCAL_ORCHESTRATOR`, `OFFICIAL_ASR_CONFIG_MISSING`, `HARD_BLOCKED_MISSING_SCHEMA`, `LOCAL_TTS_AVAILABLE`, or `MANUAL_TRANSCRIPT_FALLBACK`.

Diagnostics must not display:

- AppKey
- Authorization
- full endpoint URL
- request body
- local `config.local.json` contents
