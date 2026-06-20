# Final Productization v1.4

Date: 2026-06-20

No AppKey, Authorization value, full endpoint URL, or local config content is recorded.

## Scope

Task 4 finishes the remaining L3 productization work before Claude global red-team and cloud device validation. It does not run provider network smoke and does not claim official paths that have not been validated.

## Status Matrix

| Area | Status | User-visible path | Honest limit |
| --- | --- | --- | --- |
| ASR Long official path | HARD_BLOCKED | Recording/audio artifact creates ASR job lifecycle; manual transcript fallback still enters L3 pipeline. | Current app mapping lacks upload, polling, and result schema. |
| PDF processing | PARTIAL | PDF artifact, page record, page OCR seam, manual page text fallback, L3 pipeline entry. | Native PDF text parsing remains parser-pending. |
| TTS | LOCAL_FALLBACK | Listen-review can invoke Android local TextToSpeech through a `LocalTtsPlayer` port for summary/wrong-answer/review-card text. | Official TTS network path is not claimed; device validation still owns actual engine availability. |
| Translation | NOT_CONFIGURED / SEAM_ONLY | Translation request/result is reachable and preserves original evidence. | No fake local translation is generated. |
| Function Calling | LOCAL_FALLBACK | Local ToolOrchestrator produces explainable tool steps for text/image/audio/PDF/question-bank input. | Official Function Calling is not claimed. |
| Semantic index | COMPLETE local | App-private semantic index records persist and reload; local search and similar-question fallback are available. | Official embedding vectors are not faked. |
| Mastery trends | COMPLETE local | Attempts create mastery history and trend stats for Review/Course detail. | Advanced long-term ML prediction remains future work. |
| Exam report | COMPLETE local | Exam submit produces score, accuracy, weak points, evidence coverage, recommendations, Markdown report text. | No external export file is generated in this task. |
| DOCX/XLSX/PPTX | PARTIAL | Existing ZIP/XML best-effort parsing remains available with error states. | Complex formatting, charts, images, and rich layout remain limited. |
| Multi-choice / short answer | PARTIAL | Multi-choice strict grading works; short-answer self-assessment and AI-grading seam remain honest. | Short-answer automatic AI grading is not complete. |

## Pipeline Evidence

- `LocalSemanticIndexRecord` stores local lexical vectors in app-private storage.
- `ToolStepRecord` explains planned tool usage without endpoint or credential values.
- `MasteryHistoryEvent` records answer-driven state transitions.
- `ExamResultReport` now includes accuracy, weak knowledge points, evidence coverage, recommendations, and Markdown text.
- `PdfDocumentArtifact` and `PdfPageArtifact` make PDF page OCR/manual fallback explicit.

## Protected Areas

Provider smoke logic, `config.local.json`, Home main CTA, Theme/Advanced Appearance, Settings appearance, Gradle, `.github`, `.codex_work`, and `app/libs` were not part of this sprint.
