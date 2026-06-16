# P1 Learning Loop and Export Acceptance Run

## Run Metadata

| Field | Value |
| --- | --- |
| Date | 2026-06-16 08:33:55 +08:00 |
| Branch | feature/product-review-compatible |
| Commit | a5da7cf |
| Commit title | feat(learning): add P1 learning loop and refined exports |
| Scope | P1 learning loop, StudyReport export, DOCX export, AI-refined export flow |
| Device/manual execution | Device not executed. This run is command-level and static acceptance only. |

## Command Verification

| Command | Result | Notes | Blocker |
| --- | --- | --- | --- |
| `git diff --check` | PASS | No whitespace errors. | No |
| `scripts\qa\current_preflight.ps1 -Quick` | PASS | 15 total, 13 passed, 2 skipped. Secrets scan passed. Config presence checked without reading content. | No |
| `.\gradlew.bat :core:test` | PASS | Core unit tests completed successfully. | No |
| `.\gradlew.bat :app:testDebugUnitTest` | PASS | App debug unit tests completed successfully. | No |
| `.\gradlew.bat :app:assembleDebug` | PASS | Debug APK assembled successfully. | No |
| `scripts\qa\current_preflight.ps1` | PASS | 16 total, 16 passed, 0 failed, 0 skipped. Includes core test, app unit test, assemble, secrets scan, qwen guard, SDK import guard. | No |

## Practice Generation

| Acceptance item | Result | Evidence / note |
| --- | --- | --- |
| Can generate questions from course evidence | PASS | Covered by P1 automated test suite and full preflight. Generated practice is tied to current course analysis rather than a generic question bank. |
| Insufficient evidence does not fabricate questions | PASS | Covered by practice generation tests; expected behavior is an insufficient-material result instead of invented items. |
| Source metadata exists | PASS | P1 routing keeps source vocabulary aligned with Cloud / On-device / Manual / Safe placeholder semantics. |

## Practice Feedback

| Acceptance item | Result | Evidence / note |
| --- | --- | --- |
| Choice / true-false feedback works | PASS | App unit tests passed; feedback stores result state and explanation path. |
| Short-answer fallback supports needs-review / self-check | PASS | Seam behavior is covered by automated tests; manual/self-check fallback remains available when model scoring is unavailable. |
| Feedback links knowledge point and evidence | PASS | Feedback path is evidence-bound; no generic explanation path is accepted for persisted learning feedback. |

## Weakness Hub

| Acceptance item | Result | Evidence / note |
| --- | --- | --- |
| Wrong answers enter weakness data | PASS | Automated learning-loop tests passed. |
| Wrong / correct counts update | PASS | Weakness and mastery update behavior is covered by P1 tests. |
| `recommendedAction` exists | PASS | Weakness items expose next action for practice/review routing. |

## Review Priority

| Acceptance item | Result | Evidence / note |
| --- | --- | --- |
| Sorts by wrong answers, weakness, and stale review | PASS | Review priority tests passed through app/core suites. |
| Review page can display today tasks | PASS | App unit tests passed; no device UI smoke was executed in this run. |

## Export

| Format | Result | Notes |
| --- | --- | --- |
| Markdown / Text | PASS | Existing renderer remains available. |
| HTML web learning report | PASS | Existing HTML renderer remains available. |
| PDF printable report | PASS | Debug build and export tests passed; PDF renderer remains part of the export set. |
| DOCX / Word editable report | PASS | Added and tested as a real OpenXML package. |
| Course essence audio script | PASS | Script export remains available. TTS audio file generation remains configuration-dependent. |

## DOCX Acceptance

| Acceptance item | Result | Evidence / note |
| --- | --- | --- |
| Real OpenXML zip package | PASS | DOCX renderer tests unzip the artifact. |
| Contains `[Content_Types].xml` | PASS | Verified by DOCX renderer tests. |
| Contains `word/document.xml` | PASS | Verified by DOCX renderer tests. |
| Contains course title, evidence, AI source, and user confirmation notes | PASS | Verified by DOCX renderer tests and app export tests. |
| XML escaping is safe | PASS | Renderer tests cover escaped XML content. |

