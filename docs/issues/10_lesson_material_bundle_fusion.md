# Title
Implement LessonMaterialBundle fusion foundation

## Background
ClassMate needs to combine transcript, slide OCR, board OCR, manual notes, and imported text into one analysis-ready lesson.

## Goal
Create `LessonMaterialBundle` and `LessonFusionEngine` foundation.

## Scope
- Data model.
- Source ordering.
- Evidence source refs.
- Conversion to analyzer input.

## Non-goals
- No provider protocol change.
- No validator weakening.
- No third-party scraping.

## Proposed files/modules
- `core/material/LessonMaterialBundle`
- `core/material/MaterialEvidenceRef`
- `core/material/LessonFusionEngine`

## Data model sketch
- `LessonMaterialBundle(materials, transcriptSessions, ocrDocuments, manualNotes, glossary)`
- `MaterialEvidenceRef(sourceType=transcript|slide_ocr|blackboard_ocr|manual_note|imported_text, quote, timeRange, pageId, blockId)`

## UI entry points
- Import Hub.
- Live Companion.
- Evidence detail future source chips.

## Privacy/security notes
- Do not store credentials.
- Do not export internal model request context or raw service output.

## Acceptance criteria
- Imported text and manual transcript can fuse.
- Source refs survive fusion.
- Analyzer receives clean classroom text.
- Existing CourseAnalyzer path unchanged.

## Tests
- Multi-source bundle ordering.
- Evidence source refs generated.
- Empty sources safely fail.

## Dependencies
- Transcript and OCR abstractions.

## Suggested owner
Claude

## Priority
P0

## Risk
High
