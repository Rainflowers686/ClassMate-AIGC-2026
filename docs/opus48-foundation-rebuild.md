# Opus 4.8 Foundation Rebuild — ClassMate v0.4

This document captures the one-shot foundation rebuild performed on branch
`foundation/opus48-rebuild`. It is the source of truth for what changed,
why, and what Codex picks up next.

The v0.3.5 cloud-smoke build remains tagged at `v0.3.5-cloud-smoke` and is
the rollback target if anything in v0.4 fails on the V2502A cloud emulator.

## 1. Goal

Turn the v0.3.5 cloud-smoke skeleton into a credible, evidence-closed,
themable foundation — the version we are willing to take to the final round.

Specifically:

- Eliminate the "structure invalid + 100% match" contradiction observed on
  the cloud emulator after `Load demo`.
- Replace static replay (DemoProvider reading `demo_output.json`) with a
  local rule provider that operates on the real input, so the demo path
  itself is an exemplar of the evidence-chain claim.
- Surface strict / lenient match rates separately so honesty is built into
  the UI.
- Introduce a real design system (3 themes, tokens) — no more naked
  `MaterialTheme.colorScheme.*` calls in business screens.
- Keep BlueLM as an honest placeholder until official contract is supplied.

## 2. Root cause of the v0.3.5 contradiction

`ClassMateViewModel.loadDemoInput` joined `demo_input.segments` with `"\n\n"`
into `courseText`, then `Segmenter.segment` greedily merged those 3 ~60-char
paragraphs into a single chunk (`seg_001`) because each was below
`TARGET_CHARS=150`. Meanwhile `DemoProvider.analyzeCourse` ignored `input`
and replayed the static `demo_output.json` which still referenced
`seg_001 / seg_002 / seg_003`. So `EvidenceValidator` saw
`input.segments = [seg_001]` but `result.segments` ids
`= {seg_001, seg_002, seg_003}` — schema check fails. The match rate stayed
high because the validator's span check fell back to `result.correctedText`
when an input id was missing. Hence the contradiction.

## 3. Architecture changes

```
core/
  adapter/
    ModelProvider.kt              unchanged
    ProviderConfig.kt             unchanged
    ModelCallException.kt         unchanged
    JsonExtractor.kt              unchanged
    PromptBuilder.kt              unchanged
    LocalRuleProvider.kt          NEW — replaces DemoProvider
    CompatibleProvider.kt         doc-only update
    BlueLMProvider.kt             unchanged behavior (honest placeholder)
  network/                        unchanged
  segmenter/                      unchanged (now bypassed on demo path)
  validation/
    ResultValidator.kt            ENHANCE — takes (result, input?), typed issues
    ValidationIssue.kt            NEW — typed Kind + ownerId + detail
  evidence/
    EvidenceValidator.kt          ENHANCE — strict + lenient rates
    EvidenceValidationResult.kt   ENHANCE — strict + lenient
  logging/
    ModelCallLog.kt               ENHANCE — strict & lenient rate fields
    RedactedLogger.kt             ENHANCE — emits both rates

app/
  domain/                         NEW
    ProviderResolver.kt           NEW — config.provider → ModelProvider
    AnalyzeCourseUseCase.kt       NEW — primary → fallback orchestration
  data/
    ApiConfigRepository.kt        unchanged
    DemoInputRepository.kt        ENHANCE — dropped loadDemoOutputRaw
  state/
    ClassMateUiState.kt           ENHANCE — themeId, validationIssues, both match rates
    ClassMateViewModel.kt         REFACTOR — delegates to UseCase, sets segments verbatim on demo load
  ui/
    AppRoot.kt                    REWRITE — wires Settings, theme provider
    screens/                      NEW dir — all 8 screens rewritten here
      HomeScreen.kt
      CourseInputScreen.kt
      HotwordScreen.kt
      AnalyzeScreen.kt
      TimelineScreen.kt
      QuizScreen.kt
      ReviewPlanScreen.kt
      SettingsScreen.kt           NEW
    components/                   REWRITE — KnowledgePointCard, SegmentCard, QuizCard, ReviewPlanCard
    designsystem/                 NEW dir
      AppScaffold.kt
      BrandSurface.kt
      GlassCard.kt
      HeroCard.kt
      PrimaryButton.kt
      SectionHeader.kt
      StatusDot.kt
    theme/
      Theme.kt                    REWRITE — picks colors per ThemeId
      ThemeId.kt                  NEW — FocusGlass / VividStudy / LowPower
      ClassMateColors.kt          NEW — token bundle per theme × dark/light
      ClassMateSpacing.kt         NEW
      ClassMateShapes.kt          NEW
      ClassMateMotion.kt          NEW
      ClassMateTypography.kt      NEW
```

