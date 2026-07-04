# Core LLM Code Package Guide - 1.14.18

## Included Capability Areas

- BlueLM/qwen provider request path:
  - `ProviderAskChatClient`
  - `CloudModelQualityProfile`
  - `AnalysisIntensity`
  - HTTP transport and config resolver code
- AI use cases:
  - course analysis
  - knowledge summary
  - related knowledge
  - quiz generation
  - feedback optimization
  - polished study-pack export
- Official capability seams:
  - OCR
  - ASR
  - TTS
  - provider diagnostics
- Safety and diagnostics:
  - dry-run/live-smoke
  - `PersistentDebugEventLog`
  - safe export policy

## Generate Package

```powershell
cd "D:\Edge Download\AIGC\ClassMate"
powershell -ExecutionPolicy Bypass -File scripts\submission\package_core_llm_code.ps1
```

Output directory:

```text
_ai_outputs\core_llm_code_package
```

The script copies source/docs only. It excludes build outputs, APK/AAB/AAR, `config.local.json`, OfficialDemos, and local credentials.
