# ClassMate

> v1.6 official runtime wiring note (2026-06-20): OCR remains the app-level official product path for image/photo/OCR text. Query Rewrite, Embedding, and Text Similarity now have a unified official runtime gateway in the L3 learning-pipeline publish path; when a real app adapter succeeds, provenance is recorded as `OFFICIAL_RUNTIME_USED`, and when config/adapter/runtime is missing or fails, the same pipeline falls back to local rewrite, lexical semantic index, and local similarity without blocking study output. This note does not claim a live official network call was run in this task.

ClassMate 是面向 vivo AIGC 创新赛应用赛道的课堂学习助手。它不是普通录音总结工具，而是把课堂资料整理成可追溯、可练习、可复习、可导出的学习闭环。

当前稳定基线：

- `main`：当前稳定基线。
- `feature/product-review-compatible`：长期实验分支，用于产品化、兼容性和竞赛材料迭代。
- 当前同步基线 commit：`a4c38cc`。
- Stage 10 product UI 来源 commit：`d374db9`。

当前 provider smoke / L3 readiness（2026-06-18）：

- 官方 product-facing provider smoke 已有 4 项真实通过：OCR、QUERY_REWRITE、TEXT_SIMILARITY、EMBEDDING。
- Query Rewrite 此前 blocked 的根因是 smoke harness 请求体 schema 错误；官方 docId 2061 要求 `prompts` schema，Claude 专项修复后真实 network smoke 已 `PASS`。
- OCR 是当前 app-level 官方产品路径：图片/拍照/OCR 文本进入 LessonSource/Evidence，仍受配置控制并保留 fallback。
- Query Rewrite 在 app 内当前是 learning query planning / local fallback / seam；不表述为实时官方 Query Rewrite 调用。
- Embedding 在 app 内当前是本地持久化 lexical semantic index / embedding record seam；不表述为实时官方向量调用。
- Text Similarity 在 app 内当前是 local similarity fallback / seam；不表述为实时官方 rerank/similarity 调用。
- Translation、official TTS、Function Calling 仍按 seam-only / local fallback / not configured 处理。
- ASR Long：core `VivoAsrProvider` 1739 create/upload/run/progress/result 合约存在；app demo 状态是 recording artifact + ASR job seam + manual transcript fallback，官方上传/轮询/结果回填需单独非敏感音频验证。
- 下一主线不是继续加功能，而是 App-level L3 云真机/真机学习闭环验证。

## 核心闭环

主学习链路：

```text
资料输入 -> 蓝心理解 -> 证据校验 -> 知识地图 -> Ask / Practice / Review / Export
```

ClassMate 的核心原则是“模型只生成学习草稿，本地负责证据定位、校验和学习状态更新”。只有通过解析和校验的结果才进入知识地图、微测、复习任务和导出报告。

## AI 路径

当前 AI 路径是：

```text
云端蓝心 -> 端侧蓝心 -> 安全占位
```

- 云端蓝心：官方 BlueLM / qwen3.5-plus 文本模型路径，用于主课程分析和问答。
- 端侧蓝心：vivo 端侧 BlueLM 3B，用于弱网、本地能力和多模态学习草稿。
- 安全占位：最终兜底状态，只说明模型不可用或结果未通过校验，不伪造智能分析结果。

安全口径：

- 不把 LocalRule 当成用户可见的智能能力展示。
- 不把 DeepSeek 或 Compatible Demo 当成复赛主路径。
- 不声称多模态替代 OCR。
- 不声称自动 OCR 已完成。
- 录音不会被描述为已具备自动转写闭环。
- 不把安全占位包装成模型成功。

## 端侧 BlueLM 状态

当前端侧能力已接入到学习链路：

- 官方模型目录：`/sdcard/1225/1.7.0.4_1225_mtk9500`
- 文本生成诊断。
- 多模态 `callVit` 诊断。
- 真实图片诊断不落库。
- 图片/拍照生成可编辑学习草稿。
- 用户确认草稿后进入 `CourseAnalysis`，仍需要 JSON parse 与 validators 通过才落库。

