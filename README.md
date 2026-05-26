# ClassMate v0.2.5 — Technical Probe

> 本仓库是 ClassMate v0.2 工程规格书的 **v0.2.5 技术探针版**。
> 目的：先把骨架立起来，验证模型适配层、Schema、Demo 回退、证据校验通路，再进入 v0.3 主流程。
> 本探针 **不实现** 完整 APP，**不调用** 真实模型，**不携带** 任何真实 API Key。

---

## 1. 项目简介

ClassMate 是面向课程内容的证据链式讲解与微测复习助手。本探针版只完成：

1. Android 工程骨架（Gradle + Kotlin + Compose）；
2. `ModelProvider` 抽象 + 三种占位实现：`BlueLMProvider`、`CompatibleProvider`、`DemoProvider`；
3. 本地配置读取（不硬编码 API Key）；
4. `course_analysis_result.schema.json`；
5. `demo_input.json` / `demo_output.json`；
6. `EvidenceValidator`：校验 `source_segment_id` / `related_kp_id` / `evidence_span`；
7. 首页：点击按钮加载 demo 输出并展示一个最小知识点列表；
8. 英文日志、Demo 回退、Schema 校验、证据校验链路打通。

未实现 / 显式禁止：登录、社区、小 V、原子通知、负一屏、穿戴、长期学习画像、真实端侧大模型部署。

---

## 2. 运行环境

| 项 | 版本 |
| --- | --- |
| Android Studio | Hedgehog 2023.1.1 及以上 |
| AGP | 8.2.x |
| Kotlin | 1.9.22 |
| Compile / Target SDK | 34 |
| Min SDK | 26 |
| JDK | 17 |

---

## 3. 配置方式

### 3.1 API Key 配置

API Key **不进入代码仓库**。在工程根目录复制 `config.example.json` 为 `config.local.json`，并填入真实凭据：

```bash
cp config.example.json config.local.json
```

```json
{
  "provider": "demo",
  "api_base_url": "https://example.api.endpoint",
  "app_id": "YOUR_APP_ID",
  "app_key": "YOUR_APP_KEY"
}
```

- `provider` 取值：`demo` / `bluelm` / `compatible`
- v0.2.5 探针默认走 `demo`，不会发起任何真实网络请求。
- `config.local.json` 已在 `.gitignore` 中忽略。

### 3.2 运行方式

1. Android Studio 打开 `ClassMate/` 根目录；
2. 等待 Gradle 同步（首次同步会自动生成 `gradle/wrapper/gradle-wrapper.jar` 与 `gradlew` / `gradlew.bat`，仓库中故意不携带二进制 wrapper jar）；
3. 选择 `app` 配置，连接模拟器或 vivo 真机；
4. 点击 Run。

首页展示标题与一个按钮 **"加载 Demo 输出"**。点击后：

- 从 `assets/demo_output.json` 读取课程分析结果；
- 经 `EvidenceValidator` 校验；
- 展示知识点列表（名称 / 重要性 / 难度 / 来源段 ID）；
- 同时打印一条英文 redacted 日志到 Logcat（tag `ClassMateLog`）。

---

## 4. 使用 demo 数据复现演示流程

不需要任何凭据：

1. 保持 `config.local.json` 缺省或 `provider=demo`；
2. 启动 APP；
3. 点击 "加载 Demo 输出"；
4. 屏幕展示由 `assets/demo_output.json` 解析得到的知识点列表；
5. Logcat 输出一条 `task=course_analysis provider=demo success=true schema_valid=true evidence_match_rate=...` 的日志。

Demo 输出明确标注来自 `DemoProvider`，**不伪装为真实模型调用**。

---

## 5. 目录结构

```text
ClassMate/
├── app/                              Android 应用模块（UI / Activity / 资源 / assets）
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/classmate/app/
│       │   ├── MainActivity.kt
│       │   ├── ui/HomeScreen.kt
│       │   ├── ui/theme/Theme.kt
│       │   ├── data/ApiConfigRepository.kt
│       │   └── data/DemoInputRepository.kt
│       ├── res/                      字符串 / 主题
│       └── assets/
│           ├── demo_input.json
│           ├── demo_output.json
│           ├── schema/course_analysis_result.schema.json
│           └── prompts/
├── core/                             纯 Kotlin 库模块（无 Android 依赖）
│   ├── build.gradle.kts
│   └── src/main/java/com/classmate/core/
│       ├── model/                    CourseAnalysisInput / Result 等数据类
│       ├── adapter/                  ModelProvider 接口 + 3 个占位实现
│       ├── evidence/EvidenceValidator.kt
│       ├── logging/RedactedLogger.kt
│       └── segmenter/Segmenter.kt
├── examples/                         独立于 app assets 的样例数据（方便仓库浏览）
│   ├── demo_course.txt
│   ├── demo_hotwords.json
│   ├── demo_input.json
│   └── demo_output.json
├── prompts/                          Prompt 文档（仓库可见副本）
│   ├── 00_system_rules.md
│   └── 01_course_analysis.md
├── schema/
│   └── course_analysis_result.schema.json
├── proof/
│   └── logs_redacted/                空目录，v0.3 起填入 redacted 调用日志
├── config.example.json
├── .gitignore
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── gradle/libs.versions.toml
```

