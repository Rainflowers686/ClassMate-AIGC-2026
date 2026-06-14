# Title
Add basic speaker labels teacher/student/unknown

## Background
Classroom material may include teacher explanation and student questions. First version should support simple labels without claiming voiceprint recognition.

## Goal
Represent basic speaker labels for transcript segments.

## Scope
- `SpeakerLabel`.
- Manual or provider-supplied label.
- Display in Live and export when safe.

## Non-goals
- No voiceprint recognition.
- No diarization claims.
- No identity storage.

## Proposed files/modules
- `core/material`
- Live Companion UI display adapter

## Data model sketch
- `SpeakerLabel(kind: teacher | student | unknown, confidence)`

## UI entry points
- Live segment list.
- Future transcript detail.

## Privacy/security notes
- Do not store student identity.
- Default to unknown when uncertain.

## Acceptance criteria
- Segments can be labeled teacher/student/unknown.
- Unknown is default.
- UI copy does not imply voiceprint recognition.

## Tests
- Default label is unknown.
- Manual label persists through fusion.

## Dependencies
- Transcript segment model.

## Suggested owner
Claude

## Priority
P1

## Risk
Low
