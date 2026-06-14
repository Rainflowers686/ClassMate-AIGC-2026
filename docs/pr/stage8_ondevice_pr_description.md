# Stage 8A OnDevice BlueLM 3B — PR Description Draft

> 此 PR 描述草稿供明天 Claude 完成真实 reflection bridge 后使用。
> 当前状态：dev 分支（准备 merge 到 main/feature 分支）。
> 不声称端侧真机已跑通。

---

## Summary

为 ClassMate 接入端侧 BlueLM 3B（蓝心大模型 3B 参数量版本）的本地推理能力。通过 reflection bridge 对接官方 SDK (`llm-sdk-release.aar`)，实现纯文本流式生成和实验性多模态 VIT 编码。采用三层 Provider Chain 架构：端侧 → 云端 BlueLM → 本地规则兜底，端侧不可用时自动降级。

## Background

### 为什么接端侧模型

1. **隐私合规**：教育场景中未成年学生的对话数据不出设备，满足隐私法规要求。
2. **弱网/离线可用**：大学课堂、自习室、图书馆等网络不稳定环境下的基础智能服务。
3. **低延迟体验**：本地推理消除网络往返延迟，简单问答更流畅。
4. **成本控制**：端侧推理不消耗云端 API 配额。
5. **架构完整**：Provider Chain 从双节点（云端 + 本地规则）扩展为三节点（端侧 + 云端 + 本地规则），设计更鲁棒。

### 为什么现在接入

- 官方 SDK 已就绪（通过 demo 源码构建 AAR，javap 验证 API 签名）
- 复赛阶段展示完整的分层架构设计
- 为后续端云协同和离线场景奠定基础

## Changes

### 已完成（准备工作，本轮文档包）

- [x] 官方 demo 源码构建 AAR 并验证 API 签名 (`javap`)
- [x] AAR 本地放置到 `app/libs/llm-sdk-release.aar`（不入仓库）
- [x] `.gitignore` 规则：`app/libs/*.aar` / `app/libs/**/*.so`
- [x] Stage 8A-2: 真实 SDK QA 包（preflight、build record、smoke plan、multimodal bridge design、command cheatsheet）
- [x] Stage 8A-3: 真机测试计划（55+ 项）、proof pack checklist（17 screenshots）、演示脚本（6–8 min）、错误文案库（20 条）、后续 issue 草案（30 个）、proof pack 生成脚本
- [x] Stage 8A-4: 后置红队提示词（13 维度）、回归测试路线（20 项）、真机失败答辩话术（10 场景）、静态检查脚本（10 项）

### 待 Claude 接入（下一个 commit）

- [ ] Reflection bridge：`LlmConfig` / `LlmManager` / `TokenCallback` 反射调用
- [ ] `OnDeviceBlueLM` Provider 实现
- [ ] ProviderChain 注册端侧 Provider（优先级：端侧 > 云端 > 本地规则）
- [ ] Settings 端侧诊断页面（SDK_PRESENT / init / generate / callVit 实时状态）
- [ ] 多模态 diagnostic 对接（`callVit(byte[], int, int)` 仅限诊断页面）
- [ ] Fallback 逻辑：端侧失败 → 云端 → 本地规则

## API Verification (javap)

```
# 已确认的 SDK 签名
LlmConfig:  modelPath, nPredict, nCtx, nThreads, topK, topP, temperature, npuPower, multimodal
LlmManager: init(LlmConfig): int, callVit(byte[], int, int): int, generate(String, TokenCallback): void, interrupt(): void, release(): void
TokenCallback: onToken(String), onComplete() [无参数], onError(int, String)
```

## Security Boundaries

### ✅ 确保的安全边界

- AAR 不被提交（`.gitignore: app/libs/*.aar`）
- 端侧对话文本不上传任何服务器
- 端侧图片处理不离开设备（callVit 本地编码）
- 原始 prompt 不持久化、不导出
- 模型输出经过 redaction 再展示
- Manifest 不新增危险存储权限（`MANAGE_EXTERNAL_STORAGE`、`WRITE_EXTERNAL_STORAGE` 均不存在）
- 所有 SDK 调用通过 reflection bridge（无 `import com.vivo.llmsdk` 直接引用），AAR 缺失时项目仍可编译
- 密钥不入仓（`config.local.json` 在 `.gitignore` 中）

### ❌ 不做的事情

- 不将端侧多模态接入 Ask/Report/Practice 主链路（仅限 Settings 诊断页面）
- 不将 DeepSeek / Compatible Demo 作为复赛展示主路径
- 不绕过现有 validators（SafetyValidator、FormatValidator、RedactionValidator）
- 不修改云端 BlueLM 主路径的请求逻辑
- 不削弱 `enable_thinking=false` guard
- 不在日志中记录 prompt / output 内容

## Test Plan

### 自动化测试

- `./gradlew :core:test` — core 模块单元测试
- `./gradlew :app:testDebugUnitTest` — app 模块单元测试
- `./gradlew :app:assembleDebug` — 编译验证
- `secrets_scan.ps1` — 密钥扫描
- `stage8a4_post_bridge_static_check.ps1` — 10 项静态检查

### 真机测试

参照 `docs/testing/stage8a3_real_device_test_sheet.md`（55+ 项）和 `docs/testing/stage8a4_post_bridge_regression.md`（20 项回归）。

### Fallback 测试

- 端侧 SDK 缺失时：自动回退云端，不崩溃
- 端侧初始化失败时：显示错误状态，自动回退云端
- 端侧生成超时时：自动回退云端
- 云端不可用时：自动回退本地规则

## Competition Display Notes

- **主路径**：云端 Official BlueLM → 端侧 BlueLM 3B → 本地规则兜底
- **不展示**：DeepSeek / Compatible Demo 作为主路径
- **展示策略**：Settings 诊断页面证明 SDK 真实接入；演示脚本诚实标注"实验性"
- **失败话术**：见 `docs/competition/stage8a4_ondevice_failure_talking_points.md`

## Why AAR Is Not Committed

1. **安全策略**：AAR 是供应商二进制，包含未公开内部实现。
2. **授权限制**：官方 SDK 协议通常限制二进制公开分发。
3. **Git 不适合管理二进制大文件**：AAR 体积大，版本管理应走内部构建系统。
4. **验证方式**：CI 通过 javap 检查 AAR API 签名，不在 CI 中编译期强依赖 AAR。

## Risks & Mitigation

| 风险 | 缓解措施 |
|------|----------|
| 端侧 SDK 在某些设备不可用 | Provider Chain 自动 fallback 到云端 |
| 端侧推理质量不如云端 | 简单任务优先端侧，复杂任务自动路由云端 |
| AAR 版本升级 API 变更 | reflection bridge 运行时检测，签名不匹配时降级 |
| 真机环境差异导致测试无法全部通过 | 准备失败话术，将失败转化为 fallback 设计正确的论据 |
| 多模态 VIT 在部分设备不可用 | 标注为实验性功能，不进入学习主链路 |

## Related Documents

- Architecture: `docs/architecture/stage8a2_multimodal_bridge_design.md`
- Test: `docs/testing/stage8a3_real_device_test_sheet.md`
- Demo: `docs/competition/stage8a3_ondevice_demo_script.md`
- Error Copy: `docs/product/stage8a3_ondevice_error_copy.md`
- Red Team: `docs/prompts/claude_stage8a2_post_bridge_red_team.md`
- Regression: `docs/testing/stage8a4_post_bridge_regression.md`
- Judge Q&A: `docs/competition/stage8a2_judge_qna_ondevice.md`
