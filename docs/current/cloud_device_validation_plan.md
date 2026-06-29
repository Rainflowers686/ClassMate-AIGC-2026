> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# Cloud Device Validation Plan

Date: 2026-06-20

## GO/NO-GO Gate

Run `scripts\qa\demo_device_provision.ps1` before recording or cloud-device validation. The script checks presence and permission status only; it must not read `config.local.json` content or run provider network smoke.

Required GO/NO-GO items:

- `APP_INSTALLED`
- `CLOUD_CONFIG_PRESENT` (presence only)
- `ON_DEVICE_MODEL_PRESENT` for edge-model fallback demos
- `STORAGE_PERMISSION_READY`
- `RECORD_AUDIO_READY`
- `CAMERA_READY`
- `L3_DEMO_DATA_READY`

If `CLOUD_CONFIG_PRESENT` is NO-GO, do not claim official runtime success for Query Rewrite, Embedding, Text Similarity, OCR, Translation, TTS, Function Calling, or ASR Long. Use local/manual fallback routes.

If `ON_DEVICE_MODEL_PRESENT` is NO-GO, do not demo edge-model fallback as available on that device.

## Validation Order

1. Run `scripts\qa\demo_device_provision.ps1` and record GO/NO-GO statuses.
2. Fresh install and launch.
3. Theme and Advanced Appearance smoke only; do not reopen UI polish scope unless broken.
4. Text paste -> L3 pipeline -> Course detail.
5. Official OCR image path and manual OCR fallback.
6. Official runtime diagnostics for Query Rewrite: expect `OFFICIAL_RUNTIME_USED` only if configured runtime succeeds; otherwise validation-pending/not-configured/fallback with exact blocker.
7. Official runtime diagnostics for Embedding: expect `vectorSource=OFFICIAL` only if configured runtime succeeds; otherwise local lexical fallback.
8. Official runtime diagnostics for Text Similarity: expect `scoreSource=OFFICIAL` only if configured runtime succeeds; otherwise local similarity fallback.
9. Real Practice wrong answer -> wrong book -> Review queue -> mastery trend.
10. Random quiz and exam report.
11. Markdown and CSV question bank import.
12. DOCX, XLSX, PPTX controlled imports with quality guard statuses.
13. PDF artifact -> page status -> manual page text -> L3 pipeline.
14. Recording artifact -> ASR job -> manual transcript fallback -> timeline/evidence.
15. Android local TTS fallback on summary, wrong-answer explanation, and review card.
16. Translation request not-configured behavior or official runtime success only if a validated adapter is provisioned.
17. Tool orchestration diagnostics and semantic search.
18. Restart app and confirm history, theme, model config, semantic index, wrong book, review queue, mastery history, and exam reports do not crash or leak sensitive data.

## Pass / Fail Criteria

| Area | Pass | Fail |
| --- | --- | --- |
| App launch | App opens without crash and diagnostics do not show secrets. | Crash, stuck splash, or secret-bearing diagnostics. |
| Advanced Appearance | Theme/accent/font sanity remains intact. | Appearance state reset or text overflow blocking normal use. |
| Text L3 pipeline | Summary, evidence, knowledge points, questions, review queue appear. | Silent failure or no recovery/fallback. |
| OCR | OCR evidence created when config succeeds, or explicit fallback shown when missing/failed. | Claims OCR success without evidence or fallback. |
| Query Rewrite | Runtime status is used/ready/not-configured/fallback with exact blocker. | Claims runtime used when no successful call occurred. |
| Embedding | Official vector or local vector is visible through provenance. | Missing vector record or fake official vector. |
| Text Similarity | Official or local score source is visible. | Similarity source missing or overstated. |
| Practice loop | Wrong answer creates explanation, evidence, wrong book, review item, mastery update. | Answer shown before submit or wrong-book loop breaks. |
| Recording/ASR | Recording artifact and manual transcript fallback work. | Automatic ASR is claimed without validation. |
| Local TTS | Android local TTS fallback is available or explicit unavailable status is shown. | Official TTS success is claimed without validation. |

## Not Part Of This Pass

- Real provider network smoke.
- Official ASR Long upload/polling/result validation.
- Official TTS/Translation/Function Calling runtime success unless a validated adapter is provisioned.
- Full PDF native text parsing.
- Official Query Rewrite / Embedding / Text Similarity runtime success unless the v1.7 production adapter is configured and the cloud/device run proves successful output.

## Failure Reporting

Use these statuses: `COMPLETE`, `PARTIAL`, `OFFICIAL_ADAPTER_INJECTED`, `OFFICIAL_RUNTIME_ATTEMPTED`, `OFFICIAL_RUNTIME_USED`, `OFFICIAL_RUNTIME_READY`, `OFFICIAL_APP_WIRING_PENDING`, `LOCAL_FALLBACK`, `SEAM_ONLY`, `NOT_CONFIGURED`, `CORE_CONTRACT_PRESENT_APP_WIRING_PENDING`, `HARD_BLOCKED`, `TASK_5_FUTURE`.