注：`schema/`、`prompts/`、`examples/` 在仓库根目录有一份**可读副本**；app 运行时实际读取 `app/src/main/assets/` 下的同名文件。两者保持一致由人工同步，v0.3 起考虑用 Gradle 任务自动拷贝。

---

## 6. 核心代码说明

### 6.1 ModelProvider 抽象

`core/adapter/ModelProvider.kt` 定义统一入口：

```kotlin
interface ModelProvider {
    val name: String
    suspend fun analyzeCourse(input: CourseAnalysisInput): CourseAnalysisResult
}
```

三种占位实现：

| 实现 | 状态 | 行为 |
| --- | --- | --- |
| `DemoProvider` | 可用 | 直接返回 `demo_output.json` 反序列化得到的 `CourseAnalysisResult` |
| `BlueLMProvider` | 占位 | 抛出 `NotImplementedError("BlueLM provider not wired in v0.2.5 probe")` |
| `CompatibleProvider` | 占位 | 抛出 `NotImplementedError("OpenAI-compatible provider not wired in v0.2.5 probe")` |

v0.3 起为 `BlueLMProvider` / `CompatibleProvider` 接入真实 HTTP 调用。

### 6.2 EvidenceValidator

`core/evidence/EvidenceValidator.kt` 校验：

1. 所有 `knowledge_points[].source_segment_id` 必须命中输入 segments；
2. 所有 `quizzes[].source_segment_id` 必须命中输入 segments；
3. 所有 `quizzes[].related_kp_id` 必须命中知识点；
4. `evidence_span` 是否包含在对应 segment 的 `corrected_text` 中，统计 match rate。

返回 `EvidenceValidationResult`：包含 `schemaPassed`、`missingRefs`、`spanMismatches`、`matchRate`。

### 6.3 RedactedLogger

`core/logging/RedactedLogger.kt` 输出**英文** Logcat 行，结构与规格书第 13.1 节一致。**不打印** API Key / App Key / 用户隐私 / 原始音频。

---

## 7. 日志样例

```text
ClassMateLog: {"timestamp":"2026-05-26T12:00:00+08:00","provider":"demo","task":"course_analysis","input_segment_count":1,"hotword_count":5,"success":true,"latency_ms":3,"schema_valid":true,"evidence_match_rate":1.0,"error_type":null}
```

---

## 8. 已实现 / 模拟展示 / 后续规划 能力边界

### 8.1 已实现（v0.2.5 探针）

- Android 工程骨架与 Compose 首页；
- `ModelProvider` 抽象与三种占位实现；
- 本地配置读取（`config.local.json`）；
- `course_analysis_result.schema.json` v0.1；
- Demo 输入 / 输出样例；
- `EvidenceValidator` 与英文 redacted 日志；
- 首页加载 demo 输出并渲染最小知识点列表；
- Demo 回退路径与失败兜底入口。

### 8.2 v0.3 计划实现

- 课程文本导入页 / 热词输入页 / 分析进度页 / 时间轴页 / 微测页 / 复习计划页；
- `BlueLMProvider` 接入兼容大模型 HTTP 调用；
- `Segmenter` 规则分段（按时间戳 / 自然段 / 标点 + 长度）；
- 课堂状态机与微测答题反馈；
- Prompt 文件加载与 `JsonSchemaValidator` 接入。

### 8.3 模拟展示（不会在 v0.2.5 实现）

- 小 V 唤起、原子通知、负一屏推送 — 仅做概念演示，标注为后续规划。

### 8.4 显式禁止

- 真实 vivo 系统级深度集成；
- 端侧大模型部署；
- 课中实时录音；
- 长期学习画像；
- 任何登录 / 社区 / 穿戴功能。

---

## 9. 下一步进入 v0.3 的任务清单

见 [`docs/v0.3-tasklist.md`](docs/v0.3-tasklist.md)。
