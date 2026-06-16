# Full P0-P2 End-to-End Device Acceptance Run

## 1. Date / Branch / Commit

| Field | Value |
| --- | --- |
| Date | 2026-06-16 Asia/Shanghai |
| Branch | `feature/product-review-compatible` |
| Commit | `92ac170 docs: record P2 AI learning experience acceptance run` |
| Scope | P0-P2 full end-to-end device acceptance QA |
| Result | Command-level acceptance passed; device execution blocked by no connected device |

## 2. Command Verification Table

| Command | Result | Blocker | Notes |
| --- | --- | --- | --- |
| `git status --short` | PASS | No | Working tree was clean before this acceptance document was created. |
| `git diff --check` | PASS | No | No whitespace errors. |
| `scripts\qa\current_preflight.ps1 -Quick` | PASS | No | 15 total: 13 passed, 0 failed, 2 skipped. |
| `.\gradlew.bat :core:test` | PASS | No | Build successful. |
| `.\gradlew.bat :app:testDebugUnitTest` | PASS | No | Build successful. |
| `.\gradlew.bat :app:assembleDebug` | PASS | No | Debug APK assembled successfully. |
| `scripts\qa\current_preflight.ps1` | PASS | No | 16 total: 16 passed, 0 failed, 0 skipped. |

Security notes from preflight:

- `config.local.json` presence was checked only by the preflight script; content was not read.
- `app/libs/llm-sdk-release.aar` is gitignored and not tracked.
- No direct SDK import was detected.
- qwen `enable_thinking=false` guard is intact.
- Secrets scan passed.

## 3. Device Info

| Item | Result |
| --- | --- |
| `adb devices` | No connected device rows returned. |
| Device model | Not available. |
| Android version | Not available. |
| Device execution status | Device not connected. |

Because no device was connected, the device-specific acceptance steps below were not executed. No install, launch, screenshot, or manual tap-path result is claimed in this document.

## 4. Installation Result

| Step | Result | Notes |
| --- | --- | --- |
| Force portrait | NOT EXECUTED | Requires connected device. |
| Install APK | NOT EXECUTED | Requires connected device. |
| Launch App | NOT EXECUTED | Requires connected device. |
| Crash check | NOT EXECUTED | Requires connected device. |

APK available for later device test:

- `app\build\outputs\apk\debug\app-debug.apk`

## 5. Text Import Path

| Step | Result | Notes |
| --- | --- | --- |
| Open Import | NOT EXECUTED | Device not connected. |
| Paste classroom text | NOT EXECUTED | Device not connected. |
| Trigger analysis | NOT EXECUTED | Device not connected. |
| Observe processing state | NOT EXECUTED | Device not connected. |
| Verify Course Detail | NOT EXECUTED | Device not connected. |
| Verify knowledge map / evidence / source label | NOT EXECUTED | Device not connected. |
| Verify Ask / Practice / Export entries | NOT EXECUTED | Device not connected. |

Expected screenshots when device is available:

- `01_text_import.png`
- `02_analysis_processing.png`
- `03_course_detail_from_text.png`

## 6. Image Path

| Step | Result | Notes |
| --- | --- | --- |
| Open image/photo entry | NOT EXECUTED | Device not connected. |
| Select image or take photo | NOT EXECUTED | Device not connected. |
| Observe OCR / on-device draft state | NOT EXECUTED | Device not connected. |
| Confirm editable draft | NOT EXECUTED | Device not connected. |
| Verify CourseAnalysis and Course Detail | NOT EXECUTED | Device not connected. |

Expected screenshots when device is available:

- `04_image_entry.png`
- `05_image_draft_ocr_or_ondevice.png`
- `06_course_detail_from_image.png`

Required manual wording check:

- No multimodal-vs-OCR overclaim.
- No OCR-complete overclaim.

## 7. Transcript Path

| Step | Result | Notes |
| --- | --- | --- |
| Open audio/transcript entry | NOT EXECUTED | Device not connected. |
| Confirm manual transcript fallback when ASR is not configured | NOT EXECUTED | Device not connected. |
| Paste transcript text | NOT EXECUTED | Device not connected. |
| Confirm TranscriptDraft | NOT EXECUTED | Device not connected. |
| Verify CourseAnalysis and Course Detail | NOT EXECUTED | Device not connected. |

Expected screenshots when device is available:

- `07_transcript_entry.png`
- `08_manual_transcript_fallback.png`
- `09_course_detail_from_transcript.png`

Required manual wording check:

- No real-time ASR-complete overclaim.
- No automatic lecture-listening overclaim.
- Manual transcript fallback must not be described as provider ASR.

## 8. Ask Path

