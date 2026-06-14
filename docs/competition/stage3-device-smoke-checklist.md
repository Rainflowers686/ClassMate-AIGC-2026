# Stage 3 真机 Smoke Checklist

适用场景：评审前、答辩前、真机装包后逐项检查。  
规则：不要截图密钥；不要把完整 AppID/AppKEY 发给任何 AI；不要提交本地配置文件。

每一项都记录：

- 操作步骤：手机上怎么做。
- 期望结果：应该看到什么。
- 异常时截图位置：失败时优先截哪一屏，避免截到密钥。

## 0. 安装与基础环境

| 编号 | 操作步骤 | 期望结果 | 异常时截图位置 |
|---|---|---|---|
| 0.1 | 安装 `app-debug.apk`，首次启动 App。 | App 正常进入 Home，不卡死、不崩溃。 | Home 首屏；不要截 debug import 文本框。 |
| 0.2 | 打开 Settings，查看 Build 信息。 | 能看到版本、build type、built at、commit short。 | Settings 的 Build 卡片。 |
| 0.3 | 打开 Settings 的 Privacy / security 文案。 | 明确说明密钥不入仓、日志脱敏、导出不含敏感内容。 | Privacy 卡片。 |

## 1. Debug 导入 official_bluelm 配置

| 编号 | 操作步骤 | 期望结果 | 异常时截图位置 |
|---|---|---|---|
| 1.1 | Settings -> Debug config import，粘贴本机测试用 official_bluelm JSON。 | 导入成功；只显示 credential_present=true、masked appId/appKey。 | 导入结果预览；必须遮挡输入框内容。 |
| 1.2 | 确认 model 为 `qwen3.5-plus`。 | 当前 BlueLM 配置显示 qwen3.5-plus。 | Provider profile 卡片或 masked preview。 |
| 1.3 | 点击 Test BlueLM connection。 | provider=BLUELM，status=OK，http_status=200，content preview 或 content length 安全显示。 | BlueLM diagnostic 结果；不要截粘贴框。 |
| 1.4 | 若失败，检查网络、AppID/AppKEY、app_id header、设备代理。 | 失败只显示短错误码、latency、http_status/vendor_code 等安全字段。 | Diagnostic 结果；不要截真实 key。 |

## 2. Home 首页检查

| 编号 | 操作步骤 | 期望结果 | 异常时截图位置 |
|---|---|---|---|
| 2.1 | 返回 Home。 | 显示当前模式、最近课堂或导入提示、Review 今日状态。 | Home 首屏。 |
| 2.2 | 点击 Import learning material。 | 进入 Import Hub。 | Home 到 Import 的跳转后页面。 |
| 2.3 | 点击 Live Companion。 | 进入 Live Companion 手动课堂页。 | Live Companion 首屏。 |
| 2.4 | 点击 Export report。 | 若已有历史，导出成功并显示文件名/路径摘要；无历史则提示无可导出报告。 | Toast 或 Export 区域，不截本机绝对隐私路径。 |

## 3. Import Hub 文本导入

| 编号 | 操作步骤 | 期望结果 | 异常时截图位置 |
|---|---|---|---|
| 3.1 | 在 Import Hub 粘贴课堂文本，填写标题。 | 字数统计更新，Generate knowledge timeline 可点击。 | Import Hub 文本区，但不要包含隐私课堂全文。 |
| 3.2 | 点击 Generate knowledge timeline。 | 进入 Analyze，完成后进入 Knowledge Timeline。 | Analyze 阶段页；若失败截 Settings redacted log。 |
| 3.3 | 使用 Import .txt 选择一个本地 `.txt` 文件。 | 文件文本进入课堂文本框，可继续分析。 | Import Hub 文件导入后页面。 |
| 3.4 | 使用 Import .md 选择一个本地 `.md` 文件。 | Markdown 原文进入课堂文本框，可继续分析。 | Import Hub 文件导入后页面。 |
| 3.5 | 点击音频、视频、OCR、网络视频链接占位入口。 | 只提示暂未接入/后续接入/可先粘贴；不触发解析、下载或网络抓取。 | 占位提示或 Toast。 |

## 4. Live Companion 手动课堂

