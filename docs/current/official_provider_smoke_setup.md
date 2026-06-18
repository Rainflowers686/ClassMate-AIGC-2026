# Official Provider Smoke Setup v5

This note explains how to run the official provider smoke harness safely. v5 is deliberately conservative: generic cloud model config can describe the large-model path, but it cannot make OCR, ASR, retrieval, translation, TTS, or function-calling smoke `READY`. v5 also validates composed URLs before any network request is sent.

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

## v5 Usage

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

In network mode, the harness prints its header before capability execution, creates the output directory before the request, and writes an initial `RUNNING` result before the provider call starts. This prevents a provider hang from leaving the operator without `smoke_result.md` / `smoke_result.json`.

Offline timeout self-test:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -SelfTestTimeout -OutputDir .codex_work\official_provider_smoke\timeout_selftest -TimeoutSeconds 3
```

This mode does not read local config values and does not send a network request. It starts a child process that sleeps for 60 seconds, verifies that the parent kills it after `TimeoutSeconds`, and finalizes the result as `FAIL_TIMEOUT`.

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
- `officialProviders.ocr`
- `officialProviders.queryRewrite`
- `officialProviders.textSimilarity`
- `officialProviders.embedding`
- `officialProviders.translation`
- `officialProviders.tts`
- `officialProviders.functionCalling`
- `officialProviders.asrLong`

## Official Provider Config Schema v1

Specialized official capabilities use dedicated groups under `officialProviders`. These groups are only read when `-UseLocalConfig` is explicit, and only field presence is reported. A copy/paste-safe template lives in `docs/current/official_provider_config_template.md`; keep real values only in local `config.local.json`.

Use placeholders in docs/examples only:

```json
{
  "officialProviders": {
    "ocr": { "enabled": true, "baseUrl": "<your-value>", "endpointPath": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },
    "queryRewrite": { "enabled": true, "baseUrl": "<your-value>", "endpointPath": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },
    "textSimilarity": { "enabled": true, "baseUrl": "<your-value>", "endpointPath": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },
    "embedding": { "enabled": true, "baseUrl": "<your-value>", "endpointPath": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },
    "translation": { "enabled": true, "baseUrl": "<your-value>", "endpointPath": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },
    "tts": { "enabled": true, "baseUrl": "<your-value>", "endpointPath": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },
    "functionCalling": { "enabled": true, "baseUrl": "<your-value>", "endpointPath": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" },
    "asrLong": { "enabled": true, "baseUrl": "<your-value>", "authHeader": "Authorization", "authValue": "<your-value>" }
  }
}
```

If a capability-specific group has `enabled=true`, endpoint field presence, and auth field presence, `-ExplainConfig -UseLocalConfig` can mark that capability as `READY`. It still does not send a request unless `-RunNetwork` is also passed.

Minimum fields for first smoke:

- `enabled=true`
- `baseUrl`
- `endpointPath` when the official path is not already encoded by the smoke harness
- `authHeader`
- `authValue`

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

| Capability | v5 mapping behavior | Next action |
|---|---|---|
| OCR | `READY` only with explicit OCR env endpoint/auth or capture-specific local config. Generic BlueLM/qwen config is not enough. | First real network smoke candidate after endpoint is confirmed. |
| ASR_LONG | `READY` only with capture-specific config and task-flow paths; requires non-sensitive test audio. | Run late; do not use private classroom recordings. |
| QUERY_REWRITE | `MISSING` unless explicit endpoint/auth is supplied or a future retrieval-specific local config is added. | Review official `query_rewrite_base` schema before network smoke. |
| TEXT_SIMILARITY | `MISSING` unless explicit endpoint/auth is supplied or a future retrieval-specific local config is added. | Review official `/rerank` endpoint before network smoke. |
| EMBEDDING | `MISSING` unless explicit endpoint/auth is supplied or a future retrieval-specific local config is added. | Parser seam is safe; no vector DB is involved. |
| TRANSLATION | `SEAM_ONLY` without explicit live endpoint mapping. | Add provider mapping or provide explicit env endpoint. |
| TTS | `SEAM_ONLY` without explicit live websocket mapping. | Keep course essence script-only fallback. |
| FUNCTION_CALLING | `SEAM_ONLY`; internal function router remains source of truth. | Add official cloud tool adapter only when request schema is confirmed. |

With Schema v1, `officialProviders.<capability>` can also make the corresponding capability `READY` when the group has endpoint and auth field presence. This is preferred over reusing `topLevel.bluelm`.

## ConfigMissing vs EndpointMappingMissing

- `ConfigMissing`: endpoint may be known, but auth or required local fields are absent.
- `EndpointMappingMissing`: the script cannot derive a reliable live endpoint.
- `SeamReadyButEndpointMappingMissing`: a project seam exists, but no confirmed provider request mapping exists.
- `RequestSchemaMissing`: endpoint may exist, but request body is not ready.
- `FAIL_INVALID_URI`: base URL plus endpoint path did not form a valid HTTP(S) URI. The request is treated as not sent.
- `FAIL_TIMEOUT`: request exceeded `-TimeoutSeconds`. Network calls run through the shared parent-process hard-timeout wrapper, so a hung provider call must still write a sanitized timeout result.
- `FAIL_HTTP_404_ENDPOINT_SUSPECT`: HTTP 404 after a request was attempted. Check route, path, method, and endpoint shape first; 404 is not proof of auth failure.

## URL Composition Rules

The smoke harness composes endpoint URLs in two steps:

1. Normalize `baseUrl` by trimming a trailing slash and adding an `https://` scheme only when no scheme is present.
2. Normalize `endpointPath` by preserving a leading `/`, adding one when missing, and preserving an existing query string.

Smoke-only trace fields such as `requestId=classmate-smoke` are appended as query parameters with `?` or `&`. They must never be concatenated directly after the host. After composition, the script validates the final URL with `System.Uri.TryCreate`. Invalid URI results are reported as `FAIL_INVALID_URI` with `requestSent=False`.

`requestSent=True` now means the child process entered the provider request path. The following cases keep `requestSent=False`: dry-run, config missing, endpoint mapping missing, seam-only, request schema missing, missing input, invalid URI, and the pre-request `RUNNING` partial result.

## Hard Timeout Behavior

All live capability requests use the shared `Invoke-SmokeHttpRequestWithTimeout` path. The wrapper disables PowerShell progress output, launches a separate PowerShell child process for the HTTP call, redirects stdout/stderr to files, polls the child every 200 ms, waits no longer than `-TimeoutSeconds`, and force-stops the child process on timeout. The parent never uses `Start-Process -Wait`, `Wait-Process`, `Wait-Job`, `Receive-Job`, or synchronous stream `ReadToEnd`.

Expected timeout result:

- `status=FAIL_TIMEOUT`
- `requestAttempted=True`
- `requestSent=True`
- `uriValidated=True`
- `sanitizedError=Timed out after configured timeout`

`DryRun` and `ExplainConfig` never enter this network path.

If a child process exits without a parseable result file, the final status is `FAIL_NETWORK_CHILD_NO_RESULT`. A normal completed run must not leave the final result as `RUNNING`; `RUNNING` is only the pre-request partial result used to make Ctrl+C interruptions diagnosable.

Smoke results include only sanitized endpoint shape: scheme configured, host configured, path segment count, last path segment, query keys, method, content type, payload kind, and path source. They must not include the full endpoint, auth value, AppKey, cookie, or token.

## How To Interpret `-ExplainConfig -UseLocalConfig`

Run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig -UseLocalConfig
```

Read each target capability:

- `endpointMapping=READY`: the script can form a capability-specific endpoint from explicit env or `officialProviders.<capability>`.
- `authMapping=READY`: an auth field exists, but the value is not printed.
- `requestSchema=READY`: the smoke request shape is mapped.
- `requestSent=False`: explain mode never sends network requests.

If output says `officialProviders missing`, add the `officialProviders` block to local `config.local.json`. If output says `topLevel.bluelm only configures cloud model`, do not copy that value into OCR/Retrieval/TTS by assumption; use official capability endpoint values.

To run only OCR after configuration and explicit authorization:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -RunNetwork -Capability OCR -UseLocalConfig -TimeoutSeconds 20
```

Smoke outputs under `.codex_work/official_provider_smoke/` are local artifacts. Confirm they remain untracked with:

```powershell
git ls-files .codex_work
```

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
