# ClassMate v0.3.5 — Real-Model Probe

> 当前版本：**v0.3.5_real_model_probe**，在 v0.3 主流程骨架基础上接入了
> 可用的真实 HTTP 调用链与可校验的兜底策略。
> 目标：把 ClassMate 从 “DemoProvider 主流程可运行” 推进到
> **“真实模型调用链可接入、可验证、可留证据”**。
>
> 仍 **不实现**：登录、社区、小 V、原子通知、负一屏、穿戴、长期画像、
> 真实课堂录音、BlueLM 真实接入（缺官方契约）、UI 美化。

---

## 1. 项目简介

ClassMate 是面向课程内容的证据链式讲解与微测复习助手。v0.3.5 在 v0.3 基础上
新增：

- `core/network/` — `HttpEngine` 抽象 + `SimpleHttpEngine`（JDK
  `HttpURLConnection` 实现）；Provider 业务逻辑只依赖接口，可替换；
- `core/adapter/CompatibleProvider` — 真实 HTTP 调用，针对任何
  OpenAI-compatible chat/completions 端点；
- `core/adapter/JsonExtractor` — 处理纯 JSON / ```json 代码块 / 含散文响应；
- `core/adapter/ModelCallException` — 6 种 reason，驱动 ViewModel 兜底决策；
- `core/adapter/ProviderConfig` — 分 `compatible` / `bluelm` 两段配置；
- 闭合校验链 `JsonExtractor → deserialize → ResultValidator →
  EvidenceValidator → RedactedLogger → UI state`；
- AnalyzeScreen 显示 **当前 Provider / 是否使用兜底 / 校验状态 /
  证据命中率**；
- `RedactedLogger` 日志新增 `structure_valid`、`fallback_used`、
  `api_key_redacted` 字段；
- `proof/logs_redacted/sample_model_call_redacted.jsonl` — 脱敏日志样例。

---

## 2. 运行环境

| 项 | 版本 |
| --- | --- |
| Android Studio | Hedgehog 2023.1.1 及以上 |
| AGP | 8.2.2 |
| Kotlin | 1.9.22 |
| Compose Compiler | 1.5.8 |
| Compose BOM | 2024.02.00 |
| Compile / Target SDK | 34 |
| Min SDK | 26 |
| JDK | 17 |

---

## 3. Provider 当前状态

| Provider | 状态 | 行为 |
| --- | --- | --- |
| `DemoProvider` | **已接入主流程** | 解析 `assets/demo_output.json` 返回 `CourseAnalysisResult` |
| `CompatibleProvider` | **真实 HTTP 已接线** | 走 `SimpleHttpEngine` → OpenAI-compatible `chat/completions`；失败时由 ViewModel 兜底到 demo |
| `BlueLMProvider` | **占位、安全接线** | `analyzeCourse` 抛 `ModelCallException(PROVIDER_NOT_IMPLEMENTED)`，附说明 “BlueLM provider is not configured or official API contract is missing.” ViewModel 捕获后回退 demo |

⚠ 仓库内 **没有** BlueLM 官方 endpoint、请求体、签名算法。为避免伪造，
`BlueLMProvider` 只接线了 config 读取、`HttpEngine` 依赖、错误分类，**不会**
发出任何网络请求。要走真实 BlueLM，需要先补齐：

- 课堂分析任务对应的 base_url；
- 请求体格式（model 名、temperature、消息体）；
- App Key 签名算法（header 名、被签名 payload、hash）；
- 响应体结构（content 字段路径）。

---

## 4. 配置方式（`config.local.json`）

API Key **不进入代码仓库**。复制根目录 `config.example.json` 为
`config.local.json` 并填入凭据：

```json
{
  "provider": "demo",
  "compatible": {
    "api_base_url": "https://your-compatible-endpoint/v1/chat/completions",
    "api_key": "YOUR_API_KEY",
    "model": "YOUR_MODEL_NAME"
  },
  "bluelm": {
    "api_base_url": "FILL_FROM_OFFICIAL_DOCS",
    "app_id": "YOUR_APP_ID",
    "app_key": "YOUR_APP_KEY",
    "model": "FILL_FROM_OFFICIAL_DOCS"
  }
}
```

- `provider` 取 `demo` / `compatible` / `bluelm`。
- `config.local.json` 已被 `.gitignore` 屏蔽（`*.local.json` 兜底）。
- 设备查找顺序：
  1. `/data/data/com.classmate.app/files/config.local.json`
  2. `assets/config.example.json`（仅占位符，永不含真实 key）
  3. 硬编码 `provider=demo` 兜底
- 推送到设备示例：

  ```bash
  adb push config.local.json /data/data/com.classmate.app/files/
  ```

⚠ 任何形式的 key、App Key、Authorization 完整头都 **禁止** 写入代码、
assets、commit message、日志、issue。

---

## 5. 运行流程

### 5.1 跑 Demo 路径（无需 key）

1. 打开 [ClassMate/](.) 根目录，等待 Gradle 同步；
2. 默认 `provider=demo`，直接 Run；
3. **HomeScreen → CourseInputScreen → HotwordScreen → AnalyzeScreen →
   TimelineScreen → QuizScreen → ReviewPlanScreen** 走通；
4. AnalyzeScreen 上的 Provider 卡显示
   `当前 Provider: demo / 是否使用兜底: 否 / 校验状态: 通过 / 证据命中率: 100%`。

### 5.2 跑 CompatibleProvider 真实调用

1. 在 `config.local.json` 中：
   - 把 `provider` 改为 `"compatible"`；
   - 填入 `compatible.api_base_url` / `api_key` / `model`（任何 OpenAI-compatible
     提供商均可：DashScope、DeepSeek、SiliconFlow、Together、本地 vLLM 等）；
2. 推送到设备 `/data/data/com.classmate.app/files/config.local.json`；
3. 启动应用，进入 AnalyzeScreen 点 “调用 compatible 分析”；
4. 成功时 Provider 卡显示 `当前 Provider: compatible / 是否使用兜底: 否`，
   `证据命中率` 取决于模型遵守 verbatim 引用规则的程度；
5. 任何异常（HTTP_ERROR / DESERIALIZE_FAILED / VALIDATION_FAILED 等）
   都会自动回退到 demo，并把 Provider 卡的 `是否使用兜底` 标红，
   `Provider note:` 显示 reason。

### 5.3 检查 fallback

UI 直接显示，但要看完整日志：

```bash
adb logcat -s ClassMateLog
```

每次分析会输出一条 JSON Line，例如：

```json
{"timestamp":"2026-05-27T10:18:11+08:00","provider":"compatible","task":"course_analysis","input_segment_count":3,"hotword_count":5,"success":true,"latency_ms":3842,"structure_valid":true,"evidence_match_rate":0.83,"fallback_used":false,"error_type":null,"api_key_redacted":true}
```

`fallback_used=true` 时 `error_type` 会带上 `ModelCallException.Reason` 与
裁剪过的 message。

---

## 6. 校验与日志（闭合链）

```text
raw model output
  │
  ├─► JsonExtractor.extract            (剥 ```json 围栏 + 平衡 brace 扫描)
  │
  ├─► Json.decodeFromString            (CourseAnalysisResult)
  │
  ├─► ResultValidator                  (引用闭合 / 范围 / answer_index 边界)
  │     └─► 致命问题 → ModelCallException(VALIDATION_FAILED)
  │
  ├─► EvidenceValidator                (schemaPassed + evidenceMatchRate)
  │     └─► matchRate < 1.0 不抛错，记录日志 + UI 降级高亮
  │
  ├─► RedactedLogger                   (ModelCallLog 闭合字段集)
  │
  └─► UI state                         (activeProvider / fallbackUsed /
                                        structureValid / evidenceMatchRate)
```

### 6.1 日志脱敏

字段集合由 [core/logging/ModelCallLog.kt](core/src/main/kotlin/com/classmate/core/logging/ModelCallLog.kt)
锁死；`RedactedLogger` 只会序列化这个 data class 的字段。**严禁** 出现：

- 真实 API Key
- 真实 App KEY
- 完整 Authorization 头
- 未授权课堂文本
- 隐私信息

`api_key_redacted` 字段恒为 `true`，作为审计 grep 锚点。

样例日志：[proof/logs_redacted/sample_model_call_redacted.jsonl](proof/logs_redacted/sample_model_call_redacted.jsonl)
（标记 `"_sample": true`；非真实 call，仅用于展示字段形态）。

---

## 7. 目录结构（v0.3.5 增量）

```text
ClassMate/
├── core/src/main/kotlin/com/classmate/core/
│   ├── adapter/
│   │   ├── ModelProvider.kt
│   │   ├── DemoProvider.kt
│   │   ├── CompatibleProvider.kt        ← 真实 HTTP
│   │   ├── BlueLMProvider.kt            ← 安全占位
│   │   ├── ProviderConfig.kt            ← compatible/bluelm 分段
│   │   ├── PromptBuilder.kt
│   │   ├── JsonExtractor.kt             ← NEW
│   │   └── ModelCallException.kt        ← NEW (6 种 reason)
│   ├── network/                         ← NEW 包
│   │   ├── HttpEngine.kt
│   │   ├── HttpRequest.kt
│   │   ├── HttpResponse.kt
│   │   ├── HttpError.kt
│   │   └── SimpleHttpEngine.kt
│   ├── evidence/
│   ├── logging/
│   ├── model/
│   ├── segmenter/
│   └── validation/
├── app/src/main/
│   ├── AndroidManifest.xml              ← +INTERNET permission
│   ├── assets/config.example.json       ← 新版结构（占位符）
│   └── java/com/classmate/app/
│       ├── data/ApiConfigRepository.kt  ← 支持嵌套 compatible/bluelm
│       ├── state/ClassMateUiState.kt    ← +activeProvider/fallbackUsed/structureValid
│       ├── state/ClassMateViewModel.kt  ← 兜底策略
│       └── ui/AnalyzeScreen.kt          ← ProviderStatusCard
├── config.example.json                  ← 新版结构（占位符）
├── proof/logs_redacted/
│   ├── README.md
│   └── sample_model_call_redacted.jsonl ← NEW
└── README.md                            ← 你正在看
```

---

## 8. 能力边界

### 8.1 已实现

- 6 个主流程页面 + 首页；
- `Segmenter` 真实规则分段；
- `DemoProvider`（主流程）；
- `CompatibleProvider`（真实 HTTP，OpenAI-compatible）；
- `JsonExtractor` 处理 fenced / 散文包裹响应；
- `ResultValidator` + `EvidenceValidator` 双层校验；
- ViewModel 兜底策略（compatible/bluelm 失败 → demo，错误分类入日志）；
- AnalyzeScreen 显示 Provider / fallback / 校验 / 命中率；
- `RedactedLogger` 闭合字段集英文 JSON Line；
- 脱敏日志样例文件。

### 8.2 占位 / 未实现

- `BlueLMProvider` —— 缺官方契约，不发请求；
- 复习计划仍取自模型输出，**未做** 基于本地 quiz state 重新生成；
- JSON Schema 文件存在但 **运行时未做完整 schema 校验**，仅依赖
  `ResultValidator` + `EvidenceValidator` 双层校验；
- HTTP 没有重试 / 指数退避（一次失败直接兜底）。

### 8.3 显式禁止 / 永不在本工程实现

- 登录 / 注册 / 社区；
- 小 V 真实唤起、原子通知真实接入、负一屏真实接入；
- 课中实时录音；
- 端侧大模型部署；
- 穿戴；
- 长期学习画像；
- 未授权使用任何第三方大模型 key；
- 任何形式的 “已完整接入蓝心大模型” / “学习效率提升 XX%” / “课堂实时录音已实现” 文案。

---

## 9. 命名约定

- Kotlin 字段一律 camelCase（`sourceSegmentId`、`evidenceSpan`、`reviewPlan` …）
- JSON 字段保留 snake_case，由 `@SerialName` / 显式映射桥接
- UI 不直接处理原始 JSON 字符串，所有数据先反序列化到 data class
- 真实密钥永远不进入仓库、日志、commit、issue

---

## 10. 下一步任务清单

见 [docs/v0.3-tasklist.md](docs/v0.3-tasklist.md)。
