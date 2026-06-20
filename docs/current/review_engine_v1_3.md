# Review Engine v1.3

Date: 2026-06-20

## Product Goal

Review Engine v1.3 strengthens the long-term learning loop: wrong answers update mastery, mastery feeds review scheduling, and daily review stats become visible.

## Completed

- Wrong answers continue to create wrong-book records and mark related knowledge points as weak.
- Correct answers update attempts and can improve mastery state.
- Review queue due dates continue to use `NextReviewPolicy`.
- `ReviewDailyStats` now summarizes due today, overdue, weak, wrong questions, mastered, and total knowledge points.
- Review page shows the daily review card alongside the existing wrong-book/evidence block.
- Course detail can show exam report summaries and semantic/tool-plan diagnostics.

## Honest States

| Feature | Status | Notes |
| --- | --- | --- |
| Wrong book | COMPLETE for L3 questions | User answer, correct answer, explanation, and evidence are retained. |
| Review queue | COMPLETE for local pipeline | New knowledge points and wrong answers enter the queue. |
| Mastery stats | PARTIAL | Counts and states exist; trend charts are future work. |
| Daily review card | PARTIAL | Daily summary is visible; long-term trend and streak are future work. |
| Forgetting curve | LOCAL_FALLBACK | Rule-based due dates; no advanced spaced-repetition model yet. |

## Task 4 Future

- Mastery trend chart by course and knowledge point.
- Streak/lapse tracking.
- Smarter priority scoring with elapsed time, repeated errors, and confidence.
