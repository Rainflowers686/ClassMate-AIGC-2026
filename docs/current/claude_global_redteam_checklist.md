# Claude Global Red-team Checklist

Date: 2026-06-20

## Function Authenticity

- Verify text, Markdown, CSV, DOCX, XLSX, PPTX, PDF artifact/manual text, image OCR seam, audio artifact, and manual transcript paths.
- Verify Practice default is real quiz, not self-assessment.
- Verify wrong answers update wrong book, mastery, review queue, and trend history.
- Verify exam report includes score, accuracy, weak points, evidence coverage, and recommendations.

## Official Tool Authenticity

- OCR / Query Rewrite / Embedding / Text Similarity provider smoke status remains PASS in docs.
- OCR may be described as app-level productized for image/photo/OCR text into evidence, with config-gated fallback.
- Query Rewrite / Embedding / Text Similarity must be described with v1.7 production runtime injection: `OFFICIAL_RUNTIME_READY / VALIDATION_PENDING` until a demo/cloud device proves `OFFICIAL_RUNTIME_USED`; otherwise not-configured or local fallback.
- No real network smoke is run in this checklist.
- ASR Long official path must show core contract present but app wiring/validation pending unless a later non-sensitive audio validation proves the path.
- Translation and official TTS must not claim success when not configured.
- Function Calling must identify local orchestrator vs official path.
- Diagnostics must show official runtime configured, official runtime used, fallback used, and exact blocker for OCR, Query Rewrite, Embedding, Text Similarity, ASR Long, Translation, TTS, Function Calling, and Edge model.

## Competitor Gap Review

- Lecture mode: recording artifact, ASR job lifecycle, manual transcript, timeline, evidence, learning package.
- Practice engine: real answer, multi-choice grading, short-answer self-assessment seam, exam report, wrong book.
- Review engine: daily card, weak points, mastery stats, trends, semantic related items.

## Security

- Do not read or print `config.local.json`.
- Do not display AppKey, Authorization, full endpoint, raw provider request body, APK/AAB/AAR, font files, or local key files.
- Confirm `.codex_work` is not tracked.

## UI and Text Risk

- Check chip/button/bottom-nav labels for overflow.
- Check Course detail diagnostics are understandable and not overly technical for normal users.
- Check dark theme readability remains acceptable.

## Device Risk

- Validate Android local TTS availability per device.
- Validate recording permission flow.
- Validate file picker MIME behavior across PDF/DOCX/XLSX/PPTX/audio/image.
- Validate app-private persistence after process restart.
- Validate `scripts\qa\demo_device_provision.ps1` GO/NO-GO before any cloud device recording.

## Demo Readiness

- Run the demo script once without network smoke.
- Capture failures with exact status labels, not vague pass/fail summaries.
- Produce GO / NO-GO for cloud-device validation and list P0/P1/P2 issues for the next Codex patch if needed.
