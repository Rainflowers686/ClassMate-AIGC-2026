# Claude v3 Review Handoff

Date: 2026-06-20

## Repo State Placeholder

- Branch: `feature/product-review-compatible`
- Commit: fill with `git rev-parse HEAD` during handoff.
- Scope: post-v1.7 final documentation/status sync before cloud-device validation.

## Current Real Status

- L3 learning pipeline, real Practice, Review/Wrong Book/Mastery, Input Superhub, persistence, and diagnostics are installed.
- OCR has smoke PASS and an app-level official path for image/photo/OCR text into LessonSource/Evidence, gated by config.
- Query Rewrite, Embedding, and Text Similarity have smoke PASS and v1.7 official-first production adapter injection. Live `OFFICIAL_RUNTIME_USED` still requires demo/cloud-device validation.
- ASR Long has core Vivo 1739 contract present; app live upload/poll/result validation is pending. Demo route is recording artifact + ASR job seam + manual transcript fallback.
- Translation and official TTS remain not configured/seam unless future validation proves otherwise. Android local TTS fallback is available.
- Function Calling is local orchestrator active; official Function Calling is not claimed complete.
- Edge model fallback strategy is wired, but device availability depends on `/sdcard/1225` and permissions.

## Official Runtime Matrix

| Capability | Runtime claim allowed now | What Claude should challenge |
| --- | --- | --- |
| OCR | COMPLETE / CONFIG_GATED | Verify fallback and provenance; do not require network smoke in review. |
| Query Rewrite | OFFICIAL_RUNTIME_READY / VALIDATION_PENDING | Verify factory injection and no overclaim of runtime used before device validation. |
| Embedding | OFFICIAL_RUNTIME_READY / VALIDATION_PENDING | Verify `officialVector` support and local fallback persistence. |
| Text Similarity | OFFICIAL_RUNTIME_READY / VALIDATION_PENDING | Verify score provenance and local fallback. |
| ASR Long | CORE_CONTRACT_PRESENT / APP_VALIDATION_PENDING | Verify no automatic transcription claim. |
| Translation | SEAM_ONLY / NOT_CONFIGURED | Verify no fake translated text. |
| Official TTS | OFFICIAL_NOT_CONFIGURED / LOCAL_TTS_AVAILABLE | Verify no official TTS or voice clone claim. |
| Function Calling | LOCAL_ORCHESTRATOR_ACTIVE / OFFICIAL_NOT_CONFIGURED | Verify local vs official wording. |
| Edge model | DEVICE_VALIDATION_PENDING | Verify `/sdcard/1225` dependency is not generalized to all devices. |

## Review Questions For Claude

1. GO / NO-GO for cloud-device validation.
2. P0/P1/P2 findings with file references.
3. Whether official runtime injection is reliable enough for validation.
4. Whether any README/docs text still overclaims official capability completion.
5. Whether the demo route is stable and honest.
6. Whether competitor comparisons overstate lecture ASR, official retrieval, or practice depth.
7. Whether any security wording or scripts risk printing secrets.
8. Final docs edits or Codex patch requests if needed.

## Required Claude Output

- GO / NO-GO for cloud-device validation.
- P0 / P1 / P2 issue list.
- Final recommended demo route.
- Final docs edits.
- Remaining Codex patch list, if any.
