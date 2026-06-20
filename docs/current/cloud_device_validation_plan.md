# Cloud Device Validation Plan

Date: 2026-06-20

## Order

1. Run `scripts\qa\demo_device_provision.ps1` and record GO/NO-GO statuses.
2. Fresh install and launch.
3. Theme and Advanced Appearance smoke only; do not reopen UI polish scope unless broken.
4. Text paste -> L3 pipeline -> Course detail.
5. Real Practice wrong answer -> wrong book -> Review queue -> mastery trend.
6. Random quiz and exam report.
7. Markdown and CSV question bank import.
8. DOCX, XLSX, PPTX controlled imports with quality guard statuses.
9. PDF artifact -> page status -> manual page text -> L3 pipeline.
10. Image/photo OCR path and manual OCR fallback.
11. Recording artifact -> ASR job -> manual transcript fallback -> timeline/evidence.
12. Android local TTS fallback on summary, wrong-answer explanation, and review card.
13. Official runtime diagnostics: Query Rewrite / Embedding / Text Similarity should show used, app-wiring-pending, not-configured, or fallback with exact blocker.
14. Translation request not-configured behavior or official runtime success only if a validated adapter is provisioned.
15. Tool orchestration diagnostics and semantic search.
16. Restart app and confirm history, theme, model config, semantic index, wrong book, review queue, mastery history, and exam reports do not crash or leak sensitive data.

## Not Part Of This Pass

- Real provider network smoke.
- Official ASR Long upload/polling/result validation.
- Official TTS/Translation/Function Calling runtime success unless a validated adapter is provisioned.
- Full PDF native text parsing.
- Official Embedding or Text Similarity runtime success unless a validated adapter returns vectors/scores.

## Failure Reporting

Use these statuses: `COMPLETE`, `PARTIAL`, `OFFICIAL_RUNTIME_USED`, `OFFICIAL_RUNTIME_READY`, `OFFICIAL_APP_WIRING_PENDING`, `LOCAL_FALLBACK`, `SEAM_ONLY`, `NOT_CONFIGURED`, `CORE_CONTRACT_PRESENT_APP_WIRING_PENDING`, `HARD_BLOCKED`, `TASK_5_FUTURE`.
