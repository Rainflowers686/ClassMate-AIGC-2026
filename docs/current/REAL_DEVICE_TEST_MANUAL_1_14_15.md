# Real Device Test Manual - 1.14.15 / 128

## A. Confirm Installed APK

1. Open Settings.
2. Open developer settings.
3. Check the Build card.

Expected:
- Version shows `1.14.15 (128)`.
- Commit shows the installed short hash.
- Build time and build variant are visible.

## B. Copy Debug Events

1. In developer settings, open the redacted log area.
2. Tap `复制调试事件`.
3. Paste the copied text into the test note.

Expected:
- The log contains only event names, counts, booleans, categories, and exception types.
- It does not contain course source text, question text, or credential values.

## C. Material Submission And Auto Quiz Preparation

1. Add fresh material.
2. Generate the course.
3. Copy DebugEventLog.

Expected events:
- `material.submit.clicked`
- `material.submit.completed`
- `course.analysis.started`
- `course.analysis.completed`
- `l3.snapshot.published`
- `quiz.auto_prepare.started`
- `quiz.auto_prepare.generated_count`
- `quiz.auto_prepare.filtered_count`
- `quiz.auto_prepare.final_count`

If no quiz appears, check for:
- `quiz.auto_prepare.failed_reason`
- `practice.builder.empty_reason`

## D. Practice Question Source

1. Start practice from Review.
2. Start practice from the course timeline.
3. Copy DebugEventLog after each path.

Expected events:
- `practice.start.clicked`
- `practice.start.source`
- `practice.builder.courseId`
- `practice.builder.question_count_before_gate`
- `practice.builder.question_count_after_gate`
- `practice.screen.render.question_count`
- `practice.screen.first_question_type`
- `practice.screen.has_fill_blank`

Interpretation:
- If before-gate count is greater than zero but after-gate count is zero, the questions were generated but rejected by the quality gate.
- If after-gate count is greater than zero but render count is zero, the UI state wiring is wrong.

## E. Low-Quality Question Gate

Start practice and inspect copied events.

Expected:
- Bad questions do not render.
- Rejections appear as `quiz.quality.rejected` with reason only, such as `meta_wording`, `generic_stem`, `no_evidence`, `duplicate_option`, `weak_fill_blank`, or `true_false_over_limit`.

## F. Complete Practice

1. Answer a practice session.
2. Tap complete once.
3. Repeat with a rapid double tap.

Expected:
- No crash.
- The app returns to Review.
- DebugEventLog contains `practice.complete.clicked`, `practice.complete.summary_built`, and `practice.complete.navigate_review`.
- Any failure is logged as `practice.complete.error` with exception type only.

## G. Practice Back Arrow

1. Open a practice session.
2. Tap the top-left arrow.
3. Repeat with system back.

Expected:
- The app returns to Review.
- DebugEventLog contains `practice.back.clicked`, `practice.back.target_review`, and `practice.back.used_pop_back_stack value=false`.
- The app does not exit.
