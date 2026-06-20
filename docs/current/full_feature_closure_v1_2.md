# Full Feature Closure v1.2

Date: 2026-06-20

## Scope

This pass installs the remaining Lecture / Practice / Review / Official Tool capabilities at code level before unified device validation. It does not run real provider network smoke and does not read `config.local.json`.

## Input Superhub

| Input | Status | Code-level behavior |
| --- | --- | --- |
| Text paste | COMPLETE | Directly enters LessonSource and L3 pipeline. |
| TXT / MD | COMPLETE | Decoded through text import and enters course text. |
| CSV | COMPLETE | Can enter question bank parser. |
| DOCX | BEST_EFFORT | ZIP/XML extraction from `word/document.xml`; parsed as question bank if it matches template, otherwise course text. |
| XLSX | BEST_EFFORT | ZIP/XML extraction from shared strings and sheet XML; supports simple `stem,a,b,c,d,answer,explanation` tables. |
| PPTX | BEST_EFFORT | ZIP/XML extraction from slide XML text; complex decks remain limited. |
| PDF | PARSER_PENDING | File artifact is recorded; manual text/OCR fallback is required. |
| Image / photo | OCR_READY_SEAM | Image path is reachable; official OCR remains config-gated with manual OCR text fallback. |
| Audio file / recording | PARTIAL | Audio artifact and ASR Long job seam are recorded; manual transcript fallback enters L3. |

## Lecture Mode

| Capability | Status | Notes |
| --- | --- | --- |
| Start/stop recording | PARTIAL | App-private recording artifact seam exists; permission failure is explicit. |
| ASR Long job | SEAM_ONLY / NOT_CONFIGURED | `AsrLongJob` tracks status; no fake upload/poll success. |
| Manual transcript fallback | COMPLETE | Creates transcript segments and evidence with `MANUAL_TRANSCRIPT_FALLBACK`. |
| Segment timeline | COMPLETE for fallback | Manual/audio transcript segments have time ranges; fallback-generated segments are marked. |
| Summary / key points / doubts / actions | COMPLETE | L3 snapshot contains summary, key takeaways, review focus, and action items. |
| Knowledge graph | PARTIAL | `KnowledgeGraphEdge` creates related/example edges; UI shows first relations as a knowledge map. |
| Micro questions | COMPLETE | Generates 3-5 evidence-bound questions. |
| Review queue | COMPLETE | Knowledge points enter queue; wrong answers update due/priority. |
| Recording evidence chain | PARTIAL | Transcript evidence supports time ranges; direct audio seek/playback is TASK_3_FUTURE. |

## Practice / Exam / Review

- Real quiz: COMPLETE for single choice and basic true/false.
- Multi-choice: PARTIAL model/seam.
- Short answer: SEAM_ONLY / future AI grading.
- Random quiz: COMPLETE, samples from current generated/imported question pool.
- ExamSession: PARTIAL, supports start/submit/score/write-back; advanced timer and sections are future work.
- Wrong book: COMPLETE, visible in Review with answer, correct answer, explanation, evidence, retry count model.
- Similar question recommendation: SEAM_ONLY / LOCAL_FALLBACK, backed by local similarity status and model.
- NextReviewPolicy: COMPLETE rule seam: weak/learning due today, reviewing tomorrow, mastered after three days.

## Official Tool Productization

See `official_tool_productization_matrix.md`. OCR / Query Rewrite / Embedding / Text Similarity have app-level paths. Translation, TTS, Function Calling, ASR Long, and Edge model are represented by `L3OfficialToolSeams` with honest seam/fallback states unless configured and validated later.

## Task 3 Future

- Full ASR Long upload/poll/result flow.
- PDF parser or OCR-per-page pipeline.
- Robust DOCX/XLSX/PPTX rich formatting support.
- Native multi-choice and short-answer grading UI.
- Durable vector store and semantic retrieval UI.
- TTS playback controls and official TTS network path.
- Function Calling provider-backed orchestration.
- Exam timer, sections, topic analytics.
