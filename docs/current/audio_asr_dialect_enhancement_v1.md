# Long Audio / ASR / Dialect Enhancement

Date: 2026-06-21

## Product Goal

ClassMate treats classroom audio as learning evidence, not just a recording file. A recording or transcript can become:

- transcript timeline
- audio evidence
- knowledge points
- micro quiz
- review plan
- wrong-book evidence
- study pack export material

## User Path

1. Record classroom audio or import/paste transcript.
2. Choose normal classroom, accent/dialect enhanced, or mixed-speaker classroom mode.
3. Run transcript path if available, or paste/edit transcript fallback.
4. Confirm transcript.
5. Generate the learning loop.
6. Open EvidenceDetail from quiz, wrong book, or review plan.

## Current Implementation Status

| Area | Status | Notes |
| --- | --- | --- |
| Audio artifact | PARTIAL / READY | File name, duration, size, mime type, source label, and state are tracked. |
| ASR job state | SEAM_READY / CONFIG_REQUIRED | Job/chunk states exist. Live official upload/poll/result still needs device validation. |
| Manual transcript fallback | USED_IN_LEARNING_LOOP | Confirmed transcript enters the same L3 publish path. |
| Dialect free speech | CORE / FALLBACK_ONLY | Dialect mode enters audio capability plan and transcript post-processing. |
| Transcript post-processing | USED_IN_LEARNING_LOOP | Keeps raw/corrected text, glossary hits, and low-confidence flags. |
| Audio evidence detail | USED_IN_LEARNING_LOOP | Shows source, time range, transcript snippet, low-confidence warnings, and linked learning assets. |
| Audio seek playback | VALIDATION_PENDING | UI clearly states playback positioning needs real-device validation when unavailable. |

## Quality Guard

Low-confidence transcript segments require user confirmation before being treated as high-confidence learning evidence. Correction must be conservative and must preserve raw transcript text.
