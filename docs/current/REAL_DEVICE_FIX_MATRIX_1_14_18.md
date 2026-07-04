# ClassMate 1.14.18 Real-Device Fix Matrix

Version: `1.14.18 / versionCode 131`

| Issue | 1.14.17 Evidence | Root Cause | 1.14.18 Fix | Expected Diagnostics |
| --- | --- | --- | --- | --- |
| Completing practice exits/crashes | `summary_built`, `state_cleared`, `review_state_ready`, `navigate_review_done`, then `SlotTableKt.key` at `screen=REVIEW` | Crash is after business completion and before Review render, likely top-level Compose screen switch/key instability | Completion now sets pending Review navigation; root consumes after a frame | `navigation.review.pending_set`, `navigation.review.deferred_frame`, `navigation.review.applied`, `navigation.review.consumed` |
| Practice -> Review screen switch can crash before Review logs | No `review.render.started` after crash | Top-level `Crossfade(currentScreen)` is a high-risk transition | Removed top-level Crossfade and dynamic animation wrapper | Review entry should log `review.compose.enter` |
| Need more crash evidence if still failing | Only top frame was persisted | Diagnostics lacked full stack | Persist `stackTraceTop20` and `causeStackTop10` | Copied diagnostics show stack lists |
| Review list keys could still be suspect | 1.14.17 added duplicate-key count | Review list still needed immutable UI inputs and checkpoints | Review lists copied before rendering and checkpoint logs added | `review.render.duplicate_key_count=0` |
| Back arrow from practice | Earlier reports said it can exit app | Back relied on immediate navigation after state clear | Back sets deferred Review navigation and clears active practice | `practice.back.clicked`, `practice.back.state_cleared`, `navigation.review.consumed` |

## Must Retest

1. Install 1.14.18 debug APK.
2. Confirm Settings -> Developer -> diagnostics shows `versionName 1.14.18`, `versionCode 131`, and the new commit.
3. Complete a 1-question, 4-question, and 5-question practice session.
4. Confirm app returns to Review without crash.
5. Copy diagnostics and verify no `crash.uncaught` after the last completion.
