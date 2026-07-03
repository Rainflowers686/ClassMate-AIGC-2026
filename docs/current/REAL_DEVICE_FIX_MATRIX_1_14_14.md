# Real Device Fix Matrix - 1.14.14 / 127

| Issue | Fix | Verification |
| --- | --- | --- |
| Quiz questions are only prepared after tapping start/generate | `publishL3Snapshot` now begins automatic practice preparation after material submission and course analysis | `FreshInstallLearningFlowRegressionTest` asserts prepared quiz state before start practice |
| Low-quality quiz questions reach the practice UI | Added `QuizQualityGate`; cloud/local generated questions are sanitized and quality-scored before display | `QuizQualityGateTest`; `PracticeFlowTest`; full app unit tests |
| Practice completion can crash or exit incorrectly | Completion and practice exit paths now route to `Tab.REVIEW` rather than generic back-stack pop | `PracticeFlowTest` asserts completion, top-left exit, and system back return to Review |
| Practice back arrow exits app | `exitPractice()` clears practice state and calls `selectTab(Tab.REVIEW)` | `practiceSystemBackReturnsToReviewTab`; `exitPracticeClearsSession` |
| Duplicate or click-time-only generation | Existing prepared questions are reused; generation is skipped unless no good prepared session exists or the user regenerates | Fresh-install regression flow |
| Evidence detail page lacks quiz for some knowledge points | Deferred by user request; existing evidence-backed questions must not be removed | Manual retest, no forced scope in this patch |

## Boundaries

- This patch does not alter the BlueLM/qwen request parameter mapping.
- This patch does not change official ASR/OCR provider configuration.
- Local fallback remains honest and never appears as BlueLM output.
