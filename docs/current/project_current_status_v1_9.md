# Project Current Status v1.9

Date: 2026-06-21

This status document reflects the `feature/audio-official-loop-hardening-v1` hardening line. It is a product-state document, not a raw vendor-navigation inventory. No secrets, endpoint URLs, Authorization values, request bodies, model files, or `config.local.json` contents are recorded.

## Product Direction

ClassMate is now centered on the Android App learning loop:

```text
Input material -> Evidence -> Knowledge points -> Micro quiz -> Wrong book -> Mastery -> Review plan -> Study pack export
```

The BlueLM low-code canvas is not the core implementation path. It remains research or optional future enhancement only.

## Effective Capability Matrix

ClassMate keeps 18 effective capabilities in the product matrix:

| # | Capability | Priority | Current status | Learning-loop value |
| --- | --- | --- | --- | --- |
| 1 | Large model | CORE | USED_IN_LEARNING_LOOP | Organizes class material into summary, knowledge, quiz, and review. |
| 2 | Function calling | CORE | FALLBACK_ONLY / SEAM_READY | Explains and plans the learning tool chain with local fallback. |
| 3 | Image generation | EXPERIMENTAL | CONFIG_REQUIRED | Generates study diagram prompts when enabled. |
| 4 | Video generation | EXPERIMENTAL | CONFIG_REQUIRED | Generates review video storyboard when enabled. |
| 5 | General OCR | CORE | USED_IN_LEARNING_LOOP / CONFIG_REQUIRED | Image/photo text enters evidence and L3. |
| 6 | Text translation | ENHANCEMENT | CONFIG_REQUIRED | Supports bilingual evidence and review explanation when configured. |
| 7 | Text embedding | CORE | FALLBACK_ONLY / READY | Builds searchable semantic index with official-first path and local vector fallback. |
| 8 | Text similarity | CORE | FALLBACK_ONLY / READY | Links evidence, similar knowledge, and similar wrong questions. |
| 9 | Query rewrite | CORE | FALLBACK_ONLY / READY | Normalizes Ask, review, and wrong-question queries. |
| 10 | Realtime short ASR | CORE | SEAM_READY | Short classroom audio can enter the transcript loop when configured. |
| 11 | Long audio dictation | CORE | CONFIG_REQUIRED / FALLBACK_ONLY | Long recordings enter transcript timeline through ASR or manual fallback. |
| 12 | Long audio transcription | CORE | CONFIG_REQUIRED / FALLBACK_ONLY | Transcript segments feed knowledge, quiz, review, and evidence. |
| 13 | Dialect free speech | CORE | FALLBACK_ONLY / SEAM_READY | Accent/dialect classroom mode marks low-confidence segments and protects raw text. |
| 14 | Simultaneous interpretation | EXPERIMENTAL | CONFIG_REQUIRED / SEAM_READY | Bilingual transcript draft when enabled; no realtime claim without runtime success. |
| 15 | Audio generation | ENHANCEMENT | CONFIG_REQUIRED | Generates read-aloud review scripts; audio generation waits for config. |
| 16 | Edge 3B large model | CORE | FALLBACK_ONLY | Weak-network/offline/private fallback for summary, quiz, review, and export draft. |
| 17 | Edge text audit | CORE | USED_IN_LEARNING_LOOP / FALLBACK_ONLY | Safety guard for user input and generated learning assets. |
| 18 | Edge capability files | CORE | USED_IN_LEARNING_LOOP / FALLBACK_ONLY | Device readiness for edge resources without tracking model contents. |

## Cloud / Edge / Local Routing

Each learning loop can explain its route:

- `BLUE_LM_CLOUD`: primary for class organization when configured.
- `EDGE_3B`: fallback for weak network, offline, or privacy-sensitive learning.
- `LOCAL_RULE`: minimum safe route to keep evidence, quiz, review, diagnosis, and export available.

The app copy should use learning language, for example: “cloud model organized classroom highlights,” “edge model can help offline review,” or “local rules kept the study loop available.”

## Long Audio And Dialect Mode

Long-audio learning has:

- recording/audio artifact metadata
- ASR job state and chunk status
- manual transcript fallback
- glossary-based transcript post-processing
- raw and corrected transcript text
- low-confidence segment markers
- dialect/accent/mixed-speaker mode
- audio evidence linked to knowledge, wrong book, and review tasks

Seek playback remains a real-device validation item.

## Study Pack Export

Course Detail now exports a study pack from the L3 snapshot. It can use existing PDF, DOCX, Word-compatible HTML, Markdown, HTML, and plain-text export paths.

The study pack includes summary, knowledge points, key concepts, micro quiz answers and explanations, wrong-book records, remediation hints, 20-minute review plan, diagnosis, evidence index, low-confidence notes, and a short cloud/edge/local capability-use note.

Exports must not include secrets, local config details, request bodies, provider debug output, or internal smoke wording.

## Validation Boundary

This document does not claim that every official runtime was live-used on a cloud device. Official-first paths require demo/cloud-device validation. Missing config, runtime failure, or unavailable edge resources must fall back without breaking the learning loop.
