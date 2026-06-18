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

## 2026-06-17 OCR Smoke URL Composition Finding

After local `officialProviders.ocr` was configured, value-free explain mode reported:

- `OCR endpointMapping=READY`
- `OCR authMapping=READY`
- `OCR requestSchema=READY`
- `requestSent=False`

A later explicitly authorized OCR network smoke failed before a valid provider request could be sent. The old result classified it as `FAIL_NETWORK`, but the root cause was an invalid URI generated by the smoke URL composition step. The bad shape was a host followed directly by `=classmate-smoke`; no endpoint value, key value, or authorization value is recorded here.

Correct interpretation:

- The failure was not evidence that the OCR endpoint or local key was rejected.
- The request should be considered not sent because URI binding failed before `Invoke-WebRequest` could make a valid request.
- v5 fixes this by safe-joining `baseUrl` and `endpointPath`, appending smoke trace data as query parameters, validating with `System.Uri.TryCreate`, and returning `FAIL_INVALID_URI` with `requestSent=False` for invalid composed URLs.

Next safe order:

1. Run `-ExplainConfig -UseLocalConfig`.
2. Confirm OCR remains `endpointMapping=READY`, `authMapping=READY`, and `requestSchema=READY`.
3. Only after explicit authorization, rerun OCR network smoke with `-TimeoutSeconds`.

## 2026-06-17 OCR HTTP 404 Diagnosis

A later explicitly authorized OCR smoke produced:

- status: `FAIL_HTTP_404`
- `requestSent=True`
- `requestAttempted=True`
- `uriValidated=True`
- config source: `LOCAL_CONFIG_OFFICIAL_PROVIDER`
- mapping source: `LOCAL_CONFIG_OFFICIAL_PROVIDER`
- endpoint mapping/auth mapping/request schema: `READY`
- secret leaked: no

Doc 1737 alignment:

- public path suffix: `general_recognition`

## 2026-06-18 Query Rewrite Smoke Hang Finding

Observed operator report:

- `QUERY_REWRITE` was `READY` in `-ExplainConfig -UseLocalConfig`
- network smoke was explicitly invoked with `-RunNetwork`
- the terminal appeared to hang before printing the harness header
- no `smoke_result.md` / `smoke_result.json` was produced before Ctrl+C

Root cause in the harness:

- The header was printed after all capability execution, so a blocked request hid startup status.
- Result files were written after all capability execution, so a blocked request left no partial result.
- The direct HTTP call could still stall before `-TimeoutSeconds` produced a usable result file.

Fix recorded:

- Header output now happens before capability execution.
- Network mode writes a pre-request `RUNNING` result.
- All live requests use the shared hard-timeout wrapper.
- Timeout is recorded as `FAIL_TIMEOUT` with sanitized status and no endpoint/key values.

No network request was run as part of this documentation update.

## 2026-06-18 Query Rewrite Timeout Finalization v3

Follow-up observation:

- The harness printed its header and wrote `smoke_result.md` / `smoke_result.json`.
- After Ctrl+C, the latest result still showed `QUERY_REWRITE` as `RUNNING`.
- This means the pre-request partial result worked, but final timeout overwrite did not complete.

Root cause in the previous hardening:

- The HTTP request still lived behind PowerShell job primitives.
- In the affected environment, the parent script did not reliably regain control after `TimeoutSeconds`.
- Because the parent did not regain control, it could not overwrite `RUNNING` with `FAIL_TIMEOUT`.

v3 fix recorded:

- The live request now runs in a separate PowerShell child process.
- The parent script polls the child process every 200 ms.
- When elapsed time reaches `TimeoutSeconds`, the parent force-stops the child process.
- The parent then writes a final `FAIL_TIMEOUT` result over the pre-request `RUNNING` result.
- If the child exits without a parseable result, the final status is `FAIL_NETWORK_CHILD_NO_RESULT`.

No network request was run as part of this fix.

