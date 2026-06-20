# L3 Pipeline Demo Script

Date: 2026-06-20

Target length: 2-3 minutes.

## Final Route

| Step | Operation | Expected screen | Behind capability | Risk / fallback |
| --- | --- | --- | --- | --- |
| 1 | Run `scripts\qa\demo_device_provision.ps1`. | GO/NO-GO list for config presence, model directory, storage, audio, camera, app install, demo data. | Demo readiness guard. | If NO-GO, use local/manual fallback route and do not claim official runtime success. |
| 2 | Open Home and tap `整理一份新资料`. | Input Superhub / import entry. | Stable Home CTA. | Do not modify or reframe Home during demo. |
| 3 | Paste short Markdown classroom material. | Material confirmation. | Text paste -> L3 pipeline. | Use text/Markdown as the stable route; avoid complex PDF first. |
| 4 | Generate learning package. | Summary, knowledge map, evidence, generated questions, review queue. | L3 pipeline, tool plan, semantic index. | If cloud model unavailable, show explicit fallback status. |
| 5 | Ask an evidence-grounded question. | Answer with evidence or "not found in this lesson". | Evidence retrieval; Query Rewrite official-first path if configured. | If Query Rewrite fallback is used, say local planning fallback, not official success. |
| 6 | Show official runtime diagnostics. | OCR used/ready; QR/Embedding/Similarity ready/used/fallback with exact blocker. | v1.7 production official retrieval injection. | Only say `OFFICIAL_RUNTIME_USED` when diagnostics prove successful runtime output. |
| 7 | Show official OCR single-point demo. | OCR text enters LessonSource/Evidence or fallback status appears. | OCR official app-level path, config-gated. | If config missing/fails, show manual OCR text fallback. |
| 8 | Open `专项练习`. | Stem/options only before submit. | Real Practice Engine. | Do not use self-assessment buttons as practice. |
| 9 | Choose a wrong answer and submit. | User answer, correct answer, explanation, evidence. | Grading, explanation, evidence chain. | If evidence missing, show explicit "no evidence" state. |
| 10 | Open Review / Wrong Book / Mastery. | Wrong record, review queue item, weak mastery state. | Wrong book, review queue, mastery history. | Restart validation should confirm persistence. |
| 11 | Open simulated exam report. | Score, accuracy, weak knowledge points, evidence coverage, report text. | ExamReport. | Keep as a controlled short exam. |
| 12 | Optional: edge fallback. | Edge available/used or unavailable/local-rule fallback. | Edge model fallback strategy. | Only demo if `/sdcard/1225` and permissions are GO. |
| 13 | Optional: audio artifact. | Recording artifact, ASR job status, manual transcript fallback, timeline/evidence. | ASR Long seam and transcript fallback. | Do not present automatic ASR unless live validation has been completed. |
| 14 | Optional: local TTS. | Summary/wrong explanation/review card playback or local unavailable status. | Android local TTS fallback. | Do not claim official TTS. |
| 15 | Close with diagnostics matrix. | Honest statuses for official/runtime/fallback/blocker. | Status matrix. | Leave validation-pending items visible. |

## Honest Notes

- OCR / Query Rewrite / Text Similarity / Embedding have passed provider smoke, but this demo does not run network smoke.
- OCR is the app-level official product path for image/photo/OCR text into LessonSource/Evidence. It remains config-gated and has manual OCR text fallback.
- Query Rewrite now runs through the v1.7 production official retrieval factory. Show `OFFICIAL_RUNTIME_USED` only if demo/cloud validation proves the injected Vivo adapter succeeded; otherwise show validation-pending, not-configured, or local fallback.
- Embedding now stores official/local vector provenance. Show official vectors only when the runtime adapter returns vectors; otherwise show `LOCAL_FALLBACK`.
- Text Similarity now stores score provenance. Show official rerank only when the runtime adapter returns scores; otherwise show local similarity fallback.
- ASR Long is not presented as completed. Recording produces an app-private audio artifact record; core VivoAsrProvider 1739 contract exists, but app upload/poll/result validation is pending; manual transcript fallback remains available.
- Word/Excel/PPTX are best-effort with quality guards. Use Markdown/CSV for stable question-bank demo, and use DOCX/XLSX/PPTX only as controlled import demos.
- `回忆复盘 / 自评复习` is separate from `专项练习`; do not demo self-report buttons as real practice.
- PDF remains artifact/page OCR seam/manual fallback unless OCR text is supplied.
- Translation / official TTS / official Function Calling / official ASR Long must be shown as `SEAM_ONLY`, `NOT_CONFIGURED`, `LOCAL_ORCHESTRATOR`, `LOCAL_TTS_AVAILABLE`, `OFFICIAL_APP_WIRING_PENDING`, `CORE_CONTRACT_PRESENT_APP_WIRING_PENDING`, or `OFFICIAL_TTS_NOT_CONFIGURED` unless a later validation actually proves the injected runtime adapter succeeded.

## Do Not Demo Live

- recording to automatic transcription
- complex PDF automatic text extraction
- official Query Rewrite / Embedding / Text Similarity / Translation / TTS / Function Calling runtime success unless a later cloud-device validation proves it
- on-device model behavior unless `ON_DEVICE_MODEL_PRESENT` and storage permission are both GO
