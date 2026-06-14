# Title
Export multimodal evidence report

## Background
Once lesson materials include transcript, slides, board OCR, notes, and imported text, exports should show evidence source safely.

## Goal
Extend export report to include multimodal evidence source summaries.

## Scope
- Source type summary.
- Evidence refs without private paths.
- Glossary summary.
- Transcript time ranges when available.

## Non-goals
- No raw service output.
- No private local paths.
- No full internal request context.

## Proposed files/modules
- `core/exporting`
- `core/material`

## Data model sketch
- `MaterialEvidenceRef`
- `ExportEvidenceSourceSummary`

## UI entry points
- Export report.
- History report.

## Privacy/security notes
- Redact sensitive tokens.
- Do not include credentials.
- Do not include raw provider response.

## Acceptance criteria
- Report shows source type: transcript, slide OCR, board OCR, manual note, imported text.
- Time range shown only when available.
- No private absolute file paths.
- Existing md/html/txt export still works.

## Tests
- Export includes multimodal source labels.
- Sensitive words are redacted.
- Empty source summary is safe.

## Dependencies
- LessonMaterialBundle.
- MaterialEvidenceRef.

## Suggested owner
Codex

## Priority
P2

## Risk
Medium
