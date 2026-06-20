# ASR Long Productization Report

Date: 2026-06-20

## Current Status

ASR Long is `HARD_BLOCKED_MISSING_SCHEMA` for the official network path and `LOCAL_FALLBACK` for manual transcript entry.

## Implemented

- `AsrLongJob` now records provider status, upload status, polling status, transcript text, transcript segments, error code, and timestamps.
- Recording/audio artifact import creates an ASR Long job.
- Missing official config maps to `OFFICIAL_ASR_CONFIG_MISSING`.
- Present config without upload/poll/result schema maps to `HARD_BLOCKED_MISSING_SCHEMA`.
- Transcript fill-in maps to `TRANSCRIPT_READY` and enters the same L3 pipeline with transcript timeline, summary, evidence, knowledge points, questions, review queue, and mastery.

## Precise Blocker

The current app mapping does not include the official ASR Long upload API shape, polling API shape, or transcript result schema. Without those schemas the app must not claim official ASR success.

## Fallback

Manual transcript fallback is complete for L3 entry. Segments are generated from paragraphs/sentences and marked fallback-generated.