| 编号 | 操作步骤 | 期望结果 | 异常时截图位置 |
|---|---|---|---|
| 4.1 | 输入课程标题。 | 标题留存在 Live Companion session。 | Live 标题输入区。 |
| 4.2 | 点击 Start。 | 状态变为 RUNNING，计时开始。 | Status 卡片。 |
| 4.3 | 手动添加 2-3 条课堂片段。 | 片段数正确增加，最近片段列表显示。 | Recent segments 卡片；不要包含隐私正文。 |
| 4.4 | 点击 Pause，再点击 Continue。 | 状态在 PAUSED/RUNNING 间切换，片段仍保留。 | Status 卡片。 |
| 4.5 | 点击 End class。 | 状态变为 ENDED，拼接后的课堂文本进入分析输入。 | Status + 最近片段。 |
| 4.6 | 点击 Generate timeline。 | 复用现有 provider resolver，不绕开 BlueLM/Compatible/LocalFallback。 | Analyze 页与完成后的 Timeline。 |
| 4.7 | 检查页面文案。 | 明确显示“手动/模拟转写演示，暂未接入真实 ASR”；没有录音权限弹窗。 | Live 顶部说明；若弹权限框需截图。 |

## 5. Timeline / Evidence / Ask This Lesson

| 编号 | 操作步骤 | 期望结果 | 异常时截图位置 |
|---|---|---|---|
| 5.1 | 打开 Knowledge Timeline。 | 课程标题、provider provenance、知识点数、微测题数、原文段数正常显示。 | Timeline 顶部卡片。 |
| 5.2 | 点击一个知识点。 | 进入 Evidence detail，看到原文证据链。 | Evidence detail；不要截完整隐私原文。 |
| 5.3 | 返回 Timeline，输入一个本节课问题并点击 Ask。 | 显示 answer、groundedness/fallback 标记、related knowledge points。 | Ask This Lesson 卡片。 |
| 5.4 | 问一个资料中没有依据的问题。 | 显示 not_found 或 partial，不伪造证据。 | Ask 结果卡片。 |
| 5.5 | 点击 Start quiz。 | 进入 Quiz 页面。 | Quiz 首题。 |

## 6. Quiz

| 编号 | 操作步骤 | 期望结果 | 异常时截图位置 |
|---|---|---|---|
| 6.1 | 回答一道题。 | 显示正确/错误反馈，答案解释出现。 | Quiz 答题后状态。 |
| 6.2 | 查看证据解释。 | 解释与知识点/证据关联，不是文本匹配题。 | Quiz 解释区域。 |
| 6.3 | 连续做完多题。 | 进度更新，LearningStore 产生 attempt。 | Quiz 结束或 Review 更新。 |

## 7. Review / Weakness Hub

| 编号 | 操作步骤 | 期望结果 | 异常时截图位置 |
|---|---|---|---|
| 7.1 | 进入 Review。 | 今日待复习、预计时间、弱点数、待复核数显示。 | Review 顶部。 |
| 7.2 | 点击 已掌握/太难/需要例题/证据不对。 | ReviewTask 计数更新，任务优先级或 needsHumanReview 状态变化。 | 对应任务卡片。 |
| 7.3 | 查看 Weakness Hub。 | wrongAnswer、tooHard、needExample、evidenceWrong 进入薄弱点；mastered 会降低或移除。 | Weakness Hub 卡片。 |
| 7.4 | 点击视频推荐。 | 只打开白名单来源搜索 URL，不下载、不抓取、不后台解析。 | 外部浏览器地址栏，仅显示搜索页。 |
| 7.5 | 点击 Export review report。 | 导出成功，显示文件名和路径摘要。 | Export 卡片或 Toast。 |

## 8. History / Course Library

| 编号 | 操作步骤 | 期望结果 | 异常时截图位置 |
|---|---|---|---|
| 8.1 | 进入 History。 | Course library 和 Recent lessons 可见。 | History 首屏。 |
| 8.2 | 多次分析同名/相近课程后查看课程库。 | 同课程聚合为一个 CourseSummary，显示课堂次数、知识点总数、微测题总数、待复习任务数。 | Course card。 |
| 8.3 | 点击课程卡。 | 进入课程详情，显示课堂记录列表和相关复习任务。 | Course detail。 |
| 8.4 | 重新打开某次课堂。 | 回到该课堂 Timeline，不改变 provider 配置。 | Timeline 顶部 provenance + Settings provider 卡片。 |
| 8.5 | 删除一条 History 记录。 | 该历史消失，但 provider 配置和 LearningStore 任务不被清空。 | History 删除前后 + Review 任务仍在。 |

