# L3 Pipeline Demo Script

Date: 2026-06-20

Target length: 2-3 minutes.

## Script

1. Open ClassMate Home.
2. Tap `整理一份新资料`.
3. Before recording, run `scripts\qa\demo_device_provision.ps1` and keep the result as GO/NO-GO evidence. If it is NO-GO, use the fallback route below.
4. Open `L3 演示包` or paste a short classroom material.
5. In the material tray, confirm the text and optional question bank.
6. Tap `生成 L3 本地学习闭环`.
7. On the course page, show:
   - summary
   - knowledge map
   - L3 学习闭环 card
   - honest provider chips: OCR app-level path, Query Rewrite local planning seam, Embedding local lexical index, Text Similarity local similarity fallback
   - L3 能力诊断
   - transcript timeline when using manual transcript fallback
   - semantic index record/search status and local tool orchestration plan
8. Tap `专项练习`.
9. Show that only stem/options are visible before submit.
10. Choose a wrong option and tap `提交答案`.
11. Show:
    - user answer
    - correct answer
    - explanation
    - source evidence
12. Tap `完成练习`, then open `复习计划`.
13. Show:
    - wrong book count
    - wrong question detail
    - review queue item
    - weak mastery state
14. Optional: return to the course page and tap `模拟考试` to show score, accuracy, weak knowledge points, evidence coverage, and Markdown report text availability.
15. Optional: tap `随机 3 题`, `随机 5 题`, or `随机 10 题` to show question sampling from the same generated/imported pool.
16. Optional: import a Markdown multi-choice question with `Answer: A,B`, choose only one correct option, and show `PARTIAL` grading path in tests / diagnostics.
17. Optional: import a PDF file, show `PDF document ... PDF_TEXT_PARSER_PENDING` and `PDF page ... PAGE_OCR_SEAM_READY`, then paste manual page text and generate the same L3 pipeline.
18. Optional: import DOCX/PPTX/XLSX demo files and show extraction quality: `COMPLETE`, `PARTIAL`, `TEMPLATE_REQUIRED`, or `EMPTY_FILE`.
19. Optional: create a classroom recording artifact, show `OFFICIAL_ASR_CONFIG_MISSING` or `CORE_CONTRACT_PRESENT_APP_WIRING_PENDING`, paste manual transcript fallback, then show transcript timeline + evidence chain.
20. Optional: request listen-review and show `LOCAL_TTS_AVAILABLE` when Android local TTS fallback is prepared.
21. Optional: request translation and show `OFFICIAL_TRANSLATION_NOT_CONFIGURED` when official translation is not configured.
22. Optional: show diagnostics for Translation / TTS / Function Calling / Edge model without claiming official success.

## Honest Notes

- OCR / Query Rewrite / Text Similarity / Embedding have passed provider smoke, but this demo does not run network smoke and does not claim live official calls for Query Rewrite, Embedding, or Text Similarity.
- OCR is the app-level official product path for image/photo/OCR text into LessonSource/Evidence. It remains config-gated and has manual OCR text fallback.
- Query Rewrite is shown as learning query planning/local fallback seam, not a live official Query Rewrite call.
- Embedding is shown as local persistent lexical semantic index + embedding record seam, not live official vectors.
- Text Similarity is shown as local similarity fallback/seam, not live official rerank.
- ASR Long is not presented as completed. Recording produces an app-private audio artifact record; core VivoAsrProvider 1739 contract exists, but app upload/poll/result validation is pending; manual transcript fallback remains available.
- Word/Excel/PPTX are best-effort with quality guards. Use Markdown/CSV for stable question-bank demo, and use DOCX/XLSX/PPTX only as controlled import demos.
- `回忆复盘 / 自评复习` is separate from `专项练习`; do not demo self-report buttons as real practice.
- PDF remains artifact/page OCR seam/manual fallback unless OCR text is supplied.
- Translation / official TTS / official Function Calling / official ASR Long must be shown as `SEAM_ONLY`, `NOT_CONFIGURED`, `LOCAL_ORCHESTRATOR`, `LOCAL_TTS_AVAILABLE`, `CORE_CONTRACT_PRESENT_APP_WIRING_PENDING`, or `OFFICIAL_TTS_NOT_CONFIGURED` unless a later validation actually proves them.

## Do Not Demo Live

- recording to automatic transcription
- complex PDF automatic text extraction
- official Embedding / Text Similarity / Translation / TTS / Function Calling live calls
- on-device model behavior unless `ON_DEVICE_MODEL_PRESENT` and storage permission are both GO
