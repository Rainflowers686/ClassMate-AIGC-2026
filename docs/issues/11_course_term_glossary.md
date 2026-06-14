# Title
Add course term glossary foundation

## Background
Glossaries can improve ASR hotwords, OCR correction, model wording consistency, and quiz terminology.

## Goal
Introduce `CourseTerm` and `TermGlossary` data with built-in subject examples.

## Scope
- Data model.
- Built-in subjects.
- Import/Live course selection plan.
- Export glossary summary.

## Non-goals
- No automatic private term upload.
- No hidden cloud sync.

## Proposed files/modules
- `core/glossary`
- docs/testing glossary fixtures

## Data model sketch
- `CourseTerm(term, aliases, subject, definition, examples, priority, source)`
- `TermGlossary(subject, terms, version)`

## UI entry points
- Import subject selector.
- Live subject selector.
- Export glossary summary.

## Privacy/security notes
- User custom terms may contain sensitive content; do not log raw custom terms beyond user-visible storage.
- No credentials in glossary.

## Acceptance criteria
- Built-in terms for high math, physics, discrete math, C++, Marxism basics.
- Terms can be selected by subject.
- Export can include safe glossary summary.

## Tests
- Glossary loads by subject.
- Aliases are preserved.
- No sensitive fields.

## Dependencies
- Lesson material bundle.

## Suggested owner
Codex

## Priority
P1

## Risk
Low
