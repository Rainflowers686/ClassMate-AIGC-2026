> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# Red-team P0 Fix v1.5

Date: 2026-06-20

> Superseded current-status note: v1.7 adds production official retrieval adapter injection through `OfficialRuntimeGatewayFactory.production()` for Query Rewrite, Embedding, and Text Similarity. v1.5's local/seam wording remains the historical red-team correction; current status is production-adapter-injected with local fallback unless the official runtime succeeds with real config.

## Scope

This pass fixes credibility and demo-readiness risks before cloud device validation. It does not add provider smoke coverage and does not run network smoke.

## Official Capability Wording

- OCR: official network smoke PASS and app-level image/photo/OCR-text path into LessonSource/Evidence. Config-gated, with fallback.
- Query Rewrite: official network smoke PASS, but app path is learning query planning/local fallback/seam.
- Embedding: official network smoke PASS, but app path is local persistent lexical semantic index plus embedding record seam.
- Text Similarity: official network smoke PASS, but app path is local similarity fallback/seam.
- Translation / official TTS / Function Calling: seam-only, local fallback, or not configured.
- ASR Long: core VivoAsrProvider doc 1739 contract exists; app wiring and non-sensitive audio validation are pending.

## Demo Readiness

`scripts\qa\demo_device_provision.ps1` checks device/app/config/model/permission/demo-data presence and prints GO/NO-GO only. It does not read config contents, print credentials, or call provider network smoke.

## ASR Long

- Core status: PRESENT.
- App status: PARTIAL / app wiring pending.
- Demo status: recording artifact + ASR job seam + manual transcript fallback.
- Fallback status: COMPLETE for L3 manual transcript entry.

## Import Quality

DOCX/XLSX/PPTX extraction now runs `ExtractedTextQuality` and records quality in `ImportReport`. PDF remains parser pending with page OCR/manual text fallback.

## Permissions

Bluetooth permissions were removed because there is no real Bluetooth device feature. `MANAGE_EXTERNAL_STORAGE` remains documented for `/sdcard/1225` model-directory access and user-selected local learning materials.

## Persistence

L3 critical data is saved to app-private `classmate_l3_store.json`: lesson source, evidence, questions, attempts, wrong book, review queue, mastery stats/history, and exam reports.

## Demo Route

Recommended live route:

1. Provision device and record GO/NO-GO.
2. Text/Markdown classroom material -> L3 learning package.
3. Evidence-grounded Ask.
4. Real practice wrong answer -> explanation/evidence -> wrong book/review queue/mastery.
5. Image OCR single-point demo.
6. On-device model only when model directory and storage permission are GO.
7. Audio only as artifact + manual transcript fallback.
8. Office/PDF only as controlled best-effort/fallback demos.
9. Diagnostics matrix with honest states.
