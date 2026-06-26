# ClassMate Cloud Real Device Test Plan v1

## 中文摘要（当前真实状态）

云真机抽测优先顺序：①预置凭据 / 模型 / 权限后，确认一次云端分析来源 = 云端蓝心；②主链文本闭环（知识点 + 证据回溯 + 无依据不编造的 Ask）；③练习闭环（微测 → 错题 → 复习 → 掌握度 → 学习诊断）；④OCR 真演示；⑤断网兜底（端侧 / 本地基础整理 / 安全占位 诚实分级）；⑥Study Pack 导出（脱敏 + A4 + 8 段核对）；⑦音频 / 方言放最后，只讲手动 / 导入转写。需真机抽测的能力：大模型、通用 OCR、长语音转写、端侧 3B、端侧能力文件。不写所有能力已真机跑通。

---

Status: READY_FOR_CLOUD_DEVICE_VALIDATION

This plan validates the current Android App learning loop on a real/cloud device. It does not require the BlueLM low-code canvas and does not require running provider network smoke during validation.

## Goals

- Confirm that normal users can complete multimodal learning loops inside the Android App.
- Confirm EvidenceDetail can trace back TEXT, OCR_IMAGE, DOCUMENT, and AUDIO evidence.
- Confirm Practice, WrongBook, ReviewPlan, and LearningDiagnosis update after real answers.
- Capture failures with enough screenshots/logs to reproduce code-level issues.

## Device Requirements

- Android device or cloud device with the debug APK installed.
- Camera permission available for photo/OCR path.
- Microphone permission available for recording path.
- File picker access for TXT/Markdown/PDF path.
- Network available if cloud BlueLM or official OCR validation is desired.
- Optional: `/sdcard/1225` model directory for on-device fallback validation.

## Pre-Test Commands

Run from repo root:

```powershell
git diff --check
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
scripts\qa\current_preflight.ps1
scripts\qa\cloud_device_precheck.ps1
```

The precheck only reports whether `config.local.json` exists. It must not read the file content and must not run provider network smoke.

Debug APK:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## GO/NO-GO Before Recording

GO only when:

- `scripts\qa\cloud_device_precheck.ps1` passes.
- debug APK exists at `app\build\outputs\apk\debug\app-debug.apk`.
- the App launches on the cloud device.
- required permissions for the selected path are granted.
- test assets from `test_assets_manifest_v1.md` are available on the device.

NO-GO when:

- build or unit tests fail.
- forbidden files are tracked.
- camera/microphone/file picker permission blocks the path under test.
- EvidenceDetail crashes instead of showing fallback.
- the demo would require claiming automatic ASR or PDF full parsing as complete.

## Permissions

Before recording final proof video, grant or verify:

- CAMERA
- RECORD_AUDIO
- file picker access through Android document picker
- optional all-files access only when validating the preloaded on-device model directory

## Required Test Assets

Use `docs/current/test_assets_manifest_v1.md`.

## Path 1: Text / Markdown Learning Loop

Steps:

1. Open ClassMate.
2. Tap the main import/learning CTA.
3. Paste the classroom text from the asset manifest.
4. Generate the learning loop.
5. Open Course Detail.
6. Verify summary, knowledge points, evidence count, quiz count, and review queue count.
7. Start Practice.
8. Pick a wrong answer and submit.
9. Open Review.
10. Open WrongBook entry, view mistake reason/remediation, then view evidence.

Expected:

- Course Detail shows L3 learning loop stats.
- Practice does not reveal answers before submit.
- Wrong answer creates WrongBook, WEAK mastery, ReviewQueue update, and LearningDiagnosis.
- EvidenceDetail displays TEXT evidence and does not crash.

Record:

- paste and generate
- submitted wrong answer
- WrongBook detail
- EvidenceDetail

## Path 2: Image / OCR Learning Loop

Steps:

1. Open Import.
2. Choose image/photo OCR entry.
3. Capture or select a lesson image.
4. Confirm OCR draft text or paste recognized text if OCR is unavailable.
5. Confirm image draft.
6. Open Course Detail and generate/verify learning loop.
7. Start Practice, answer wrong, open WrongBook/ReviewPlan evidence.

Expected:

- UI states that image was saved as an evidence asset.
- OCR draft text is visible before confirmation.
- EvidenceDetail for OCR_IMAGE attempts to render thumbnail/original image.
- If thumbnail cannot load, it shows a clear fallback instead of crashing.
- Quiz/WrongBook/ReviewPlan can trace back to image evidence.

Record:

- image evidence asset label
- OCR draft text
- EvidenceDetail image preview or fallback

## Path 3: Document / PDF Page Text Learning Loop

Steps:

