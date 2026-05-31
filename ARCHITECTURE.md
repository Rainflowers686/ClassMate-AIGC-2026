# Architecture

Two modules: **`app`** (Android · Jetpack Compose) and **`core`** (pure Kotlin/JVM domain). The UI
talks only to `core`'s `CourseAnalyzer`; it never holds a provider or a credential.

## Modules

- **`core`** — domain & logic, no Android. Unit-tested with `:core:test`.
  `model/ analysis/ provider/ prompt/ parser/ validation/ evidence/ feedback/ review/ logging/ sample/`
- **`app`** — Compose UI. `MainActivity`, `navigation/`, `state/`, `platform/`,
  `ui/{theme, design, components, screens}`.

See [ADR-0002](docs/decisions/0002-core-kotlin-jvm-module.md) for why `core` is JVM-only.

## The analysis pipeline

```
CourseSession ─► PromptBuilder ─► provider.generate() ─► ProviderResult
                                                              │ Success(rawModelText)
                                                              ▼
                               JsonExtractor ─► AnalysisJsonParser ─► CourseAnalysisResult
                                                              │
                                  ResultValidator + EvidenceValidator
                                          │ ok          │ fail
                                          ▼             ▼
                                         UI       RedactedLogger ─► next provider (fallback)
```

`CourseAnalyzer.analyze()` iterates `ProviderResolver.providersInOrder()`
(`BlueLM → Compatible → LocalFallback`). For each provider success it extracts JSON, parses to
domain objects, and validates; **any** failure (non-2xx, parse, validation) logs a redacted line
and falls through to the next provider. The local fallback always produces a valid result, so the
app is never empty.

## Data model (v1, `schemaVersion = 1`)

Every aggregate carries `schemaVersion`; embedded value objects version with their parent.

| Type | Key fields | Traceability rule |
|---|---|---|
| `CourseSession` | `segments`, `rawText`, `sourceKind` | text only (no audio) |
| `CourseSegment` | `id`, `index`, `text`, offsets | addressable unit for evidence |
| `EvidenceSpan` | `sourceSegmentId`, `startChar`, `endChar`, `quote` | the atom of the evidence chain |
| `KnowledgePoint` | `title`, `summary`, `importance`, `difficulty` | **binds `sourceSegmentId` + ≥1 `EvidenceSpan`** |
| `QuizQuestion` (+`QuizOption`) | `type`, `stem`, `options`, `explanation` | **binds `testedKnowledgePointIds` + evidence** |
| `QuizAttempt` | `selectedOptionIds`, `isCorrect` | feeds learning state |
| `FeedbackEvent` (+`FeedbackType`) | `type`, `targetKind`, `targetId` | 7 feedback types |
| `LearningState` (+`KnowledgePointState`) | per-KP `mastery`, `attempts` | derived `MasteryLevel` |
| `ReviewPlan` (+`ReviewStep`, `ReviewBasis`) | ordered steps, minutes | **each step binds `knowledgePointIds`** |
| `CourseAnalysisResult` | `knowledgePoints`, `quizQuestions`, `provenance` | the object the validators check |

Question types are learning-oriented: `CONCEPT_UNDERSTANDING`, `JUDGMENT`, `APPLICATION`,
`ERROR_ANALYSIS`, `TRANSFER` — never "which line matches the text".

## Providers

`ModelProvider { kind; isAvailable(); generate(request): ProviderResult }`.

- **`BlueLMProvider`** — primary. Real structure (prompt → body → sign → POST → read envelope →
  map status). Inert in round 1 (default `NoNetworkTransport` + `UnconfiguredBlueLmSigner` →
  `CONFIG_MISSING`). Never fabricates a response.
- **`CompatibleProvider`** — OpenAI-compatible backup (bearer key). Same lifecycle.
- **`LocalFallbackProvider`** — bounded, deterministic, offline heuristic emitting the **same**
  wire JSON, so it is parsed + validated identically. Safety net only.
- **`ProviderResolver`** — builds providers from `ProviderConfigBundle` and exposes them in order.
  Nothing else leaves the class, so no raw provider/credential reaches the UI.
- **`ProviderError`** — closed enum + numeric HTTP status only; no bodies. `ProviderResult` is a
  sealed `Success(rawModelText)` / `Failure(error)`.

## Evidence, parsing, validation

- **`PromptBuilder`** encodes the 10 output rules (JSON only, real concepts, merge duplicates,
  evidence binding, learning-oriented questions, no-evidence-no-output, fail→empty).
- **`JsonExtractor`** recovers a JSON object from pure JSON, ```json fences, or stray prose
  (string-/escape-aware brace matching).
- **`AnalysisJsonParser`** assigns ids, resolves `evidenceQuotes` → `EvidenceSpan` via
  `EvidenceResolver` (storing the **actual** source substring), and maps question→KP title
  references to ids (reference closure).
- **`EvidenceValidator`** checks each span anchors to the source. **`ResultValidator`** checks
  evidence presence, reference closure, and a correct option per question.
- **`ProviderConfigSafetyCheck`** classifies placeholder vs. real secret and scans example configs
  (recording field names only, never values).

## Feedback & review loop

`LearningStateUpdater` (pure) folds `QuizAttempt`s and `FeedbackEvent`s into `LearningState`
(mastery deltas; flags for `not_accurate` / `evidence_wrong` / `too_hard`; boosts for
`already_mastered`). `ReviewPlanner` (pure, deterministic) scores knowledge points by
importance + difficulty + (1−mastery) + wrong answers + feedback flags, and emits ordered,
KP-bound `ReviewStep`s with a `ReviewBasis`.

## Logging

`RedactedLogEntry` is safe by construction — it only has
`provider / status / latency / validation / fallbackUsed / errorType`. There is nowhere to put a
key, prompt, course text, or response body. See [SECURITY.md](SECURITY.md).

## App: navigation, state, theming

- **State** lives in `AppViewModel` (`ui: ClassMateUiState` via Compose state). It owns the
  in-memory back stack and the learning-loop state, and is the only thing that touches `core`.
- **Navigation** is a `Crossfade` over the back-stack's current `Screen`; `BackHandler` pops.
- **Theming** — `ClassMateTheme(themeOption, darkTheme)` provides a Material3 `ColorScheme` plus
  `ClassMateExtendedColors` (evidence highlight, success/warning, hero gradient). Focus / Vitality /
  Flow, each light + dark.
