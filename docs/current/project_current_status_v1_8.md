> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# Project Current Status v1.8

Date: 2026-06-20

This is the status-freeze document after Official Retrieval Runtime Injection v1.7. It records what is complete, what is official-first but validation-pending, and what remains seam-only or local fallback. No secrets, endpoint URLs, Authorization values, request bodies, or `config.local.json` contents are recorded here.

## Status Freeze

- Branch: `feature/product-review-compatible`.
- Product scope: UI/Theme/Advanced Appearance baseline, L3 learning pipeline, Input Superhub, Practice/Review/Wrong Book/Mastery/ExamReport, local persistence, and official runtime gateway.
- v1.7 fix: production `AppViewModel` now creates the official runtime through `OfficialRuntimeGatewayFactory.production()` rather than a no-argument ConfigMissing-only gateway.
- Validation boundary: this document does not claim live official network calls were run during v1.8.

## Official Capability Matrix

| Capability | Smoke status | Current App status | Honest demo wording |
| --- | --- | --- | --- |
| OCR | PASS | COMPLETE / CONFIG_GATED | App-level official path is demoable for image/photo/OCR text into LessonSource/Evidence when config is present; manual fallback remains. |
| Query Rewrite | PASS | OFFICIAL_RUNTIME_READY / VALIDATION_PENDING | Production gateway injects Vivo Query Rewrite adapter. `OFFICIAL_RUNTIME_USED` requires demo/cloud config and runtime success; local query planning fallback remains. |
| Embedding | PASS | OFFICIAL_RUNTIME_READY / VALIDATION_PENDING | Production gateway injects Vivo Embedding adapter. Official vectors can be saved as `vectorSource=OFFICIAL`; local lexical vector fallback remains. |
| Text Similarity | PASS | OFFICIAL_RUNTIME_READY / VALIDATION_PENDING | Production gateway injects Vivo Text Similarity adapter for ranking/evidence/similar-question paths; local similarity fallback remains. |
| ASR Long | not live-validated in app | CORE_CONTRACT_PRESENT / APP_UPLOAD_POLL_RESULT_VALIDATION_PENDING | Core `VivoAsrProvider` 1739 contract exists. Demo remains recording artifact + ASR job seam + manual transcript fallback until non-sensitive audio validation. |
| Translation | not product-smoked in app | SEAM_ONLY / NOT_CONFIGURED | Product records and derived artifact path exist; official network path must not be claimed complete. |
| Official TTS | not product-smoked in app | LOCAL_TTS_AVAILABLE / OFFICIAL_NOT_CONFIGURED | Android local TTS fallback is the current product path. Voice identity features are not part of the ClassMate learning loop. |
| Function Calling | not product-smoked in app | LOCAL_ORCHESTRATOR_ACTIVE / OFFICIAL_NOT_CONFIGURED | Local ToolOrchestrator is active; official Function Calling remains future unless configured and validated. |
| Edge model | device dependent | FALLBACK_STRATEGY_READY / DEVICE_VALIDATION_PENDING | Fallback strategy is wired; real availability depends on `/sdcard/1225` and permissions. |

## L3 Product Status

Complete or partial user-visible paths:

- Text/Markdown classroom input can generate summary, evidence, knowledge points, questions, review queue, and mastery updates.
- Real Practice default is answer-submit-grade, not self-assessment.
- Wrong answers update wrong book, review queue, mastery state, and persistence.
- ExamReport records score, weak knowledge points, wrong questions, evidence coverage, and recommendations.
- Input Superhub supports text/TXT/MD/CSV, controlled DOCX/XLSX/PPTX best-effort extraction with quality guards, PDF artifact/page fallback, image OCR seam, and audio artifact/manual transcript fallback.
- LocalSemanticIndex persists local lexical vectors and can search evidence/questions; official vector source is supported when the official runtime succeeds.

## Not Complete

- Live official ASR Long upload/poll/result validation.
- Complex PDF native text extraction.
- Official Translation, Official TTS, and Official Function Calling live runtime validation.
- Universal edge-model availability across devices.
- Complex Office rich-format parsing beyond best-effort and template-based paths.

## Next Sequence

1. Claude v3 global review using `docs/current/claude_v3_review_handoff.md`.
2. Resolve any P0/P1 docs or wiring issues.
3. Run `scripts\qa\demo_device_provision.ps1` on the target device.
4. Cloud-device validation following `docs/current/cloud_device_validation_plan.md`.
5. Patch only validated bugs; do not expand scope.
