# Full Feature Closure v1.2

Date: 2026-06-20

## Scope

This pass installs the remaining Lecture / Practice / Review / Official Tool capabilities at code level before unified device validation. It does not run real provider network smoke and does not read `config.local.json`.

Current status note (v1.5): official wording has been narrowed after red-team review. OCR is the app-level official product path. Query Rewrite, Embedding, and Text Similarity are smoke PASS but app usage is local/seam/fallback until live app validation proves otherwise. ASR Long core contract exists, but app wiring and non-sensitive audio validation are pending.

## Input Superhub

| Input | Status | Code-level behavior |
| --- | --- | --- |
| Text paste | COMPLETE | Directly enters LessonSource and L3 pipeline. |
| TXT / MD | COMPLETE | Decoded through text import and enters course text. |
| CSV | COMPLETE | Can enter question bank parser. |
| DOCX | BEST_EFFORT with quality guard | ZIP/XML extraction from `word/document.xml`; parsed as question bank if it matches template, otherwise course text. Empty/suspicious output is flagged. |
| XLSX | BEST_EFFORT with quality guard | ZIP/XML extraction from shared strings and sheet XML; supports simple `stem,a,b,c,d,answer,explanation` tables. |
| PPTX | BEST_EFFORT with quality guard | ZIP/XML extraction from slide XML text; complex decks remain limited and quality is surfaced. |
| PDF | PARSER_PENDING | File artifact is recorded; manual text/OCR fallback is required. |
| Image / photo | OCR_READY_SEAM | Image path is reachable; official OCR remains config-gated with manual OCR text fallback. |
| Audio file / recording | PARTIAL | Audio artifact and ASR Long job seam are recorded; manual transcript fallback enters L3. |

## Lecture Mode

| Capability | Status | Notes |
| --- | --- | --- |
| Start/stop recording | PARTIAL | App-private recording artifact seam exists; permission failure is explicit. |
| ASR Long job | APP_WIRING_PENDING / NOT_CONFIGURED | Core VivoAsrProvider 1739 contract exists; `AsrLongJob` tracks app status; no fake upload/poll success. |
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

See `official_tool_productization_matrix.md`. OCR has the app-level official path for image/photo/OCR text. Query Rewrite, Embedding, and Text Similarity have app local/seam/fallback paths despite provider smoke PASS. Translation, TTS, Function Calling, ASR Long, and Edge model are represented by honest seam/fallback states unless configured and validated later.

## Task 3 Future

- Full ASR Long app upload/poll/result validation.
- PDF parser or OCR-per-page pipeline.
- Robust DOCX/XLSX/PPTX rich formatting support.
- Native multi-choice and short-answer grading UI.
- Durable vector store and semantic retrieval UI.
- TTS playback controls and official TTS network path.
- Function Calling provider-backed orchestration.
- Exam timer, sections, topic analytics.
