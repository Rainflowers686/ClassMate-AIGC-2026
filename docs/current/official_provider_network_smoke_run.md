# Official Provider Network Smoke Run

## Date / Branch / Commit

- Date: 2026-06-17
- Branch: `feature/product-review-compatible`
- Commit at start: `767cc0b`
- Scope: Official provider smoke workflow v3 mapping diagnostics

## Smoke Script Version

- Script: `scripts/qa/official_provider_smoke.ps1`
- Version intent: provider-aware endpoint mapping diagnostics v3
- Default mode: dry-run
- Network mode: requires explicit `-RunNetwork`
- Local config mode: requires explicit `-UseLocalConfig`
- Output directory: `.codex_work/official_provider_smoke/`

## DryRun Result

Dry-run was executed with:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -DryRun
```

Result: PASS.

Dry-run request summary:

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

## Explain Config Without Local Read

Executed:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig
```

Observed value-free status:

- `config.local.json exists`: true
- `local config read`: false
- detected local groups: none, because local config was not read
- no request was sent

| Capability | Endpoint mapping | Auth mapping | Request schema | Mapping source |
|---|---|---|---|---|
| OCR | READY | MISSING | READY | PROVIDER_CODE_DEFAULT |
| QUERY_REWRITE | READY | MISSING | READY | PROVIDER_CODE_DEFAULT |
| TEXT_SIMILARITY | READY | MISSING | READY | PROVIDER_CODE_DEFAULT |
| TRANSLATION | SEAM_ONLY | MISSING | GENERIC_ONLY | NONE |
| TTS | SEAM_ONLY | MISSING | GENERIC_ONLY | NONE |
| FUNCTION_CALLING | SEAM_ONLY | MISSING | GENERIC_ONLY | NONE |
| EMBEDDING | READY | MISSING | READY | PROVIDER_CODE_DEFAULT |

## Explain Config With Local Read

Executed only because this v3 diagnostic task explicitly required:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig -UseLocalConfig
```

Observed value-free status:

- `config.local.json exists`: true
- `local config read`: true
- detected group: `topLevel.bluelm`
- no local value was printed
- no request was sent

| Capability | Endpoint mapping | Auth mapping | Request schema | Config source | Mapping source | Request sent |
|---|---|---|---|---|---|---:|
| OCR | READY | READY | READY | LOCAL_CONFIG_BLUELM | LOCAL_CONFIG_BLUELM | false |
| QUERY_REWRITE | READY | READY | READY | LOCAL_CONFIG_BLUELM | LOCAL_CONFIG_BLUELM | false |
| TEXT_SIMILARITY | READY | READY | READY | LOCAL_CONFIG_BLUELM | LOCAL_CONFIG_BLUELM | false |
| TRANSLATION | SEAM_ONLY | READY | GENERIC_ONLY | LOCAL_CONFIG_BLUELM | NONE | false |
| TTS | SEAM_ONLY | READY | GENERIC_ONLY | LOCAL_CONFIG_BLUELM | NONE | false |
| FUNCTION_CALLING | SEAM_ONLY | READY | GENERIC_ONLY | LOCAL_CONFIG_BLUELM | NONE | false |
| EMBEDDING | READY | READY | READY | LOCAL_CONFIG_BLUELM | LOCAL_CONFIG_BLUELM | false |

Important correction from v2:

- OCR is no longer `EndpointMappingMissing` under local-config opt-in.
- OCR is `READY / READY / READY` from top-level BlueLM-compatible local config plus provider-code endpoint mapping.
- Translation, TTS, and Function calling remain seam-only until a confirmed live endpoint mapping is added or explicit env endpoints are provided.

## Network Smoke Result

Network smoke not executed.

Reason:

- This task explicitly prohibited `-RunNetwork`.
- The script defaults to dry-run.
- Real network smoke requires `-RunNetwork`, one explicit `-Capability`, valid configuration, non-sensitive inputs, and explicit user authorization.

## Setup Help Result

Executed:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -PrintSetupHelp
```

Result: PASS.

The output lists only variable names and `<your-value>` placeholders. It does not print local values.

## Security Notes

- `config.local.json` was read only during the explicit `-UseLocalConfig` explanation command required by this task.
- No config value, base URL value, credential value, cookie, token, or auth value was printed or written.
- The local AAR presence was checked only by path existence; content was not read.
- No network request was sent.
- `.codex_work` output is local-only and not intended for Git.
- Excluded capabilities remain outside this smoke workflow.

## Next Recommended Smoke Order

After explicit user authorization, run one capability at a time:

1. `OCR`
2. `QUERY_REWRITE`
3. `TEXT_SIMILARITY`
4. `TRANSLATION`, after endpoint mapping is confirmed or env endpoint is provided
5. `TTS`, after endpoint mapping is confirmed or env endpoint is provided
6. `FUNCTION_CALLING`, after endpoint mapping is confirmed or env endpoint is provided
7. `EMBEDDING`
8. `ASR_LONG`, only after a non-sensitive test audio file is prepared