Deleted:
- `core/adapter/DemoProvider.kt`
- `app/src/main/assets/demo_output.json`
- 7 old screen files under `app/src/main/java/com/classmate/app/ui/*Screen.kt`

## 4. Provider routing (v0.4)

| `config.provider` | Effective provider | Notes |
| --- | --- | --- |
| `demo`, `local`, blank, unknown | `LocalRuleProvider` (`name="local"`) | Default. Generates result from real input. |
| `compatible` | `CompatibleProvider` over HttpEngine | OpenAI-compatible chat/completions. Failure → fallback to local. |
| `bluelm` | `BlueLMProvider` (always `PROVIDER_NOT_IMPLEMENTED`) | Never makes a request. Falls back to local. |

The HeroCard on AnalyzeScreen surfaces `activeProvider` AND
`requestedProvider` simultaneously, so a reviewer can always tell that a
"successful" result came from a local rule provider rather than a real model.

## 5. Validation chain (v0.4)

```
input ─► Provider.analyzeCourse ─► CourseAnalysisResult
                                          │
                                          ├─► ResultValidator(result, input)
                                          │     ├─ EMPTY_SEGMENTS
                                          │     ├─ KP_SOURCE_SEGMENT_NOT_IN_RESULT
                                          │     ├─ KP_SOURCE_SEGMENT_NOT_IN_INPUT  ← input cross-check (NEW)
                                          │     ├─ KP_IMPORTANCE_OUT_OF_RANGE
                                          │     ├─ KP_DIFFICULTY_OUT_OF_RANGE
                                          │     ├─ QUIZ_SOURCE_SEGMENT_NOT_IN_RESULT
                                          │     ├─ QUIZ_SOURCE_SEGMENT_NOT_IN_INPUT
                                          │     ├─ QUIZ_RELATED_KP_MISSING
                                          │     ├─ QUIZ_ANSWER_INDEX_OUT_OF_RANGE
                                          │     └─ REVIEW_PLAN_KP_MISSING
                                          │
                                          ├─► EvidenceValidator(input, result)
                                          │     ├─ strictEvidenceMatchRate   ← input only
                                          │     └─ lenientEvidenceMatchRate  ← input OR correctedText
                                          │
                                          ├─► RedactedLogger
                                          │
                                          └─► UiState (HeroCard: 4 metrics)
```

Failure modes:
- `LocalRuleProvider` is required to pass both validators by construction —
  if it ever flunks, that is a code bug, not a graceful fallback.
- Non-local providers that flunk fall back to local with `fallbackUsed=true`
  and `lastProviderError = reason: message`.

## 6. UI themes (v0.4)

Three themes via `ThemeId`:

- `FocusGlass` — default. Off-white canvas, 学术蓝 brand, half-transparent
  glass cards with 1 px stroke, light noise-free gradient.
- `VividStudy` — warm orange + indigo accents, light radial gradient,
  glass intact.
- `LowPower` — flat solids, no glass / gradient / shadow / motion,
  +1 px outline replaces depth. Tested for V2502A and low-end devices.

Tokens live in `app/ui/theme/`:
- `ClassMateColors` (per theme × dark/light)
- `ClassMateSpacing` (2 / 4 / 8 / 12 / 16 / 24 / 32)
- `ClassMateShapes` (8 / 12 / 16 / 20 / pill)
- `ClassMateMotion` (120 / 200 / 250 ms tween + one allowed spring)
- `ClassMateTypography` (display / headline / title / body / label / caption)

