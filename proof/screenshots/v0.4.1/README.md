# v0.4.1 Screenshot Proof Checklist

This directory is reserved for manually uploaded screenshots for the
`v0.4.1-foundation-ci-pass` stable foundation.

Do not commit account details, API keys, local config, raw provider response
text, private course content, or unredacted device/vendor identifiers. This
README is the checklist only; real screenshots should be captured and added
manually after review.

## Target Device

- Preferred: vivo cloud device V2502A or equivalent.
- Orientation: portrait.
- Theme: use the demo theme intended for competition review.
- Input data: use non-private demo course text only.

## Required Files

Use the exact filenames below so reviewers can inspect the flow in order:

| Order | Filename | Page / State |
| --- | --- | --- |
| 01 | `01_home.png` | Home |
| 02 | `02_course_input.png` | Course input |
| 03 | `03_hotword.png` | Hotword input |
| 04 | `04_analyze_success.png` | Analyze success |
| 05 | `05_timeline.png` | Timeline |
| 06 | `06_evidence_highlight.png` | Evidence highlight |
| 07 | `07_quiz_feedback.png` | Quiz feedback |
| 08 | `08_review_plan.png` | Review plan |
| 09 | `09_settings.png` | Settings |

## Global Acceptance Criteria

Every screenshot must satisfy all of these checks:

- No account name, phone number, email, avatar, token, key, local path, or other
  private information is visible.
- No raw provider response body, raw prompt, raw debug JSON, stack trace, or
  internal HTTP detail is visible.
- Demo text is safe to publish and does not contain private classroom content.
- The UI is readable at phone resolution, with no clipped primary text or
  obviously broken layout.
- The screenshot captures the full app surface, not the desktop/browser around
  it.
- Filename matches the table above exactly.

## Per-Screenshot Acceptance Criteria

### `01_home.png`

- Shows the Home entry state for ClassMate.
- Primary action to begin course analysis is visible.
- No stale error or debug state is visible.

### `02_course_input.png`

- Shows the course text input screen with safe demo text.
- Input content is long enough to demonstrate segmentation.
- Navigation and primary action remain visible and readable.

### `03_hotword.png`

- Shows the hotword entry or hotword review state.
- Contains only safe demo hotwords.
- The flow to continue analysis is visible.

### `04_analyze_success.png`

- Shows a successful analysis result.
- Structure validation pass is visible.
- Strict and/or lenient evidence hit-rate indicators are visible.
- Fallback status is visible if fallback was used.

### `05_timeline.png`

- Shows the generated learning timeline.
- At least one timeline step is visible with readable title/content.
- Timeline layout is not clipped or overlapped.

### `06_evidence_highlight.png`

- Shows evidence text or evidence highlight behavior.
- Evidence span is visibly connected to the analyzed result.
- No raw debug JSON or provider body is visible.

### `07_quiz_feedback.png`

- Shows a quiz answer feedback state.
- Correct/incorrect feedback is visually clear.
- Review hint or explanation text is readable.

### `08_review_plan.png`

- Shows the generated review plan.
- Wrong-answer reinforcement, if present, is visible and understandable.
- The plan content is readable without exposing private user data.

### `09_settings.png`

- Shows Settings.
- Theme/provider controls or current settings are visible.
- No real key, local config path, account info, or private device info is shown.

## Manual Capture Notes

- Capture screenshots only after the v0.4.1 stable tag behavior is reproduced.
- Review each image locally before committing it.
- If a screenshot contains private data, retake it with safe demo data instead
  of editing around the sensitive area.
