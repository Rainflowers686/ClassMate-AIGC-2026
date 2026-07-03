# Real Device Fix Matrix - 1.14.13 / 126

| Issue | Fix | Verification |
| --- | --- | --- |
| Image upload could become unrecognizable or disappear when OCR failed | Import now saves the original/high-quality image to app-private storage, keeps a preview item, and exposes manual text fallback | `ImageDraftFlowTest`, app unit tests |
| OCR provider could receive preview/empty bytes | Picker flow now prefers original URI bytes and falls back to high-quality encoded image bytes; re-OCR uses saved original path | `ImageDraftFlowTest` |
| OCR failure left no useful recovery path | Failed/empty OCR keeps an editable draft and material item so the course can continue after manual input | `ImageDraftFlowTest` |
| Quiz options/explanations exposed OCR/meta wording | Added student-visible quiz sanitizer before PracticeSession display | `KnowledgeBasedQuizGeneratorTest`, `L3LearningPipelineTest` |
| Local fallback options were meta statements rather than subject choices | Rebuilt local fallback templates around subject concepts, formulas, and common misconceptions | `KnowledgeBasedQuizGeneratorTest` |
| No fill-in question appeared | Generator now emits fill-in questions and parser/grading model supports answerable fill-in items | `VariantQuizParserTest`, `PracticeAnswerableGateTest`, `L3LearningPipelineTest` |
| Completion could crash with fill-in/empty/missing-evidence sessions | Completion and grading are null-safe for mixed question types and duplicate completion | `PracticeFlowTest`, app unit tests |
| Evidence page may lack a micro-quiz for some knowledge points | Deferred by user request; existing evidence-backed questions are preserved | Regression tests around practice/session generation |

## Still Needs Real-Device Confirmation

- Confirm JPEG/PNG/WEBP image import keeps preview and editable OCR draft on target devices.
- Confirm official OCR missing/failing still leaves manual text entry available.
- Confirm mixed micro-quizzes include at least one fill-in question for normal 3+ question sessions.
- Confirm completing quiz returns to Review Plan without crash from Course Timeline and Review Plan entries.
