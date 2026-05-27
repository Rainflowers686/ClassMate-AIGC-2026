# ClassMate v0.3 — Main-Flow Skeleton

> 本仓库当前版本为 **v0.3 主流程工程骨架**，基于 v0.2.5 技术探针推进得到。
> 目标：跑通 课程文本导入 → 热词 → 分段 → DemoProvider 分析 → 时间轴 → 微测 → 证据回溯 → 复习计划 的端到端单机闭环。
> 仍 **不实现** 登录、社区、小 V、原子通知、负一屏、穿戴、长期画像、真实课堂录音、真实大模型 HTTP 调用。

---

## 1. 项目简介

ClassMate 是面向课程内容的证据链式讲解与微测复习助手。v0.3 的核心交付是：

- 6 个主流程页面 + 首页，全部基于 Jetpack Compose；
- 单一 `ClassMateViewModel` + `ClassMateUiState`，sealed `ClassMateScreen` 驱动导航；
- 真实的规则分段器 `Segmenter`（自然段 → 150-300 字归并）；
- `ModelProvider` 抽象 + 三种实现：`DemoProvider`（已接入主流程）/ `BlueLMProvider`、`CompatibleProvider`（占位，被调用时回退到 demo）；
- `ResultValidator`（轻量 Kotlin 引用 / 范围校验）+ `EvidenceValidator`（证据链命中率）；
- API Key 从 `config.local.json` 本地读取，**不进入仓库**；
- 英文 `RedactedLogger` JSON 单行日志，闭合字段集（`ModelCallLog`）。

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

## 3. 配置方式

### 3.1 API Key

API Key **不进入代码仓库**。复制 `config.example.json` 为 `config.local.json` 并填入凭据：

```json
{
  "provider": "demo",
  "api_base_url": "https://example.api.endpoint",
  "app_id": "YOUR_APP_ID",
  "app_key": "YOUR_APP_KEY"
}
```

- `provider` 可取 `demo` / `bluelm` / `compatible`。
- v0.3 仅 `DemoProvider` 已接入；选 `bluelm` / `compatible` 时会自动回退到 demo 并在 Logcat 提示。
- `config.local.json` 已被 `.gitignore` 屏蔽。
- 设备上的查找顺序：`/data/data/com.classmate.app/files/config.local.json` → `assets/config.example.json` → 硬编码 `provider=demo` 兜底。

### 3.2 运行方式

1. Android Studio 打开 [ClassMate/](.) 根目录；
2. 等待 Gradle 同步（首次会生成 `gradle/wrapper/gradle-wrapper.jar` 与 `gradlew*`，仓库不携带二进制 wrapper jar）；
3. 选 `app` 配置，连模拟器或 vivo 真机；
4. 点击 Run。

---

## 4. 主流程操作指引

进入 APP 后页面顺序固定为：

1. **HomeScreen** — 标题 + 副标 + 两个按钮。
   - **开始 / Start** → CourseInputScreen（空白）；
   - **加载 Demo / Load demo** → 一并填好课程标题、文本、热词，然后跳到 CourseInputScreen。
2. **CourseInputScreen** — 输入 / 粘贴课程标题与文本；按 **加载 demo_course** 可一键填入演示数据；点击 **下一步：热词**。
3. **HotwordScreen** — 输入框 + 添加 / 已有热词 chip 列表，点击 chip 可移除；**下一步：分析**。
4. **AnalyzeScreen** — 显示当前 provider、分段数、热词数。先点 **运行分段** 看 `Segmenter` 结果，再点 **调用 demo 分析** 触发 DemoProvider；分析完会显示一行 `Evidence chain: ...`。
5. **TimelineScreen** — 课程摘要 + 知识点卡片列表（重要性 / 难度 / 来源段 ID / 释义）；点 **查看证据** 在上方面板里高亮 `evidence_span`；**进入微测** 进入下一步。
6. **QuizScreen** — 一次一题，选项 + 提交 + 正确 / 错误反馈 + 证据片段 + **查看依据段落** 高亮源段；**上一题 / 下一题** 切换；底部 **查看复习计划**。
7. **ReviewPlanScreen** — 列出每条复习任务（步骤号、时长、关联知识点、原因），底部可回时间轴或回首页。

错答会把对应 `relatedKpId` 加入 `wrongKnowledgePointIds`；返回时间轴时该卡片的边框会变红。

---

## 5. 目录结构

