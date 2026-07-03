# ClassMate 1.14.14 / versionCode 127 Changelog

## Scope

This patch fixes the latest real-device practice regressions without changing the BlueLM/qwen provider architecture or the OCR three-layer text model.

## Changes

- Material submission now triggers automatic quiz preparation after course analysis, accepted subject knowledge extraction, course summary, and review plan generation.
- The Review tab can show quiz preparation states: preparing, ready with question count, insufficient material, and failed/retryable.
- `QuizQualityGate` rejects low-quality student-visible questions before they enter `PracticeSessionScreen`.
- Cloud quiz generation and local fallback quiz generation both run through `StudentVisibleQuizSanitizer` and `QuizQualityGate`.
- Practice completion returns to the Review tab/root review plan and no longer relies on a generic back-stack pop.
- Practice top-left back and system back also return to Review instead of exiting the app.
- Evidence-detail pages with no quiz for some knowledge points are intentionally not forced in this patch, per user priority.

## Preserved Behavior

- qwen3.5-plus remains the BlueLM competition-path model.
- Fast/balanced/professional reasoning mappings remain unchanged from 1.14.9.
- Formal BlueLM calls keep the long timeout strategy; dry-run and provider smoke remain short.
- OCR raw/normalized/candidate separation from 1.14.11 remains intact.
- Local fallback is never labeled as BlueLM.

## Risk

- Real BlueLM generation still depends on valid AppID/AppKey, network, and service permission.
- If material is too thin, the app should show insufficient-material state instead of storing low-quality questions.
