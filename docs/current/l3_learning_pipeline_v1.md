# ClassMate L3 Learning Pipeline v1

Date: 2026-06-20

## Current Status

L3 v1 moves ClassMate from provider smoke readiness into an app-level learning loop:

Input material -> text/evidence -> summary -> knowledge points -> micro questions -> learner answer -> explanation/evidence -> wrong book -> review queue -> mastery stats.

Update 2026-06-20 / v1.1: Practice is now split into real quiz, self-assessment review, and exam modes. "专项练习" defaults to real answer selection and does not reveal answers, explanations, or evidence until submit.

Update 2026-06-20 / v1.2: Input Superhub adds TXT/MD/CSV/DOCX/XLSX/PPTX best-effort ingestion, PDF/audio/image artifact states, ASR Long jobs, knowledge graph edges, diagnostics matrix, similar-question recommendations, and rule-based next-review policy.

Update 2026-06-20 / v1.3: Championship Upgrade adds ImportReport, PDF page fallback state, lightweight semantic index chunks, local tool orchestration plans, strict multi-choice grading, short-answer self-assessment / AI grading seam, exam reports, distractor explanation pending records, and ReviewDailyStats.

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
| Word / Excel question bank | best-effort | DOCX/XLSX ZIP/XML extraction supports simple templates; complex formatting remains TEMPLATE_REQUIRED/PARSER_PENDING. |
| PPTX | best-effort | Slide XML text extraction, complex decks remain limited. |
| PDF | partial / parser pending | Artifact plus page OCR seam and manual page text fallback; native parser remains pending. |
| Classroom recording | partial completed | Start/stop record creates app-private audio artifact record. |
| ASR Long | seam only / app wiring pending | Core VivoAsrProvider 1739 contract exists; app demo remains recording artifact + ASR job seam + manual transcript fallback until non-sensitive upload/poll/result validation. |

## Provider App-Level Usage

| Provider | App-Level Role | Current Behavior |
| --- | --- | --- |
| OCR | Image text -> lesson source/evidence | Used when configured; manual fallback otherwise. |
| QUERY_REWRITE | Standardize study/retrieval queries | Official smoke PASS; App uses learning query planning/local fallback seam, not a live official call. |
| EMBEDDING | Lesson/evidence/KP/question index records | Official smoke PASS; App builds local lexical semantic index and embedding record seam, not live official vectors. |
| EMBEDDING local index | Evidence/KP/question semantic chunks | Builds lightweight persistent local lexical vectors; real official vector use remains future validation work. |
| TEXT_SIMILARITY | Evidence match, similar KP/question attribution | Official smoke PASS; App uses local similarity fallback/seam, not live official rerank. |
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
- Rich semantic vector persistence and retrieval ranking UI beyond the v1.3 local index.
- Word/Excel/PPT rich-format parser beyond best-effort templates.
- Advanced spaced repetition scheduling.
- Real-time lecture transcription beyond the existing system-ASR/manual transcript path.
- Short-answer automatic AI grading beyond self-assessment / seam state.
- Timer/section-level exam analytics and provider-backed similar-question recommendations.
