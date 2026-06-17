# Official Provider Smoke Setup

This note explains how to run the official provider smoke harness without leaking local credentials.

## Default Rule

The smoke harness is dry-run by default:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -DryRun
```

It does not read `config.local.json` by default and does not send network requests unless `-RunNetwork` is passed.

## Setup Help

To print the required environment variable names:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -PrintSetupHelp
```

The output uses `<your-value>` placeholders only. It must not print real values.

## Explain Config

To inspect only value-free configuration status:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig
```

This reports:

- whether `config.local.json` exists
- whether local config was read
- whether each capability has a URL mapping
- whether auth is configured
- missing env names
- endpoint/request schema status

Without `-UseLocalConfig`, `local config read` must remain `False`.

## Environment Variable Mode

Each capability can be configured with:

```powershell
$env:CLASSMATE_PROVIDER_SMOKE_<CAPABILITY>_URL="<your-value>"
$env:CLASSMATE_PROVIDER_SMOKE_<CAPABILITY>_AUTH_VALUE="<your-value>"
```

Optional shared variables:

```powershell
$env:CLASSMATE_PROVIDER_SMOKE_AUTH_HEADER="<your-value>"
$env:CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE="<your-value>"
$env:CLASSMATE_PROVIDER_SMOKE_ALLOW_NO_AUTH="<your-value>"
```

Do not paste real values into docs, commits, issue comments, or screenshots.

## Local Config Opt-in

Only this form may read `config.local.json`:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -UseLocalConfig -ExplainConfig
```

or, after explicit network authorization:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -UseLocalConfig -RunNetwork -Capability OCR
```

The script maps only value presence from:

- `vivoCapture.appId`
- `vivoCapture.appKey`
- `vivoCapture.baseUrl`
- `providers.bluelm.appId`
- `providers.bluelm.appKey`
- `providers.bluelm.baseUrl`

It keeps values in memory only and does not write them to result files.

## Capability Mapping Status

The v2 result separates several states:

- `DRY_RUN_READY`: no request sent; smoke inputs and config explanation are ready.
- `SKIPPED_CONFIG_MISSING`: auth or required config is absent.
- `SKIPPED_ENDPOINT_MAPPING_MISSING`: no reliable endpoint mapping is available.
- `SKIPPED_SEAM_ONLY`: a provider seam exists, but the live endpoint mapping is not confirmed.
- `PASS`: a network request was sent and returned a successful response.
- `FAIL`: a network request was sent and failed; the error is sanitized.

Additional fields:

- `configSource`: `ENV`, `LOCAL_CONFIG_OPT_IN`, or `NONE`
- `localConfigRead`: true only when `-UseLocalConfig` is passed
- `endpointMappingStatus`: `READY`, `MISSING`, or `SEAM_ONLY`
- `authMappingStatus`: `READY` or `MISSING`
- `requestSchemaStatus`: `READY`, `MISSING`, or `GENERIC_ONLY`

## Safe Capability Order

`-AllSafe` covers:

1. OCR
2. Query rewrite
3. Text similarity
4. Translation
5. TTS
6. Function calling
7. Embedding

Long audio and dev-lab capabilities are not included in `-AllSafe`.

## Security Notes

- Do not commit `.codex_work`.
- Do not print or screenshot local config values.
- Do not use private user photos, classroom recordings, or personal data as smoke inputs.
- Long audio smoke requires a synthetic, non-sensitive test audio file.
- Excluded official capabilities remain outside this smoke workflow.
