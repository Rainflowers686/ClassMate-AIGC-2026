# proof/logs_redacted/

Redacted call logs collected during real-device or cloud-emulator runs.

Rules (spec §13.1):

- English-only field names and values.
- **Never** commit raw API Key / App Key / user PII / raw audio.
- One JSON-lines file per session, filename `api_call_<task>_<yyyymmdd_hhmm>_redacted.jsonl`.

v0.2.5 ships this folder empty — nothing has been collected yet because no
real provider is wired. v0.3's first BlueLM call should produce
`api_call_course_analysis_<timestamp>_redacted.jsonl` here.
