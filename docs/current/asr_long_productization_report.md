# ASR Long Productization Report

Date: 2026-06-20

## Current Status

Core contract: PRESENT.
App-level wiring: PARTIAL.
Network smoke in this task: NOT RUN.
Demo status: recording artifact + ASR job seam + Manual transcript fallback.

## Implemented

- `AsrLongJob` now records provider status, upload status, polling status, transcript text, transcript segments, error code, and timestamps.
- Recording/audio artifact import creates an ASR Long job.
- Missing official app config maps to `OFFICIAL_ASR_CONFIG_MISSING`.
- Present official app config maps to `CORE_CONTRACT_PRESENT_APP_WIRING_PENDING` until non-sensitive audio upload/poll/result validation is completed.
- Transcript fill-in maps to `TRANSCRIPT_READY` and enters the same L3 pipeline with transcript timeline, summary, evidence, knowledge points, questions, review queue, and mastery.
- v1.6 adds `OfficialRuntimeGateway.asrLongStatus`, so diagnostics now report `OFFICIAL_APP_WIRING_PENDING` for core-present/app-pending status instead of schema-missing language.

## Core Evidence

- Core provider contract exists in `VivoAsrProvider`.
- Official doc 1739 task flow is represented as create/upload/run/progress/result.
- App demo path has not validated real upload, polling, result parsing, cancellation, timeout, or classroom-audio privacy behavior.

## Exact Remaining Gap

The blocker is not missing schema. The blocker is app-level wiring and validation:

- app adapter not validated with a non-sensitive audio file
- local config may not be provisioned on demo/cloud device
- upload/polling/result lifecycle is not exercised in the current demo app
- recording is not presented as automatic transcription
- no live upload/poll/result call was run in the v1.6 runtime wiring task

## Fallback

Manual transcript fallback is complete for L3 entry. Segments are generated from paragraphs/sentences and marked fallback-generated.
