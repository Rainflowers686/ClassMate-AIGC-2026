# Official Provider Config Template

本文件只提供 `config.local.json` 的本地填写模板。不要把真实 endpoint、appId、appKey、token、Authorization 或 cookie 写入本文档，也不要提交 `config.local.json`。

## Usage

1. 只在本机编辑 `config.local.json`。
2. 将下面模板中的 `<...>` 占位符替换为 vivo 官方控制台或官方文档确认的真实值。
3. 不要把 key 发给任何 AI、聊天工具、issue、PR、截图或文档。
4. 填完后先运行只读诊断：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig -UseLocalConfig
```

5. 只有在诊断显示目标 capability 的 `endpointMapping=READY`、`authMapping=READY`、`requestSchema=READY` 后，才考虑单能力真实 smoke。真实 smoke 需要用户再次明确授权。

## Template

```json
{
  "officialProviders": {
    "ocr": {
      "enabled": true,
      "baseUrl": "<official-ocr-base-url>",
      "endpointPath": "<official-ocr-endpoint-path>",
      "authHeader": "Authorization",
      "authValue": "<your-auth-value>"
    },
    "queryRewrite": {
      "enabled": true,
      "baseUrl": "<official-query-rewrite-base-url>",
      "endpointPath": "<official-query-rewrite-endpoint-path>",
      "authHeader": "Authorization",
      "authValue": "<your-auth-value>"
    },
    "textSimilarity": {
      "enabled": true,
      "baseUrl": "<official-text-similarity-base-url>",
      "endpointPath": "<official-text-similarity-endpoint-path>",
      "authHeader": "Authorization",
      "authValue": "<your-auth-value>"
    },
    "embedding": {
      "enabled": true,
      "baseUrl": "<official-embedding-base-url>",
      "endpointPath": "<official-embedding-endpoint-path>",
      "authHeader": "Authorization",
      "authValue": "<your-auth-value>"
    },
    "translation": {
      "enabled": true,
      "baseUrl": "<official-translation-base-url>",
      "endpointPath": "<official-translation-endpoint-path>",
      "authHeader": "Authorization",
      "authValue": "<your-auth-value>"
    },
    "tts": {
      "enabled": true,
      "baseUrl": "<official-tts-base-url>",
      "endpointPath": "<official-tts-endpoint-path>",
      "authHeader": "Authorization",
      "authValue": "<your-auth-value>"
    },
    "functionCalling": {
      "enabled": true,
      "baseUrl": "<official-function-calling-base-url>",
      "endpointPath": "<official-function-calling-endpoint-path>",
      "authHeader": "Authorization",
      "authValue": "<your-auth-value>"
    },
    "asrLong": {
      "enabled": true,
      "baseUrl": "<official-asr-base-url>",
      "authHeader": "Authorization",
      "authValue": "<your-auth-value>"
    }
  }
}
```

## Field Notes

| Field | Required | Meaning |
|---|---:|---|
| `officialProviders` | Yes | 专用官方能力配置根节点。不要用 `topLevel.bluelm` 配 OCR、ASR、Retrieval、TTS 或 Translation。 |
| `<capability>.enabled` | Yes | 本地启用该 capability 的 smoke 配置。建议先只打开一个能力。 |
| `<capability>.baseUrl` | Yes | 官方能力的 base URL。只填在 `config.local.json`，不要写进 Git 文档。 |
| `<capability>.endpointPath` | Recommended | 官方能力 endpoint path。OCR、Query Rewrite、Text Similarity、Embedding、Translation、TTS、Function Calling 建议显式填写。ASR Long 是任务流，后续可按官方 task-flow 字段扩展。 |
| `<capability>.authHeader` | Yes | 鉴权 header 名，通常是 `Authorization`。 |
| `<capability>.authValue` | Yes | 鉴权值或 token。不要提交、截图或发给 AI。 |

## Recommended First Capabilities

当前已完成真实 network smoke 的 product-facing provider：

- `ocr`: `PASS`
- `textSimilarity`: `PASS`
- `embedding`: `PASS`

当前不建议继续在 L3 readiness 主线里扩展新能力：

- `queryRewrite`: configured `READY` 且真实 network smoke `PASS`；此前 blocked 根因为 smoke 请求体 schema mismatch，已按官方 docId 2061 `prompts` schema 修复。产品仍保留 qwen3.5-plus rewrite / local safe rewrite / direct retrieval fallback。

后置或单独验证：

- `translation`
- `tts`
- `functionCalling`
- `asrLong`

`asrLong` 后置，因为它需要非敏感测试音频、上传/轮询任务流和更长 timeout。

## Smoke Order

推荐每次只跑一个 capability：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig -UseLocalConfig
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -RunNetwork -Capability OCR -UseLocalConfig -TimeoutSeconds 20
```

当前真实 smoke 结论：

| Provider | Config status | Live smoke status |
|---|---|---|
| OCR | `READY` | `PASS` |
| QUERY_REWRITE | `READY` | `PASS` |
| TEXT_SIMILARITY | `READY` | `PASS` |
| EMBEDDING | `READY` | `PASS` |

下一主线是 App-level L3 真机闭环验证。若继续 provider smoke，必须逐项显式配置、显式授权、单 capability 运行，并先确认 `-ExplainConfig -UseLocalConfig` 为 READY。

不要默认运行 `-AllSafe -RunNetwork`。不要运行声音复刻、LBS、POI、视频生成或实时语音能力。

## Safety Rules

- `config.local.json` 不入库。
- `.codex_work/official_provider_smoke/` 不入库。
- 不把 endpoint/key 写进 docs。
- 不把 key 发给任何 AI。
- 不使用真实课堂录音、用户照片或隐私文本做 smoke 输入。
- `topLevel.bluelm` 只配置云端大模型，不配置官方 OCR/ASR/Retrieval/TTS 等专用 provider。
