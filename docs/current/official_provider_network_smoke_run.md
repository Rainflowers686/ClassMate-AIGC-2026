# Official Provider Network Smoke Run

## Date / Branch / Commit

- Date: 2026-06-16
- Branch: `feature/product-review-compatible`
- Commit: `d00bbdf`
- Scope: Official provider smoke workflow v1

## Smoke Script Version

- Script: `scripts/qa/official_provider_smoke.ps1`
- Version intent: controlled provider smoke v1
- Default mode: dry-run
- Network mode: requires explicit `-RunNetwork`
- Output directory: `.codex_work/official_provider_smoke/`

## DryRun Result

Dry-run was executed with:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -DryRun
```

Result: PASS.

Generated local-only files:

- `.codex_work/official_provider_smoke/smoke_result.json`
- `.codex_work/official_provider_smoke/smoke_result.md`
- `.codex_work/official_provider_smoke/smoke.log`
- `.codex_work/official_provider_smoke/test_inputs/`
- `.codex_work/official_provider_smoke/outputs/`

## Network Smoke Result

Network smoke not executed.

Reason:

- The current task did not include explicit authorization to send real provider requests.
- The script defaults to dry-run.
- Real network smoke requires `-RunNetwork`, one explicit `-Capability`, valid environment configuration, and non-sensitive test inputs.

## Capability Table

| Capability | Mode | Status | Request sent | Notes |
|---|---|---|---:|---|
| OCR | DRY_RUN | DRY_RUN_READY | false | Non-sensitive generated image prepared. |
| QUERY_REWRITE | DRY_RUN | DRY_RUN_READY | false | Non-sensitive physics question prepared. |
| TEXT_SIMILARITY | DRY_RUN | DRY_RUN_READY | false | Three synthetic course snippets prepared. |
| TRANSLATION | DRY_RUN | DRY_RUN_READY | false | Short English course sentence prepared. |
| TTS | DRY_RUN | DRY_RUN_READY | false | Short Chinese course-audio test sentence prepared. |
| FUNCTION_CALLING | DRY_RUN | DRY_RUN_READY | false | Internal tool-call test payload prepared. |
| EMBEDDING | DRY_RUN | DRY_RUN_READY | false | Synthetic classroom text prepared. |
| ASR_LONG | Not run | Not in AllSafe | false | Requires explicit capability and non-sensitive audio. |
| IMAGE_GEN / VIDEO_GEN / SHORT_ASR / DIALECT_ASR / SIMULTANEOUS_INTERPRETATION | Not run | Dev-lab only | false | Not part of default safe smoke. |

## ConfigMissing Items

None in dry-run mode. Configuration content was not read, so actual network configuration remains unknown.

If `-RunNetwork` is used without endpoint/auth environment values, the script records `SKIPPED_CONFIG_MISSING` rather than failing the whole smoke run.

## Passed Items

- Smoke script exists and defaults to dry-run.
- Dry-run created result JSON, Markdown, log, test input, and output directories.
- Safe capability list excludes long audio and dev-lab capabilities by default.
- No request was sent.

## Failed Items

- None in dry-run mode.

## Skipped Items

- Real network provider calls: not authorized for this run.
- Long audio smoke: no non-sensitive test audio was provided.
- Dev-lab capabilities: intentionally excluded from `-AllSafe`.

## Inputs Used

All inputs are synthetic and stored under `.codex_work/official_provider_smoke/test_inputs/`:

- `ocr_smoke.png`: generated white-background OCR image with simple test text.
- `query_rewrite.txt`: non-sensitive physics query.
- `similarity.json`: synthetic course snippets.
- `translation_en.txt`: short English learning sentence.
- `tts_zh.txt`: short Chinese course-audio test sentence.
- `function_calling.json`: internal tool-call parse payload.
- `embedding.json`: synthetic evidence texts.

## Outputs Generated

- `smoke_result.json`
- `smoke_result.md`
- `smoke.log`
- `test_inputs/*`
- Empty `outputs/` directory for future network response artifacts.

These outputs stay under `.codex_work/` and are not intended for Git.

## Security Notes

- `config.local.json` presence was checked only by `Test-Path`; content was not read.
- The local AAR presence was checked only by `Test-Path`; content was not read.
- No credential, app identifier, auth header value, cookie, or token is written to the result files.
- Real network smoke must use synthetic inputs only.
- The default safe list does not include long audio, generated media, real-time speech, dialect speech, or interpretation capabilities.
- Excluded official capabilities remain outside this smoke workflow.

## No-secret Confirmation

Confirmed for this dry-run:

- No network request was sent.
- No private config content was read.
- No AAR content was read.
- `.codex_work` output is untracked.

## Next Recommended Smoke Order

Run one capability at a time after explicit authorization:

1. `scripts\qa\official_provider_smoke.ps1 -RunNetwork -Capability OCR`
2. `scripts\qa\official_provider_smoke.ps1 -RunNetwork -Capability QUERY_REWRITE`
3. `scripts\qa\official_provider_smoke.ps1 -RunNetwork -Capability TEXT_SIMILARITY`
4. `scripts\qa\official_provider_smoke.ps1 -RunNetwork -Capability TRANSLATION`
5. `scripts\qa\official_provider_smoke.ps1 -RunNetwork -Capability TTS`
6. `scripts\qa\official_provider_smoke.ps1 -RunNetwork -Capability FUNCTION_CALLING`
7. `scripts\qa\official_provider_smoke.ps1 -RunNetwork -Capability EMBEDDING`
8. `scripts\qa\official_provider_smoke.ps1 -RunNetwork -Capability ASR_LONG` only after a non-sensitive test audio file is prepared.
