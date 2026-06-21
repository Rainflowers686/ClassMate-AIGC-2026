# P2 Integrated AI Learning Experience Acceptance Run

> Current status note (2026-06-18): this file remains the historical P2 command-level acceptance record. Official provider smoke has since advanced beyond seam-only retrieval: OCR, QUERY_REWRITE, TEXT_SIMILARITY, and EMBEDDING have real network `PASS`. Query Rewrite's earlier blocked state was traced by Claude to a smoke request body schema mismatch and fixed with the official docId 2061 `prompts` schema; qwen3.5-plus rewrite, local safe rewrite, and direct retrieval fallback remain available.

## 1. Run Metadata

| Field | Value |
| --- | --- |
| Date | 2026-06-16 Asia/Shanghai |
| Branch | `feature/product-review-compatible` |
| Commit | `407c1f2 feat(learning): deepen AI study loop and report quality` |
| Scope | P2 acceptance QA for AI learning experience |
| Device/manual execution | Device not executed |
| Config handling | `config.local.json` existence checked by preflight only; content not read |

## 2. Command Verification

| Command | Result | Notes |
| --- | --- | --- |
| `git diff --check` | PASS | No whitespace errors. |
| `scripts\qa\current_preflight.ps1 -Quick` | PASS | 15 total: 13 passed, 0 failed, 2 skipped; secrets scan PASS; qwen guard PASS; SDK import guard PASS. |
| `.\gradlew.bat :core:test` | PASS | Build successful; core tests passed. |
| `.\gradlew.bat :app:testDebugUnitTest` | PASS | Build successful; app unit tests passed. |
| `.\gradlew.bat :app:assembleDebug` | PASS | Debug APK assembled successfully. |
| `scripts\qa\current_preflight.ps1` | PASS | 16 total: 16 passed, 0 failed, 0 skipped; includes core/app tests and debug assemble. |

## 3. Learning Output Readability

| Check | Result | Evidence |
| --- | --- | --- |
| Ask output has title, conclusion, evidence, next action | PASS | `LearningReadableFormatterTest` verifies Ask output keeps evidence quote and source metadata. |
| Practice feedback has conclusion, evidence, next action | PASS | `LearningReadableFormatterTest` and `PracticeGenerationAndFeedbackTest` verify evidence and next action. |
| Weakness output has reason, evidence, next action | PASS | `LearningReadableFormatter.fromWeakness` maps reason, evidence reference, suggested practice, and priority. |
| Review output has reason, estimated time, action | PASS | `ReviewPriorityEngineTest` verifies due reason, action, estimated minutes, and evidence reference. |
| Source metadata retained | PASS | Ask/Practice formatter tests retain `AiExecutionSource`; Practice item source metadata remains present. |

## 4. Ask Deepening

| Check | Result | Notes |
| --- | --- | --- |
| Evidence retrieval | PASS | `GroundedAskLessonEngine` now goes through `RetrieveCourseEvidenceUseCase`; tests cover local evidence behavior. |
| Suggested follow-ups | PASS | `AskGroundedTest` verifies follow-ups for provider and local fallback answers. |
| Add to review | PASS | `KnowledgeTimelineP2TextTest` verifies UI action; `AppViewModel.addAskAnswerToReview` adds review tasks through existing LearningStore path. |
| No evidence does not fabricate | PASS | Existing `AskGroundedTest.noEvidenceReturnsNotFoundWithoutCallingProvider` remains passing. |

## 5. Practice / Feedback

| Check | Result | Notes |
| --- | --- | --- |
| Difficulty | PASS | Practice items now carry `PracticeDifficulty`; tests verify generated items have difficulty. |
| Why this question matters | PASS | Practice items now include `whyThisQuestionMatters`; tests verify nonblank values. |
| Evidence reference | PASS | Practice generation and feedback tests verify evidence-backed items and explanations. |
| Session summary | PASS | `PracticeResult` carries weak point count and next review suggestion; existing practice flow tests still pass. |

## 6. Weakness Hub

| Check | Result | Notes |
| --- | --- | --- |
| `lastWrongAnswer` | PASS | Weakness items now preserve a recent wrong-answer reason when wrong-count evidence exists. |
| `suggestedPractice` | PASS | Weakness items now provide a suggested practice action derived from counters. |
| `reviewPriority` | PASS | Weakness items expose priority including wrong-answer and need-more-practice weight. |

## 7. Review Priority

| Check | Result | Notes |
| --- | --- | --- |
| `estimatedMinutes` | PASS | Priority items expose estimated minutes; test asserts value is present. |
| `evidenceReference` | PASS | Evidence-review tasks preserve a reference note; test asserts evidence item has it. |
| Priority reason | PASS | Existing priority reason and recommended action assertions remain passing. |

## 8. Export