Business code reads `LocalClassMateColors.current.*` only. Raw `dp` literals
and `MaterialTheme.colorScheme.*` outside `designsystem/` and `theme/` are
forbidden by convention.

User switches theme via `Settings` (Home → ⚙ link).

## 7. Cloud-emulator acceptance steps (V2502A)

Build:

```
.\gradlew.bat :app:assembleDebug
```

Install:

```
adb install app/build/outputs/apk/debug/app-debug.apk
```

Run the demo path:

1. Launch ClassMate.
2. Tap **加载 Demo / Load demo** on Home.
3. Step through `CourseInput → Hotword → Analyze`.
4. On Analyze, tap **调用 demo 分析**. The HeroCard MUST show:
   - 当前 Provider: `local`
   - config 请求: `demo`
   - 兜底: 否 (success tone)
   - 校验: 通过 (success tone)
   - 证据 strict: 100%
   - 证据 lenient: 100%
5. Tap **继续到时间轴**. Knowledge-point cards render.
6. Tap **查看证据** on a card; segment expands above with evidence
   highlighted in warm yellow.
7. Tap **进入微测**. Answer one quiz correctly, one incorrectly.
8. Tap **查看复习计划**. The wrong-answer KP shows a red rail and "错题强化" label.
9. Open ⚙ from Home → Settings: cycle FocusGlass / VividStudy / LowPower.
   All three render the same flow without crash.

Failure mode test (compatible config without keys):

1. Set `config.local.json` `provider="compatible"` with empty `compatible.api_key`.
2. Push to device, restart.
3. On Analyze, run analysis. HeroCard MUST show:
   - 当前 Provider: `local`
   - config 请求: `compatible`
   - 兜底: 是 (error tone)
   - Provider note: `CONFIG_MISSING: ...`
   - 校验: 通过 (local always passes)

## 8. Codex handover list

Priority order. Take one item, ship one PR.

1. **Add core unit tests.** Cover `LocalRuleProvider` (1-segment, multi-segment,
   no-hotword inputs), `ResultValidator` (every `ValidationIssueKind`),
   `EvidenceValidator` (strict/lenient divergence), `JsonExtractor`
   (fenced, prose-wrapped, malformed inputs). Target: ≥ 10 tests.
2. **Persist theme + provider choice** across process death. SharedPreferences
   is acceptable for v0.5.
3. **Real compatible call.** Run a single live `compatible` call against
   DashScope or DeepSeek; check in the redacted JSONL to
   `proof/logs_redacted/api_call_course_analysis_<ts>_redacted.jsonl`
   (without `_sample` field).
4. **BlueLM contract.** When official `base_url`, request body, signing scheme,
   and response envelope arrive, fill in `BlueLMProvider.analyzeCourse`. The
   ProviderResolver and fallback policy do not need changes.
5. **Cloud-emulator screenshot pack.** One screenshot per main-flow screen
   into `proof/screenshots/v0.4/`.
6. **R8 / ProGuard rules.** Add `keep` rules for
   `kotlinx.serialization.Serializer` and Compose entry points; enable
   `isMinifyEnabled = true` in release. Target: release APK ≤ 25 MB.
7. **Schema runtime validator.** Optional: integrate
   `com.networknt:json-schema-validator` for full schema validation as a
   third validator layer on top of ResultValidator.
8. **Process-death restoration.** Use `SavedStateHandle` for `ClassMateUiState`
   so rotation / kill / restore doesn't lose the analysis result.

## 9. What this rebuild explicitly did NOT do

- Login / community / long-term profile
- Real classroom recording
- Small V, Atom Notification, Negative-One-Screen integrations
- Lottie / particle animations
- Real BlueLM call (official contract missing)
- GitHub Issues / Projects / Branch Protection / CI
- PPT / video script
- Schema runtime validator (planned in handover)
- Persistence layer (in-memory only)
