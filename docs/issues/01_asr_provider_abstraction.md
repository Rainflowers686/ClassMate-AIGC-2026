# Title
Add ASR provider abstraction for future classroom transcription

## Background
ClassMate needs to catch up on recording/transcription entry points while keeping the learning loop evidence-grounded. First step is an abstraction layer, not immediate real ASR.

## Goal
Define a `TranscriptionProvider` contract that can produce transcript sessions from manual, simulated, or future official ASR inputs.

## Scope
- Provider interface.
- Result/error model.
- Fake/manual implementation for tests.
- Safe metadata only.

## Non-goals
- No real ASR API call in this issue.
- No recording permission.
- No third-party platform crawling.

## Proposed files/modules
- `core/live` or future `core/material`
- `app/platform` for Android-specific future adapters
- docs/tests as needed

## Data model sketch
- `TranscriptionProvider`
- `TranscriptionRequest`
- `TranscriptionResult`
- `TranscriptSession`
- `TranscriptSegment`
- `AudioTimeRange`

## UI entry points
- Live Companion provider selector hidden or debug-only at first.
- Import Hub future audio entry.

## Privacy/security notes
- Do not store credentials.
- Do not log raw audio paths beyond safe display names.
- Do not export model request context or service raw output.

## Acceptance criteria
- Manual provider can emit a `TranscriptSession`.
- Fake provider supports deterministic tests.
- No recording permission added.
- Current BlueLM path unchanged.

## Tests
- Fake transcription produces segments.
- Missing provider safely fails.
- No sensitive fields appear in logs.

## Dependencies
- Existing Live Companion data model.

## Suggested owner
Claude

## Priority
P0

## Risk
Medium
