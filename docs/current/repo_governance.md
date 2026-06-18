# 仓库治理说明

本文记录 ClassMate 当前仓库治理规则，优先级高于早期 Stage 文档中的旧计划。

## 当前 issue 状态

2026-06-18 当前主线：

- 官方 provider smoke 当前矩阵：OCR `PASS`、TEXT_SIMILARITY `PASS`、EMBEDDING `PASS`、QUERY_REWRITE `READY / live smoke BLOCKED / fallback available`。
- Query Rewrite 可交给 Claude/provider diagnostics 专项继续尝试修复，但不阻塞 L3 主线。
- 当前不建议继续盲目扩功能；下一主线是 App-level L3 云真机/真机端到端学习闭环验证。
- 后续 issue 应优先来自 L3 真机验收的 blocker / warning / polish，而不是新增大范围能力。

已关闭：

- `#10` completed：真实 vivo BlueLM request schema / endpoint / credential 安全接入已完成，并被后续云端与端侧链路覆盖。
- `#18` completed：自适应 quiz / review / practice 学习闭环已进入当前基线。
- `#12` not planned：CompatibleProvider 旧测试目标已过时，Compatible Demo 不再作为主参赛路径。
- `#14` not planned：v1 foundation screenshot 范围已被 Stage 8E / Stage 10 proof 取代。

仍 open：

- `#11`：PromptBuilder / AnalysisJsonParser 测试，需要改写为 Stage 10 parser、evidence、JSON stabilization 回归测试。
- `#13`：secrets scan entropy / stricter patterns，仍有效。
- `#15`：Claude audit，应改写为 Stage 10 云端 + 端侧 + 安全占位红队审查。
- `#16`：UI productization，应改写为 DeepSeek strict HTML-to-Compose / Stage 10 visual polish。
- `#17`：reviewer README / architecture notes，仍有效。
- `#19`：proof-ready checklist，应改写为 Stage 10 semifinal proof checklist。

## Issue 处理原则

- 旧 issue 不直接删除，先审计。
- 已真实完成且有代码/测试/文档证据时，才 close completed。
- 目标过时但不是完成时，close not planned，并创建 replacement issue。
- 大 issue 拆成可验证小 issue，避免“做 UI polish”这种无法验收的大包。
- issue body 必须写清 acceptance criteria、non-goals、proof/test requirements。
- 不在 issue 中写真实 AppID / AppKEY / API key、prompt、messages、vendor body、`reasoning_content`。
- 复赛相关 issue 要区分“已实现”“实验模式”“待接入”“deferred”。

## 后续 Epic

### Epic 1：UI Productization

- DeepSeek strict HTML-to-Compose refactor。
- Home / Import / Course Detail 视觉和层级 polish。
- Ask / Practice / Review / History / Export premium pages。
- Flow Companion page。
- Vitality optional theme。

### Epic 2：BlueLM Reliability

- 云端蓝心稳定性。
- 端侧蓝心路径检测。
- 多模态真实图片测试。
- 端侧 CourseAnalysis JSON 稳定性。
- 错误码解释与用户建议。

### Epic 3：Learning Loop

- Ask grounding。
- Practice feedback。
- Review scheduling。
- Weakness Hub。
- Export preview。

### Epic 4：Competition Proof

- 真机截图清单。
- 2 分钟视频脚本。
- 10 分钟答辩脚本。
- proof pack。
- judge Q&A。

### Epic 5：Engineering Quality

- CI。
- secrets scan。
- test coverage。
- release checklist。
- APK signing / packaging。
- repo hygiene。

## 模型分工

- Claude：复杂功能、架构、红队审查、竞赛叙事。
- Codex：工程实现、测试、CI、脚本、proof、文档治理。
- DeepSeek：明确设计约束下的 UI 落地、文案和批量样式调整。

DeepSeek 不是 ClassMate 的主 AI 路径；它只是后续 UI 执行协作模型。

## 分支策略

- `main`：当前稳定基线。
- `feature/product-review-compatible`：长期实验分支。
- 重大阶段通过 PR 或明确 checkpoint 同步 main。
- 阶段性提交应尽量按 docs、UI、provider、tests、proof 分开，便于回滚。

## 禁止提交内容

不得提交：

- `config.local.json`
- `local.properties`
- `secrets.properties`
- `.env*`
- `app/libs/*.aar`
- APK / AAB
- keystore / signing 文件
- build outputs
- `.gradle`
- `.codex_work`
- `.vscode`

提交前建议运行：

```powershell
git diff --check
scripts\secrets_scan\secrets_scan.ps1
bash scripts/secrets_scan/secrets_scan.sh
git ls-files config.local.json app/libs/llm-sdk-release.aar "*.apk" "*.aab" ".codex_work/*" ".vscode/*"
```

最后一条命令应无输出。
