# Real Device Fix Matrix - 1.14.17 / 130

| Issue from 1.14.16 diagnostics | Root cause | Fix | Expected log |
| --- | --- | --- | --- |
| Low-quality quiz reached practice | `SAFE_PLACEHOLDER` items were counted as generated questions | Placeholder source is rejected before storage and render | `quiz.quality.reject_placeholder` |
| Practice first question type was `QUIZ_RETRY` | Normal single-choice questions reused the retry enum | Added `SINGLE_CHOICE`; `QUIZ_RETRY` is rejected from student sessions | `quiz.quality.reject_retry_item` if it appears |
| No fill blank in generated session | Builder did not enforce type mix after gate | Sessions with 3+ accepted questions require at least one fill blank | `quiz.quality.reject_no_fill_blank` |
| `final_count` counted placeholders | Count used answerable placeholders instead of accepted student questions | Final count now excludes placeholder/retry/flagged/low-quality questions | `quiz.auto_prepare.accepted_student_question_count` |
| Crash after completion on Review | Active practice state remained while Review was rendered | Completion clears session/index/answers before explicit Review navigation | `practice.complete.state_cleared`, `practice.complete.review_state_ready` |
| Need proof if Review still crashes | No Review render key diagnostics | Review logs item/key/duplicate-key counts | `review.render.duplicate_key_count` |

## Retained Behavior

- Practice back arrow and system back explicitly navigate to Review.
- Local fallback remains labeled as local/manual, never BlueLM.
- BlueLM/qwen3.5-plus strategy is unchanged from 1.14.9.
