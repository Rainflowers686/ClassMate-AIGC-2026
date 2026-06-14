# Title
Add PPT image import and OCR-ready slide frame extraction

## Background
Classroom learning often depends on slides. ClassMate should accept user-provided PPT screenshots or exported slide images.

## Goal
Support local slide image import into OCR-ready `SlideFrame` structures.

## Scope
- Local image selection.
- Slide source type.
- Fake/manual OCR path.
- Slide title/body/caption roles.

## Non-goals
- No online PPT fetch.
- No platform crawling.
- No real OCR unless provider is ready.

## Proposed files/modules
- Import Hub UI future entry.
- `OcrDocument`
- `SlideFrame`

## Data model sketch
- `OcrPage(pageIndex, blocks, slideFrame)`
- `SlideFrame(title, bullets, figureCaptions)`

## UI entry points
- Import Hub -> PPT/slide image.

## Privacy/security notes
- Use local user-selected file only.
- Export safe source labels, not private absolute paths.

## Acceptance criteria
- User can attach local slide image placeholder.
- Fake OCR result appears in material bundle.
- Fusion keeps slide evidence source.

## Tests
- Slide frame maps to material evidence.
- No network call for placeholder path.

## Dependencies
- OCR abstraction.
- Lesson material bundle.

## Suggested owner
Claude

## Priority
P2

## Risk
Medium
