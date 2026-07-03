# ClassMate 1.14.16 / versionCode 129

## Summary

This release fixes the diagnostic blind spot found on real devices: testers could see the installed version and commit, but not the copyable DebugEventLog. Developer settings now exposes a fixed `诊断与日志` card, and diagnostic events are persisted so crash reports survive app restart.

## Changes

- Added a fixed developer diagnostics card with build identity, recent event count, last-crash status, and four actions:
  - copy full diagnostics package;
  - copy recent 200 events;
  - copy last crash;
  - clear diagnostics logs.
- Persisted DebugEventLog into app-private storage with immediate flush on append.
- Added global uncaught crash breadcrumbs. The app records exception class, top stack frame, current screen summary, build identity, and the last 30 events before handing the crash back to Android.
- Practice completion now writes `practice.complete.clicked`, `practice.complete.precheck`, count/index/answer breadcrumbs, summary start/built, navigation start/done, and error type breadcrumbs.
- Practice back arrow writes `practice.back.clicked`, `practice.back.navigate_review_start`, `practice.back.navigate_review_done`, and error type breadcrumbs.
- Logcat mirrors diagnostic events with tag `ClassMateDebug`.

## Boundary

- Diagnostics are redacted: no large course text, provider request body, or credential value is copied.
- This release does not change BlueLM/qwen mode mapping, OCR text layering, or quiz generation policy.
- The crash handler records evidence and then delegates to Android; it does not swallow crashes.
