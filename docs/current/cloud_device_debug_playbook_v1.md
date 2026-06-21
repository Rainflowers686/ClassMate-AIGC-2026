# ClassMate Cloud Device Debug Playbook v1

Use this playbook when a cloud/real-device test fails. It is for diagnosis only; do not paste secrets, config file content, Authorization headers, app keys, or full provider endpoints into bug reports.

## First Checks

Run:

```powershell
scripts\qa\cloud_device_precheck.ps1
scripts\qa\demo_device_provision.ps1
```

Check:

- debug APK exists
- unit tests pass
- no forbidden tracked files
- `config.local.json` presence only
- app installed on device
- camera/microphone permissions
- optional model directory `/sdcard/1225`

## Image / OCR Fails

Symptoms:

- no OCR draft
- image evidence has no preview
- EvidenceDetail fallback appears

Check:

1. Did the user start from image/photo entry in Import?
2. Is CAMERA permission granted for photo capture?
3. Does the UI show `图片已保存为 evidence asset`?
4. Does OCR draft/manual recognized text exist before confirmation?
5. In EvidenceDetail, is the fallback `图片预览暂不可用，已保留 OCR 文本和资产引用。` shown?

Expected fallback:

- OCR text can still enter the learning loop.
- EvidenceDetail should not crash even if thumbnail decode fails.

Collect:

- screenshot of import image draft
- screenshot of EvidenceDetail
- `adb logcat` crash excerpt if any

## Document / PDF Import Fails

Symptoms:

- TXT/Markdown does not enter pipeline
- PDF appears unsupported
- document evidence cannot be opened

Check:

1. Confirm file type: TXT/MD/CSV/PDF/DOCX/PPTX/XLSX.
2. For PDF, confirm the UI says parser pending/manual page text path.
3. Paste page text through the PDF page text entry.
4. Confirm Course Detail shows L3 evidence/questions/review counts.
5. Open ReviewPlan and tap document evidence.

Expected fallback:

- PDF native text parsing is not presented as complete.
- manual page text should create DOCUMENT evidence with page hint/snippet.

Collect:

- file name and extension only
- import report status
- screenshot of PDF page text entry
- EvidenceDetail screenshot

## Recording / Transcript Fails

Symptoms:

- recording does not save
- ASR status stuck
- transcript does not generate timeline
- audio evidence does not open

Check:

1. RECORD_AUDIO permission is granted.
2. Recording artifact appears after stop.
3. If ASR is not configured, manual transcript fallback remains visible.
4. Paste transcript and confirm generation.
5. Course Detail shows transcript timeline.
6. EvidenceDetail for AUDIO shows recording file/time range/transcript segment.

Expected fallback:

- Manual transcript enters the same learning loop.
- If seek playback is not validated, the UI says playback定位待真机验证.

Collect:

- recording card screenshot
- ASR/manual transcript status
- timeline screenshot
- AUDIO EvidenceDetail screenshot

## Practice / WrongBook / Review Does Not Update

Symptoms:

- wrong answer does not create WrongBook
- mastery remains unchanged
- ReviewPlan does not show weak point
- retry does not affect state

Check:

1. Practice page is in REAL_QUIZ mode, not self-assessment.
2. Answer was submitted, not just selected.
3. WrongBook section appears in Review.
4. WrongBook record has mistake reason and remediation hint.
5. Retry this question opens a one-question REAL_QUIZ session.
6. Correct retry increments retryCount and updates mastery/review queue.

Expected:

- WrongBook history is retained.
- ReviewQueue priority changes according to mastery rules.
- LearningDiagnosis updates after wrong answer.

Collect:

- Practice before submit
- Practice after submit
- WrongBook detail
- ReviewPlan item after retry

## EvidenceDetail Cannot Open

Symptoms:

- clicking evidence shows toast
- EvidenceDetail empty
- crash when evidence asset missing

Check:

1. Does source item have evidenceId?
2. If review item lacks evidenceId, does knowledgePoint fallback find evidence?
3. Does EvidenceDetail show `证据资产缺失，但保留文本证据。` instead of crashing?
4. Confirm sourceType: TEXT, OCR_IMAGE, DOCUMENT, AUDIO, or WEB.

Expected:

- Missing asset is a graceful fallback.
- Text evidence remains visible even when binary asset is absent.

Collect:

- source card screenshot
- EvidenceDetail screenshot
- selected evidence id from diagnostic logs if available

## APK Install / Permission Failure

Check:

```powershell
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell pm list packages com.classmate.app
adb shell dumpsys package com.classmate.app
```

If cloud device cannot grant a permission:

- mark the path as blocked by device permission
- validate the fallback path where possible
- do not change the claim to say the unavailable path is complete

## Log Capture

Safe commands:

```powershell
adb logcat -c
adb logcat -d > cloud-device-logcat.txt
```

Do not include:

- config file content
- AppKey
- Authorization
- full provider endpoint
- private student material

## Escalation Template

Use this concise format:

```text
Path:
Device:
Build APK time:
Permission state:
Input type:
Observed:
Expected:
Fallback shown:
Screenshots/recording:
Log excerpt:
Secret/config content included: no
```
