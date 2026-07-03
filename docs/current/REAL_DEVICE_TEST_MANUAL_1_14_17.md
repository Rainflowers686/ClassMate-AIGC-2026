# Real Device Test Manual - 1.14.17 / 130

## A. Confirm APK Identity

Open developer settings and confirm:

- versionName: `1.14.17`
- versionCode: `130`
- Git commit: current short hash
- Build variant: debug for the test APK

## B. Submit Material And Inspect Auto Quiz

Submit a new material package and copy the diagnostics package.

Expected events:

- `material.submit.clicked`
- `course.analysis.completed`
- `l3.snapshot.published`
- `quiz.auto_prepare.started`
- `quiz.auto_prepare.generated_count`
- `quiz.auto_prepare.accepted_student_question_count`
- `quiz.auto_prepare.final_count`

Failure interpretation:

- `quiz.quality.reject_placeholder`: placeholder was correctly blocked.
- `quiz.quality.reject_retry_item`: retry item was correctly blocked.
- `quiz.quality.reject_no_fill_blank`: generated questions lacked a fill-in question and were not shown as a real practice session.
- `quiz.auto_prepare.final_count count=0`: Review should show insufficient material instead of entering weak practice.

## C. Start Practice

Start practice from Review.

Expected events:

- `practice.builder.question_count_before_gate`
- `practice.builder.accepted_student_question_count`
- `practice.builder.placeholder_count`
- `practice.builder.retry_item_count`
- `practice.builder.question_count_after_gate`
- `practice.screen.first_question_type`
- `practice.screen.has_fill_blank`

Expected values:

- `practice.screen.first_question_type` should not be `QUIZ_RETRY`.
- For a normal 3+ question session, `practice.screen.has_fill_blank value=true`.
- If only placeholders/retry items exist, Practice should not open; Review should show the insufficient-material state.

## D. Complete Practice

Finish a practice session.

Expected event order:

- `practice.complete.clicked`
- `practice.complete.summary_start`
- `practice.complete.summary_built`
- `practice.complete.state_cleared`
- `practice.complete.review_state_ready`
- `practice.complete.navigate_review_start`
- `practice.complete.navigate_review_done`
- `review.render.started`

Expected state:

- App returns to Review, not app exit.
- If the app still crashes, copy diagnostics after restart and also run:

```powershell
adb logcat -v time ClassMateDebug:D AndroidRuntime:E *:S > classmate_crash_filtered.txt
```

## E. Practice Back Arrow

Tap the top-left practice back arrow.

Expected events:

- `practice.back.clicked`
- `practice.back.state_cleared`
- `practice.back.navigate_review_start`
- `practice.back.navigate_review_done`

Expected state:

- App returns to Review tab.
- It must not use an app-exit or pop-only path.
