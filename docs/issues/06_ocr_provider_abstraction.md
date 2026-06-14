# Title
Add OCR provider abstraction for slides, board photos, and handouts

## Background
ClassMate needs OCR-ready architecture for PPT images, blackboard photos, and handout screenshots.

## Goal
Define `OcrProvider` and result models without requiring immediate real OCR.

## Scope
- Provider interface.
- Manual fallback.
- OCR document/page/block data.

## Non-goals
- No real OCR API in this issue.
- No network image processing.
- No third-party content scraping.

## Proposed files/modules
- `core/material`
- `app/platform/ocr`

## Data model sketch
- `OcrProvider`
- `OcrDocument`
- `OcrPage`
- `OcrBlock`
- `SlideFrame`

## UI entry points
- Import Hub image/OCR placeholder.
- Future PPT image import.

## Privacy/security notes
- User explicitly imports local image.
- Do not log image content or private paths.

## Acceptance criteria
- Fake OCR provider emits document blocks.
- Manual OCR fallback accepts pasted recognized text.
- Current Import text flow unchanged.

## Tests
- Fake OCR maps pages and blocks.
- Missing OCR safely falls back to manual text.

## Dependencies
- Lesson material bundle.

## Suggested owner
Claude

## Priority
P0

## Risk
Medium
