# Stage 8A OnDevice Documentation Consistency Audit

> 审计范围：Stage 8A / 8A-2 / 8A-3 / 8A-4 / 8A-5 全部相关文档和 QA 脚本。
> 审计日期：2026-06-05
> 审计方法：grep 全文扫描 + 手动抽查
> 结果：**全部通过，无实质性错误发现**

---

## 1. 正确事实基线

| 事实 | 状态 |
|------|------|
| SDK 包名：`com.vivo.llmsdk`（非 `com.blue.lm.sdk`） | ✅ 已确认 |
| `LlmConfig.multimodal` 类型：`public boolean` | ✅ 已确认 |
| `LlmManager.callVit` 签名：`public int callVit(byte[], int, int)` | ✅ 已确认 |
| `TokenCallback.onComplete()` 签名：**无参数**，无 `LlmStats` | ✅ 已确认 |
| AAR 路径：`app/libs/llm-sdk-release.aar`，被 `.gitignore` 忽略 | ✅ 已确认 |
| 真实 reflection bridge 由 Claude/Opus 后续接入 | ✅ 已确认 |
| 真机 init/generate/callVit 尚未证明成功 | ✅ 已确认 |
| 多模态定位：Settings diagnostic（实验性），非学习主链 | ✅ 已确认 |
| 云端 Official BlueLM / qwen3.5-plus 为复赛主路径 | ✅ 已确认 |
| `enable_thinking=false` 必须保留 | ✅ 已确认 |
| Compatible/DeepSeek 不作为复赛展示主路径 | ✅ 已确认 |

---

## 2. 检查过的文件列表

### Stage 8A（基础）
- `docs/architecture/stage8_ondevice_bluelm_architecture.md`
- `docs/competition/stage8_ondevice_talking_points.md`
- `docs/issues/stage8_ondevice_failure_matrix.md`
- `docs/product/stage8_ai_feature_boundaries.md`
- `docs/product/stage8_model_api_management_spec.md`
- `docs/prompts/claude_stage8_ondevice_red_team_review.md`
- `docs/testing/stage8_model_config_acceptance.md`
- `docs/testing/stage8_ondevice_smoke_checklist.md`
- `docs/testing/stage8_ondevice_static_audit_usage.md`
- `scripts/qa/stage8_ondevice_static_audit.ps1`

### Stage 8A-2（SDK Bridge QA）
- `docs/testing/stage8a2_manual_command_cheatsheet.md`
- `docs/testing/stage8a2_ondevice_sdk_build_record.md`
- `docs/testing/stage8a2_real_sdk_smoke_plan.md`
- `docs/architecture/stage8a2_multimodal_bridge_design.md`
- `docs/prompts/claude_stage8a2_real_sdk_bridge.md`
- `docs/issues/stage8a2_real_sdk_bridge_backlog.md`
- `docs/competition/stage8a2_judge_qna_ondevice.md`
- `docs/product/stage8a2_ondevice_user_copy.md`
- `scripts/qa/stage8a2_sdk_preflight.ps1`
- `scripts/qa/stage8a2_demo_sdk_build_helper.ps1`

### Stage 8A-3（Device Test + Proof）
- `docs/testing/stage8a3_real_device_test_sheet.md`
- `docs/testing/stage8a3_tomorrow_work_order.md`
- `docs/competition/stage8a3_ondevice_proof_pack_checklist.md`
- `docs/competition/stage8a3_ondevice_demo_script.md`
- `docs/product/stage8a3_ondevice_error_copy.md`
- `docs/issues/stage8a3_after_bridge_followups.md`
- `scripts/proof/build_stage8_ondevice_proof_pack.ps1`

### Stage 8A-4（Red Team + Regression）
- `docs/testing/stage8a4_post_bridge_regression.md`
- `docs/prompts/claude_stage8a2_post_bridge_red_team.md`
- `docs/competition/stage8a4_ondevice_failure_talking_points.md`
- `scripts/qa/stage8a4_post_bridge_static_check.ps1`

### Stage 8A-5（Milestone PR / Release）
- `docs/pr/stage8_ondevice_pr_description.md`
- `docs/release/stage8_ondevice_release_notes_draft.md`
- `docs/competition/stage8_ondevice_one_page_summary.md`
- `docs/product/stage8_ondevice_user_story_map.md`
- `docs/testing/stage8_ondevice_commit_checklist.md`

**总计：35 个文件**

---

## 3. 错误包名检查：`com.blue.lm.sdk`

| 扫描范围 | 结果 |
|----------|------|
| `docs/**/*.md` | ✅ **0 hits** — 无错误包名 |
| `scripts/**/*.ps1` | ✅ **0 hits** — 无错误包名 |

**结论**：所有文档和脚本正确使用 `com.vivo.llmsdk` 包名（或不指定具体包名而用描述性文字）。无 `com.blue.lm.sdk` 误用。

---

## 4. 旧签名检查：`onComplete(LlmStats)`

| 扫描范围 | 命中 | 上下文 |
|----------|------|--------|
| `docs/prompts/claude_stage8a2_post_bridge_red_team.md:49` | 1 | 红队审查提示词中**示例错误签名**（说明要检查的旧签名） |
| `docs/product/stage8a3_ondevice_error_copy.md:64` | 1 | 错误文案库中**对比说明**："onComplete() 无参数（不是 onComplete(LlmStats)）" |
| `docs/testing/stage8_ondevice_commit_checklist.md:49` | 1 | Checklist 中**禁止项**："B-11 onComplete(LlmStats) — 旧签名不应存在" |
| `scripts/qa/stage8a4_post_bridge_static_check.ps1` | 4 | 静态检查脚本的**检测标签**（脚本本身在检查这个模式） |

