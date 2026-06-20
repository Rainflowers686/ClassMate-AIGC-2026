# Championship Upgrade v1.3

Date: 2026-06-20

No secrets, endpoint URLs, Authorization values, `config.local.json` contents, or raw smoke outputs are recorded here.

## Scope

Task 3 upgrades the already-installed L3 feature set from code-level reachability toward productized learning loops. It does not perform unified real-device validation and does not run provider network smoke.

## Completed

- Lecture package generation now exposes recording artifacts, ASR Long job state, manual transcript fallback, segmented transcript timeline, summary, knowledge graph edges, micro questions, review queue, evidence links, semantic index chunks, and local tool orchestration plan.
- Input Superhub now records `ImportReport` objects and PDF page-level fallback objects, including page OCR seam and manual page text readiness.
- Practice supports strict multi-choice grading, short-answer self-assessment / AI grading seam, exam result reports, local similar-question fallback signals, distractor explanation pending records, and random quiz quantity entry points.
- Review exposes daily review stats: due today, overdue, weak, wrong questions, mastered, and mastery distribution.
- Official tool productization now surfaces local orchestration, semantic index chunks, text similarity fallback, Translation seam, TTS listen-review seam, and Edge local-rule fallback without claiming unconfigured official success.

## Status Matrix

| Area | v1.3 status | Notes |
| --- | --- | --- |
| Recording artifact | PARTIAL | Start/stop/save model and UI status are present; playback seek remains `SEEK_SEAM_ONLY`. |
| ASR Long | NOT_CONFIGURED / SEAM_ONLY | Job model and fallback exist; official upload/poll/result flow is Task 4. |
| Transcript timeline | COMPLETE for manual/fallback text | Segments are generated with time ranges and fallback marker. |
| PDF | PARTIAL / LOCAL_FALLBACK | Artifact + page model + OCR seam + manual page text fallback; native text parser remains pending. |
| DOCX/XLSX/PPTX | BEST_EFFORT | ZIP/XML extraction strengthened in v1.2 and covered by v1.3 tests. |
| Multi-choice | PARTIAL usable | Parser preserves answer sets; UI toggles multiple options and strict/partial grading is tested. |
| Short-answer | SEAM_ONLY / SELF_ASSESSMENT | User input/self-assessment path is honest; AI grading is not claimed. |
| Semantic index | LOCAL_FALLBACK / PARTIAL | Lightweight local vector placeholder and similarity scoring are present; persistent provider-backed index is Task 4. |
| Similar question recommendation | LOCAL_FALLBACK / EXPERIMENTAL | Local similarity and UI visibility exist; large-scale recommendation remains future work. |
| TTS | SEAM_ONLY / OFFICIAL_TTS_NOT_CONFIGURED | Listen-review preparation is present; official TTS playback is not claimed. |
| Translation | SEAM_ONLY / NOT_CONFIGURED | Translation request/result seam exists; original evidence remains unchanged. |
| Function Calling | LOCAL_ORCHESTRATOR | Local planner records tool chain; official Function Calling is not claimed. |
| Edge model | LOCAL_RULE_FALLBACK | Offline study output seam exists when edge model is unavailable. |

## Guardrails

- Provider smoke logic is untouched.
- `config.local.json` is not read.
- Home CTA and Theme / Advanced Appearance / Settings appearance lines are untouched.
- No APK/AAB/AAR/font/key/local config artifact is added.

## Task 4 Future

- Official ASR Long upload, polling, callback/result parsing, and transcript backfill.
- Native PDF parser or real per-page OCR execution.
- Persistent semantic vector store with incremental updates.
- Official TTS playback validation and device-level audio controls.
- Official Translation and Function Calling provider execution.
- Rich exam sections, timer enforcement, per-topic analytics, and long-term mastery trend charts.
