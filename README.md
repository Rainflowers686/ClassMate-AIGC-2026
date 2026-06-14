# ClassMate

ClassMate 是面向 vivo AIGC 创新赛应用赛道的课堂学习助手。它不是普通录音总结工具，而是把课堂资料整理成可追溯、可练习、可复习、可导出的学习闭环。

当前稳定基线：

- `main`：当前稳定基线。
- `feature/product-review-compatible`：长期实验分支，用于产品化、兼容性和竞赛材料迭代。
- 当前同步基线 commit：`a4c38cc`。
- Stage 10 product UI 来源 commit：`d374db9`。

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
- vivo ASR / vivo OCR provider 仍属于后续接入方向。
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
- Bluetooth 相关权限：蓝牙麦克风/耳机路由和设备状态诊断。
- `MANAGE_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`：历史兼容和模型目录/文件访问相关能力。

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
