# Real Device Fix Matrix - 1.14.12 / 125

| Issue | Fix | Verification |
| --- | --- | --- |
| Micro-quiz questions were all true/false | Added knowledge-based local quiz generator and type balancer; true/false is capped and converted to single-choice when overrepresented | `KnowledgeBasedQuizGeneratorTest`, `FreshInstallLearningFlowRegressionTest` |
| Correct answers were all A | New generator distributes correct letters; PracticeSession guard remaps all-A answerable sessions | `KnowledgeBasedQuizGeneratorTest`, `PracticeFlowTest` |
| Questions copied OCR source text | Quiz stems are built from knowledge point cards; evidence is used only as grounding/explanation | `KnowledgeBasedQuizGeneratorTest` |
| Completing quiz crashed | Completion path handles empty questions, missing metadata and duplicate completion safely | `PracticeFlowTest`, app unit tests |
| Completing practice exited incorrectly | Completion now writes history and returns to Review Plan tab | `PracticeFlowTest`, `FreshInstallLearningFlowRegressionTest`, `LearningArtifactRepairIntegrationTest` |
| Evidence page may lack micro-quiz | Deferred by user request; this patch preserves existing evidence-backed questions and keeps course/review practice working | Regression tests around practice builder |

## Still Needs Real-Device Confirmation

- Confirm a fresh OCR course generates mixed micro-quiz questions on device.
- Confirm completing from Course Timeline and Review Plan both lands on Review Plan without crash.
- Confirm evidence pages that already had questions still keep them.