| Format / Section | Result | Notes |
| --- | --- | --- |
| StudyReport learning route | PASS | `StudyReportBuilder` and renderers include learning route; tests verify Markdown/HTML/DOCX output. |
| Markdown / Text | PASS | Existing StudyReport renderer tests pass. |
| HTML TOC / anchors | PASS | `StudyReportP1Test` verifies `class="toc"` and anchors such as `#overview`, `#knowledge`, `#evidence`. |
| PDF | PASS | App export tests and full assemble/preflight remain passing. |
| DOCX | PASS | `StudyReportDocxRendererTest` verifies real OpenXML package and learning route in `word/document.xml`. |
| Audio Script | PASS | Existing course essence script tests remain passing; script-only fallback remains available. |

## 9. Retrieval Providers

| Provider | Result | Notes |
| --- | --- | --- |
| QueryRewrite ConfigMissing fallback | PASS | `RetrievalProvidersTest` verifies local retrieval remains available. |
| QueryRewrite success | PASS | Test verifies rewritten query can recover evidence and source is marked cloud. |
| Similarity rerank seam | PASS | Test verifies a fake similarity provider can reorder evidence candidates. |
| Embedding seam | PASS | Test verifies embedding provider interface returns vector data without requiring a vector database. |
| Local fallback remains | PASS | Retrieval defaults to local evidence candidates when providers are unavailable. |

## 10. Translation

| Check | Result | Notes |
| --- | --- | --- |
| Derived note | PASS | Existing translation tests remain passing. |
| Original evidence unchanged | PASS | Translation stays a derived note attached to evidence/knowledge point; no mutation path introduced. |

## 11. Text Safety

| Check | Result | Notes |
| --- | --- | --- |
| Available behavior | PASS | `BasicTextSafetyProvider` remains covered by current safety tests and preflight secrets scan. |
| Unavailable behavior | PASS | Unavailable safety provider returns a caution status and does not block core learning. |
| Core learning not blocked when unavailable | PASS | Export and learning loop tests continue passing with unavailable/config-missing seams. |

## 12. Internal Function Router

| Tool | Result | Notes |
| --- | --- | --- |
| `searchEvidence` | PASS | Existing `InternalFunctionRouterTest` verifies evidence search payload. |
| `createPractice` | PASS | Existing test verifies it creates practice payload and requires confirmation. |
| `updateMastery` | PASS | Existing test verifies state-changing mastery update requires confirmation. |
| `createReviewTask` | PASS | Existing test verifies review task creation requires confirmation. |
| `exportStudyReport` | PASS | Existing test verifies report export payload. |
| `createEssenceAudioScript` | PASS | Existing test verifies script payload. |
| App usage | PASS | P2 implementation calls router from Practice, Ask-to-review, Export, and Course Essence Script paths. |

## 13. Settings / Processing UX

| Check | Result | Notes |
| --- | --- | --- |
| P2 settings items present | PASS | `SettingsModelConfigTextTest` verifies retrieval, default difficulty, DOCX, source metadata, and bilingual note copy. |
| No key rendered | PASS | Settings text test and preflight secret scan pass. |
| AI processing states | PASS | Ask, Practice generation, Export draft, and Course Essence Script paths use existing `AiProcessingUiState` or equivalent state. |

## 14. Forbidden Wording Check

| Category | Result |
| --- | --- |
| Voice-identity wording guard | PASS |
| Specific teacher voice-identity wording guard | PASS |
| Automatic lecture-listening overclaim | PASS |
| Product-replacement overclaim against recording tools | PASS |
| Multimodal/OCR replacement overclaim | PASS |
| Automatic OCR-complete overclaim | PASS |
| Real-time ASR-complete overclaim | PASS |
| Local-rule intelligence overclaim | PASS |
| Local-rule fallback overclaim | PASS |

The current preflight wording guard passed before this record was written; this record intentionally uses category wording rather than reintroducing obsolete phrasing into `docs/current`.

## 15. Device / Manual Execution

Device not executed. This run is command-level acceptance only. No connected device or cloud-device walkthrough was performed, so UI tap paths should still be verified manually before demo proof.

Suggested manual checks:

| Area | Manual Check |
| --- | --- |
| Ask | Ask a grounded question, verify evidence snippets, suggested follow-ups, and add-to-review action. |
| Practice | Start practice, verify difficulty/why-this-matters copy and feedback evidence. |
| Review | Verify generated review tasks show reason, estimated minutes, and evidence context. |
| Export | Generate Markdown, HTML, PDF, DOCX, Text, and Audio Script from the same course. |
| Settings | Confirm P2 settings copy is visible and no key content is shown. |

## 16. Blockers / Warnings / Polish

| Type | Count | Items |
| --- | ---: | --- |
| P0 Blocker | 0 | None found in command-level acceptance. |
| P1 Warning | 1 | Device/manual execution not performed. |
| P2 Polish | 1 | Manual screenshot proof should confirm the UX feels clear on real device. |

## 17. Recommended Next Step

1. Run the same flow on a real device or cloud device and capture screenshots.
2. Verify Ask add-to-review updates the visible Review page.
3. Export a DOCX and HTML report from a real course and open both outside the app.
4. If manual UX passes, prepare a release/proof commit for P2.
