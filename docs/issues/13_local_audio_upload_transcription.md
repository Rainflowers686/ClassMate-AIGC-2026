# Title
Prepare local audio upload transcription path

## Background
Users may have local classroom recordings. ClassMate should support local file upload once ASR provider is ready.

## Goal
Design and implement safe local audio selection and transcription preparation.

## Scope
- Local file picker.
- Safe metadata.
- Route to `TranscriptionProvider`.

## Non-goals
- No online audio fetch.
- No real ASR until provider is ready.
- No recording permission for file upload only.

## Proposed files/modules
- Import Hub audio entry.
- `app/platform/asr`
- `core/material`

## Data model sketch
- `MaterialSourceType.LOCAL_AUDIO`
- `TranscriptionRequest(localUri, displayName)`

## UI entry points
- Import Hub -> Local audio.

## Privacy/security notes
- Do not export private local absolute path.
- Do not log full file URI.

## Acceptance criteria
- Audio entry remains honest when ASR unavailable.
- With fake provider, local audio maps to transcript session.
- No third-party crawling.

## Tests
- Placeholder does not send network.
- Fake audio transcription maps to segments.

## Dependencies
- TranscriptionProvider abstraction.

## Suggested owner
Claude

## Priority
P2

## Risk
Medium
