# Cloud Device Validation Plan

Date: 2026-06-20

## Order

1. Fresh install and launch.
2. Theme and Advanced Appearance smoke only; do not reopen UI polish scope unless broken.
3. Text paste -> L3 pipeline -> Course detail.
4. Real Practice wrong answer -> wrong book -> Review queue -> mastery trend.
5. Random quiz and exam report.
6. Markdown and CSV question bank import.
7. DOCX, XLSX, PPTX best-effort imports.
8. PDF artifact -> page status -> manual page text -> L3 pipeline.
9. Image/photo OCR seam and manual OCR fallback.
10. Recording artifact -> ASR job -> manual transcript fallback -> timeline/evidence.
11. Android local TTS fallback on summary, wrong-answer explanation, and review card.
12. Translation request not-configured behavior.
13. Tool orchestration diagnostics and semantic search.
14. Restart app and confirm history, theme, model config, and semantic index do not crash or leak sensitive data.

## Not Part Of This Pass

- Real provider network smoke.
- Official ASR Long upload/polling.
- Official TTS/Translation/Function Calling live calls.
- Full PDF native text parsing.

## Failure Reporting

Use these statuses: `COMPLETE`, `PARTIAL`, `LOCAL_FALLBACK`, `SEAM_ONLY`, `NOT_CONFIGURED`, `HARD_BLOCKED`, `TASK_5_FUTURE`.
