# Architecture overview

Two modules, one pipeline.

```
app  (Android · Jetpack Compose)         core  (pure Kotlin/JVM, no Android)
─────────────────────────────────        ──────────────────────────────────
MainActivity / ClassMateApp               model/        domain data (+ schemaVersion)
navigation/  (in-memory back stack)       analysis/     CourseAnalyzer, CourseSegmenter
state/       AppViewModel  ───────────►   provider/     ModelProvider, BlueLMProvider,
ui/theme     Focus/Vitality/Flow                        CompatibleProvider, LocalFallback,
ui/components evidence highlight, cards               ProviderResolver, config, errors
ui/screens   the 9 screens                prompt/       PromptBuilder (the 10 rules)
platform/    DebugConfigImporter          parser/       JsonExtractor, AnalysisJsonParser
                                          validation/   ResultValidator, EvidenceValidator,
                                                        ProviderConfigSafetyCheck
                                          evidence/     EvidenceResolver
                                          feedback/     LearningStateUpdater
                                          review/       ReviewPlanner
                                          logging/      RedactedLogger
                                          sample/       SampleCourses (curated demo)
```

## The pipeline (one analysis run)

```
CourseSession ─► PromptBuilder ─► [ provider ] ─► JsonExtractor ─► AnalysisJsonParser
                                       │                                   │
                                       ▼                                   ▼
                                 ProviderResult                    CourseAnalysisResult
                                       │                                   │
                                       └───────────► ResultValidator ◄─────┘
                                                     + EvidenceValidator
                                                            │
                                       pass ────────────────┴──────────────► UI
                                       fail ─► RedactedLogger ─► next provider (fallback)
```

`CourseAnalyzer` walks `ProviderResolver.providersInOrder()` (BlueLM → Compatible → Local),
and for each success runs extract → parse → validate, falling back on any failure. The UI only
ever calls `CourseAnalyzer`; it never holds a provider or a credential.

## Why `core` is a JVM library

It has no Android dependency, so it builds fast and is unit-tested with plain JUnit
(`:core:test`). Networking is behind `HttpTransport`, so `core` pulls in no HTTP client; a real
transport is injected from the app/data layer. See
[ADR-0002](../decisions/0002-core-kotlin-jvm-module.md).

## Evidence chain

`EvidenceSpan(sourceSegmentId, startChar, endChar, quote)` is the atom. `EvidenceResolver`
locates a model quote in the source (exact, then whitespace-insensitive) and stores the **actual**
source substring, so `EvidenceValidator` is exact. A knowledge point/question with no locatable
evidence fails validation and never reaches the UI.