| Step | Result | Notes |
| --- | --- | --- |
| Open Ask from Course Detail | NOT EXECUTED | Device not connected. |
| Ask evidence-grounded question | NOT EXECUTED | Device not connected. |
| Verify answer / source / evidence snippets | NOT EXECUTED | Device not connected. |
| Verify suggested follow-ups | NOT EXECUTED | Device not connected. |
| Verify add-to-review action | NOT EXECUTED | Device not connected. |
| Ask insufficient-evidence question | NOT EXECUTED | Device not connected. |

Expected screenshots when device is available:

- `10_ask_answer_evidence.png`
- `11_ask_followups.png`
- `12_ask_no_evidence.png`

## 9. Practice / Weakness / Review Path

| Step | Result | Notes |
| --- | --- | --- |
| Enter Practice from course | NOT EXECUTED | Device not connected. |
| Generate practice questions | NOT EXECUTED | Device not connected. |
| Answer one item | NOT EXECUTED | Device not connected. |
| Verify feedback with answer, explanation, evidence, next action | NOT EXECUTED | Device not connected. |
| Intentionally answer one item wrong | NOT EXECUTED | Device not connected. |
| Verify weakness item | NOT EXECUTED | Device not connected. |
| Verify review priority and estimated minutes | NOT EXECUTED | Device not connected. |
| Open Review page and verify today tasks | NOT EXECUTED | Device not connected. |

Expected screenshots when device is available:

- `13_practice_question.png`
- `14_practice_feedback.png`
- `15_weakness_item.png`
- `16_review_priority.png`

## 10. Export Path

| Step | Result | Notes |
| --- | --- | --- |
| Open Export from Course Detail | NOT EXECUTED | Device not connected. |
| Generate learning report draft | NOT EXECUTED | Device not connected. |
| Verify processing state | NOT EXECUTED | Device not connected. |
| Verify format picker | NOT EXECUTED | Device not connected. |
| Generate HTML | NOT EXECUTED | Device not connected. |
| Generate DOCX | NOT EXECUTED | Device not connected. |
| Generate PDF | NOT EXECUTED | Device not connected. |
| Generate Audio Script | NOT EXECUTED | Device not connected. |
| Verify file can be opened or saved to visible path | NOT EXECUTED | Device not connected. |

Expected screenshots when device is available:

- `17_export_processing.png`
- `18_export_format_picker.png`
- `19_export_html_success.png`
- `20_export_docx_success.png`
- `21_export_pdf_success_or_failure.png`
- `22_audio_script_export.png`

Required manual wording check:

- No voice-clone wording.
- No specific-person voice simulation wording.
- DOCX should be described as an editable learning document.
- PDF should be described as a print-ready essence report.

## 11. Settings Path

| Step | Result | Notes |
| --- | --- | --- |
| Open Settings home | NOT EXECUTED | Device not connected. |
| Verify theme settings entry | NOT EXECUTED | Device not connected. |
| Verify model access entry | NOT EXECUTED | Device not connected. |
| Verify learning/export entry | NOT EXECUTED | Device not connected. |
| Verify developer options entry | NOT EXECUTED | Device not connected. |
| Verify model access statuses | NOT EXECUTED | Device not connected. |
| Verify learning/export defaults | NOT EXECUTED | Device not connected. |
| Verify no key content is visible | NOT EXECUTED | Device not connected. |

Expected screenshots when device is available:

- `23_settings_home.png`
- `24_settings_model_access.png`
- `25_settings_learning_export.png`
- `26_settings_developer_options.png`

## 12. Screenshots Table

| Screenshot | Status | Path |
| --- | --- | --- |
| 01-26 full E2E screenshot set | NOT CAPTURED | Device not connected. |

Screenshot output directory for a future run:

- `.codex_work/screens/full_e2e_acceptance/`

No screenshot files were created in this run.

## 13. Blockers

| Severity | Count | Items |
| --- | ---: | --- |
| P0 Blocker | 0 | None found in command-level acceptance. Device acceptance was not executed, so no device-specific product blocker can be confirmed. |

## 14. Warnings

| Severity | Count | Items |
| --- | ---: | --- |
| P1 Warning | 1 | Device not connected, so install/launch/full tap-path acceptance remains unverified. |

## 15. Polish

| Severity | Count | Items |
| --- | ---: | --- |
| P2 Polish | 1 | Future screenshot proof should evaluate visual hierarchy, wording smoothness, and export flow comfort on real screen size. |

## 16. Required Fixes

No code fixes are required from command-level acceptance.

Required next QA action:

1. Connect a device or cloud device.
2. Install `app-debug.apk`.
3. Execute the full path checklist above.
4. Save screenshots under `.codex_work/screens/full_e2e_acceptance/`.
5. Update this document with real device results.

## 17. Recommended Next Step

Run a second acceptance pass with a connected device. Treat any crash, broken CourseAnalysis entry, broken export, key exposure, or severe forbidden-copy issue as a P0 blocker. Treat fallback-only but usable paths as P1 warnings, and visual/wording quality as P2 polish.

