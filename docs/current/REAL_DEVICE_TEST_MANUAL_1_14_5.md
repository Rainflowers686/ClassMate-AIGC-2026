# Real Device Test Manual - 1.14.5 / 118

## A. OCR / Knowledge Extraction

1. Import an image containing both subject content and classroom prompt text such as "同学们注意" or "重点来了".
2. Confirm OCR text, manually correct if the low-quality warning appears, and generate the course.
3. Expected: knowledge framework and review plan focus on subject terms such as formulas, definitions, laws, algorithms, or concepts. Prompt words must not appear as knowledge points.

## B. Quiz Relevance

1. Open the generated micro quiz.
2. Submit an answer and view explanation.
3. Expected: each question names/uses a subject knowledge point, cites evidence, explains the correct answer, and gives rationales for wrong options.
4. If material is insufficient, the app should say it cannot generate reliable quiz items instead of inventing unrelated questions.

## C. Related Knowledge

1. Open course summary / related knowledge.
2. Expected: related knowledge starts from accepted course knowledge points and shows in-course evidence quotes.
3. It must not claim external API search as the source. Browser search remains only a browser intent.

## D. ASR Availability

1. On a device without system SpeechRecognizer service, open the import recording card and focus realtime-ASR panel.
2. Expected: recording still works; realtime ASR is marked unavailable; visible actions include opening speech settings, record-only fallback, or pasting transcript.
3. No fake transcript evidence should be created when ASR is unavailable.

## E. Fresh Install Appearance

1. Clear app data or install fresh.
2. Launch ClassMate.
3. Expected: default theme is "沉浸学习" and typography is "端正阅读".
4. Change theme/font, restart, and confirm the saved preference is not overwritten.

## F. Regression Commands

```powershell
git diff --check
.\gradlew.bat :core:test --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
powershell -ExecutionPolicy Bypass -File scripts\qa\current_preflight.ps1
powershell -ExecutionPolicy Bypass -File scripts\qa\cloud_device_precheck.ps1
```
