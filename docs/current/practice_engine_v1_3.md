> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# Practice Engine v1.3

Date: 2026-06-20

## Product Goal

Practice Engine v1.3 moves beyond code-level real quiz by strengthening multi-type questions, exam reports, random quizzes, wrong-book replay, and local similar-question fallback.

## Completed

- `REAL_QUIZ` remains the default for `专项练习`; answers and explanations are hidden before submit.
- Multi-choice parser now preserves answer sets such as `A,B`.
- Multi-choice grading compares selected answer sets and returns `CORRECT`, `PARTIAL`, or `WRONG`.
- Short-answer items are explicitly marked as self-assessment / `AI_GRADING_SEAM_ONLY`; automatic scoring is not claimed.
- Exam result reports include score, correct/wrong counts, elapsed time, weak knowledge points, wrong question ids, and evidence ids.
- Review page exposes random quiz entry points for 3 / 5 / 10 questions.
- Similar-question recommendation remains visible as experimental local fallback.
- Distractor explanations are tracked with `AI_EXPLANATION_PENDING` status for future AI expansion.

## Honest States

| Feature | Status | Notes |
| --- | --- | --- |
| Single choice | COMPLETE | Real select/submit/score/evidence loop. |
| True/false | COMPLETE | Parser maps true/false to two options. |
| Multi-choice | PARTIAL usable | Selection and grading work; deeper UX polish is future work. |
| Short answer | SEAM_ONLY | Self-assessment works; AI grading not claimed. |
| Exam report | PARTIAL | Code-level report and score exist; rich device result page remains future work. |
| Similar recommendations | LOCAL_FALLBACK / EXPERIMENTAL | Local text similarity is used; provider-backed recommendation is future work. |

## Task 4 Future

- Rich exam sections, timer enforcement, and per-topic analytics.
- AI-assisted short-answer grading with transparent confidence and human override.
- Persistent similar-question bank backed by real embeddings.