端侧 SDK 通过 reflection bridge 调用，不应在生产代码中直接 `import com.vivo.llmsdk`。

## 输入与学习功能

当前能力包括：

- 文本、Markdown、TXT、SRT/VTT/TXT 转写稿导入。
- 手动 OCR / PPT / 板书 / PDF 资料流。
- Live Companion 与实验模式入口。
- Knowledge Timeline、证据链、Ask This Lesson、Quiz、Practice、Review、Weakness Hub。
- Course Library / Course Detail。
- Export Center：Markdown、HTML、TXT、PDF、MindMap、Word 兼容 HTML、Slides HTML。

未完成或需诚实标注的能力：

- 不做第三方平台视频爬取。
- vivo OCR provider 已完成真实 provider smoke；App-level 图片/拍照闭环仍需 L3 真机验证。
- vivo ASR provider 仍属于后置或单独验证方向。
- 多模态图片理解不等于完整 OCR 替代。
- 声纹身份识别、底噪处理、云同步和团队协作暂缓。

## 当前 UI 基线

Stage 10 product UI 是当前界面基线，已从早期堆叠式页面重构为更产品化的学习界面。

已知状态：

- 当前 UI 可以作为 baseline 继续测试和演示。
- 视觉高级感仍需截图和真机主观验收。
- 后续可能交给 DeepSeek 严格按 `docs/design_refs/*.html` 做 HTML-to-Compose 落地。
- Ask / Practice / Quiz / Review / Export 等页面仍可能继续做分层和质感优化。

## 本地文件与密钥

本地可能存在但绝不入仓：

- `config.local.json`：本机调试配置。只能本地使用，不读取、不提交、不截图、不发给 AI。
- `app/libs/llm-sdk-release.aar`：vivo 端侧 SDK AAR，本地存在但被 `.gitignore` 忽略。
- APK / AAB / build outputs / `.gradle` / keystore / `.env*`。

密钥规则：

- 真实 AppID / AppKEY / API key 不进 Git。
- 不写入 README、docs、Issues、日志、截图、导出报告、测试快照。
- 不记录 `Authorization`、`Bearer`、prompt、messages、vendor response body、`reasoning_content`。

## 权限说明

当前 Manifest 包含以下能力相关权限：

- `INTERNET`：云端 BlueLM 调用与网络诊断。
- `CAMERA`：拍照导入学习资料和多模态图片草稿。
- `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO`：本地图片、视频、音频和转写稿资料入口。
- `RECORD_AUDIO`：Live ASR 实验模式。
- `MODIFY_AUDIO_SETTINGS`：本地 TTS / ASR 音频体验的音频路由调整；不请求蓝牙设备权限。
- `MANAGE_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`：模型目录 `/sdcard/1225`、用户选择本地学习资料和旧 Android 兼容相关能力。App 不上传用户文件，不扫描无关目录。

这些权限需要在 release / privacy audit 中逐项复核。若某项不再必要，应在正式提交前移除或降级。

## 开发验证

常用本地验证命令：

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
scripts\secrets_scan\secrets_scan.ps1
bash scripts/secrets_scan/secrets_scan.sh
git diff --check
```

敏感文件追踪检查：

```powershell
git ls-files config.local.json app/libs/llm-sdk-release.aar "*.apk" "*.aab" ".codex_work/*" ".vscode/*"
```

该命令应无输出。

## 文档入口

- 当前基线：`docs/current/stage10_baseline.md`
- 仓库治理：`docs/current/repo_governance.md`
- 文档导航：`docs/INDEX.md`
- 端侧与 proof 历史材料：`docs/competition/`、`docs/testing/`、`docs/architecture/`

旧 Stage 文档保留作历史记录，但不一定代表当前事实。以后面向评委、队友和新开发者时，应优先引用 `docs/current/` 与 README。
