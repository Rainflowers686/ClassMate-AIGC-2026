# Official Provider Network Smoke Run

## Date / Branch / Commit

- Date: 2026-06-17
- Branch: `feature/product-review-compatible`
- Commit at start: `840dcef`
- Scope: Official provider smoke workflow v4 conservative mapping and timeout diagnostics

## Smoke Script Version

- Script: `scripts/qa/official_provider_smoke.ps1`
- Version intent: strict, provider-aware smoke diagnostics v4
- Default mode: dry-run
- Network mode: requires explicit `-RunNetwork`
- Local config mode: requires explicit `-UseLocalConfig`
- Timeout: `-TimeoutSeconds`, default 20 seconds
- Output directory: `.codex_work/official_provider_smoke/`

## DryRun Result

Executed:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -DryRun -NoOpen
```

Result: PASS.

| Capability | Status | Request sent |
|---|---|---:|
| OCR | DRY_RUN_READY | false |
| QUERY_REWRITE | DRY_RUN_READY | false |
| TEXT_SIMILARITY | DRY_RUN_READY | false |
| TRANSLATION | DRY_RUN_READY | false |
| TTS | DRY_RUN_READY | false |
| FUNCTION_CALLING | DRY_RUN_READY | false |
| EMBEDDING | DRY_RUN_READY | false |

Generated local-only files:

- `.codex_work/official_provider_smoke/smoke_result.json`
- `.codex_work/official_provider_smoke/smoke_result.md`
- `.codex_work/official_provider_smoke/smoke.log`
- `.codex_work/official_provider_smoke/test_inputs/`
- `.codex_work/official_provider_smoke/outputs/`

These files are not intended for Git.

## Explain Config Without Local Read

Executed:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig -NoOpen
```

Observed value-free status:

- `config.local.json exists`: true
- `local config read`: false
- detected local groups: none, because local config was not read
- request sent: false

| Capability | Endpoint mapping | Auth mapping | Request schema | Mapping source | Request sent |
|---|---|---|---|---|---:|
| OCR | MISSING | MISSING | READY | NONE | false |
| QUERY_REWRITE | MISSING | MISSING | READY | NONE | false |
| TEXT_SIMILARITY | MISSING | MISSING | READY | NONE | false |
| TRANSLATION | SEAM_ONLY | MISSING | GENERIC_ONLY | NONE | false |
| TTS | SEAM_ONLY | MISSING | GENERIC_ONLY | NONE | false |
| FUNCTION_CALLING | SEAM_ONLY | MISSING | GENERIC_ONLY | NONE | false |
| EMBEDDING | MISSING | MISSING | READY | NONE | false |

## Explain Config With Local Read

Executed only because this strict-alignment task explicitly required value-free diagnostics:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig -UseLocalConfig -NoOpen
```

Observed value-free status:

- `config.local.json exists`: true
- `local config read`: true
- detected group: `topLevel.bluelm`
- no local value was printed
- no request was sent

| Capability | Endpoint mapping | Auth mapping | Request schema | Config source | Mapping source | Request sent |
|---|---|---|---|---|---|---:|
| OCR | MISSING | MISSING | READY | NONE | NONE | false |
| QUERY_REWRITE | MISSING | MISSING | READY | NONE | NONE | false |
| TEXT_SIMILARITY | MISSING | MISSING | READY | NONE | NONE | false |
| TRANSLATION | SEAM_ONLY | MISSING | GENERIC_ONLY | NONE | NONE | false |
| TTS | SEAM_ONLY | MISSING | GENERIC_ONLY | NONE | NONE | false |
| FUNCTION_CALLING | SEAM_ONLY | MISSING | GENERIC_ONLY | NONE | NONE | false |
| EMBEDDING | MISSING | MISSING | READY | NONE | NONE | false |

Important v4 correction:

- `topLevel.bluelm` is not treated as an OCR, ASR, retrieval, translation, TTS, or function-calling endpoint.
- OCR no longer becomes `READY` from generic BlueLM/qwen config.
- Query Rewrite / Text Similarity / Embedding no longer become `READY` from generic BlueLM/qwen config.
- Translation, TTS, and Function calling remain seam-only until a confirmed live endpoint mapping is added or explicit env endpoints are provided.

## Network Smoke Result

Network smoke not executed.

Reason:

- This task explicitly prohibited `-RunNetwork`.
- The script defaults to dry-run.
- Real network smoke requires `-RunNetwork`, one explicit `-Capability`, valid configuration, non-sensitive inputs, timeout selection, and explicit user authorization.

## Setup Help Result

Executed:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -PrintSetupHelp
```

Result expected: setup help lists env names, placeholders, and `-TimeoutSeconds`; it does not print local values.

## Security Notes

- `config.local.json` was read only during the explicit `-UseLocalConfig` explanation command required by this task.
- No config value, base URL value, credential value, cookie, token, or auth value was printed or written.
- The local AAR presence was checked only by path existence; content was not read.
- No network request was sent.
- `.codex_work` output is local-only and not intended for Git.
- Excluded capabilities remain outside this smoke workflow.

## Next Recommended Smoke Order

After explicit user authorization and capability-specific endpoint/auth configuration, run one capability at a time:

1. `OCR`
2. `QUERY_REWRITE`
3. `TEXT_SIMILARITY`
4. `TRANSLATION`, after endpoint mapping is confirmed or env endpoint is provided
5. `TTS`, after endpoint mapping is confirmed or env endpoint is provided
6. `FUNCTION_CALLING`, after endpoint mapping is confirmed or env endpoint is provided
7. `EMBEDDING`
8. `ASR_LONG`, only after a non-sensitive test audio file is prepared

## 2026-06-17 Schema v1 Update

This task upgraded the dry-run/config diagnosis path for Official Provider Config Schema v1.

Executed without network:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig -UseLocalConfig
```

Observed value-free status with the real local config opt-in:

- `config.local.json exists`: true
- `local config read`: true
- detected group: `topLevel.bluelm`
- `officialProviders exists`: false
- official provider groups: none
- no local value was printed
- no request was sent

`topLevel.bluelm` still does not map OCR, Query Rewrite, Text Similarity, Embedding, Translation, TTS, Function Calling, or ASR Long to `READY`.

Schema v1 behavior validated with a temporary fake config:

- `officialProviders.ocr` with endpoint/auth field presence maps OCR to `READY`
- `officialProviders.queryRewrite` with endpoint/auth field presence maps Query Rewrite to `READY`
- fake URL/auth values were not printed
- no network request was sent

Real network smoke remains intentionally unexecuted in this task.
