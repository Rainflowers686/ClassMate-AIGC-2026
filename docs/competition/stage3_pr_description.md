# Stage 3 PR Description

Suggested title:

```text
feat(ui): productize Live Companion with Flow visuals and design tokens
```

## Summary

Stage 3 productizes the learning shell around the existing ClassMate v1 BlueLM-first pipeline. The focus is Live Companion polish, Flow-oriented visual support, shared design tokens/components, and competition proof documentation.

这次 PR 的目标不是重写 provider，而是让 ClassMate 更像一个可演示、可答辩、可真机 smoke 的学习产品。

## What changed

- Productized Live Companion manual classroom flow.
- Added Flow-oriented visual components and scene data.
- Added shared product components and design tokens.
- Added competition docs:
  - Stage 3 smoke checklist.
  - Review readiness report.
  - Capability matrix.
  - Security proof checklist.
  - Demo script.
  - Device recording shot list.
  - Judge Q&A.
  - Acceptance matrix.
  - Competitive reference.
  - Commit split suggestions.

## What did not change

- No BlueLM protocol change.
- No Compatible provider protocol change.
- No LocalFallback behavior change.
- No ProviderResolver order change.
- No validator weakening.
- No EvidenceValidator / ResultValidator / EvidenceResolver weakening.
- No Gradle/build logic change.
- No RECORD_AUDIO permission.
- No real ASR implementation.
- No real OCR implementation.
- No real media parsing.
- Flow ambience is visual-only; no real audio playback is implemented.
- qwen3.5-plus `enable_thinking=false` is preserved.

## Safety / privacy

- No real AppID/AppKEY/API key is committed.
- No `config.local.json` is committed.
- Debug import remains local/debug-only and should only show masked credential status.
- Logs and exports must not include Authorization, prompt/messages, vendor response body, or reasoning_content.
- Compatible demo is clearly separate from the official BlueLM compliance path.

## Validation

Expected validation before merge:

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
scripts\secrets_scan\secrets_scan.ps1
git diff --check
git ls-files config.local.json local.properties secrets.properties .env .env.* *.jks *.keystore *.apk *.aab app/build core/build build .gradle
```

Also verify:

```powershell
Select-String -Path "core\src\main\kotlin\com\classmate\core\provider\VendorIo.kt","core\src\main\kotlin\com\classmate\core\provider\BlueLMDiagnostic.kt" -Pattern "enable_thinking|qwen3.5-plus"
```

## Screens to smoke test

- Home.
- Settings provider profile.
- BlueLM diagnostic.
- Import Hub paste/txt/md.
- Analyze progress.
- Knowledge Timeline.
- Evidence detail.
- Quiz.
- Review / Weakness Hub.
- Live Companion.
- History / Course Library.
- Export report.
- Capability Roadmap.

## Risk

- Medium UI risk: shared components and tokens may affect visual consistency across pages.
- Medium Live risk: Live Companion must remain honest manual/simulated mode, not imply real ASR.
- Low provider risk: provider chain should be untouched.
- High proof/security risk if screenshots accidentally capture keys; recording checklist must be followed.

## Rollback plan

- If docs are the issue, revert the docs commit only.
- If Live/Flow UI is the issue, revert the UI/product commit.
- If provider tests fail, inspect accidental provider changes first; expected Stage 3 should not modify provider protocol or resolver order.
- If screenshots/proof leak sensitive content, delete proof artifacts and rotate any exposed key before proceeding.

## Checklist

- [ ] No BlueLM protocol change.
- [ ] No ProviderResolver change.
- [ ] No validator weakening.
- [ ] No RECORD_AUDIO permission.
- [ ] Flow ambience is visual-only / no real audio.
- [ ] qwen3.5-plus `enable_thinking=false` preserved.
- [ ] No real AppID/AppKEY/API key committed.
- [ ] `config.local.json` not tracked.
- [ ] Secrets scan passed.
- [ ] Core tests passed.
- [ ] App unit tests passed.
- [ ] Debug APK builds.
- [ ] Stage 3 smoke checklist ready for真机.
