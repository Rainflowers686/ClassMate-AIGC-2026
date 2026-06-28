# Official Capability Evidence Matrix v2 (media + on-device round)

Date: 2026-06-28. Source of truth: `docs/current/official_docs_strict_alignment_report.md` (vivo AIGC doc alignment) + the downloaded `D:\Edge Download\AIGC\OfficialDemos\` (read-only; never committed). No credentials, URLs, or demo source are copied here.

## Evidence matrix

| 能力 | docId | 文档证据 (protocol) | demo 证据 | 现有 Provider transport 可复刻? | 需 AAR/新增依赖? | 本轮真实接入? | 阻塞点 |
|---|---|---|---|---|---|---|---|
| 蓝心大模型 (cloud) | 1745 | HTTP `POST /v1/chat/completions` | llm-demo.apk | 是 (HttpURLConnection) | 否 | 已接入 (BlueLMProvider / ProviderAskChatClient) | — |
| 实时短语音识别 | 1738 | **WebSocket** | 实时短语音识别python接入demo (websocket-client) | **否** (repo 仅 HTTP transport) | 需 WS 客户端依赖 | 否 → 用系统实时转写 | 无 WS transport；加 WS 依赖属"改 Gradle 依赖" |
| 长语句听写 | 1740 | **WebSocket** | 长语句听写python接入demo (websocket-client) | 否 | 需 WS 依赖 | 否 | 同上 |
| 长语音转写 | 1739 | **HTTP 任务流** `/lasr/create,upload,run,progress,result` | 长语音转写python接入demo (requests, wav/pcm/m4a/mp3/aac/ogg) | **是** | 否 | **已接入 seam** (`VivoAsrProvider`, config-gated, 经 `CaptureGateway.transcribeAudio`) | 仅缺真实凭据做 net smoke (blocker: 需 AppKey) |
| 方言自由说 | 2065 | WebSocket | 方言自由说python接入demo | 否 | 需 WS 依赖 | 否 | 同 1738 |
| 同声传译 | 2068 | WebSocket (ASR+翻译+TTS 流) | 同声传译python接入demo | 否 | 需 WS 依赖 | 否 (实验入口) | 同 1738 |
| TTS 音频生成 | 1735 | **wss:// WebSocket** | (无独立 demo) | 否 | 需 WS 依赖 | 否 → **用系统 Android TextToSpeech 真实生成音频文件** | 官方 TTS 是 WS；系统 TTS 已真实落地 |
| 文本审核 | 1804 | Android 端侧 SDK | aisdk-cms-local-1.0.0.0.aar + TextModerationDemo-vivo | 否 | 需 AAR | 否 (seam) | 需 AAR，本轮禁改 app/libs |
| 视频内嵌字幕 | — | 官方无此 API | — | N/A (Android MediaExtractor) | 否 | **真实尝试** (`VideoSubtitleExtractor` 读文本轨)，无轨诚实 unsupported | 多数视频无文本轨 |
| 端侧 3B 大模型 | 1802 | **Android SDK/AAR** 本地模型 | llm-sdk-release.aar + llm/ + llm-demo.apk | 否 (本地推理) | 需 AAR (本地 gitignored, 已存在) | 已接入 (反射 bridge, 设备就绪时可用) | 见 P0-6 |
| 蓝心生成变式题 | 1745 | HTTP (复用大模型) | — | 是 | 否 | **已接入** (BlueLM→解析→质量门→练习) | — |

## 结论 (本轮可真实接入的边界)

- **HTTP 类官方能力可用现有 transport 复刻**：大模型(1745)✅、长语音转写(1739)✅seam、OCR/检索/相似度/嵌入(已 PASS)。
- **WebSocket 类官方能力 (1738/1740/2065/2068/1735)** 当前 repo 无 WebSocket transport；接入需引入 WS 客户端依赖 (如 OkHttp WebSocket)，属"改 Gradle 依赖"，本轮按禁区**不接**，诚实标 seam/deferred。系统 `SpeechRecognizer` (实时转写) + Android `TextToSpeech` (音频文件) 作为**真实系统服务**承接，文案标"系统实时转写""系统 TTS"，**绝不写"官方 ASR/蓝心 TTS"**。
- **AAR 类 (1804 文本审核)** 本轮禁改 `app/libs`，标 seam。
- **端侧 3B (1802)** AAR 本地存在(gitignored)，反射 bridge 已接；非内置设备见 P0-6。

## P0-6 — 非内置端侧模型的设备 (云真机 / 其他手机) 方法

**问题**：端侧 3B 依赖本地 AAR + 模型文件 + 受支持设备 (arm64-v8a, minSdk28)。云真机 / 其他手机可能没有模型文件或 SDK，无法用端侧。

**方法 (已是架构既定，本轮强化诚实状态 + 防回归)**：

1. **端侧是次要能力，云端蓝心是主链路**。统一路由 `Cloud-first → On-device → Local-rule` (`AiCapabilityRouter`)：所有设备先走云端蓝心 (HTTP，所有设备可用，含云真机)，端侧仅作离线/隐私增强。
2. **缺 AAR 也能编译运行**：`app/build.gradle.kts` 的 `hasOnDeviceSdk` 标志——AAR 缺失时正常构建，反射 bridge 返回 unavailable，自动落云端/本地，**绝不崩溃、绝不阻断**。
3. **诚实就绪态**：端侧未就绪时 UI 显示"端侧模型未就绪——已自动使用云端蓝心/本地整理；无需端侧也能用"，**不写"端侧已就绪"**。设置·能力中心给授权/模型路径/重检诊断。
4. **云端也不可用时**：自动进入"本地基础整理 (LOCAL_RULE)"——真实知识点/题目/证据，非空占位。
5. **断网/无任何模型**：最终落"本地基础整理 / 安全占位"，诚实分级。

→ 净结论：**任何设备 (云真机、无端侧手机) 都可用完整学习闭环**，端侧只是加分项，其缺失被路由透明降级，状态对用户诚实。
