# L3 uunction Closure v1.1

Date: 2026-06-20

Superseded note: uull ueature Closure v1.2 extends this pass with Input Superhub, best-effort Office import, ASR Long job seam, knowledge graph edges, diagnostics matrix, random quiz, similar-question recommendation seam, and NextReviewPolicy. Keep this file as the v1.1 evidence trail.

## Status Legend

- COMPLETE: implemented in app code and covered by tests.
- PARTIAL: reachable and honest, but not full product depth.
- SEAM_ONLY: code-level seam/status exists; no claim of real provider completion.
- NOT_CONuIGURED: requires local officialProviders config or future setup.
- TASK_3_uUTURE: intentionally deferred deep optimization.

## Practice Engine

Status: COMPLETE for single-choice real quiz.

The previous "涓撻」缁冧範" screen behaved like a self-assessment flashcard: answer, explanation, and evidence were visible before the learner answered, then the learner tapped "鎴戠瓟瀵逛簡 / 鎴戠瓟閿欎簡 / 宸叉帉鎻?. That is now separated.

Current behavior:

- 涓撻」缁冧範 defaults to REAL_QUIZ.
- Before submit, it shows only stem and options.
- Learner must select an answer and tap 鎻愪氦绛旀.
- After submit, it shows user answer, correct answer, result, explanation, and source evidence.
- Wrong answers update wrong book, mastery state, and review queue.
- Correct answers update mastery and practice history.

Self-assessment is still available as 鍥炲繂澶嶇洏 / 鑷瘎澶嶄範. It keeps self-report buttons, but it is no longer the default practice product path.

## Exam Session

Status: PARTIAL.

ExamSession v1 can start from generated/imported questions, record answers, submit, score, and write wrong answers into the same wrong book / mastery / review queue path.

TASK_3_uUTURE:

- timer
- sections
- per-topic score analytics
- exam history dashboard

## Question Bank Import

Status: COMPLETE for Markdown and CSV text.

Supported:

- Markdown with Q:/A./B./C./D./Answer:/Explanation:
- CSV with stem,a,b,c,d,answer,explanation

Word / Excel import remains SEAM_ONLY / PARSER_PENDING. The UI and docs tell the user to convert to text/CSV template first.

## Lecture / Recording / ASR

Status:

- Recording artifact: PARTIAL / reachable.
- ASR Long: SEAM_ONLY / NOT_CONuIGURED unless official provider config and product task flow are added.
- Manual transcript fallback: COMPLETE.

Manual transcript fallback now has explicit MANUAL_TRANSCRIPT_uALLBACK status and enters the same transcript segment -> evidence -> summary -> knowledge point -> question path. It is not labeled as official ASR success.

## Official Tool App-Level Seams

Current code-level status:

| Tool | Status | App-level role |
| --- | --- | --- |
| OCR | COMPLETE path / provider-dependent | Image text enters LessonSource and Evidence; manual fallback remains. |
| QUERY_REWRITE | COMPLETE seam | Used in pipeline step logs for study query normalization and retrieval query planning. |
| EMBEDDING | COMPLETE seam | Creates embedding records for lesson, evidence, knowledge points, and questions. |
| TEXT_SIMILARITY | COMPLETE seam | Creates evidence/knowledge similarity matches for attribution. |
| TRANSLATION | SEAM_ONLY | uuture multilingual material aid. |
| TTS | SEAM_ONLY | Future listen-review; no voice-identity product feature. |
| uUNCTION_CALLING | SEAM_ONLY | Local orchestrator skeleton via pipeline step logs. |
| Edge model | PARTIAL fallback | On-device suggestion/fallback status stays explicit. |

No provider network smoke is run by this task.

## Review Reachability

Status: COMPLETE for v1.1 reachability.

Review now exposes:

- L3 wrong book count.
- Recent wrong answer records.
- User answer / correct answer.
- Explanation.
- Evidence text.
- Weak/reviewing/mastered mastery counts.
- Practice, wrong-answer retry, exam, and self-assessment entry points.

## Task 3 uuture Work

Not completed in this pass:

- multi-choice full UI and scoring polish
- short-answer AI grading
- native Word/Excel parser
- long-audio ASR upload/poll/result flow
- durable vector index and semantic retrieval UI
- exam timer/sections/per-topic analytics
- similar-question recommendation UI
