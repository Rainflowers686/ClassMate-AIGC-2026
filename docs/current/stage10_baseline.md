# Stage 10 当前基线

本文记录 ClassMate 在 Stage 10 之后的当前事实基线，供评委材料、后续开发和 issue 清理使用。

## Commit 状态

- 当前同步基线 commit：`a4c38cc`
- Stage 10 Product UI 来源 commit：`d374db9`
- `main` 与 `feature/product-review-compatible` 当前已同步到 `a4c38cc`。
- `51d635e` 是将 product-review-compatible 基线同步回 main 的桥接提交。

本地 `git log` 可能只显示同步后的少数提交，这是因为当前分支已整理为新的稳定基线。Stage 10 来源 commit `d374db9` 仍作为 UI 重构来源记录。

## Stage 8E 能力摘要

Stage 8E 之后，ClassMate 的 AI 链路已经从早期 cloud-only 进入“云端 + 端侧 + 安全占位”的结构：

```text
云端蓝心 -> 端侧蓝心 -> 安全占位
```

已接入能力：

- 官方端侧模型路径：`/sdcard/1225/1.7.0.4_1225_mtk9500`
- 端侧文本生成。
- 多模态 `callVit`。
- 真实图片诊断。
- 图片/拍照生成可编辑学习草稿。
- 用户确认后，草稿进入 `CourseAnalysis`。
- 端侧 CourseAnalysis 输出必须经过 JSON parse 与 validators，通过后才落库。
- invalid JSON、validation failed、unavailable 均不得污染知识库。

安全口径：

- 多模态图片理解不是 OCR 的完整替代。
- 图片诊断不自动落库。
- 用户确认是图片/拍照资料进入学习链路的边界。

## Stage 10 UI 摘要

Stage 10 完成了当前 product UI baseline。主要页面已经重构到更接近真实学习 App 的结构：

- Home
- Import
- Course Detail
- Knowledge Timeline
- Ask / Practice / Quiz / Review
- History / Course Library
- Export Center
- Settings

当前 UI 是可继续测试和演示的 baseline，但还不是最终主观验收完成状态。

后续可能由 DeepSeek 严格按 `docs/design_refs/*.html` 做 HTML-to-Compose 落地，重点提升：

- 首页和导入 flow 的第一印象。
- Course Detail 信息层级。
- Ask / Practice / Quiz / Review 的高级感。
- Export Center 与 proof 页面。
- Focus / Flow / Vitality 设计方向的一致性。

## 当前已知不足

- UI 主观高级感仍需真机截图验收。
- Ask / Practice / Quiz / Review / Export 页面仍可能需要后续重构。
- 端侧 SDK、模型文件和多模态能力无法由 CI 完整验证，只能通过真机或云真机验证。
- Manifest 权限需要 release / privacy audit，尤其是 `RECORD_AUDIO`、`MANAGE_EXTERNAL_STORAGE`、`WRITE_EXTERNAL_STORAGE`。
- README 与历史 docs 正在迁移；旧 Stage 文档不一定代表当前事实。
- 旧 issue 中仍存在 v1 / Stage 1 / cloud-only 语义，需要清理或改写。

## 当前红线

- 不读取 `config.local.json` 内容。
- 不提交 `config.local.json`。
- 不提交 `app/libs/*.aar`。
- 不提交 APK / AAB / build outputs / `.gradle`。
- 不 direct import `com.vivo.llmsdk`；SDK 通过 reflection bridge。
- qwen3.5-plus 使用 profile-aware deep thinking：`DEEP_STUDY` / `BALANCED` 在兼容支持时发送 `enable_thinking=true`，不支持时由 capability flag 安全省略。
- LocalRule / `LOCAL_FALLBACK` 不作为用户可见智能能力展示。
- 安全占位只作为最终兜底状态。
- CourseAnalysis 持久化必须由 validators gate。
- 不削弱 ResultValidator / EvidenceValidator / EvidenceResolver。
- 不把 DeepSeek / Compatible Demo 说成复赛主 AI 路径。
- 不声称自动 OCR 已完成。
- 不声称多模态替代 OCR。

## 验证状态口径

- CI 验证：JVM/unit tests、debug APK build、secrets scan。
- 真机验证：端侧 SDK、模型目录、文本 generate、多模态 callVit、真实图片诊断、拍照/图片草稿。
- 待验证：最终 UI 主观验收、release 权限隐私审计、复赛 proof 视频和截图完整性。
- Deferred：vivo ASR provider、vivo OCR provider、云同步、团队协作、声纹身份识别、底噪处理。