## 9. Settings

| 编号 | 操作步骤 | 期望结果 | 异常时截图位置 |
|---|---|---|---|
| 9.1 | 查看 Provider profile。 | Official BlueLM / Compatible demo / Local fallback 状态清晰。 | Provider profile 卡片。 |
| 9.2 | 查看 Capability Roadmap。 | 分为 Connected、Planned seams、Demo enhancement；不声称 ASR/OCR 已完成。 | Capability Roadmap 卡片。 |
| 9.3 | 查看 BuildInfo。 | 版本、build type、built at、commit 显示。 | Build 卡片。 |
| 9.4 | 查看 Privacy。 | 明确密钥不入仓、debug import 内存态、日志/导出脱敏。 | Privacy 卡片。 |

## 10. Provider Profile 回归

| 编号 | 操作步骤 | 期望结果 | 异常时截图位置 |
|---|---|---|---|
| 10.1 | official_bluelm 模式分析文本。 | BlueLM 优先；失败时才 LocalFallback；日志显示 fallback_used。 | Settings redacted log。 |
| 10.2 | demo_compatible 模式分析文本。 | Compatible -> BlueLM(若可用) -> LocalFallback。 | Settings provider profile + redacted log。 |
| 10.3 | local_only 模式分析文本。 | 只走 LocalFallback，零网络请求。 | Settings provider profile + redacted log。 |
| 10.4 | 回归检查 qwen 请求体。 | `qwen3.5-plus` 仍有 `enable_thinking=false`。 | 本地命令输出，不需要手机截图。 |
| 10.5 | 回归检查 ProviderResolver 顺序。 | official_bluelm / demo_compatible / local_only 顺序未变。 | 单测输出或 Settings provider order。 |

## 11. Export 文件检查

| 编号 | 操作步骤 | 期望结果 | 异常时截图位置 |
|---|---|---|---|
| 11.1 | 导出 Markdown 报告。 | 包含课程名、知识点、证据、微测、复习任务、课程库、薄弱点摘要。 | 文件预览的目录/标题区域。 |
| 11.2 | 导出 HTML 报告。 | 浏览器可打开，结构与 Markdown 一致。 | 浏览器顶部和主要章节。 |
| 11.3 | 导出 TXT 报告。 | 可打开，内容可读。 | 文本文件前几段。 |
| 11.4 | 搜索导出文件敏感词。 | 不含 key、Authorization、prompt/messages、reasoning_content、vendor response body。 | 搜索结果为 0 的截图。 |

## 12. Flow / Live 主题检查

| 编号 | 操作步骤 | 期望结果 | 异常时截图位置 |
|---|---|---|---|
| 12.1 | 切换 Focus / Flow / Vitality。 | 主题切换正常，不影响分析和复习数据。 | Settings Theme + Home。 |
| 12.2 | Flow 或 Live 场景下进入 Live Companion。 | 明确手动/模拟转写，不伪装真实 ASR，不申请录音权限。 | Live 顶部说明。 |
| 12.3 | 检查是否出现录音权限弹窗。 | 不应出现。 | 若出现，截图权限弹窗。 |

## 13. 结束前本地回归命令

在电脑端运行：

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
scripts\secrets_scan\secrets_scan.ps1
bash scripts/secrets_scan/secrets_scan.sh
git diff --check
git ls-files config.local.json local.properties secrets.properties .env .env.* *.jks *.keystore *.apk *.aab app/build core/build build .gradle
Select-String -Path "core\src\main\kotlin\com\classmate\core\provider\VendorIo.kt","core\src\main\kotlin\com\classmate\core\provider\BlueLMDiagnostic.kt" -Pattern "enable_thinking|qwen3.5-plus"
```

期望：Gradle 通过、secrets scan 通过、`git diff --check` 无输出、敏感追踪文件检查无输出、qwen guard 仍存在。
