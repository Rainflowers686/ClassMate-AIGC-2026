# ClassMate v0.4 — Foundation Rebuild

> **当前版本：v0.4 foundation rebuild**（Opus 4.8 one-shot），基于 v0.3.5
> cloud-smoke 整体重构。
> 目标：让 ClassMate 从"主流程可跑"推进到 **"证据链可信、兜底可见、主题可换、
> 决赛可答辩"**。
>
> v0.3.5 兜底版本仍保留在 tag `v0.3.5-cloud-smoke`，本轮重构如果在云真机出现
> 问题，可随时回退。

---

## 1. 这一版做了什么

- **证据链闭合**：彻底修复 v0.3.5 出现的"校验有问题 + 命中率 100%"自相矛盾。
  根因是 Segmenter 合并 + DemoProvider 静态回放双重错位。详见
  [docs/opus48-foundation-rebuild.md](docs/opus48-foundation-rebuild.md) §2。
- **DemoProvider → LocalRuleProvider**：新提供者根据 input.segments 与
  hotwords 实时生成 KP / quiz / review plan / evidence_span，所有引用按构造
  闭合，strict 命中率 100%。
- **strict vs lenient 双轨**：EvidenceValidator 同时给出严格（仅匹配输入原文）
  与宽松（允许 correctedText 兜底）命中率，UI 同屏显示，不再可能产生误导指标。
- **典型校验问题**：ResultValidator 现在输出类型化 `ValidationIssue` 列表，
  分 10 种 kind，UI 直接列出原因与归属 id。
- **三主题设计系统**：FocusGlass（默认答辩）/ VividStudy（活力学习）/ LowPower
  （低端机 / 省电）。所有页面统一走 design tokens（Colors / Spacing / Shapes /
  Motion / Typography），业务页禁用裸 dp / 裸 Color。
- **HeroCard 面子页**：Analyze 屏现在以一张半透明 Hero 卡正面展示
  Provider / 兜底 / 校验 / strict / lenient 五项指标。
- **ViewModel 拆 UseCase**：新增 `AnalyzeCourseUseCase` + `ProviderResolver`，
  ViewModel 只负责状态与导航。
- **Settings 页**：主题切换 + Provider 信息 + 关于。
- **BlueLM 保持诚实占位**：没有官方契约前永远抛 `PROVIDER_NOT_IMPLEMENTED`，
  UI 自动回退到 local，并把 "回退 = 是" 标红。

---

## 2. 运行环境

| 项 | 版本 |
| --- | --- |
| Android Studio | Hedgehog 2023.1.1+ |
| AGP | 8.2.2 |
| Kotlin | 1.9.22 |
| Compose Compiler | 1.5.8 |
| Compose BOM | 2024.02.00 |
| Compile / Target SDK | 34 |
| Min SDK | 26 |
| JDK | 17 |

---

## 3. Provider 当前状态（v0.4）

| Provider | 状态 | 行为 |
| --- | --- | --- |
| `LocalRuleProvider` (`name="local"`) | **默认主路径** | 基于 input.segments + hotwords 规则式产出；保证 strict 命中率 100% |
| `CompatibleProvider` | **真实 HTTP 已接线** | OpenAI-compatible chat/completions；失败自动回退 local |
| `BlueLMProvider` | **诚实占位** | 抛 `PROVIDER_NOT_IMPLEMENTED`，绝不发请求；自动回退 local |

⚠ 仓库内 **没有** BlueLM 官方 endpoint / 请求体 / 签名算法。为避免伪造，
BlueLMProvider 只持有 config 读取与错误分类，**不会发出任何网络请求**。
要走真实 BlueLM 需要先补齐：

- 课程分析任务的 base_url；
- 请求体格式（model 名、temperature、消息体）；
- App Key 签名算法（header 名、被签名 payload、hash）；
- 响应体结构（content 字段路径）。

---

## 4. 配置方式（`config.local.json`）

API Key **不进入仓库**。复制 `config.example.json` → `config.local.json`：

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

- `provider` 取 `demo` / `local` / `compatible` / `bluelm`（`demo` 与 `local` 等价，
  都走 LocalRuleProvider）。
- `config.local.json` 被 `.gitignore` 屏蔽。
- 设备查找顺序：
  1. `/data/data/com.classmate.app/files/config.local.json`
  2. `assets/config.example.json`（仅占位符）
  3. 硬编码 `provider=demo` 兜底
- 推送示例：

  ```bash
  adb push config.local.json /data/data/com.classmate.app/files/
  ```