1. Open Import.
2. Import TXT or Markdown asset.
3. Verify it enters the learning loop and creates DOCUMENT/TEXT evidence as appropriate.
4. Import a PDF sample.
5. Confirm PDF parser pending state.
6. Use the visible PDF page text entry to paste page number text.
7. Add page text and generate learning loop.
8. Open ReviewPlan and view document evidence.

Expected:

- TXT/Markdown enters the pipeline directly.
- PDF does not claim full native parsing.
- PDF page text creates DOCUMENT evidence with file/page hint/snippet.
- ReviewPlan can open document EvidenceDetail.

Record:

- PDF parser pending status
- page text entry
- Document EvidenceDetail

## Path 4: Recording / Transcript Learning Loop

Steps:

1. Open Import.
2. Start and stop classroom recording if device permission is granted.
3. Verify an audio material card or ASR job is visible.
4. Paste transcript text.
5. Confirm transcript and generate learning loop.
6. Open Course Detail timeline.
7. Start Practice or ReviewPlan and open AUDIO evidence.

Expected:

- Recording creates an audio artifact record.
- If official ASR is unavailable, UI keeps manual transcript fallback visible.
- Transcript generates timeline segments, knowledge points, questions, review queue, and AUDIO evidence.
- EvidenceDetail shows recording name/time range/transcript segment.
- If seek playback is not validated, it says playback定位待真机验证.

Record:

- recording artifact
- transcript confirmation
- timeline
- AUDIO EvidenceDetail

## Path 5: WrongBook Retry Loop

Steps:

1. Create a wrong answer from Practice.
2. Open Review.
3. Find WrongBook.
4. Verify question, user answer, correct answer, mistake reason, remediation hint, related knowledge, and evidence.
5. Tap retry this question.
6. Answer correctly and submit.
7. Return to Review.

Expected:

- retryCount increments.
- Mastery moves out of WEAK where rules allow.
- ReviewQueue priority is reduced or next review is adjusted.
- WrongBook history remains; it is not silently deleted.

Record:

- WrongBook detail before retry
- retry question screen
- post-retry ReviewPlan status

## Path 6: LearningDiagnosis Loop

Steps:

1. Answer at least one question wrong.
2. Open Review.
3. Confirm LearningDiagnosis section.
4. Open Course Detail and confirm diagnosis summary.
5. Tap diagnosis evidence if present.

Expected:

- Diagnosis shows weak knowledge points, common mistake type, review pressure, mastered points when available, and next study tasks.
- Diagnosis conclusions are tied to evidence or knowledge point.
- If evidence is missing, diagnosis degrades gracefully.

Record:

- Review diagnosis card
- Course Detail diagnosis summary
- evidence trace from diagnosis

## Path 7: Study Pack Export

Steps:

1. Complete a text or OCR learning loop.
2. Open Course Detail.
3. Confirm the learning status overview shows material, knowledge point, quiz, wrong book, review, and evidence counts.
4. Tap Generate study pack / export learning material.
5. Choose PDF first.
6. If PDF save/share fails, retry with Word-compatible HTML or Markdown.
7. Open the generated artifact where the device allows.

Expected:

- Export content comes from the L3 snapshot, not raw input text only.
- Study pack includes summary, knowledge points, micro quiz answers, wrong-book section when present, 20-minute review plan, diagnosis, evidence source index, and low-confidence notes.
- Export does not expose keys, config file contents, request bodies, or internal provider/smoke wording.
- Word-compatible HTML is available as a fallback if DOCX/PDF handling is blocked by the device.

Record:

- Course Detail learning status overview
- export format selector
- generated PDF or Word-compatible HTML result
- fallback message if one format fails

## Failure Capture

For every failure, capture:

- screen recording from 10 seconds before failure
- screenshot of current page
- exact input type and file name
- App screen name
- whether permission was denied
- `adb logcat` excerpt if available, excluding secrets or raw config

Suggested local commands:

```powershell
adb logcat -c
adb logcat -d > cloud-device-logcat.txt
```

Do not paste keys, Authorization headers, or config file contents into issues.

## Pass / Fail Criteria

PASS:

- All six paths are reachable through visible App controls.
- At least one TEXT, OCR_IMAGE, DOCUMENT, and AUDIO evidence detail path opens without crashing.
- WrongBook retry changes L3 state.
- ReviewPlan and LearningDiagnosis show evidence-backed next actions.
- Study pack export can produce at least PDF or Word-compatible HTML/Markdown from the current learning loop.

FAIL:

- Any core path is only reachable by tests/ViewModel and not from UI.
- EvidenceDetail crashes or blocks learning loop.
- WrongBook/ReviewQueue/Mastery do not update after answer submission.
- UI claims automatic ASR/PDF full parsing when only fallback is available.
- Export content is only raw input text or contains secrets/internal provider diagnostics.
