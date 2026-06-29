> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

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
| 閫氱敤璁剧疆 | PASS | Settings home exposes the general settings path. |
| 寮€鍙戣€呰缃?| PASS | Developer settings is separated from normal user configuration. |
| 澶栬涓庝富棰?| PASS | General settings includes appearance/theme with Focus / Flow / Vitality. |
| AI 妯″瀷閰嶇疆 | PASS | AI model configuration is under general settings, not hidden in developer settings. |
| 闅愮涓庢潈闄?| PASS | General settings includes privacy/permission explanations. |
| 瀛︿範涓庡鍑?| PASS | Learning/export settings remain visible. |
| 娌夋蹈寮忚儗鏅煶 | PASS | Ambient audio settings remain visible with authorized loop-audio copy. |

## AI Model Configuration

| Check | Result | Notes |
| --- | --- | --- |
| 钃濆績澶фā鍨?/ 瀹樻柟姣旇禌妯″瀷 | PASS | Official BlueLM/qwen path is presented as the primary official option. |
| AppID default | PASS | Default AppID is `2026374747`. |
| AppKey masked | PASS | AppKey entry is hidden by default; saved state shows configured/masked status only. |
| 淇濆瓨閰嶇疆 | PASS | Save action is present and covered by unit tests. |
| 鍒犻櫎閰嶇疆 | PASS | Delete action is present and requires confirmation. |
| 鎭㈠榛樿 AppID | PASS | Restore default AppID action is present. |
| 鍏朵粬妯″瀷 API Key | PASS | Custom model option has a masked API Key field. |
| 楂樼骇 JSON 閰嶇疆 | PASS | Advanced JSON config is collapsible. |
| 闈炴硶 JSON 閿欒鎻愮ず | PASS | Invalid JSON is rejected with a clear error and does not save. |

## Missing Key Dialog

| Check | Result | Notes |
| --- | --- | --- |
| Shows 鈥滅◢鍚庘€?| PASS | Dialog includes a dismiss action. |
| Shows 鈥滃幓璁剧疆鈥?| PASS | Dialog includes a settings navigation action. |
| 绋嶅悗缁х画 fallback | PASS | Dismiss closes the dialog without blocking the current cloud-to-on-device/manual fallback path. |
| 鍘昏缃烦杞?AI 妯″瀷閰嶇疆椤?| PASS | ViewModel sets a settings deep link to AI model config and switches to Settings tab. |
| 浼氳瘽鑺傛祦 | PASS | Prompt uses per-feature session throttling to avoid repeated prompts. |

## Secret Guard

| Check | Result | Notes |
| --- | --- | --- |
| 涓嶆樉绀哄畬鏁?key | PASS | UI state exposes masked/status fields only. |
| 涓嶆彁浜?`config.local.json` | PASS | Preflight confirms `config.local.json` is not tracked. |
| 涓嶆彁浜?`.codex_work` | PASS | No `.codex_work` files are tracked or modified. |
| 涓嶆墦鍗?config 鍐呭 | PASS | Preflight reports presence only; config content was not read. |
| 涓嶅啓鐪熷疄 key 鍒?docs/tests | PASS | This QA document contains no real key, token, endpoint, or Authorization value. |

## Forbidden Copy Check

| Phrase | Result |
| --- | --- |
| 用户可见文案一致性检查 | PASS |
| 旧模型名、旧演示路径、误导性能力声明、敏感能力入口与开发路径残留 | Not found in user-facing copy |

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

1. Open Settings and verify the hierarchy: Settings home -> 閫氱敤璁剧疆 -> AI 妯″瀷閰嶇疆.
2. Check masked input behavior for AppKey and custom API Key.
3. Save a non-production test credential locally, restart the app, and confirm configured status persists.
4. Delete the saved config and confirm the cloud path returns to missing while fallback remains available.
5. Trigger CourseAnalysis or Ask without cloud config and verify the missing-key dialog actions.
