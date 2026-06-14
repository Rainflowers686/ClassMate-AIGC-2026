# Stage 8A 端侧 BlueLM 3B 接入架构

本文定义 ClassMate Stage 8A 端侧 BlueLM 3B 的工程边界、验收路径和安全策略。当前材料是架构与 QA 规划，不代表端侧 SDK 已经接通或真机验证已经通过。

## 1. 为什么 Stage 8A 优先端侧模型

- 复赛版已经有 Official BlueLM 云端主路径，但弱网、断网、隐私敏感学习场景需要本地智能兜底。
- 规则兜底只能保证可用，难以给出高质量解释、报告建议和练习提示。
- vivo 端侧模型能力符合比赛生态方向，适合成为决赛亮点。
- Stage 8A 应先从诊断、小输入、兜底建议做起，避免一次性改动核心长文本分析链路。

## 2. 云端 BlueLM 与端侧 3B 的区别

| 项目 | Official BlueLM 云端 | OnDevice BlueLM 3B |
|---|---|---|
| 调用方式 | HTTPS 文本生成接口 | Android 本地 SDK |
| 典型入口 | qwen3.5-plus 云端主链路 | `LlmManager` / `LlmConfig` |
| 凭据 | 运行态密钥配置 | 本地 SDK、模型目录、设备能力 |
| 默认职责 | 参赛合规主路径 | 弱网、本地隐私和小任务智能兜底 |
| 风险 | 网络、限流、配置错误 | SDK 文件、模型目录、native、权限、设备兼容 |

两者不能混写：云端是 HTTP API，端侧是 Android SDK。端侧不可用时必须显示 unavailable 或明确错误，不能伪装为已接通。

## 3. 推荐 Provider 链

复赛默认链路：

```text
Official BlueLM cloud -> OnDeviceBlueLM -> LocalRule
```

本地兜底链路：

```text
LocalProviderChain: OnDeviceBlueLM -> LocalRule
```

要求：

- Official BlueLM 仍是复赛主路径。
- OnDeviceBlueLM 是智能兜底，不替代云端主路径。
- LocalRule 是最后安全兜底，不包装成模型成功。
- provider path 必须可见，例如 `official_bluelm`、`ondevice_bluelm_3b`、`local_rule`。

## 4. 端侧 SDK 形态

预期 SDK 形态：

- AAR：`llm-sdk-release.aar`
- 核心类：`LlmManager`、`LlmConfig`
- 模型目录：`/sdcard/1225`
- 常见方法：`init`、`generate`、`interrupt`、`release`
- 回调：增量 token、完成状态、错误码

实现要求：

- 初始化和生成必须在后台线程，不能阻塞 UI。
- SDK 缺失、模型目录缺失、设备不支持时返回 unavailable。
- native library 加载失败必须映射为短错误标签。
- 不记录完整模型输入、完整模型输出、供应商原始响应或内部推理字段。

## 5. 首批适合接入的功能

| 功能 | 适合程度 | Stage 8A 建议 |
|---|---|---|
| Settings 诊断 | 高 | 首先验证 SDK、模型目录、init、generate、release |
| Ask This Lesson 本地兜底 | 高 | 小输入、证据约束、适合端侧输出 |
| StudyReport 摘要和下一步 | 中 | 只处理结构化结果，不导出模型原始交互 |
| Review / Practice 建议 | 中 | 给出解释和练习建议，不替代学习状态规则 |
| CourseAnalyzer 本地模式 | 低 | 长文本和严格 schema 风险高，后续再做 |

## 6. 任务 Profile

| Profile | 用途 | 策略 |
|---|---|---|
| `ANALYSIS` | 课堂结构化分析 | 优先云端，端侧暂不作为默认长文本主路径 |
| `ASK` | 问这节课 | 证据约束，缺证据时拒绝或降级 |
| `REPORT` | 学习报告润色 | 只处理已验证结构化数据 |
| `PRACTICE` | 练习解释 | 生成建议，不改学习状态 |
| `FALLBACK` | 兜底 | 短输出、低风险、清楚标注 provider path |

## 7. 权限与 native 风险

- `/sdcard/1225` 是当前端侧模型目录假设，目录不存在时只显示 unavailable。
- `MANAGE_EXTERNAL_STORAGE` 风险较高，若生产实现必须使用，应在复赛材料中解释用途和边界。
- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` 不应为了导出或普通资料流随意新增。
- APU/native library 可能依赖设备型号、ABI、系统权限和 SDK 包版本，必须真机 smoke。
- 如果需要 `mediatek.permission.ACCESS_APU_SYS`，必须明确该权限只服务端侧 SDK，不扩大数据访问范围。

## 8. 不允许做的事

- 不伪造 SDK 已接通。
- 不绕过 ResultValidator、EvidenceValidator 或 EvidenceResolver。
- 不把端侧输出直接写入最终结果。
- 不记录完整模型输入、完整模型输出、供应商原始响应或内部推理字段。
- 不在导出、截图、日志、Git、StudyReport 中出现真实密钥。
- 不把 Compatible Demo 或外部模型增强描述成复赛官方主路径。

## 9. 后续文本审核 SDK

端侧文本审核适合接 Export / Share / AI 输出安全链。Stage 8A 仅规划验收边界，不写生产接入：

```text
CmsLocalFrame.init(context, listener)
TextModeration(text, callback, timeout)
```

文本审核只作为安全增强，不替代证据校验和学习结果校验。