## AI-refined Export Flow

| Acceptance item | Result | Evidence / note |
| --- | --- | --- |
| Generates StudyReport draft before format selection | PASS | App state/export flow tests cover the draft-ready state before artifact generation. |
| User selects format after draft | PASS | Export Center format chooser includes PDF, DOCX, HTML, Markdown, Text, and audio script. |
| Uses AI processing dialog or equivalent state | PASS | Export flow exposes processing state for report refinement. |
| DOCX/PDF failure has HTML/Text fallback guidance | PASS | Export UI has fallback messaging and alternate format path. |

## Course Essence Audio

| Acceptance item | Result | Evidence / note |
| --- | --- | --- |
| Script generation works | PASS | P1 automated tests passed. |
| TTS missing returns script-only | PASS | ConfigMissing path keeps script export available. |
| No prohibited voice-cloning or specific-person voice copy in product code | PASS | Current preflight and source wording checks passed. |

## Translation

| Acceptance item | Result | Evidence / note |
| --- | --- | --- |
| Translation is stored as a derived note | PASS | P1 tests passed; original evidence remains unchanged. |
| Original evidence is not mutated | PASS | Covered by translation-assisted learning acceptance tests. |

## Text Safety

| Acceptance item | Result | Evidence / note |
| --- | --- | --- |
| Provider unavailable does not block core learning | PASS | Safety seam keeps learning flow available with status marking. |
| Export/share safety path does not leak secrets | PASS | Secrets scan and export redaction checks passed. |

## Internal Function Router

| Tool | Result | Notes |
| --- | --- | --- |
| `searchEvidence` | PASS | Covered by internal tool router tests. |
| `createPractice` | PASS | Covered by internal tool router tests. |
| `updateMastery` | PASS | Covered by internal tool router tests. |
| `createReviewTask` | PASS | Covered by internal tool router tests. |
| `exportStudyReport` | PASS | Covered by internal tool router tests. |
| `createEssenceAudioScript` | PASS | Covered by internal tool router tests. |

## Settings

| Acceptance item | Result | Evidence / note |
| --- | --- | --- |
| Learning and export configuration appears | PASS | App tests and full preflight passed after Settings update. |
| DOCX appears in default export format copy | PASS | Covered by export/settings text tests. |
| TTS, translation, and text-safety status appear | PASS | Settings content tests passed. |
| No key is rendered | PASS | Secrets scan passed; settings copy does not render credential values. |

## Forbidden Wording Check

Result: PASS.

Current preflight scanned current baseline wording and passed. Additional acceptance attention covered:

- No voice-cloning or specific-person voice positioning in product code.
- No exaggerated ASR/OCR completion claim in product code.
- No competitor-replacement claim in product code.
- No LocalRule-as-intelligence wording in product code.
- No DeepSeek or Compatible path promoted as the competition main path.

This section intentionally records categories rather than repeating risky phrases as product copy.

## Device / Manual Execution

Device not executed.

This run did not connect a physical device or cloud device, did not install APK manually, and did not perform tap-through UI proof. Do not treat this document as final device proof. Use a later device smoke run to validate:

1. Practice flow from a real imported course.
2. Review page visual state.
3. Export Center save/share behavior.
4. DOCX file opening in Word/WPS or another compatible editor.
5. PDF print preview.

## Blockers / Warnings / Polish

| Level | Count | Items |
| --- | ---: | --- |
| P0 Blocker | 0 | None found in command-level acceptance. |
| P1 Warning | 1 | Device/manual execution was not run. |
| P2 Polish | 1 | Visual quality of Export Center and report previews still needs real-device screenshot review. |

## Recommended Next Step

Run a device smoke pass focused on Export Center:

1. Import or open a course with evidence and practice attempts.
2. Generate a StudyReport draft.
3. Export Markdown/Text, HTML, PDF, DOCX, and audio script.
4. Open DOCX in Word/WPS-compatible app.
5. Confirm exported files do not contain credentials, raw prompts, vendor body, or reasoning content.

