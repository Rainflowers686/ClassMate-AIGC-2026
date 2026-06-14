# Title
Research and prepare official vivo OCR integration

## Background
OCR can bring slides, board photos, and handouts into the same learning analysis pipeline.

## Goal
Research official vivo OCR capabilities and prepare a safe integration plan.

## Scope
- Official documentation research.
- Request/response mapping plan.
- Privacy and file permission plan.

## Non-goals
- Do not implement real OCR until official docs are confirmed.
- Do not guess endpoint or credentials.

## Proposed files/modules
- `app/platform/ocr/VivoOcrProvider`
- `core/material/OcrDocument`

## Data model sketch
- `VivoOcrProvider`
- `OcrBlock(text, boundingBox, confidence, role)`

## UI entry points
- Import Hub image/OCR.
- PPT image import.

## Privacy/security notes
- No credential values in code/docs/logs.
- Do not save full raw service body.

## Acceptance criteria
- Official requirements documented.
- Safe fake integration tests planned.
- User consent and file handling documented.

## Tests
- Fake vivo OCR response maps to blocks.
- Error maps to safe labels.

## Dependencies
- OCR provider abstraction.

## Suggested owner
Human

## Priority
P1

## Risk
High
