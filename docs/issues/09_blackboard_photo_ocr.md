# Title
Prepare blackboard photo OCR flow

## Background
Board writing is important classroom evidence. First version should support local board photo import with OCR-ready structure.

## Goal
Add safe blackboard photo OCR preparation path.

## Scope
- Local photo import entry.
- OCR block roles.
- Manual fallback when OCR unavailable.

## Non-goals
- No camera capture unless separately approved.
- No real OCR in this issue.
- No automatic identity or classroom privacy capture.

## Proposed files/modules
- Import Hub future board photo entry.
- `OcrDocument`
- `MaterialEvidenceRef`

## Data model sketch
- `MaterialSourceType.BLACKBOARD_PHOTO`
- `OcrBlock(role=formula|body|unknown)`

## UI entry points
- Import Hub -> Blackboard photo.

## Privacy/security notes
- Warn users not to import photos containing private student info.
- Do not export private local paths.

## Acceptance criteria
- Board photo source type exists in material bundle.
- Manual OCR fallback can attach recognized text.
- Evidence source can say blackboard OCR.

## Tests
- Board OCR fake block fuses into lesson text.
- Missing OCR provider uses safe placeholder.

## Dependencies
- OCR abstraction.
- Lesson fusion engine.

## Suggested owner
Claude

## Priority
P2

## Risk
Medium
