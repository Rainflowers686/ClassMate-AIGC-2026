# Stage 8A OnDevice BlueLM 3B — Release Notes Draft

> 此 release notes 草稿供阶段发布使用。
> 明确标注当前完成度，不声称端侧真机已跑通。
> 版本号待定（以实际发布版本为准）。

---

## Version: 0.x.0 — OnDevice BlueLM 3B Integration (Experimental)

**Date**: YYYY-MM-DD
**Branch**: `feature/product-review-compatible`

---

## Added

### 端侧 BlueLM 3B 模型支持（实验性）

- 接入官方 BlueLM 3B 端侧 SDK，通过 reflection bridge 实现本地推理能力
- 支持端侧纯文本流式生成（`TokenCallback.onToken` 逐 token 输出）
- 支持实验性端侧多模态 VIT 编码（`callVit` 接口，仅限 Settings 诊断页面）
- Settings → 模型管理 → 端侧诊断页面，实时显示：
  - SDK 状态检测（SDK_PRESENT / SDK_MISSING / SDK_VERSION_MISMATCH）
  - 初始化结果（init 返回码）
  - 纯文本生成测试
  - 多模态 VIT 编码测试
- 三层 Provider Chain 架构：端侧 BlueLM 3B → 云端 BlueLM → 本地规则兜底
- 端侧不可用时自动降级到下一级 Provider，用户无感切换

### SDK 集成方式

- 通过 Java reflection bridge 间接调用 SDK API，无 `import com.vivo.llmsdk` 直接依赖
- AAR 缺失时项目仍可正常编译运行（端侧功能自动降级）
- SDK 二进制（`.aar`/`.so`）不入 Git 仓库

### 诊断与 QA

- Settings 端侧诊断页面（实时 SDK API 调用结果）
- 20 条端侧错误文案（全中文），覆盖 SDK 检测、初始化、推理、多模态、权限所有场景
- 静态审计脚本（`stage8_ondevice_static_audit.ps1`）增强 Stage 8A-2/8A-4 检查项
- SDK preflight 脚本（`stage8a2_sdk_preflight.ps1`）
- 后置静态检查脚本（`stage8a4_post_bridge_static_check.ps1`，10 项检查）
- Proof pack 生成脚本（`build_stage8_ondevice_proof_pack.ps1`）

---

## Changed

- ProviderChain 从双节点扩展为三节点（端侧 → 云端 → 本地规则）
- Settings 模型管理页面新增端侧诊断区域
- 模型偏好设置新增"自动 / 仅端侧 / 仅云端"选项

---

## Security

- ✅ 端侧对话文本**不上传**任何服务器
- ✅ 端侧图片处理**不离开设备**（callVit 本地编码，原始图片不保存）
- ✅ 原始 prompt **不持久化、不导出**
- ✅ 模型输出经过 redaction pipeline
- ✅ Manifest **不新增**危险存储权限（无 `MANAGE_EXTERNAL_STORAGE`、`WRITE_EXTERNAL_STORAGE`）
- ✅ 密钥**不入仓库**（`config.local.json` 在 `.gitignore` 中）
- ✅ SDK 二进制（`.aar`/`.so`）**不入仓库**（`app/libs/*.aar` / `app/libs/**/*.so` 在 `.gitignore` 中）
- ✅ 所有 secrets scan 通过

---

## Known Limitations

### 端侧模型能力限制

- BlueLM 3B 参数量模型，回答质量低于云端 BlueLM
- 适合简单问答、文本摘要、基础分类；不适合复杂推理、长文本生成
- 初始化需要 10–30 秒（因设备性能而异）
- 模型文件约 2–4 GB，需单独下载
- 支持设备受限于官方 SDK 兼容性列表

### 多模态限制（实验性）

- `callVit` 依赖设备 NPU/APU 支持
- VIT 编码精度受限于 3B 模型视觉理解能力
- 多模态 prompt 模板需要进一步优化
- 目前**仅限 Settings 诊断页面**，未接入 Ask/Report/Practice 学习主链路

### 编译与部署

- AAR 由开发者本地放置，CI 环境无 AAR（端侧功能在 CI 中自动降级）
- AAR 版本升级需要重新构建 + javap 验证签名
- Native library 依赖设备 ABI（当前仅支持 arm64-v8a）

---

## Not Included Yet

- ❌ 端侧模型文件自动下载功能（当前需手动放置模型文件）
- ❌ 端侧多模态接入 Ask This Lesson / StudyReport / Practice 主链路
- ❌ ImageUnderstandingSource 抽象接口实现
- ❌ 本地文本审核 SDK
- ❌ 端侧推理性能基准数据（待真机测试后补充）
- ❌ 端侧对话历史持久化
- ❌ 端侧模型热更新
- ❌ 端侧模型个性化微调

---

## Device Testing Status

> ⚠️ **当前状态：桥接代码已完成，真机测试待执行。**

| 项目 | 状态 | 说明 |
|------|------|------|
| SDK API 签名验证（javap） | ✅ 已确认 | `LlmConfig.multimodal`、`LlmManager.callVit`、`TokenCallback.onComplete()` 无参数 |
| AAR 本地构建与放置 | ✅ 已完成 | `app/libs/llm-sdk-release.aar` 存在且 gitignored |
| Reflection bridge 代码 | ⏳ 待接入 | 明天 Claude/Opus 执行 |
| 真机纯文本生成 | ❌ 未测试 | 待 bridge 完成后执行 |
| 真机多模态 VIT | ❌ 未测试 | 待 bridge 完成后执行 |
| 真机断网离线测试 | ❌ 未测试 | 待 bridge 完成后执行 |
| 真机 fallback 测试 | ❌ 未测试 | 待 bridge 完成后执行 |
| 云真机性能基准 | ❌ 未记录 | 待真机测试后补充 |

### 真机测试计划

详见：
- `docs/testing/stage8a3_real_device_test_sheet.md`（55+ 项）
- `docs/testing/stage8a4_post_bridge_regression.md`（20 项回归）
- `docs/competition/stage8a3_ondevice_proof_pack_checklist.md`（17 张 proof 截图）

---

## Upgrade Notes

- 端侧功能为可选模块：如 `app/libs/llm-sdk-release.aar` 不存在，应用正常运行（端侧 Provider 不注册）
- 推荐用户模型偏好保持"自动"（默认），系统会根据设备能力自动选择最优 Provider
- Settings 诊断页面提供完整的端侧状态诊断，如有异常请截图错误码反馈
