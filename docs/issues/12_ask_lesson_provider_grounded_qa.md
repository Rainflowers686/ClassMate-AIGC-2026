# Title
Connect Ask This Lesson to provider with grounded JSON answers

## Background
Ask This Lesson currently needs to stay grounded in lesson evidence. Provider-backed answers must not invent evidence.

## Goal
Use current provider chain for controlled lesson Q&A with evidence refs.

## Scope
- Controlled answer schema.
- Evidence quote validation.
- Fallback when ungrounded.

## Non-goals
- No free-form unverified answers.
- No raw provider output shown.
- No validator weakening.

## Proposed files/modules
- `core/ask`
- `core/analysis` adapter
- Timeline UI entry

## Data model sketch
- `LessonQuestion`
- `LessonAnswer(answer, relatedKnowledgePoints, evidenceRefs, groundedness)`

## UI entry points
- Timeline.
- History detail.
- Course detail.

## Privacy/security notes
- Do not log full question context if it includes course text.
- Do not export raw provider response.

## Acceptance criteria
- Valid JSON with locatable evidence displays grounded.
- Missing evidence downgrades to partial or not found.
- local_only sends no network.

## Tests
- Valid grounded response.
- Natural language response safely falls back.
- Unlocated evidence not fabricated.

## Dependencies
- Existing Ask Lesson parser.
- Provider chain.

## Suggested owner
Claude

## Priority
P2

## Risk
High
