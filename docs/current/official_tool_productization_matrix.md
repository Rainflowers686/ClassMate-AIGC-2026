# Official Tool Productization Matrix

Date: 2026-06-20

No secrets, endpoint URLs, or Authorization values are recorded here.

| Capability | Smoke status | App-level status | Product path |
| --- | --- | --- | --- |
| OCR | PASS | COMPLETE path / config-gated | Image/photo OCR text can enter LessonSource and Evidence; manual OCR text fallback remains. |
| QUERY_REWRITE | PASS | COMPLETE seam | Pipeline step logs standardize learning questions and retrieval query planning. |
| EMBEDDING | PASS | COMPLETE seam | Embedding records are created for lesson, evidence, knowledge points, and questions. |
| TEXT_SIMILARITY | PASS | COMPLETE seam | Similarity matches and similar-question recommendation seams are created. |
| TRANSLATION | not product-smoked in app | SEAM_ONLY / NOT_CONFIGURED | `L3OfficialToolSeams` exposes a translation request/result seam for multilingual material aid; original evidence remains unchanged. |
| TTS | not product-smoked in app | SEAM_ONLY / OFFICIAL_TTS_NOT_CONFIGURED | `L3OfficialToolSeams` exposes listen-review preparation; official TTS remains unclaimed unless configured. No voice clone. |
| FUNCTION_CALLING | not product-smoked in app | LOCAL_ORCHESTRATOR / SEAM_ONLY | `L3OfficialToolSeams` creates local orchestration plans and step logs; official Function Calling is not claimed. |
| ASR_LONG | not product-smoked in app | SEAM_ONLY / NOT_CONFIGURED | Recording/audio artifacts create ASR Long jobs; manual transcript fallback is complete. |
| Edge model | local availability dependent | LOCAL_RULE_FALLBACK / PARTIAL | `L3OfficialToolSeams` exposes offline summary/practice/review fallback output when edge model is unavailable. |

## Diagnostics Contract

The app may show capability and status labels such as `READY_SEAM_USED`, `RECORD_CREATED`, `MATCH_CREATED`, `LOCAL_ORCHESTRATOR`, `ASR_NOT_CONFIGURED`, or `MANUAL_TRANSCRIPT_FALLBACK`.

Diagnostics must not display:

- AppKey
- Authorization
- full endpoint URL
- request body
- local `config.local.json` contents
