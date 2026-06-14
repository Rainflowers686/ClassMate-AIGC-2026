# Title
Prepare local video upload safe pipeline

## Background
Users may have local authorized classroom videos. ClassMate can prepare a safe local-only path without platform crawling.

## Goal
Support user-selected local video as a future material source.

## Scope
- Local file selection.
- Optional audio-track handoff to transcription provider.
- Optional user-provided subtitle/manual transcript.

## Non-goals
- No third-party platform video crawling.
- No unauthorized content extraction.
- No automatic online subtitle download.

## Proposed files/modules
- Import Hub video entry.
- `MaterialSourceType.LOCAL_VIDEO`
- `TranscriptionProvider` adapter

## Data model sketch
- `LocalVideoMaterial(displayName, duration, transcriptSession?)`
- `MaterialEvidenceRef(sourceType=local_video, timeRange)`

## UI entry points
- Import Hub -> Local video.

## Privacy/security notes
- Warn user to import only authorized classroom videos.
- Do not expose private paths in export.

## Acceptance criteria
- Placeholder clearly says local file only.
- Fake provider can create transcript from local video source.
- No online fetch path exists.

## Tests
- Local video placeholder is offline.
- Evidence ref can carry local video time range.

## Dependencies
- Transcription provider.
- Lesson bundle.

## Suggested owner
Later

## Priority
P3

## Risk
High
