# Lecture Mode v1.3

Date: 2026-06-20

## Product Goal

Lecture Mode v1.3 turns a recording or transcript into a classroom learning package: transcript timeline, summary, key points, doubts, knowledge graph edges, micro questions, review queue, and evidence chain.

## Completed

- Recording artifact model remains reachable from the import page with start/stop/save state.
- Saved audio artifacts create ASR Long jobs with honest `ASR_NOT_CONFIGURED` or `PENDING_ASR_CONFIG` status.
- Manual transcript fallback enters the same L3 pipeline and produces `TranscriptSegment` entries.
- Transcript timeline is visible in course detail, including time range, source type, and fallback marker.
- Audio/manual transcript evidence uses `AUDIO_TRANSCRIPT` or `MANUAL_TRANSCRIPT`.
- Course detail exposes semantic index and tool orchestration status for the generated learning package.

## Honest States

| Capability | Status | Notes |
| --- | --- | --- |
| Real-time recording | PARTIAL | Artifact and record status exist; real-device audio validation is still required. |
| ASR Long official path | NOT_CONFIGURED / SEAM_ONLY | No official success is claimed without configured provider execution. |
| Manual transcript fallback | COMPLETE | Pasted transcript text can generate segments, evidence, summary, questions, review queue, and mastery. |
| Timeline | COMPLETE for fallback text | Time ranges are generated locally when true ASR timestamps are unavailable. |
| Evidence seek | SEAM_ONLY | Evidence stores time ranges; playback seek is reserved for future device validation. |

## Task 4 Future

- Official ASR Long upload/poll/result parsing.
- Audio playback and evidence seek-to-time.
- Speaker-aware timeline and confidence visualization.
