# Real Device Test Manual - 1.14.16 / 129

## A. Confirm APK Identity

1. Open ClassMate.
2. Go to Settings.
3. Open developer settings.
4. Find the fixed `诊断与日志` card.

Expected:
- Current version: `1.14.16`.
- versionCode: `129`.
- Git commit: the installed short hash.
- Build time and build variant are visible.

## B. Copy Diagnostics

In `诊断与日志`, verify these buttons are visible:

- `复制完整诊断包`
- `复制最近 200 条事件`
- `复制上次崩溃`
- `清空诊断日志`

Expected:
- The buttons are visible without opening any collapsible log area.
- They work without network and without official provider configuration.
- Copy success shows an in-app message.

## C. Auto Quiz Trace

After submitting material and generating a course, copy the full diagnostics package.

Expected event sequence:

- `material.submit.clicked`
- `course.analysis.completed`
- `l3.snapshot.published`
- `quiz.auto_prepare.started`
- `quiz.auto_prepare.generated_count`
- `quiz.auto_prepare.filtered_count`
- `quiz.auto_prepare.final_count`

If no questions are prepared, look for:

- `quiz.auto_prepare.failed_reason`
- `practice.builder.empty_reason`

## D. Practice Question Source Trace

Start practice from Review and from the course timeline, then copy recent events.

Expected events:

- `practice.start.clicked`
- `practice.start.source`
- `practice.builder.question_count_before_gate`
- `practice.builder.question_count_after_gate`
- `practice.screen.render.question_count`
- `practice.screen.first_question_type`
- `practice.screen.has_fill_blank`

Interpretation:

- before-gate > 0 and after-gate = 0: generated questions were rejected by the quality gate.
- after-gate > 0 and render = 0: UI state wiring is wrong.

## E. Practice Completion Crash Trace

1. Start a practice session.
2. Tap complete.
3. If the app crashes, reopen ClassMate.
4. Go to Settings -> developer settings -> `诊断与日志`.
5. Tap `复制完整诊断包`.

Expected breadcrumbs before any crash:

- `practice.complete.clicked`
- `practice.complete.precheck`
- `practice.complete.question_count`
- `practice.complete.current_index`
- `practice.complete.answer_count`
- `practice.complete.summary_start`
- `practice.complete.summary_built`
- `practice.complete.navigate_review_start`
- `practice.complete.navigate_review_done`
- or `practice.complete.error` with exception type only.

## F. Practice Back Arrow Trace

1. Start practice.
2. Tap the top-left arrow.
3. Copy recent events.

Expected:

- `practice.back.clicked`
- `practice.back.navigate_review_start`
- `practice.back.navigate_review_done`
- `practice.back.target_review`
- The app returns to Review and does not exit.

## G. Logcat Fallback

If the in-app diagnostics package cannot be copied, use:

```powershell
adb logcat -v time ClassMateDebug:D AndroidRuntime:E *:S > classmate_crash_filtered.txt
```

The `ClassMateDebug` stream contains redacted event names, counts, categories, and exception classes. It should not contain course source text or credential values.
