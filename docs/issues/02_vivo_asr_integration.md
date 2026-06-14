# Title
Research and prepare official vivo ASR integration

## Background
Live Companion currently uses manual/simulated transcript segments. To catch up with recording transcription products, ClassMate needs official ASR integration planning.

## Goal
Research official vivo ASR capabilities and prepare an implementation plan.

## Scope
- API capability research.
- Permission and privacy analysis.
- Data mapping into `TranscriptSession`.
- Smoke test plan.

## Non-goals
- Do not implement real ASR until official docs and credentials workflow are confirmed.
- Do not request recording permission for architecture-only work.
- Do not crawl online videos.

## Proposed files/modules
- `app/platform/asr`
- `core/material`
- docs proof checklist

## Data model sketch
- `VivoAsrProvider`
- `AudioTimeRange`
- `TranscriptSegment`
- `SpeakerLabel`

## UI entry points
- Live Companion future real transcription mode.
- Import Hub local audio file path.

## Privacy/security notes
- User must explicitly choose or record material.
- Never expose credential values.
- Do not save full service raw output.

## Acceptance criteria
- Written integration plan with required official docs listed.
- Permission UX documented.
- Mapping from ASR result to transcript segments defined.

## Tests
- Fake ASR response maps to segments.
- Failure maps to safe error labels.

## Dependencies
- `TranscriptionProvider` abstraction.

## Suggested owner
Human

## Priority
P1

## Risk
High
