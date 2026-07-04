# ClassMate Current Documentation

Current version: `1.14.18 / versionCode 131`

This folder is the current documentation entry point. Historical files remain for audit, but the latest status should be read through `DOCUMENT_INDEX.md` and the 1.14.18 documents.

## Quick Links

1. [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)
2. [CHANGELOG_1_14_18.md](CHANGELOG_1_14_18.md)
3. [REAL_DEVICE_FIX_MATRIX_1_14_18.md](REAL_DEVICE_FIX_MATRIX_1_14_18.md)
4. [REAL_DEVICE_TEST_MANUAL_1_14_18.md](REAL_DEVICE_TEST_MANUAL_1_14_18.md)
5. [BUILD_AND_RELEASE_GUIDE_1_14_18.md](BUILD_AND_RELEASE_GUIDE_1_14_18.md)
6. [FINAL_SUBMISSION_CHECKLIST_1_14_18.md](FINAL_SUBMISSION_CHECKLIST_1_14_18.md)
7. [FINAL_ROUND_MATERIAL_PLAN_1_14_18.md](FINAL_ROUND_MATERIAL_PLAN_1_14_18.md)
8. [CORE_LLM_CODE_PACKAGE_GUIDE_1_14_18.md](CORE_LLM_CODE_PACKAGE_GUIDE_1_14_18.md)
9. [DEMO_VIDEO_SCRIPT_3MIN_VERTICAL_1_14_18.md](DEMO_VIDEO_SCRIPT_3MIN_VERTICAL_1_14_18.md)
10. [PPT_CONTENT_DRAFT_1_14_18.md](PPT_CONTENT_DRAFT_1_14_18.md)

## Current Product Statement

ClassMate is an evidence-bound learning-loop app: classroom material becomes subject knowledge, evidence, micro practice, feedback, review planning, and AI-polished export.

## Current Engineering Statement

1.14.18 focuses on the diagnosed Practice -> Review Compose crash path. Practice completion no longer switches Review synchronously in the button call stack; it records a pending Review navigation and the app root consumes it after a frame. The high-risk top-level Crossfade screen switch has been removed.

## Submission Workspace

Round-two submission material drafts live in:

```text
docs/submission/round2/
```

These are templates and copy-ready materials. Real screenshots, final poster, video, APK, and code package zip still require human production.
