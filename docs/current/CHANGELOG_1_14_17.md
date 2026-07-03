# ClassMate 1.14.17 / versionCode 130

## Summary

This release is based on the 1.14.16 real-device diagnostic log. Auto quiz preparation was running, but `SAFE_PLACEHOLDER` and `QUIZ_RETRY` items were reaching the practice page, and practice completion crashed after Review navigation. 1.14.17 blocks placeholder/retry items from student practice and clears active practice state before rendering Review.

## Changes

- Added `SINGLE_CHOICE` as the real student single-choice practice type.
- Kept `QUIZ_RETRY` as a non-student retry/placeholder type and rejected it in the final quality gate.
- Rejected `SAFE_PLACEHOLDER` in `QuizQualityGate`; local high-quality fallback questions are normalized to `MANUAL`, not BlueLM.
- `auto_prepare.final_count` and `practice.builder.question_count_after_gate` now count only accepted student questions.
- Sessions with at least three accepted questions must include a fill-in question, otherwise they enter the insufficient-material state.
- Practice completion now clears active practice session/index/answers before switching to Review.
- Review render diagnostics now record key count and duplicate key count.

## Boundaries

- BlueLM/qwen3.5-plus mode mapping is unchanged.
- Official provider diagnostics and persistent logs are unchanged.
- Evidence-detail pages with missing per-knowledge-point quizzes remain deferred.
