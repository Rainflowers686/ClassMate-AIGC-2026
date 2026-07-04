# ClassMate 1.14.18 Real-Device Test Manual

## Install Identity

1. Install the debug APK built from this repository.
2. Open Settings -> Developer settings -> Diagnostics and logs.
3. Confirm:
   - versionName: `1.14.18`
   - versionCode: `131`
   - Git commit: current local 1.14.18 commit
   - Build variant: `debug`

## Practice Completion Crash Retest

1. Import material and let automatic quiz preparation finish.
2. Start a micro-quiz from Review.
3. Answer all questions.
4. Tap Complete.
5. Expected:
   - No app exit.
   - No crash.
   - App returns to Review tab/root Review Plan.
   - Diagnostics include:
     - `practice.complete.summary_built`
     - `practice.complete.state_cleared`
     - `practice.complete.review_state_ready`
     - `navigation.review.pending_set`
     - `navigation.review.deferred_frame`
     - `navigation.review.applied`
     - `navigation.review.consumed`
     - `review.compose.enter`

## Back Arrow Retest

1. Start a practice session.
2. Tap the top-left back arrow.
3. Expected:
   - App does not exit.
   - App returns to Review.
   - Diagnostics include `practice.back.clicked`, `practice.back.state_cleared`, and `navigation.review.consumed`.

## If It Still Crashes

1. Reopen the app.
2. Settings -> Developer settings -> Diagnostics and logs -> Copy full diagnostics package.
3. Also run:

```powershell
adb logcat -v time ClassMateDebug:D AndroidRuntime:E *:S > classmate_crash_filtered.txt
```

4. Attach both files. 1.14.18 should include `stackTraceTop20` and `causeStackTop10`.