Offline self-test result:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -SelfTestTimeout -TimeoutSeconds 3
```

Observed:

- mode: `SELF_TEST`
- capability: `TIMEOUT_SELF_TEST`
- status: `FAIL_TIMEOUT`
- elapsed: 4 seconds
- request sent: false
- local config read: false
- final result did not remain `RUNNING`

This validates the timeout finalization path without touching the real Query Rewrite endpoint.
- method: `POST`
- content type: `application/x-www-form-urlencoded`
- payload kind: form body with base64 `image`, `pos`, and `businessid`
- query key: `requestId`

Root cause found in the smoke harness:

- Query separator detection used PowerShell wildcard matching with `?`.
- In PowerShell wildcard patterns, `?` means any single character.
- The harness therefore appended `&requestId=classmate-smoke` to URLs that did not yet have a query string.
- That made the endpoint route shape wrong while still producing a syntactically valid URI.

v5 diagnosis/fix:

- Query separator detection now checks for a literal `?`.
- Sanitized endpoint shape is included in smoke results without full URL, host value, auth value, or key.
- HTTP 404 is classified as `FAIL_HTTP_404_ENDPOINT_SUSPECT`, meaning route/endpoint mapping should be checked first. It is not proof that OCR auth failed.

## 2026-06-17 OCR Provider Network Smoke PASS

Source: local smoke result under `.codex_work/official_provider_smoke/ocr_20260617_224223/` (local-only, not tracked).

Result summary:

- capability: `OCR`
- docId: `1737`
- tier: `product-facing`
- network executed: true
- mode: `NETWORK`
- status: `PASS`
- sanitized status: `HTTP 200`
- request sent: true
- request attempted: true
- URI validated: true
- method: `POST`
- content type: `application/x-www-form-urlencoded`
- payload kind: `FORM`
- path last segment: `general_recognition`
- query keys: `requestId`
- config source: `LOCAL_CONFIG_OFFICIAL_PROVIDER`
- mapping source: `LOCAL_CONFIG_OFFICIAL_PROVIDER`
- provider path source: `CONFIG`
- endpoint mapping: `READY`
- auth mapping: `READY`
- request schema: `READY`
- missing env/config fields: none
- secret leaked: no

This PASS verifies the previous OCR 404 was resolved by the v5 URL composition fix. The smoke harness now composes the OCR route with the correct public path suffix and appends `requestId` as a query key rather than as part of the path. OCR is now the first real network PASS among the official product-facing provider smoke targets.

## 2026-06-18 Query Rewrite Live Smoke Blocked

Current Query Rewrite status:

- configured: `READY`
- `endpointMapping`: `READY`
- `authMapping`: `READY`
- `requestSchema`: `READY`
- live smoke: `BLOCKED`
- latest observed live-smoke symptom: final local result remains `RUNNING`
- request shape: `POST`, `application/json`, `GENERIC_JSON`, path last segment `query_rewrite_base`
- secret leaked: no

Interpretation:

- This is not evidence that the official Query Rewrite provider is unavailable.
- It is evidence that the current smoke/runtime live path for Query Rewrite is blocked in this local environment.
- Query Rewrite is an enhancement provider; it is not a P0/P1/P2/L3 product blocker.
- ClassMate can continue to use cloud large model rewriting through qwen3.5-plus when cloud model access is available.
- If Query Rewrite is unavailable or blocked, product retrieval should fall back to local safe rewrite or direct local evidence retrieval.

Decision:

- Do not continue spending the current validation pass on Query Rewrite live smoke.
- Keep the local config for future diagnosis.
- Move the next official provider smoke work to Text Similarity and Embedding.

Next recommended official provider smoke order:

1. `TEXT_SIMILARITY`
2. `EMBEDDING`

Before each real network smoke, add the corresponding `officialProviders.<capability>` local config group, run `-ExplainConfig -UseLocalConfig`, and confirm `endpointMapping=READY`, `authMapping=READY`, and `requestSchema=READY`. Do not reuse `topLevel.bluelm` as a specialized provider endpoint.

## 2026-06-18 Text Similarity Provider Network Smoke PASS

Source: local smoke result under `.codex_work/official_provider_smoke/text_similarity_20260618_123444/` (local-only, not tracked).

Result summary:

- capability: `TEXT_SIMILARITY`
- tier: `product-facing`
- network executed: true
- mode: `NETWORK`
- status: `PASS`
- request sent: true
- request attempted: true
- URI validated: true
- method: `POST`
- content type: `application/json`
- payload kind: `GENERIC_JSON`
- path last segment: `rerank`
- query keys: `requestId`
- config source: `LOCAL_CONFIG_OFFICIAL_PROVIDER`
- mapping source: `LOCAL_CONFIG_OFFICIAL_PROVIDER`
- provider path source: `CONFIG`
- endpoint mapping: `READY`
- auth mapping: `READY`
- request schema: `READY`
- missing env/config fields: none
- secret leaked: no

This PASS makes Text Similarity the second real network PASS among the official product-facing provider smoke targets, after OCR.

Query Rewrite remains:

- configured: `READY`
- live smoke: `BLOCKED`
- blocker scope: not a product blocker; the product can continue through qwen3.5-plus query rewrite when available, or local safe rewrite/direct retrieval fallback otherwise.

Next recommended official provider smoke target:

1. `EMBEDDING`

Before running Embedding network smoke, add `officialProviders.embedding`, run `-ExplainConfig -UseLocalConfig`, and confirm `endpointMapping=READY`, `authMapping=READY`, and `requestSchema=READY`.