**结论**：所有 7 次命中均为**教育性引用**（说明"这是错的，不要这样做"），非生产代码中的错误使用。✅ 无实质性问题。

---

## 5. callVit 返回类型检查：`void callVit`

| 扫描范围 | 结果 |
|----------|------|
| `docs/**/*.md` | ✅ **0 hits** — 无 `void callVit` |
| `scripts/**/*.ps1` | ✅ **0 hits** — 无 `void callVit` |
| 正确引用 | `docs/testing/stage8a2_manual_command_cheatsheet.md:96` 使用 `public int callVit(byte[], int, int)` ✅ |

**结论**：所有文档正确使用 `int callVit` 返回类型。无 `void callVit` 误用。

---

## 6. 过度表述检查："真机已跑通"

| 扫描范围 | 命中 | 上下文 |
|----------|------|--------|
| `docs/**/*.md` | 8 | **全部为否定句**：`不声称端侧真机已跑通` / `不代表端侧模型已经真机跑通` |

**结论**：✅ 全部命中均为免责声明或否定表述，无过度宣称。

---

## 7. 过度表述检查："多模态完整接入学习闭环"

| 扫描范围 | 命中 | 上下文 |
|----------|------|--------|
| `docs/**/*.md` | 7 | **全部为否定/限制句**：`不声称多模态已完整学习闭环完成` / `多模态未接入主链路` / `不代表多模态已经完整进入学习主链路` |

**结论**：✅ 全部命中均为免责声明或限制说明，无过度宣称。

---

## 8. 错误建议检查："提交 AAR"

| 扫描范围 | 命中 | 上下文 |
|----------|------|--------|
| `docs/**/*.md` | 8 | **全部为否定句**：`不提交 AAR` / `不提交 llm-sdk-release.aar` / `永远不要 git add` / `不提交 SDK 二进制` |

**结论**：✅ 全部命中均为"不要提交"禁令，无错误建议。

---

## 9. 错误建议检查："新增危险存储权限"

| 扫描范围 | 命中 | 上下文 |
|----------|------|--------|
| `docs/**/*.md` | 3 | **全部为否定/合规句**：`不新增危险存储权限` / `不添加 MANAGE_EXTERNAL_STORAGE` |

**结论**：✅ 全部命中均为禁令或合规说明，无错误建议。

---

## 10. 错误表述检查："DeepSeek/Compatible 是复赛主路径"

| 扫描范围 | 命中 | 上下文 |
|----------|------|--------|
| `docs/**/*.md` | 18 | **全部为否定/区分句**：`不作为复赛展示主路径` / `Compatible Demo 仅展示增强` / `Official BlueLM 是主路径` |

**结论**：✅ 全部命中均为正确的路径区分表述，无将 Compatible/DeepSeek 写成复赛主路径的错误。

---

## 11. 错误表述检查："端侧完全替代云端"

| 扫描范围 | 命中 | 上下文 |
|----------|------|--------|
| `docs/**/*.md` | 4 | **全部为否定/限制句**：`不声称"端侧模型可完全替代云端"` / `不声称端侧完全替代云端` |

**结论**：✅ 全部命中均为禁令或诚实限制说明，无过度宣称。

---

## 12. 发现的问题与修复建议

### 无实质性错误发现

经过对 35 个文档和脚本的全面审计，**未发现任何实质性的包名错误、签名错误、返回类型错误或过度表述**。

所有文档在以下关键事实上保持一致：
- SDK 包名 `com.vivo.llmsdk`
- `callVit` 返回 `int`
- `onComplete()` 无参数
- 真机测试状态诚实标注
- 多模态实验性定位明确
- AAR 不入仓禁令
- 权限最小化
- 云端 BlueLM 主路径不变

### 文档质量评价

| 维度 | 评分 | 说明 |
|------|------|------|
| 事实一致性 | ★★★★★ | 35/35 文件一致 |
| 包名正确性 | ★★★★★ | 无错误包名 |
| API 签名正确性 | ★★★★★ | 无错误签名 |
| 过度宣称控制 | ★★★★★ | 所有"可能过度"的表述均为禁止声明 |
| 比赛口径安全 | ★★★★★ | 主路径/外部模型/多模态实验性定位均正确 |
| 交叉引用完整性 | ★★★★☆ | INDEX.md 已建立完整索引 |

### 建议（非修复）

1. **INDEX.md 维护**：随着 Stage 8A-6 加入，INDEX.md 的 §11–§16 覆盖了 Stage 8A 全部子阶段，索引结构清晰。
2. **未来文档规范**：新文档继续使用 `com.vivo.llmsdk` 包名（不缩写为 `com.blue.lm.sdk`），继续保持"真机待验证"的诚实表述。
3. **静态检查脚本**：`stage8a4_post_bridge_static_check.ps1` 已覆盖大部分检查项，建议在 Stage 8A-6 中增强过表述检测。

---

## 审计结论

**PASS** — 全部通过。Stage 8A / 8A-2 / 8A-3 / 8A-4 / 8A-5 所有文档在关键事实上保持一致，无错误包名、无错误 API 签名、无过度表述、无比赛口径风险。文档质量为高，适合提交。
