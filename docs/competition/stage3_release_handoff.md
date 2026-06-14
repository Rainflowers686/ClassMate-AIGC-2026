# Stage 3 Release Handoff

用途：给未来自己、队友和提交前整理使用。  
范围：本文只描述 Stage 3 当前交付状态，不执行提交，不声明未完成能力已完成。

## 1. Stage 3 完成了什么

Stage 3 的目标是把 ClassMate 从“核心链路可跑通”推进到“更像真实手机端学习产品”的状态。

当前交付重点：

- Live Companion 从简单手动片段页升级为更产品化的课堂伴学体验。
- 新增 Flow 视觉数据和组件，用于课堂/沉浸场景的局部增强。
- 通过 design tokens 和 shared product components 统一部分 UI 表达。
- 保留官方 BlueLM / Compatible Demo / LocalFallback 主链路，不把视觉重构和 provider 重构混在一起。
- Codex 侧补齐 competition docs、proof checklist、演示脚本、Q&A、验收矩阵和提交建议。

## 2. 生产改动文件清单

以下为当前工作树中可见的 Claude Stage 3 生产改动文件名，仅作交接，不在本文中读取源码内容。

### app UI

- `app/src/main/java/com/classmate/app/ui/screens/live/LiveCompanionScreen.kt`

### component

- `app/src/main/java/com/classmate/app/ui/components/ProductComponents.kt`
- `app/src/main/java/com/classmate/app/ui/components/FlowComponents.kt`

### design token

- `app/src/main/java/com/classmate/app/ui/design/Tokens.kt`

### Flow data / scene

- `app/src/main/java/com/classmate/app/ui/flow/`
- `app/src/test/java/com/classmate/app/ui/flow/`

## 3. docs 改动文件清单

### Stage 3 proof / competition docs

- `docs/competition/stage3-device-smoke-checklist.md`
- `docs/competition/review-readiness.md`
- `docs/competition/capability-matrix.md`
- `docs/competition/security-proof-checklist.md`
- `docs/competition/rematch-demo-script.md`
- `docs/competition/device-recording-shot-list.md`
- `docs/competition/judge-qna.md`
- `docs/competition/feature-acceptance-matrix.md`
- `docs/competition/competitive-reference.md`
- `docs/competition/commit-split-suggestions.md`
- `docs/competition/stage3_release_handoff.md`
- `docs/competition/stage3_pr_description.md`
- `docs/competition/stage3_commit_commands.md`
- `docs/competition/stage3_issue_backlog.md`

### Design handoff

- `docs/design_refs/design-handoff-summary.md`

## 4. 真实可用能力

当前可以作为真实已实现能力描述：

- BlueLM / qwen3.5-plus 官方文本生成主路径。
- Compatible demo mode，仅作为展示增强。
- LocalFallback，用于无配置、弱网或主路径失败时保底。
- Import paste / txt / md。
- Live manual -> analysis -> History -> LearningStore -> Export。
- Timeline / Evidence / Quiz / Review / Weakness Hub。
- Course Library、MindMap、Video white-list recommendations、Capability Roadmap。

## 5. 诚实占位

以下能力不能说成已经完成：

- ASR 未接入。
- OCR 未接入。
- 音频/视频解析未接入。
- 网络视频链接不抓取平台内容。
- Flow 白噪音仅为视觉/体验方向占位，未播放真实音频。
- 端侧 3B 模型、文本向量、文本审核仍是规划或调研方向。

## 6. 安全说明

- 本轮交接不读取真实 `config.local.json` 内容。
- 不输出真实 AppID/AppKEY/API key。
- Debug import 只能显示 masked/present 状态。
- 日志、导出、History、LearningStore 和 proof 材料不得写入密钥、Authorization、完整 prompt/messages、vendor response body 或 reasoning_content。
- 导出报告只应包含学习业务数据：课程、知识点、证据、微测、复习任务、课程库、薄弱点、MindMap 和白名单搜索链接。

## 7. 当前推荐提交顺序

推荐拆两个 commit：

1. `docs(proof): add stage 3 competition materials`
   - 包含 `docs/competition/*.md` 和 `docs/design_refs/design-handoff-summary.md`。
   - 风险低，可先提交。

2. `feat(ui): productize Live Companion with Flow visuals and design tokens`
   - 包含 Claude Stage 3 生产改动。
   - 需要确认 app/core tests、assembleDebug、secrets scan 均通过。

如果时间极紧，可以合并为一个 commit，但不推荐，因为 docs 和 UI 生产改动的风险不同。

## 8. 真机测试优先级

P0 必测：

- Settings official_bluelm 导入和 BlueLM diagnostic。
- Import paste/txt/md -> Timeline。
- Timeline -> Evidence -> Quiz。
- Review -> Weakness Hub。
- Live manual -> Generate timeline -> History -> Review -> Export。
- Export 文件敏感词检查。

P1 建议测：

- Course Library 多课程聚合。
- Compatible demo diagnostic。
- local_only 零网络 fallback。
- Flow/Focus/Vitality 主题切换。

P2 proof 截图：

- Home。
- Settings provider profile。
- BlueLM diagnostic。
- Timeline。
- Evidence。
- Quiz。
- Review / Weakness Hub。
- Live Companion。
- History / Course Library。
- Export 报告脱敏片段。

## 9. 提交前最低检查

```powershell
git diff --check
git ls-files config.local.json local.properties secrets.properties .env .env.* *.jks *.keystore *.apk *.aab app/build core/build build .gradle
scripts\secrets_scan\secrets_scan.ps1
```

生产代码 commit 前还应跑：

```powershell
.\gradlew.bat :core:test
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```
