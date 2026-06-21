# ClassMate

ClassMate is an Android classroom learning app for turning real study material into a traceable learning loop:

```text
Input material -> Evidence -> Knowledge points -> Micro quiz -> Wrong book -> Review plan -> Study pack export
```

The current product line is the ClassMate Android App. The BlueLM low-code canvas is not the main implementation path; it remains a research or optional future enhancement.

## Current Scope

Current branch family:

- `feature/max-learning-loop-v1`: unified multimodal learning loop.
- `feature/audio-official-loop-hardening-v1`: long-audio, official-capability, export, and middle-layer hardening.

The app supports:

- Text / Markdown input into the L3 learning loop.
- Image/photo OCR input with OCR image evidence assets.
- TXT/Markdown/CSV/document import with best-effort Office extraction and PDF page-text fallback.
- Recording/audio transcript input with audio evidence and transcript timeline.
- Real quiz, wrong book, mastery updates, review queue, and learning diagnosis.
- EvidenceDetail for text, OCR image, document, audio, and web evidence.
- Study pack export through existing PDF, DOCX, Word-compatible HTML, Markdown, HTML, and plain-text paths.

## Effective Capability Matrix

ClassMate’s product matrix contains exactly 18 effective learning capabilities:

1. Large model
2. Function calling
3. Image generation
4. Video generation
5. General OCR
6. Text translation
7. Text embedding
8. Text similarity
9. Query rewrite
10. Realtime short ASR
11. Long audio dictation
12. Long audio transcription
13. Dialect free speech
14. Simultaneous interpretation
15. Audio generation
16. Edge 3B large model
17. Edge text audit
18. Edge capability files

Experimental capabilities are disabled by default:

- Study diagram generation
- Review short-video storyboard generation
- Bilingual classroom simultaneous-interpretation draft

When a generation provider is not configured, ClassMate only creates learning prompts, storyboards, scripts, or bilingual transcript drafts. It does not claim generated images, videos, or realtime interpretation unless the runtime actually succeeds.

## Model Routing

ClassMate uses a layered route:

```text
BlueLM cloud -> Edge 3B -> Local rules
```

- BlueLM cloud: primary route for organizing classroom material into summary, knowledge points, quiz, review plan, and grounded Ask flows when configured.
- Edge 3B: fallback for weak network, offline, and privacy-sensitive study support when device resources are present.
- Local rules: minimum guaranteed route so evidence, quiz, review, diagnosis, and export do not stop when cloud or edge routes are unavailable.

User-facing copy should describe learning value, such as “cloud model organized the class highlights” or “local rules kept the study loop available.” It should not expose internal terms such as provider bodies, adapter wiring, or smoke labels in learning content or exports.

## Audio And ASR

Long-audio learning is treated as a classroom study package path:

- Recording creates an audio artifact.
- ASR job state tracks chunk status, errors, transcript status, and fallback.
- Manual transcript fallback remains available.
- Transcript post-processing preserves raw and corrected text, marks low-confidence segments, and supports dialect/accent classroom mode.
- Audio evidence shows recording/source label, time range, transcript snippet, low-confidence warning, linked knowledge points, linked wrong answers, and linked review tasks.

Audio playback/seek remains a real-device validation item unless a device path proves it stable.

## Study Pack Export

Course Detail exposes a study pack export path. Export content comes from the L3 learning loop, not from a raw text dump. The generated learning pack includes:

- course title and generated time
- source types
- AI-organized summary
- knowledge points
- key concepts and easy mistakes
- micro quiz with correct answers and explanations
- wrong book with mistake reason and remediation hint
- 20-minute review plan with checkbox-style tasks
- learning diagnosis
- evidence source index
- low-confidence notes
- brief cloud/edge/local capability-use note

Exports must not include keys, local config content, request bodies, provider debug payloads, or internal smoke wording.

## Validation Commands

Run from the repository root:

```powershell
git diff --check
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
scripts\qa\current_preflight.ps1
scripts\qa\cloud_device_precheck.ps1
```

Do not run live provider network smoke during ordinary product validation.

## Sensitive Files

Do not read, print, commit, or share:

- `config.local.json`
- `app/libs/*`
- APK / AAB / AAR build artifacts
- keystores, keys, fonts, or local model files
- `.codex_work`

Device model resources are checked by presence only. No model file contents should be copied into docs, logs, tests, or exports.
