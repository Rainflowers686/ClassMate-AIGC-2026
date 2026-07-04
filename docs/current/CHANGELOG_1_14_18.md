# ClassMate 1.14.18 / versionCode 131 Changelog

Commit: local 1.14.18 commit to be produced after validation.

## What Changed

- Fixed the remaining practice-completion crash path using the 1.14.17 real-device diagnostics.
- Completion summary, learning-record writeback, and active-practice cleanup already succeeded in 1.14.17; the crash happened after Review navigation, before Review render diagnostics started.
- Practice completion and practice back now set a deferred Review navigation request. The app root consumes it after a frame, avoiding same-call-stack state cleanup plus screen switch.
- Removed the top-level `Crossfade(targetState = currentScreen)` navigation wrapper. Practice -> Review now uses a plain screen switch.
- Review diagnostics now include compose checkpoints around Review entry and list setup.
- Crash diagnostics now persist `stackTraceTop20` and `causeStackTop10`.
- Added repository-side round-two submission material drafts and a safe core LLM code packaging script.

## Kept Unchanged

- BlueLM/qwen3.5-plus three-mode strategy from 1.14.9.
- OCR raw/normalized/subject-candidate separation.
- Persistent developer diagnostics and `ClassMateDebug` logcat tag.
- `QuizQualityGate`, `StudentVisibleQuizSanitizer`, placeholder/retry rejection, and fill-blank requirements.
- Official provider dry-run/live-smoke behavior.

## Risk

- This patch targets the Compose navigation/key crash indicated by diagnostics. It still needs a final real-device run to confirm no `SlotTableKt.key` crash remains.
- Evidence-detail pages with some knowledge points lacking micro-quiz remain deferred by prior user approval.
