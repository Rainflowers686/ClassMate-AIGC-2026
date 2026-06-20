# ClassMate L3 Learning Pipeline v1

Date: 2026-06-20

## Current Status

L3 v1 moves ClassMate from provider smoke readiness into an app-level learning loop:

Input material -> text/evidence -> summary -> knowledge points -> micro questions -> learner answer -> explanation/evidence -> wrong book -> review queue -> mastery stats.

Update 2026-06-20 / v1.1: Practice is now split into real quiz, self-assessment review, and exam modes. "专项练习" defaults to real answer selection and does not reveal answers, explanations, or evidence until submit.

## Completed In This Pass

- Text classroom material can be converted into an L3 snapshot with summary, evidence, knowledge points, 3-5 micro questions, review queue items, and mastery stats.
- Existing successful CourseAnalysis results are mirrored into the L3 snapshot, so cloud/on-device analysis paths gain L3 evidence and provider step logs.
- Question bank import v1 supports Markdown-style and simple CSV-style text.
- Wrong answers update the L3 wrong book, mastery state, and review queue, while the existing LearningStore still records quiz attempts.
- Real Practice Engine v1.1 supports option selection, submit, scoring, post-submit explanation/evidence, wrong book write-back, and mastery/review queue updates.
- Self-assessment remains available as "回忆复盘 / 自评复习" and no longer acts as the default "专项练习" product path.
- ExamSession v1 can start from generated/imported questions, submit, score, and write wrong answers into the same L3 loop.
- App-level provider seams now exist for OCR, Query Rewrite, Embedding, and Text Similarity. They do not run network requests in app tests.
- Classroom recording now has an app-level recording record and app-private audio artifact seam. ASR Long remains honest: not configured unless official config exists.

## Input Status

| Input | Status | Notes |
| --- | --- | --- |
| Text / Markdown material | completed | Enters L3 pipeline and existing CourseAnalysis path. |
| OCR image material | completed path / provider-dependent | OCR text enters the same evidence pipeline. Failures keep manual fallback. |
| Question bank text | completed | Markdown and CSV style parsing. |
| Word / Excel question bank | seam only | Template/planning only; copy/export to text first. |
| Classroom recording | partial completed | Start/stop record creates app-private audio artifact record. |
| ASR Long | seam only | Status is ASR_NOT_CONFIGURED/PENDING_ASR_CONFIG unless configured; manual transcript fallback is explicit. |

## Provider App-Level Usage

| Provider | App-Level Role | Current Behavior |
| --- | --- | --- |
| OCR | Image text -> lesson source/evidence | Used when configured; manual fallback otherwise. |
| QUERY_REWRITE | Standardize study/retrieval queries | Seam records READY_SEAM_USED when configured; local safe rewrite otherwise. |
| EMBEDDING | Lesson/evidence/KP/question index records | Builds embedding record model; provider-ready status when configured. |
| TEXT_SIMILARITY | Evidence match, similar KP/question attribution | Builds similarity match model; provider-ready status when configured. |
| TRANSLATION | Multilingual material support | SEAM_ONLY unless configured; not part of the main L3 blocker path. |
| TTS | Listen-review / course essence playback | SEAM_ONLY unless configured; no voice clone. |
| FUNCTION_CALLING | Local tool orchestration plan | Step-log skeleton only unless official provider is configured. |

## Mastery Rules v1

- New knowledge points start as LEARNING.
- Wrong answer marks the related point WEAK and keeps it due today.
- Correct answer moves the point toward REVIEWING.
- Multiple correct answers without wrong answers can become MASTERED.

## Not Completed

- Full ASR Long upload/poll/result product flow.
- Rich semantic vector persistence and retrieval ranking UI.
- Word/Excel native parser.
- Advanced spaced repetition scheduling.
- Real-time lecture transcription beyond the existing system-ASR/manual transcript path.
- Multi-choice and short-answer automatic grading beyond the current model/seam.
- Timer/section-level exam analytics and similar-question recommendations.
