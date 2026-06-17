# Official Provider Smoke Setup v3

This note explains how to run the official provider smoke harness without leaking local credentials. The v3 harness is provider-aware: it can derive endpoint mapping from existing ClassMate provider code and official-doc-derived paths, while still requiring explicit opt-in before it reads local config or sends network requests.

## Default Rule

The smoke harness is dry-run by default:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -DryRun
```

Default mode does not read `config.local.json`, does not read the local AAR, and does not send network requests.

## v3 Usage

Setup help:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -PrintSetupHelp
```

Value-free config explanation without reading local config:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig
```

Value-free config explanation with explicit local-config opt-in:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig -UseLocalConfig
```

Real network smoke is still explicit and single-capability:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -RunNetwork -Capability OCR -UseLocalConfig
```

Do not run `-RunNetwork` unless the user has explicitly authorized a real provider request.

## Why Config Is Not Read by Default

`config.local.json` can contain local credentials. The script only checks whether the file exists unless `-UseLocalConfig` is passed. With `-UseLocalConfig`, the script reads only enough structure to decide field presence and mapping readiness. It never prints or writes credential values, base URL values, auth header values, cookie values, or tokens.

## Local Config Opt-in

When `-UseLocalConfig` is passed, v3 detects these groups by name only:

- `vivoCapture`
- `providers.bluelm`
- `providers.qwen`
- top-level BlueLM-compatible fields
- `officialProviders`
- `officialProviders.vivoCapture`

It maps recognized value presence into smoke status:

- `LOCAL_CONFIG_VIVO_CAPTURE`
- `LOCAL_CONFIG_BLUELM`
- `LOCAL_CONFIG_QWEN`

It may report missing field names such as `appId`, `appKey`, `baseUrl`, or `vivoCapture or providers.bluelm or providers.qwen or top-level BlueLM`. It must not report the corresponding values.

## Env Override

Environment variables are still supported and override local config:

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

- `READY`: endpoint path can be derived from env, provider code, or local-config opt-in.
- `MISSING`: no reliable endpoint mapping is available.
- `SEAM_ONLY`: a provider seam exists, but the live endpoint mapping is not confirmed.

`authMappingStatus`:

- `READY`: auth source is available from env or local-config opt-in.
- `MISSING`: auth fields are absent or not readable under current options.

`requestSchemaStatus`:

- `READY`: request body shape is known enough for a controlled smoke.
- `MISSING`: request body shape is not ready.
- `GENERIC_ONLY`: a generic seam exists, but a confirmed live provider schema is not mapped.

`mappingSource`:

- `ENV_EXPLICIT`
- `LOCAL_CONFIG_VIVO_CAPTURE`
- `LOCAL_CONFIG_BLUELM`
- `LOCAL_CONFIG_QWEN`
- `PROVIDER_CODE_DEFAULT`
- `OFFICIAL_DOC_DEFAULT`
- `NONE`

## Capability Mapping Notes

| Capability | v3 mapping behavior | Next action |
|---|---|---|
| OCR | Uses provider-code path `/ocr/general_recognition`; with local config or env auth it can become `READY`. | First real network smoke candidate. |
| ASR_LONG | Uses provider-code task-flow root `/lasr`; request schema is ready, but non-sensitive test audio is required. | Run after OCR/retrieval smoke and only with synthetic audio. |
| QUERY_REWRITE | Uses provider-code path `/query-rewrite-api/predict`. | Smoke after OCR. |
| TEXT_SIMILARITY | Uses provider-code path `/similarity-model-api/predict`. | Smoke after query rewrite. |
| EMBEDDING | Uses provider-code path `/embedding-model-api/predict/batch`. | Smoke after similarity; no vector DB involved. |
| TRANSLATION | Current project has a translation seam, but no confirmed live endpoint mapping in provider code. | Provide explicit env endpoint or add live provider mapping later. |
| TTS | Current project has course essence audio script/TTS seam, but no confirmed live endpoint mapping in provider code. | Provide explicit env endpoint or add live provider mapping later. |
| FUNCTION_CALLING | Current project has internal function router and official adapter seam, but no confirmed live endpoint mapping. | Keep internal router as source of truth; add official endpoint mapping only when safe. |

## ConfigMissing vs EndpointMappingMissing

- `ConfigMissing`: endpoint mapping may be known, but auth or required local fields are absent.
- `EndpointMappingMissing`: the script cannot derive a reliable live endpoint.
- `SeamReadyButEndpointMappingMissing`: a project seam exists, but no confirmed provider request mapping exists.
- `RequestSchemaMissing`: endpoint may exist, but request body is not ready.

Use `-ExplainConfig -UseLocalConfig` first. If endpoint is `READY` and auth is `READY`, a later explicit `-RunNetwork -Capability <name>` can send a request. If either is missing, do not run network smoke yet.

## Recommended Real Smoke Order

1. OCR
2. QUERY_REWRITE
3. TEXT_SIMILARITY
4. TRANSLATION
5. TTS
6. FUNCTION_CALLING
7. EMBEDDING
8. ASR_LONG

`ASR_LONG` is later because it requires a non-sensitive audio file and task-flow polling. Do not use private classroom recordings for smoke.

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
