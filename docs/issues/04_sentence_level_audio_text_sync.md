# Title
Add sentence-level audio-text sync foundation

## Background
Recording products often support following text while audio plays. ClassMate should start with honest sentence-level sync.

## Goal
Represent sentence or segment-level audio ranges without pretending to have word-level timestamps.

## Scope
- `AudioTimeRange`.
- Segment-level evidence time reference.
- Export-safe display.

## Non-goals
- No word-level sync unless official ASR returns real timestamps.
- No fake timing interpolation.
- No audio playback requirement in this issue.

## Proposed files/modules
- `core/material`
- future evidence UI adapter

## Data model sketch
- `AudioTimeRange(startMs, endMs, sourceMediaId)`
- `MaterialEvidenceRef(timeRange)`

## UI entry points
- Evidence detail future “source time” label.
- Export report source section.

## Privacy/security notes
- Do not expose private local absolute paths in export.
- Use safe media display names.

## Acceptance criteria
- Transcript segment can carry an audio range.
- Evidence ref can point to a segment time range.
- No word token data generated without source timestamps.

## Tests
- Segment range serialization.
- Missing range safely displays as absent.

## Dependencies
- Transcript session model.

## Suggested owner
Claude

## Priority
P1

## Risk
Medium
