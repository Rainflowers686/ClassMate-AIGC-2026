# Claude Global Red-team Checklist

Date: 2026-06-20

## Function Authenticity

- Verify text, Markdown, CSV, DOCX, XLSX, PPTX, PDF artifact/manual text, image OCR seam, audio artifact, and manual transcript paths.
- Verify Practice default is real quiz, not self-assessment.
- Verify wrong answers update wrong book, mastery, review queue, and trend history.
- Verify exam report includes score, accuracy, weak points, evidence coverage, and recommendations.

## Official Tool Authenticity

- OCR / Query Rewrite / Embedding / Text Similarity provider smoke status remains PASS in docs.
- No real network smoke is run in this checklist.
- ASR Long official path must show HARD_BLOCKED when upload/poll/result schema is missing.
- Translation and official TTS must not claim success when not configured.
- Function Calling must identify local orchestrator vs official path.

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

## Demo Readiness

- Run the demo script once without network smoke.
- Capture failures with exact status labels, not vague pass/fail summaries.
