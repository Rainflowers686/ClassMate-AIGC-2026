# Competitor Gap Closure Plan

Date: 2026-06-20

## Against Lecture-Mode Apps

Completed in L3 v1:

- Classroom material and transcript text can become structured knowledge points.
- Evidence cards remain attached to generated questions and explanations.
- Recording records exist, with honest ASR Long status and manual transcript fallback.

Remaining:

- Full long-audio ASR task flow.
- Speaker-aware timeline and audio playback-to-evidence alignment.
- Live lecture mode with continuous transcript quality checks.

## Against Practice-Only Apps

Completed in L3 v1:

- Imported material generates micro questions with correct answers.
- Imported question bank creates a small quiz.
- Wrong answers update a wrong book, review queue, and mastery stats.
- Questions and explanations cite evidence.

Completed in L3 Function Closure v1.1:

- "专项练习" is now a real quiz path: answer options are shown first, and answer/explanation/evidence appear only after submit.
- The old self-report flow is separated as "回忆复盘 / 自评复习" and no longer masquerades as practice.
- ExamSession v1 starts, submits, scores, and writes wrong answers into the wrong book/review queue/mastery loop.
- Review exposes wrong book records, mastery counts, and evidence text so the loop is reachable, not only modeled.

Completed / installed in Full Feature Closure v1.2:

- Input Superhub covers text, TXT/MD/CSV, DOCX/XLSX/PPTX best-effort, PDF artifact fallback, image/OCR seam, and audio/ASR seam.
- Lecture mode has recording artifact, ASR Long job seam, manual transcript fallback, segment timeline, summary, knowledge graph edges, micro questions, review queue, and evidence chain.
- Practice adds random quiz and similar-question recommendation seam.
- Review adds rule-based next-review policy and stronger diagnostics for official tool statuses.

Deepened in Championship Upgrade v1.3:

- PDF input is now page-aware at code level: artifact, page fallback model, page OCR seam, manual page text readiness, and import report.
- Practice supports multi-choice answer sets and strict/partial grading instead of collapsing `A,B` into `A`.
- Short-answer practice remains honest as self-assessment / AI grading seam; it is not mislabeled as fully automatic.
- Exam result reports record score, elapsed time, wrong questions, weak knowledge points, and evidence ids.
- Review daily stats now summarize due, overdue, weak, wrong-book, mastered, and total knowledge points.
- Semantic index chunks and local text similarity fallback provide a lightweight evidence/question matching path.
- Tool orchestration plans show local OCR / ASR / Query Rewrite / Embedding / Similarity / LLM / Question Generation / Review Update chains.

Red-team P0 fix v1.5:

- Official capability wording is narrowed: smoke PASS is separated from live app official calls.
- OCR is the app-level official path for image/photo/OCR text into evidence, config-gated with fallback.
- Query Rewrite, Embedding, and Text Similarity now have the v1.6 official runtime gateway in the app path; they remain local fallback unless an injected app adapter returns official rewrite/vector/rerank output.
- ASR Long is corrected to core contract present / app wiring pending / manual transcript fallback.
- DOCX/XLSX/PPTX imports have extraction quality guards before demo use.
- Wrong book, review queue, mastery history, attempts, evidence/questions, and exam reports persist through app-private L3 storage.

Remaining:

- Official ASR Long upload/poll/result app validation with non-sensitive audio.
- Native PDF parser or real per-page OCR execution.
- Rich Word/Excel/PPT rich-format parsing beyond the simple ZIP/XML best-effort templates.
- Provider-backed vector store and large-scale similar-question recommendation.
- Official TTS playback, Translation execution, and official Function Calling runtime validation.
- Rich exam mode with sections, timer enforcement, and per-topic score analytics.
- Long-term mastery trend charts, streak/lapse tracking, and smarter spaced repetition.

## Current Mainline

Do not keep expanding features blindly. The next mainline is App-level L3 real-device walkthrough:

1. Import classroom text.
2. Generate L3 local learning loop.
3. Answer one question wrong.
4. Verify wrong book, review queue, mastery stats, and evidence.
5. Repeat with OCR text and question bank.

Provider smoke remains separate and must not be re-run unless explicitly requested.
