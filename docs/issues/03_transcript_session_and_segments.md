# Title
Standardize TranscriptSession and TranscriptSegment for multi-source lessons

## Background
Live Companion needs a stable transcript model that can serve manual notes, ASR, local audio, and future video audio tracks.

## Goal
Define transcript session data that can be fused into lesson materials and evidence references.

## Scope
- Session lifecycle.
- Segment ordering.
- Source labels.
- Safe timestamps.

## Non-goals
- No real ASR.
- No word-level sync unless timestamps are real.

## Proposed files/modules
- `core/live`
- `core/material`
- app state adapters

## Data model sketch
- `TranscriptSession(id, title, source, startedAt, endedAt, segments)`
- `TranscriptSegment(id, text, source, timeRange, speaker, confidence)`

## UI entry points
- Live Companion.
- History detail future transcript view.

## Privacy/security notes
- Store only user-provided lesson text and safe metadata.
- Do not store credentials or service raw payloads.

## Acceptance criteria
- Manual Live session maps to standardized transcript.
- Ended session can be fused into lesson text.
- Segment count and order remain stable.

## Tests
- Append/pause/resume/end session.
- Convert transcript to lesson text.
- Persistence does not include credentials.

## Dependencies
- Existing Live Companion.

## Suggested owner
Claude

## Priority
P0

## Risk
Medium
