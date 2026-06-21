# Official Provider Config Template

鏈枃浠跺彧鎻愪緵 `config.local.json` 鐨勬湰鍦板～鍐欐ā鏉裤€備笉瑕佹妸鐪熷疄 endpoint銆乤ppId銆乤ppKey銆乼oken銆丄uthorization 鎴?cookie 鍐欏叆鏈枃妗ｏ紝涔熶笉瑕佹彁浜?`config.local.json`銆?
## Usage

1. 鍙湪鏈満缂栬緫 `config.local.json`銆?2. 灏嗕笅闈㈡ā鏉夸腑鐨?`<...>` 鍗犱綅绗︽浛鎹负 vivo 瀹樻柟鎺у埗鍙版垨瀹樻柟鏂囨。纭鐨勭湡瀹炲€笺€?3. 涓嶈鎶?key 鍙戠粰浠讳綍 AI銆佽亰澶╁伐鍏枫€乮ssue銆丳R銆佹埅鍥炬垨鏂囨。銆?4. 濉畬鍚庡厛杩愯鍙璇婃柇锛?
```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig -UseLocalConfig
```

5. 鍙湁鍦ㄨ瘖鏂樉绀虹洰鏍?capability 鐨?`endpointMapping=READY`銆乣authMapping=READY`銆乣requestSchema=READY` 鍚庯紝鎵嶈€冭檻鍗曡兘鍔涚湡瀹?smoke銆傜湡瀹?smoke 闇€瑕佺敤鎴峰啀娆℃槑纭巿鏉冦€?
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
| `officialProviders` | Yes | 涓撶敤瀹樻柟鑳藉姏閰嶇疆鏍硅妭鐐广€備笉瑕佺敤 `topLevel.bluelm` 閰?OCR銆丄SR銆丷etrieval銆乀TS 鎴?Translation銆?|
| `<capability>.enabled` | Yes | 鏈湴鍚敤璇?capability 鐨?smoke 閰嶇疆銆傚缓璁厛鍙墦寮€涓€涓兘鍔涖€?|
| `<capability>.baseUrl` | Yes | 瀹樻柟鑳藉姏鐨?base URL銆傚彧濉湪 `config.local.json`锛屼笉瑕佸啓杩?Git 鏂囨。銆?|
| `<capability>.endpointPath` | Recommended | 瀹樻柟鑳藉姏 endpoint path銆侽CR銆丵uery Rewrite銆乀ext Similarity銆丒mbedding銆乀ranslation銆乀TS銆丗unction Calling 寤鸿鏄惧紡濉啓銆侫SR Long 鏄换鍔℃祦锛屽悗缁彲鎸夊畼鏂?task-flow 瀛楁鎵╁睍銆?|
| `<capability>.authHeader` | Yes | 閴存潈 header 鍚嶏紝閫氬父鏄?`Authorization`銆?|
| `<capability>.authValue` | Yes | 閴存潈鍊兼垨 token銆備笉瑕佹彁浜ゃ€佹埅鍥炬垨鍙戠粰 AI銆?|

## Recommended First Capabilities

褰撳墠宸插畬鎴愮湡瀹?network smoke 鐨?product-facing provider锛?
- `ocr`: `PASS`
- `textSimilarity`: `PASS`
- `embedding`: `PASS`

褰撳墠涓嶅缓璁户缁湪 L3 readiness 涓荤嚎閲屾墿灞曟柊鑳藉姏锛?
- `queryRewrite`: configured `READY` 涓旂湡瀹?network smoke `PASS`锛涙鍓?blocked 鏍瑰洜涓?smoke 璇锋眰浣?schema mismatch锛屽凡鎸夊畼鏂?docId 2061 `prompts` schema 淇銆備骇鍝佷粛淇濈暀 qwen3.5-plus rewrite / local safe rewrite / direct retrieval fallback銆?
鍚庣疆鎴栧崟鐙獙璇侊細

- `translation`
- `tts`
- `functionCalling`
- `asrLong`

`asrLong` 鍚庣疆锛屽洜涓哄畠闇€瑕侀潪鏁忔劅娴嬭瘯闊抽銆佷笂浼?杞浠诲姟娴佸拰鏇撮暱 timeout銆?
## Smoke Order

鎺ㄨ崘姣忔鍙窇涓€涓?capability锛?
```powershell
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -ExplainConfig -UseLocalConfig
powershell -ExecutionPolicy Bypass -File scripts\qa\official_provider_smoke.ps1 -RunNetwork -Capability OCR -UseLocalConfig -TimeoutSeconds 20
```

褰撳墠鐪熷疄 smoke 缁撹锛?
| Provider | Config status | Live smoke status |
|---|---|---|
| OCR | `READY` | `PASS` |
| QUERY_REWRITE | `READY` | `PASS` |
| TEXT_SIMILARITY | `READY` | `PASS` |
| EMBEDDING | `READY` | `PASS` |

涓嬩竴涓荤嚎鏄?App-level L3 鐪熸満闂幆楠岃瘉銆傝嫢缁х画 provider smoke锛屽繀椤婚€愰」鏄惧紡閰嶇疆銆佹樉寮忔巿鏉冦€佸崟 capability 杩愯锛屽苟鍏堢‘璁?`-ExplainConfig -UseLocalConfig` 涓?READY銆?
不要默认运行 `-AllSafe -RunNetwork`。默认只验证 ClassMate 当前 18 项有效能力中的明确目标能力；实验性媒体、实时语音和未配置能力必须单项授权后再验证。
## Safety Rules

- `config.local.json` 涓嶅叆搴撱€?- `.codex_work/official_provider_smoke/` 涓嶅叆搴撱€?- 涓嶆妸 endpoint/key 鍐欒繘 docs銆?- 涓嶆妸 key 鍙戠粰浠讳綍 AI銆?- 涓嶄娇鐢ㄧ湡瀹炶鍫傚綍闊炽€佺敤鎴风収鐗囨垨闅愮鏂囨湰鍋?smoke 杈撳叆銆?- `topLevel.bluelm` 鍙厤缃簯绔ぇ妯″瀷锛屼笉閰嶇疆瀹樻柟 OCR/ASR/Retrieval/TTS 绛変笓鐢?provider銆?
