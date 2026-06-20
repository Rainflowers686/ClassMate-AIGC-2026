# L3 Pipeline Demo Script

Date: 2026-06-20

Target length: 2-3 minutes.

## Script

1. Open ClassMate Home.
2. Tap `整理一份新资料`.
3. Open `L3 演示包` or paste a short classroom material.
4. In the material tray, confirm the text and optional question bank.
5. Tap `生成 L3 本地学习闭环`.
6. On the course page, show:
   - summary
   - knowledge map
   - L3 学习闭环 card
   - provider seam chips for OCR / Query Rewrite / Embedding / Text Similarity
   - L3 能力诊断
   - transcript timeline when using manual transcript fallback
   - semantic index record/search status and local tool orchestration plan
7. Tap `专项练习`.
8. Show that only stem/options are visible before submit.
9. Choose a wrong option and tap `提交答案`.
10. Show:
    - user answer
    - correct answer
    - explanation
    - source evidence
11. Tap `完成练习`, then open `复习计划`.
12. Show:
    - wrong book count
    - wrong question detail
    - review queue item
    - weak mastery state
13. Optional: return to the course page and tap `模拟考试` to show score, accuracy, weak knowledge points, evidence coverage, and Markdown report text availability.
14. Optional: tap `随机 3 题`, `随机 5 题`, or `随机 10 题` to show question sampling from the same generated/imported pool.
15. Optional: import a Markdown multi-choice question with `Answer: A,B`, choose only one correct option, and show `PARTIAL` grading path in tests / diagnostics.
16. Optional: import a PDF file, show `PDF document ... PDF_TEXT_PARSER_PENDING` and `PDF page ... PAGE_OCR_SEAM_READY`, then paste manual page text and generate the same L3 pipeline.
17. Optional: import DOCX/PPTX/XLSX demo files and show best-effort extraction or template-required errors.
18. Optional: create a classroom recording artifact, show `OFFICIAL_ASR_CONFIG_MISSING` or `HARD_BLOCKED_MISSING_SCHEMA`, paste manual transcript fallback, then show transcript timeline + evidence chain.
19. Optional: request listen-review and show `LOCAL_TTS_AVAILABLE` when Android local TTS fallback is prepared.
20. Optional: request translation and show `OFFICIAL_TRANSLATION_NOT_CONFIGURED` when official translation is not configured.
21. Optional: show diagnostics for Translation / TTS / Function Calling / Edge model without claiming official success.

## Honest Notes

- OCR / Query Rewrite / Text Similarity / Embedding have passed provider smoke, but this demo does not run network smoke.
- ASR Long is not presented as completed. Recording produces an app-private audio artifact record; missing official upload/poll/result schema is shown as `HARD_BLOCKED_MISSING_SCHEMA` when applicable; manual transcript fallback remains available.
- Word/Excel/PPTX are best-effort. Use Markdown/CSV for stable question-bank demo, and use DOCX/XLSX/PPTX as best-effort import demos.
- `回忆复盘 / 自评复习` is separate from `专项练习`; do not demo self-report buttons as real practice.
- PDF remains artifact/page OCR seam/manual fallback unless OCR text is supplied.
- Translation / official TTS / official Function Calling / official ASR Long must be shown as `SEAM_ONLY`, `NOT_CONFIGURED`, `LOCAL_ORCHESTRATOR`, `LOCAL_TTS_AVAILABLE`, `HARD_BLOCKED`, or `OFFICIAL_TTS_NOT_CONFIGURED` unless a later validation actually proves them.
