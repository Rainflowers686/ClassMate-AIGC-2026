# ClassMate 1.14.15 / versionCode 128 Changelog

## Scope

This patch adds real-device path tracing and minimum safety repairs for the practice flow. It is intended to prove whether the installed APK, material submission, automatic quiz preparation, practice builder, completion action, and practice back arrow are using the current code path.

## Changes

- Developer settings now expose safe build metadata: versionName, versionCode, git commit short hash, build time, and build variant.
- Added an in-memory `DebugEventLog` with the latest 100 redacted events and a copy button in developer settings.
- Material submission and course publication now record `material.submit.*`, `course.analysis.*`, and `l3.snapshot.published` events.
- Automatic quiz preparation now records start, existing-good-question skip, generated count, filtered count, final count, and failure reason.
- Practice startup now records source, course presence, question counts before and after the quality gate, and empty reasons.
- Practice screen render now records question count, first question type, and whether fill-in questions are present.
- Final practice UI entry re-runs the student-visible quality gate. Rejected questions are not displayed; rejection reasons are logged without question text.
- Practice completion is wrapped with null-safe and bounds-safe logging. Failures record exception type only and return safely to Review when possible.
- Practice top-left back and system back from practice record explicit Review-tab navigation instead of using a generic stack pop.

## Preserved Behavior

- BlueLM/qwen3.5-plus model, reasoning-mode mapping, formal timeout strategy, and short dry-run behavior are unchanged.
- OCR raw/normalized/candidate separation remains unchanged.
- Local fallback remains clearly labeled as local and is not presented as a cloud result.

## Risk

- This patch is primarily diagnostic and path-stabilizing. If a real device still shows old behavior, the copied DebugEventLog should identify whether the installed APK is stale, automatic quiz preparation did not start, the quality gate rejected all questions, or navigation was not routed through the practice screen path.
