# Settings IA v2 and AI Model Config Acceptance Run

## Date / Branch / Commit

- Date: 2026-06-17 21:22:12 +08:00
- Branch: feature/product-review-compatible
- Commit: 067b45d feat(settings): add persistent AI model configuration flow
- Scope: acceptance QA only; no new feature development in this run.

## Command Verification

| Command | Result | Notes |
| --- | --- | --- |
| `git status --short` | PASS | Clean before this QA document was created. |
| `git diff --check` | PASS | No whitespace errors. |
| `scripts\qa\current_preflight.ps1 -Quick` | PASS | 15 passed, 0 failed, 2 skipped; config presence only, content not read. |
| `.\gradlew.bat :app:testDebugUnitTest` | PASS | App debug unit tests passed. |
| `.\gradlew.bat :app:assembleDebug` | PASS | Debug APK assembled successfully. |
| `scripts\qa\current_preflight.ps1` | PASS | 18 passed, 0 failed, 0 skipped; includes `:core:test`, `:app:testDebugUnitTest`, and `:app:assembleDebug`. |

## Settings IA v2

| Item | Result | Evidence |
| --- | --- | --- |
| 通用设置 | PASS | Settings home exposes the general settings path. |
| 开发者设置 | PASS | Developer settings is separated from normal user configuration. |
| 外观与主题 | PASS | General settings includes appearance/theme with Focus / Flow / Vitality. |
| AI 模型配置 | PASS | AI model configuration is under general settings, not hidden in developer settings. |
| 隐私与权限 | PASS | General settings includes privacy/permission explanations. |
| 学习与导出 | PASS | Learning/export settings remain visible. |
| 沉浸式背景音 | PASS | Ambient audio settings remain visible with authorized loop-audio copy. |

## AI Model Configuration

| Check | Result | Notes |
| --- | --- | --- |
| 蓝心大模型 / 官方比赛模型 | PASS | Official BlueLM/qwen path is presented as the primary official option. |
| AppID default | PASS | Default AppID is `2026374747`. |
| AppKey masked | PASS | AppKey entry is hidden by default; saved state shows configured/masked status only. |
| 保存配置 | PASS | Save action is present and covered by unit tests. |
| 删除配置 | PASS | Delete action is present and requires confirmation. |
| 恢复默认 AppID | PASS | Restore default AppID action is present. |
| 其他模型 API Key | PASS | Custom model option has a masked API Key field. |
| 高级 JSON 配置 | PASS | Advanced JSON config is collapsible. |
| 非法 JSON 错误提示 | PASS | Invalid JSON is rejected with a clear error and does not save. |

## Missing Key Dialog

| Check | Result | Notes |
| --- | --- | --- |
| Shows “稍后” | PASS | Dialog includes a dismiss action. |
| Shows “去设置” | PASS | Dialog includes a settings navigation action. |
| 稍后继续 fallback | PASS | Dismiss closes the dialog without blocking the current cloud-to-on-device/manual fallback path. |
| 去设置跳转 AI 模型配置页 | PASS | ViewModel sets a settings deep link to AI model config and switches to Settings tab. |
| 会话节流 | PASS | Prompt uses per-feature session throttling to avoid repeated prompts. |

## Secret Guard

| Check | Result | Notes |
| --- | --- | --- |
| 不显示完整 key | PASS | UI state exposes masked/status fields only. |
| 不提交 `config.local.json` | PASS | Preflight confirms `config.local.json` is not tracked. |
| 不提交 `.codex_work` | PASS | No `.codex_work` files are tracked or modified. |
| 不打印 config 内容 | PASS | Preflight reports presence only; config content was not read. |
| 不写真实 key 到 docs/tests | PASS | This QA document contains no real key, token, endpoint, or Authorization value. |

## Forbidden Copy Check

| Phrase | Result |
| --- | --- |
| doubao | PASS |
| 豆包 | PASS |
| 声音复刻 | PASS |
| 老师声音克隆 | PASS |
| 自动听课 | PASS |
| 已完成实时 ASR | PASS |
| 多模态替代 OCR | PASS |
| LocalRule 智能 | PASS |

## Device / Manual Execution

- Device not executed.
- This run is command-level and source/test acceptance only. No real network request, cloud-device session, or manual app navigation was executed.

## Blockers / Warnings / Polish

| Type | Count | Items |
| --- | ---: | --- |
| P0 Blocker | 0 | None |
| P1 Warning | 1 | Device/manual execution not performed in this run. |
| P2 Polish | 0 | None recorded from command-level QA. |

## Recommended Next Step

Run a focused device pass for Settings IA v2:

1. Open Settings and verify the hierarchy: Settings home -> 通用设置 -> AI 模型配置.
2. Check masked input behavior for AppKey and custom API Key.
3. Save a non-production test credential locally, restart the app, and confirm configured status persists.
4. Delete the saved config and confirm the cloud path returns to missing while fallback remains available.
5. Trigger CourseAnalysis or Ask without cloud config and verify the missing-key dialog actions.
