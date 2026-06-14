# ClassMate

**基于蓝心大模型的「证据式课堂理解 + 自适应微测复习」助手。**

ClassMate turns one lecture's text into real, *evidence-bound* knowledge points, learning-oriented
micro-tests, and a review plan that adapts to your answers and feedback. It is built for the vivo
AIGC 创新赛 (复赛 / 决赛) and designed to demo well from day one.

> Not a note app, not an AI summariser. The difference: the model genuinely understands the
> lecture, every conclusion is **traceable to original-text evidence**, micro-tests serve
> **learning** (not text matching), and the review plan **optimises from feedback**.

---

## Status (round 1)

| Check | Command | Result |
|---|---|---|
| Core unit tests | `.\gradlew.bat :core:test` | ✅ 34 tests pass |
| App unit tests | `.\gradlew.bat :app:testDebugUnitTest` | ✅ pass |
| Debug build | `.\gradlew.bat :app:assembleDebug` | ✅ `app-debug.apk` (~9 MB) |

A buildable Android + Jetpack Compose project with a clean `app` / `core` split, the full domain
(models, providers, prompt, parser, validators, feedback, review, logging), a three-theme UI with
all nine screens, CI, and a secrets scan. BlueLM is wired but inert until real keys are injected.

## Tech matrix (deliberately conservative & known-good)

Gradle **8.4** · AGP **8.2.2** · Kotlin **1.9.22** · Compose compiler **1.5.8** ·
Compose BOM **2024.02.00** · JDK **17** · `compileSdk/targetSdk 34` · `minSdk 24`.

## Project layout

```
ClassMate/
├─ app/    Android app (Jetpack Compose): MainActivity, navigation/, state/, platform/, ui/{theme,design,components,screens}
├─ core/   Pure Kotlin/JVM domain: model/ analysis/ provider/ prompt/ parser/ validation/ evidence/ feedback/ review/ logging/ sample/
├─ data/   examples/ (sample course) · schema/ (model output JSON Schema) · prompts/ (prompt spec)
├─ docs/   architecture/ · product/ · competition/ · decisions/ (ADRs)
├─ proof/  screenshots/ · videos/ · logs_redacted/ · audit/
├─ scripts/ ci/ · secrets_scan/ · proof/
├─ .github/ workflows/ (Android CI + secrets scan) · ISSUE_TEMPLATE/
├─ README.md · ARCHITECTURE.md · ROADMAP.md · SECURITY.md · config.example.json
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for the pipeline and data model.

## The nine screens

Home · Import (text only) · Analyze (product flow) · Knowledge timeline · Evidence detail
(highlighted source) · Quiz (with rationale + evidence) · Review plan (做什么 / 为什么 / 几分钟) ·
Feedback (7 types) · Settings (model config, theme, debug import, privacy, redacted logs).

## Themes

Three switchable themes; **Focus** is the default (academic ink + paper, knowledge indigo, amber
evidence highlight). **Vitality** (youthful, encouraging) and **Flow** (calm, immersive) are one
tap away in Settings. See [ADR-0003](docs/decisions/0003-focus-default-theme.md).

## Build & run

Prerequisites: **JDK 17** and the **Android SDK** (platform 34, build-tools 34). Point Gradle at
your SDK via a local, git-ignored `local.properties`:

```
sdk.dir=/path/to/Android/Sdk
```

Then:

```powershell
# Windows
.\gradlew.bat :core:test
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

```bash
# macOS / Linux
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug
```

Open in Android Studio and run the `app` configuration on a device / emulator (or vivo 云真机).

## BlueLM credentials (where real keys go)

Real `AppID` / `AppKEY` **never** enter the repository. They are injected locally via:

1. a git-ignored `config.local.json` (copy [`config.example.json`](config.example.json), which
   contains only `YOUR_BLUELM_APP_ID` / `YOUR_BLUELM_APP_KEY` placeholders), or
2. the **debug-only** config import entry in Settings (round 1: inspects safely, does not persist).

Going live also needs a real `HttpTransport` and `BlueLmSigner` injected into `ProviderResolver`
(the reserved seams). See [SECURITY.md](SECURITY.md) and
[ADR-0001](docs/decisions/0001-bluelm-first.md).

## Security in one line

Logs record only `provider / status / latency / validation / fallback_used / error_type` — never a
key, prompt, course text, or vendor response. CI runs a secrets scan. Full rules in
[SECURITY.md](SECURITY.md).
