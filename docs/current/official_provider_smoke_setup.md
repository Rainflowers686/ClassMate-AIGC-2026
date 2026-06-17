# Official Provider Smoke Setup v4

This note explains how to run the official provider smoke harness safely. v4 is deliberately conservative: generic cloud model config can describe the large-model path, but it cannot make OCR, ASR, retrieval, translation, TTS, or function-calling smoke `READY`.

## Default Rule

Default mode is dry-run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -DryRun
```

Default mode:

- does not read `config.local.json`
- does not read the local AAR
- does not send network requests
- writes only local smoke output under `.codex_work/official_provider_smoke/`

## v4 Usage

Setup help:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -PrintSetupHelp
```

Value-free config explanation without local config read:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig
```

Value-free config explanation with explicit local-config opt-in:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig -UseLocalConfig
```

Real network smoke is explicit, single-capability, and timeout-bounded:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -RunNetwork -Capability OCR -UseLocalConfig -TimeoutSeconds 20
```

Do not run `-RunNetwork` unless the user explicitly authorizes a real provider request.

## Why Config Is Not Read by Default

`config.local.json` can contain local credentials. The script checks only file existence unless `-UseLocalConfig` is passed. With `-UseLocalConfig`, it reads structure and field presence only. It must not print or write credential values, base URL values, auth header values, cookie values, or tokens.

Allowed value-free group names:

- `vivoCapture`
- `providers`
- `providers.bluelm`
- `providers.qwen`
- `topLevel.bluelm`
- `officialProviders`
- `officialProviders.vivoCapture`

## Conservative Mapping Policy

Generic cloud model config means only this:

- cloud text generation may be configured
- qwen / BlueLM-compatible path may exist

Generic cloud model config does not mean:

- OCR endpoint is configured
- ASR endpoint is configured
- query rewrite endpoint is configured
- similarity endpoint is configured
- embedding endpoint is configured
- translation endpoint is configured
- TTS websocket endpoint is configured
- function calling live endpoint is configured

## Env Override

Environment variables are still the clearest way to run one capability smoke:

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

The script output uses only variable names and `<your-value>` placeholders.

## Mapping States

`endpointMappingStatus`:

- `READY`: endpoint path can be derived from explicit env or capability-specific local config.
- `MISSING`: no reliable endpoint mapping is available.
- `SEAM_ONLY`: a project seam exists, but the live endpoint mapping is not confirmed.

`authMappingStatus`:

- `READY`: auth source is available from env or capability-specific local-config opt-in.
- `MISSING`: auth fields are absent or not readable under current options.

`requestSchemaStatus`:

- `READY`: request body shape is known enough for controlled smoke.
- `MISSING`: request body shape is not ready.
- `GENERIC_ONLY`: a generic seam exists, but confirmed live provider schema is not mapped.

`mappingSource`:

- `ENV_EXPLICIT`
- `LOCAL_CONFIG_VIVO_CAPTURE`
- `PROVIDER_CODE_DEFAULT`
- `NONE`

`LOCAL_CONFIG_BLUELM` and `LOCAL_CONFIG_QWEN` are intentionally not valid mapping sources for specialized provider smoke.

## Capability Mapping Notes

| Capability | v4 mapping behavior | Next action |
|---|---|---|
| OCR | `READY` only with explicit OCR env endpoint/auth or capture-specific local config. Generic BlueLM/qwen config is not enough. | First real network smoke candidate after endpoint is confirmed. |
| ASR_LONG | `READY` only with capture-specific config and task-flow paths; requires non-sensitive test audio. | Run late; do not use private classroom recordings. |
| QUERY_REWRITE | `MISSING` unless explicit endpoint/auth is supplied or a future retrieval-specific local config is added. | Review official `query_rewrite_base` schema before network smoke. |
| TEXT_SIMILARITY | `MISSING` unless explicit endpoint/auth is supplied or a future retrieval-specific local config is added. | Review official `/rerank` endpoint before network smoke. |
| EMBEDDING | `MISSING` unless explicit endpoint/auth is supplied or a future retrieval-specific local config is added. | Parser seam is safe; no vector DB is involved. |
| TRANSLATION | `SEAM_ONLY` without explicit live endpoint mapping. | Add provider mapping or provide explicit env endpoint. |
| TTS | `SEAM_ONLY` without explicit live websocket mapping. | Keep course essence script-only fallback. |
| FUNCTION_CALLING | `SEAM_ONLY`; internal function router remains source of truth. | Add official cloud tool adapter only when request schema is confirmed. |

## ConfigMissing vs EndpointMappingMissing

- `ConfigMissing`: endpoint may be known, but auth or required local fields are absent.
- `EndpointMappingMissing`: the script cannot derive a reliable live endpoint.
- `SeamReadyButEndpointMappingMissing`: a project seam exists, but no confirmed provider request mapping exists.
- `RequestSchemaMissing`: endpoint may exist, but request body is not ready.
- `FAIL_TIMEOUT`: request exceeded `-TimeoutSeconds`.
- `FAIL_HTTP_404_ENDPOINT_SUSPECT`: HTTP 404 from a suspect generic/provider-default mapping.

## Recommended Real Smoke Order

1. OCR
2. QUERY_REWRITE
3. TEXT_SIMILARITY
4. TRANSLATION
5. TTS
6. FUNCTION_CALLING
7. EMBEDDING
8. ASR_LONG

`ASR_LONG` is later because it requires a non-sensitive audio file and task-flow polling.

## Excluded Capabilities

- Voice clone / sound cloning: excluded. Do not prepare smoke, entry, or task.
- LBS / POI / geocoding: excluded. Do not prepare smoke, entry, or task.

These excluded capabilities must not appear in `-AllSafe` or as product-facing smoke targets.

## Security Notes

- Do not commit `.codex_work`.
- Do not print or screenshot local config values.
- Do not use private user photos, classroom recordings, or personal data as smoke inputs.
- Long audio smoke requires synthetic, non-sensitive test audio.
- Dev-lab capabilities are not included in `-AllSafe`.
- No network request is sent without `-RunNetwork`.
