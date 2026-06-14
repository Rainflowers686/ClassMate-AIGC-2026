# Codex Stage 8B Testing and Proof Prompt

> 这是写给 Codex 的**后续**任务提示词。
> **前提条件**：Claude/Opus 已完成 Stage 8B 生产代码（多模态接入 MaterialBundle + 端侧审核 seam）。
> **Codex 只做**：测试文档、proof 脚本、真机测试模板、评委问答补充。
> **不碰生产代码**。

---

## 前提确认

在开始前确认：

- [ ] Claude Stage 8B 代码已合并
- [ ] 生产代码改动在 `core/src/` 和 `app/src/` 中已完成
- [ ] 你（Codex）**不修改任何生产代码**

---

## 允许修改

* `docs/testing/`
* `docs/competition/`
* `docs/product/`
* `scripts/qa/`
* `scripts/proof/`

---

## 禁止修改

* `app/src/`
* `core/src/`
* `app/build.gradle.kts` / `core/build.gradle.kts`
* `AndroidManifest.xml`
* `.github/workflows`
* `app/libs/llm-sdk-release.aar`
* `config.local.json`
* `local.properties`

---

## 硬约束

1. **不读 `config.local.json` 内容**，只能 `Test-Path`。
2. **不运行 Gradle**（除非明确要求）。
3. **不 commit / push / tag**。
4. **不写真实密钥**。
5. **不声称端侧多模态真机已跑通**。
6. **不声称多模态已完整接入学习闭环**。
7. **不把 DeepSeek / Compatible Demo 写成复赛主路径**。
8. **不提交 AAR**。

---

## 任务

### T1: 多模态真机测试记录表

新增 `docs/testing/stage8b_multimodal_real_device_test_sheet.md`：
- 参考 `docs/testing/stage8a3_real_device_test_sheet.md` 的格式
- 覆盖：图片导入、callVit、理解文本预览、编辑、入库、Timeline、Ask 引用、Quiz 生成、StudyReport 标注
- 至少 30 项测试

### T2: 多模态 Proof 截图清单

新增 `docs/competition/stage8b_multimodal_proof_screenshot_list.md`：
- 覆盖 Stage 8B 全流程截图
- 每张截图标注不能泄露的信息

### T3: 端侧审核集成测试模板

新增 `docs/testing/stage8b_moderation_test_template.md`：
- 覆盖 result=0/1/2 的场景
- 覆盖审核失败 fallback 场景

### T4: 评委问答补充

新增 `docs/competition/stage8b_moderation_talking_points.md`：
- 为什么需要端侧文本审核
- 审核和云端审核的关系
- 审核失败时的用户影响
- 不上传审核内容

### T5: Proof Pack 增强

修改 `scripts/proof/build_stage8_ondevice_proof_pack.ps1`（如果存在）：
- 将 Stage 8B 相关文档加入 copy plan
- 不破坏已有逻辑

### T6: INDEX.md 更新

追加 Stage 8B Testing and Proof 小节。

---

## 产出清单

| 序号 | 产出 | 类型 |
|------|------|------|
| T1 | `docs/testing/stage8b_multimodal_real_device_test_sheet.md` | 测试 |
| T2 | `docs/competition/stage8b_multimodal_proof_screenshot_list.md` | Proof |
| T3 | `docs/testing/stage8b_moderation_test_template.md` | 测试 |
| T4 | `docs/competition/stage8b_moderation_talking_points.md` | 评委问答 |
| T5 | `scripts/proof/build_stage8_ondevice_proof_pack.ps1` (修改) | 脚本 |
| T6 | `docs/INDEX.md` (更新) | 索引 |

---

## 不做的

- ❌ 不做真机测试
- ❌ 不做截图拍摄
- ❌ 不做录屏
- ❌ 不运行 Gradle
- ❌ 不提交 AAR
- ❌ 不读 config.local.json
- ❌ 不修改生产代码