⚠ 任何形式的 key、App Key、Authorization 完整头都 **禁止** 写入代码、
assets、commit message、日志、issue。

---

## 5. 构建

### 5.1 Debug APK

Windows：

```powershell
.\gradlew.bat :app:assembleDebug
```

macOS / Linux：

```bash
./gradlew :app:assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`。

### 5.2 安装到云真机 / 设备

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 6. 云真机复测步骤（V2502A）

详细步骤见 [docs/opus48-foundation-rebuild.md §7](docs/opus48-foundation-rebuild.md#7-云真机acceptance-stepsv2502a)。
简版：

1. 启动 → 首页 → **加载 Demo / Load demo**。
2. 走完 `CourseInput → Hotword → Analyze`。
3. 在 Analyze 点 **调用 demo 分析**。Hero 卡必须显示：
   - Provider · `local`（config 请求：`demo`）
   - 兜底：否
   - 校验：通过
   - strict：100%
   - lenient：100%
4. **继续到时间轴** → KP 列表渲染。点 **查看证据**，证据段以暖黄色高亮。
5. **进入微测** → 答对 1 道、答错 1 道。
6. **查看复习计划** → 错答题对应步骤显示红色边条 + "错题强化"标签。
7. 首页 → ⚙ → Settings → 切换 FocusGlass / VividStudy / LowPower 三主题，
   三套都能跑完全流程。

---

## 7. 校验与日志（闭合链）

```text
input ─► Provider ─► CourseAnalysisResult
                              │
                              ├─► ResultValidator(result, input)
                              │     └─► 10 种 ValidationIssue.Kind
                              │
                              ├─► EvidenceValidator(input, result)
                              │     ├─► strictEvidenceMatchRate (input only)
                              │     └─► lenientEvidenceMatchRate (+ correctedText)
                              │
                              ├─► RedactedLogger → ModelCallLog
                              │
                              └─► UiState → HeroCard (4 metrics)
```

### 7.1 日志样例

```json
{"timestamp":"2026-05-29T10:15:32+08:00","provider":"local","task":"course_analysis","input_segment_count":3,"hotword_count":5,"success":true,"latency_ms":3,"structure_valid":true,"strict_evidence_match_rate":1.0,"lenient_evidence_match_rate":1.0,"fallback_used":false,"error_type":null,"api_key_redacted":true}
```

字段集合由 [`core/logging/ModelCallLog.kt`](core/src/main/kotlin/com/classmate/core/logging/ModelCallLog.kt)
锁死。**严禁** 出现真实 key、Authorization、隐私文本。

样例日志：[proof/logs_redacted/sample_model_call_redacted.jsonl](proof/logs_redacted/sample_model_call_redacted.jsonl)
（带 `"_sample": true` 标记）。

---

## 8. UI 主题简表

| 主题 | 定位 | 启用 |
| --- | --- | --- |
| Focus Glass · 静研 | **默认**。答辩 / 学习场景，半透明卡片 + 极淡渐变 + 学术蓝 | 默认；首启自动选 |
| Vivid Study · 活力 | 学生自习，暖橙 + 紫蓝 + 径向极淡渐变 | Settings 手切 |
| Low Power · 省电 | 低端机 / 系统省电模式，纯实色 + 1px 描边，**关闭半透明 / 渐变 / 阴影 / 动画** | Settings 手切 / 系统省电触发（v0.5 自动化） |

详细 token / 动效原则见
[docs/opus48-foundation-rebuild.md §6](docs/opus48-foundation-rebuild.md#6-ui-themes-v04)
与 RFC §7 (历史会话)。

---

## 9. 能力边界

### 9.1 已实现

- 8 个页面 + 设置页；
- Segmenter（用户输入路径）；
- LocalRuleProvider（demo 主路径）；
- CompatibleProvider（真实 HTTP）；
- BlueLMProvider（诚实占位）；
- ResultValidator（10 种 typed issue + input 交叉校验）；
- EvidenceValidator strict + lenient；
- HeroCard 显示 Provider / 兜底 / 校验 / strict / lenient；
- 错题回标（Timeline 红条 + Review Plan "错题强化"）；
- 三主题切换（FocusGlass / VividStudy / LowPower）；
- RedactedLogger 闭合字段 JSON Line；
- 兜底可见（fallback_used 在 UI 与日志都显式标注）。

### 9.2 未实现 / 占位

- BlueLM 真实接入（缺官方契约）；
- 持久化（Theme / Provider 选择、quiz state 跨重启）；
- core 单元测试（v0.4 暂无；Codex 第 1 优先）；
- R8 / ProGuard 规则；
- 真实 compatible 调用日志归档。

### 9.3 显式禁止 / 永不在本工程实现

- 登录 / 注册 / 社区；
- 小 V 真实唤起、原子通知、负一屏；
- 课中实时录音；
- 端侧大模型部署；
- 穿戴；
- 长期学习画像；
- 未授权使用任何第三方大模型 key；
- 任何形式的 "已完整接入蓝心大模型" / "学习效率提升 XX%" / "已接入小 V" /
  "已接入原子通知" / "已接入负一屏" / "真实课堂录音已实现" /
  "vivo 生态深度集成" 等夸大文案。

---

## 10. 目录结构（v0.4）

```text
ClassMate/
├── core/src/main/kotlin/com/classmate/core/
│   ├── adapter/
│   │   ├── ModelProvider.kt
│   │   ├── LocalRuleProvider.kt          ← NEW，取代 DemoProvider
│   │   ├── CompatibleProvider.kt
│   │   ├── BlueLMProvider.kt             ← 诚实占位
│   │   ├── ProviderConfig.kt
│   │   ├── PromptBuilder.kt
│   │   ├── JsonExtractor.kt
│   │   └── ModelCallException.kt
│   ├── network/
│   ├── segmenter/
│   ├── validation/
│   │   ├── ResultValidator.kt            ← ENHANCED，typed issues + input 交叉
│   │   └── ValidationIssue.kt            ← NEW
│   ├── evidence/                         ← ENHANCED，strict + lenient
│   ├── logging/                          ← ENHANCED，双 match rate
│   └── model/
├── app/src/main/java/com/classmate/app/
│   ├── data/
│   ├── domain/                           ← NEW
│   │   ├── ProviderResolver.kt
│   │   └── AnalyzeCourseUseCase.kt
│   ├── state/
│   ├── ui/
│   │   ├── AppRoot.kt
│   │   ├── designsystem/                 ← NEW
│   │   │   ├── AppScaffold.kt
│   │   │   ├── BrandSurface.kt
│   │   │   ├── GlassCard.kt
│   │   │   ├── HeroCard.kt
│   │   │   ├── PrimaryButton.kt
│   │   │   ├── SectionHeader.kt
│   │   │   └── StatusDot.kt
│   │   ├── theme/                        ← REWRITE
│   │   │   ├── Theme.kt
│   │   │   ├── ThemeId.kt
│   │   │   ├── ClassMateColors.kt
│   │   │   ├── ClassMateSpacing.kt
│   │   │   ├── ClassMateShapes.kt
│   │   │   ├── ClassMateMotion.kt
│   │   │   └── ClassMateTypography.kt
│   │   ├── components/                   ← REWRITE (4 cards)
│   │   └── screens/                      ← NEW dir (8 screens)
│   └── MainActivity.kt
├── docs/
│   ├── opus48-foundation-rebuild.md      ← NEW，本轮重构主文档
│   └── v0.3-tasklist.md                  历史归档
├── proof/logs_redacted/
│   ├── README.md
│   └── sample_model_call_redacted.jsonl
├── schema/course_analysis_result.schema.json
├── prompts/
└── examples/
```

---

## 11. 命名约定

- Kotlin 字段 camelCase；JSON 字段 snake_case，由 `@SerialName` / 显式映射桥接。
- UI 不直接处理原始 JSON 字符串。
- 业务页面只能读 `LocalClassMateColors.current.*`，不允许直接读
  `MaterialTheme.colorScheme.*`。
- 业务页面不允许裸 dp / 裸 Color / 裸 tween；统一走 design tokens。
- 真实密钥永远不进入仓库、日志、commit、issue。

---

## 12. 下一步（Codex 接手清单）

完整版见 [docs/opus48-foundation-rebuild.md §8](docs/opus48-foundation-rebuild.md#8-codex-handover-list)。
高优先级 8 项：

1. core 单测（LocalRuleProvider / Validators / JsonExtractor）。
2. Theme + Provider 选择持久化。
3. 跑一次真实 compatible 调用，归档 redacted 日志。
4. BlueLM 官方契约就位后补 30 行接线。
5. 云真机 8 屏 screenshot 归档。
6. R8 + ProGuard 规则。
7. 运行时 schema 校验（可选 networknt）。
8. SavedStateHandle 持久化 UiState。
