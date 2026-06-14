# Roadmap

## Round 1 — foundation (done)

- [x] Buildable Android (Kotlin + Jetpack Compose) project.
- [x] `app` / `core` two-module split; `core` is pure Kotlin/JVM.
- [x] Core data model (schemaVersion + traceability + validation hooks).
- [x] Provider / Prompt / Parser / Validator / Feedback / Review skeletons (BlueLM-first).
- [x] Minimal-but-real UI: 9 screens, 3 themes, evidence highlight, design system.
- [x] `config.example.json` (placeholders only).
- [x] README / ARCHITECTURE / ROADMAP / SECURITY.
- [x] GitHub Actions: Android CI (build + tests) and secrets scan; no APK artifact.
- [x] Unit tests (core: 34 incl. evidence/validator/fallback/planner/safety; app: importer/theme).
- [x] Local build green: `:core:test`, `:app:testDebugUnitTest`, `:app:assembleDebug`.

## Round 2 — make BlueLM real (复赛)

- [ ] Implement a real `HttpTransport` (OkHttp/Ktor) in an app/data layer.
- [ ] Implement `BlueLmSigner` (vivo HMAC over appId/appKey) — keys injected locally only.
- [ ] Wire the debug config import to actually build a `ProviderConfigBundle` (BuildConfig.DEBUG).
- [ ] Confirm the real vivo endpoint path / request & response shapes; tune `PromptBuilder`.
- [ ] Persist sessions / learning state (Room or DataStore) so progress survives restarts.
- [ ] Capture proof: screenshots + redacted logs of a real BlueLM run.

## Round 3 — depth (决赛)

- [ ] Spaced-repetition scheduling on top of `ReviewPlan` (due dates, streaks).
- [ ] Richer question generation (multi-select, worked examples on demand).
- [ ] Flow theme immersive mode: white-noise / rain, focus cards, light motion.
- [ ] Per-knowledge-point mastery analytics and weak-spot dashboard.
- [ ] Optional: multi-lecture courses and cross-lecture knowledge graph.
- [ ] Accessibility & localisation pass; bundled brand font.

## Explicitly out of scope (for now)

- Audio capture / ASR (text input only by design in round 1).
- Accounts / cloud sync.
- Using the local fallback as primary intelligence.
