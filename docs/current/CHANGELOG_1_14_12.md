# ClassMate 1.14.12 / versionCode 125

## Summary

This patch focuses on real-device quiz quality and practice completion stability.

## Fixes

- Micro-quiz generation now uses accepted subject knowledge cards plus evidence, instead of copying OCR text into stems or option A.
- Local fallback quiz generation now produces mixed question types: single-choice is preferred, true/false is limited, and one-question sessions do not default to true/false.
- Correct answers are no longer fixed to A. Options are distributed deterministically and a final PracticeSession guard remaps all-A sessions before display.
- Answer explanations include the knowledge point, evidence quote, why the correct option is right, and why distractors are wrong.
- Completing a quiz is null-safe for empty sessions, missing evidence, repeated taps, and old questions with partial metadata.
- Completing practice now saves the learning record and returns to the Review Plan screen. It must not crash, exit the app, or pop to an invalid route.

## Deferred

- Evidence-detail pages that still have no micro-quiz for some individual knowledge points are not forced in this patch. Existing evidence-backed questions are preserved.

## Preserved

- BlueLM/qwen3.5-plus mode mapping remains unchanged from 1.14.9: fast = low + no thinking, balanced = medium + no thinking, professional UI Max = API high + thinking.
- Local fallback is still labelled honestly and is never presented as BlueLM output.
