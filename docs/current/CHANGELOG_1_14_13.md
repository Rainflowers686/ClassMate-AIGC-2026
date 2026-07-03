# ClassMate 1.14.13 / versionCode 126

## Summary

This patch addresses the latest real-device blockers around image OCR import, student-visible quiz quality, fill-in support, and practice completion stability.

## Fixes

- Image upload now preserves the selected file as a material item even when OCR is unavailable, returns empty text, or fails.
- OCR input now uses the original image bytes or a high-quality encoded image, not a thumbnail or low-resolution preview. Re-OCR uses the saved original path.
- OCR failures are recoverable: the user keeps image preview, can manually enter text, and can retry OCR later.
- Raw OCR text, normalized OCR text, and subject knowledge candidates remain separate. Quiz/content filters do not shorten OCR drafts or evidence.
- Student-facing quiz fields are guarded against internal meta wording such as OCR, original text, provider, fallback, raw ids, and relevance-rule phrases.
- Local fallback questions are generated from subject knowledge cards. Options now use subject misconceptions or related concepts instead of meta statements.
- Micro-quizzes now include fill-in questions where material allows, and fill-in answers are graded without relying on option lists.
- Practice completion is null-safe for mixed question types, empty sessions, missing evidence, and repeated taps. Completion returns to Review Plan.

## Deferred

- Evidence-detail pages that still have no micro-quiz for a specific knowledge point are not forced in this patch. Existing evidence-backed questions must remain intact.

## Preserved

- BlueLM/qwen3.5-plus mode mapping remains unchanged from 1.14.9: fast = low + no thinking, balanced = medium + no thinking, professional UI Max = API high + thinking.
- Local fallback is still labelled honestly and is never presented as BlueLM output.
