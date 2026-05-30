# proof/logs_redacted/

Redacted call logs collected during real-device or cloud-emulator runs.

## Rules (spec §13.1)

- English-only field names and values.
- **Never** commit raw API Key, App Key, Authorization headers, raw audio,
  user PII, or unauthorized course text.
- One JSON-lines file per session, filename
  `api_call_<task>_<yyyymmdd_hhmm>_redacted.jsonl`.
- Field set is locked by [core/logging/ModelCallLog.kt](../../core/src/main/kotlin/com/classmate/core/logging/ModelCallLog.kt).
  Add a field → edit the data class → audit the change.

## What lives here

| File | Status | Notes |
| --- | --- | --- |
| `sample_model_call_redacted.jsonl` | **SAMPLE** | Hand-crafted illustration of the v0.4 line shape. NOT collected from a real call. |

## Reading the sample

Each line is one model-analysis call. Field meanings (v0.4):

| Field | Type | Meaning |
| --- | --- | --- |
| `timestamp` | string (RFC 3339, Asia/Shanghai) | when the call was issued |
| `provider` | `local` \| `compatible` \| `bluelm` | which provider produced the result |
| `task` | string | always `course_analysis` in v0.4 |
| `input_segment_count` | int | how many input segments were sent |
| `hotword_count` | int | how many hotwords were in the prompt |
| `success` | bool | true if a CourseAnalysisResult was returned |
| `latency_ms` | int | wall-clock from request issued to result rendered |
| `structure_valid` | bool | ResultValidator(passed) AND EvidenceValidator(schemaPassed) |
| `strict_evidence_match_rate` | float \| null | fraction of evidence_span values that matched verbatim **in input text** |
| `lenient_evidence_match_rate` | float \| null | fraction matched in input OR result.correctedText |
| `fallback_used` | bool | true iff the run fell back to a lower-priority provider |
| `error_type` | string \| null | short enum-like label only; null on clean success |
| `api_key_redacted` | bool | always true at construction — the marker reviewers grep for |

## How to collect a real log

1. Put real keys in `config.local.json` and push to
   `/data/data/com.classmate.app/files/config.local.json`.
2. Set `provider` to `compatible` (the only fully-wired non-local path).
3. Run the demo flow.
4. `adb logcat -s ClassMateLog` captures the log lines.
5. The in-app `RedactedLogger` already drops keys — verify before committing.
6. Save as `api_call_course_analysis_<yyyymmdd_hhmm>_redacted.jsonl` next to
   the sample. Do NOT delete or alter the sample file.
