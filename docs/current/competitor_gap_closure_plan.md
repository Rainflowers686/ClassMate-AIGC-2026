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

Remaining:

- Native Word/Excel question bank parsing.
- Rich exam mode with timer, sections, and per-topic score analytics.
- Similar-question recommendation UI backed by persisted embeddings.

## Current Mainline

Do not keep expanding features blindly. The next mainline is App-level L3 real-device walkthrough:

1. Import classroom text.
2. Generate L3 local learning loop.
3. Answer one question wrong.
4. Verify wrong book, review queue, mastery stats, and evidence.
5. Repeat with OCR text and question bank.

Provider smoke remains separate and must not be re-run unless explicitly requested.