```text
ClassMate/
├── app/                                  Android 应用模块
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/classmate/app/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── ApiConfigRepository.kt
│       │   │   └── DemoInputRepository.kt
│       │   ├── state/
│       │   │   ├── ClassMateUiState.kt   ← UiState + sealed Screen
│       │   │   └── ClassMateViewModel.kt
│       │   ├── ui/
│       │   │   ├── AppRoot.kt            ← sealed-screen dispatch
│       │   │   ├── HomeScreen.kt
│       │   │   ├── CourseInputScreen.kt
│       │   │   ├── HotwordScreen.kt
│       │   │   ├── AnalyzeScreen.kt
│       │   │   ├── TimelineScreen.kt
│       │   │   ├── QuizScreen.kt
│       │   │   ├── ReviewPlanScreen.kt
│       │   │   ├── components/
│       │   │   │   ├── KnowledgePointCard.kt
│       │   │   │   ├── SegmentCard.kt
│       │   │   │   ├── QuizCard.kt
│       │   │   │   └── ReviewPlanCard.kt
│       │   │   └── theme/Theme.kt
│       ├── res/                          strings / themes / colors / 自适应图标 / 备份规则
│       └── assets/
│           ├── demo_input.json
│           ├── demo_output.json
│           ├── config.example.json
│           ├── schema/course_analysis_result.schema.json
│           └── prompts/{00_system_rules,01_course_analysis}.md
├── core/                                 纯 JVM Kotlin 库模块
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/classmate/core/
│       ├── model/                        CourseAnalysisInput / Result / CourseSegment / KnowledgePoint / Quiz / ReviewPlanItem
│       ├── adapter/                      ModelProvider / DemoProvider / BlueLMProvider / CompatibleProvider / PromptBuilder
│       ├── segmenter/Segmenter.kt        自然段 + 150-300 字归并 + 标点兜底
│       ├── evidence/                     EvidenceValidator + EvidenceValidationResult
│       ├── validation/ResultValidator.kt 引用闭合 + 取值范围 + answer_index 边界
│       └── logging/                      RedactedLogger + ModelCallLog
├── schema/course_analysis_result.schema.json    仓库权威副本
├── prompts/{00_system_rules,01_course_analysis}.md
├── examples/{demo_course.txt,demo_hotwords.json,demo_input.json,demo_output.json}
├── docs/v0.3-tasklist.md                 下一步给 Codex 的清单
├── proof/logs_redacted/README.md         redacted 日志落点（当前为空）
├── config.example.json
├── .gitignore
├── settings.gradle.kts / build.gradle.kts / gradle.properties / gradle/libs.versions.toml
└── README.md
```

---

## 6. 命名约定（v0.3 起统一）

- Kotlin 字段一律 camelCase（`sourceSegmentId`、`evidenceSpan`、`reviewPlan` …）
- JSON 字段保留 snake_case，由 `@SerialName` 显式映射
- UI 不直接处理原始 JSON 字符串，所有数据先反序列化到 data class

---

## 7. 当前 Provider 状态

| Provider | v0.3 状态 | 调用结果 |
| --- | --- | --- |
| `DemoProvider` | **已接入主流程** | 解析 `assets/demo_output.json` 返回 `CourseAnalysisResult` |
| `BlueLMProvider` | **占位** | `analyzeCourse` 抛 `NotImplementedError`；ViewModel 捕获并回退到 demo |
| `CompatibleProvider` | **占位** | 同上 |

切换：编辑 `config.local.json` 的 `provider` 字段。v0.3 仍只能跑 demo 路径；BlueLM / Compatible 的 HTTP 接线放在 v0.3.5+。

---

## 8. 日志样例

```text
ClassMateLog: {"timestamp":"2026-05-26T12:00:00+08:00","provider":"demo","task":"course_analysis","input_segment_count":1,"hotword_count":5,"success":true,"latency_ms":3,"schema_valid":true,"evidence_match_rate":1.0,"error_type":null}
```

字段集合由 [`core/logging/ModelCallLog.kt`](core/src/main/kotlin/com/classmate/core/logging/ModelCallLog.kt) 锁死；新增字段需改这个 data class。

---

## 9. 能力边界

### 9.1 已实现

- 6 个主流程页面 + 首页，单 Activity + Compose；
- `Segmenter` 实际规则分段；
- `DemoProvider` 主流程；
- `ResultValidator`（引用闭合 / 范围）；
- `EvidenceValidator`（命中率 / 失败降级）；
- 证据回溯 UI（点击知识点 / 题目 → 高亮源段）；
- 错题回标（错题对应 `relatedKpId` 在时间轴上红边框）；
- 本次会话复习计划展示；
- 失败兜底（任何异常不崩 APP，写入英文 redacted 日志）。

### 9.2 占位 / 模拟

- `BlueLMProvider` / `CompatibleProvider` —— 接口占位；
- 复习计划仍然取自模型输出，**未做** 本地 quiz state + difficulty + importance 重新生成；
- JSON Schema 文件存在但 **运行时未做完整 schema 校验**，目前依赖 `ResultValidator` + `EvidenceValidator` 双层校验。

### 9.3 显式禁止 / 永不在本工程实现

- 登录 / 注册 / 社区；
- 小 V 真实唤起、原子通知真实接入、负一屏真实接入；
- 课中实时录音；
- 端侧大模型部署；
- 穿戴；
- 长期学习画像。

---

## 10. 下一步任务清单

见 [docs/v0.3-tasklist.md](docs/v0.3-tasklist.md)。
