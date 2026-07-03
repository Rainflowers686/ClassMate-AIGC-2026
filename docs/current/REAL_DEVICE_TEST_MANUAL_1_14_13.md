# Real Device Test Manual - 1.14.13 / 126

## A. Image Import And OCR Recovery

1. Install fresh build and import a clear JPEG or PNG from the system photo picker.
2. Expected: the image appears in the material tray with preview before OCR success is required.
3. Disable or omit official OCR configuration and import another image.
4. Expected: the image still remains, OCR status explains that recognition is unavailable or failed, and manual text input is available.
5. Tap re-OCR after the image is saved.
6. Expected: re-OCR uses the saved original/high-quality image, not a thumbnail.

## B. Quiz Content Quality

1. Generate a course from image OCR or manual OCR text.
2. Start micro-quiz from Review Plan or Knowledge Timeline.
3. Expected: stems, options, and explanations do not contain OCR/meta wording such as OCR, original text, fallback, provider, raw id, or relevance-rule language.
4. Expected: options are subject statements or common misconceptions.
5. Expected: at least one fill-in question appears when the session has enough material.

## C. Fill-In And Mixed Completion

1. Answer one single-choice, one true/false, and one fill-in question.
2. Submit each answer.
3. Expected: fill-in answers can be entered without option buttons and grading does not crash.
4. Tap complete practice.
5. Expected: learning record is saved and the app returns to Review Plan. It must not crash or exit.
6. Double tap complete if possible.
7. Expected: completion remains idempotent and does not trigger duplicate navigation.

## D. Deferred Evidence Page Scope

Some evidence-detail pages may still show no micro-quiz for a single knowledge point. This is deferred in 1.14.13, but existing evidence-backed questions must not disappear.

## E. BlueLM/qwen Regression Guard

Do not reconfigure BlueLM while testing this patch. 1.14.13 keeps the 1.14.9 qwen3.5-plus mode mapping and only changes OCR resilience, quiz content quality, fill-in handling, and completion stability.
