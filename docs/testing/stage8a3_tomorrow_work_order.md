# Stage 8A-3 明天 Claude 工作执行单

> 这份执行单供明天 Claude/Opus 接手时使用。逐项执行，不跳跃。

---

## 前置确认

- [ ] 当前分支：`feature/product-review-compatible`
- [ ] `git pull` 拉取最新代码
- [ ] 确认本文件所在 commit 已推送（docs/scripts 变更已提交）
- [ ] 确认 `app/libs/llm-sdk-release.aar` 存在但未被追踪
  ```bash
  Test-Path app/libs/llm-sdk-release.aar    # 应 True
  git ls-files app/libs/llm-sdk-release.aar  # 应无输出
  ```

---

## Step 0: 运行预检

```bash
# SDK preflight（WARN-only，不运行 Gradle）
powershell -ExecutionPolicy Bypass -File scripts/qa/stage8a2_sdk_preflight.ps1

# Static audit
powershell -ExecutionPolicy Bypass -File scripts/qa/stage8_ondevice_static_audit.ps1

# Secrets scan
powershell -ExecutionPolicy Bypass -File scripts/secrets_scan/secrets_scan.ps1
```

如果任何一项 FAIL，先修复再继续。

---

## Step 1: Claude 读资料包

交给 Claude 以下文件（按顺序）：

1. `docs/prompts/claude_stage8a2_real_sdk_bridge.md` — bridge 实现提示词
2. `docs/architecture/stage8a2_multimodal_bridge_design.md` — 多模态 bridge 设计
3. `docs/product/stage8a3_ondevice_error_copy.md` — 错误文案库（bridge 写完后参考）
4. `docs/testing/stage8a3_real_device_test_sheet.md` — 测试记录表（bridge 写完后参考）

Claude 需要实现的内容（由提示词指定）：
- 真实 reflection bridge 对接 LlmManager / TokenCallback / callVit
- Provider 链路接入 OnDeviceBlueLM
- Settings 诊断页面更新
- 不碰已有生产逻辑，不引入编译错误

---

## Step 2: Claude 完成后验证

```bash
# 如果项目配置了 test 任务，运行（不运行 Gradle 则跳过）
# ./gradlew :core:test :app:testDebugUnitTest

# 静态审计重新跑
powershell -ExecutionPolicy Bypass -File scripts/qa/stage8_ondevice_static_audit.ps1

# 确认没有新的 forbidden tracked files
git ls-files config.local.json local.properties secrets.properties .env .env.* *.jks *.keystore *.apk *.aab app/build core/build build .gradle

# 确认 AAR 仍然未追踪
git ls-files app/libs/llm-sdk-release.aar

# 确认 Manifest 没有新增危险权限
Select-String -Path app/src/main/AndroidManifest.xml -Pattern "MANAGE_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE"
```

---

## Step 3: 真机测试顺序

按以下顺序执行（使用 `docs/testing/stage8a3_real_device_test_sheet.md` 记录）：

### 3.1 安装与环境（A 组）
1. `adb install` APK
2. 启动 → 不崩溃
3. Settings → 模型管理 → 端侧诊断 → 确认 SDK_PRESENT

### 3.2 纯文本（B 组）
4. init → 记录返回码
5. generate 短 prompt → 观察 token 流式输出
6. onComplete → 确认无参数触发
7. interrupt → 确认停止
8. release → 确认不崩溃
9. 断网测试 → 确认端侧仍可生成

### 3.3 多模态（C 组，如设备支持）
10. multimodal=true init
11. callVit → 记录返回码
12. generate 多模态 prompt → 观察输出

### 3.4 Fallback（D 组）
13. 模拟端侧不可用 → 确认自动切 LocalRule
14. 恢复可用 → 确认切回端侧

### 3.5 安全（E 组）
15. secrets scan 通过
16. 确认不导出 prompt/output

---

## Step 4: 如果真机测试失败

按错误码分类处理：

### SDK_PRESENT = false
- AAR 不存在 → 检查 `app/libs/` 目录
- AAR 存在但未加载 → 检查 `app/build.gradle.kts` 中 `implementation` 依赖

### init 返回非 0
- 错误码 1：模型文件不存在 → 检查 `/sdcard/1225/` 路径
- 错误码 2：native library 加载失败 → 检查 AAR 中是否包含当前 ABI 的 .so
- 错误码 3：参数错误 → 检查 LlmConfig 各字段是否正确填写

### generate 无输出
- onComplete 直接触发 → prompt 为空或无效
- onError → 记录错误码和 message
- 超时无回调 → 检查设备内存和 NPU 状态

### callVit 失败
- 返回非 0 → 检查 RGB byte 数组长度、设备 NPU 支持
- 方法不存在 → AAR 版本不支持多模态（用 javap 确认）

---

## Step 5: 提交策略

分两个 commit 提交：

### Commit A: 真实 SDK bridge 生产代码
```
feat(ondevice): wire real BlueLM 3B SDK bridge via reflection

- Add LlmManager/LlmConfig/TokenCallback reflection bridge
- Wire OnDeviceBlueLM provider into ProviderChain
- Add Settings diagnostic page for SDK status
- Update core provider interfaces for ondevice

Does NOT:
- Commit llm-sdk-release.aar (gitignored)
- Add new dangerous permissions
- Expose external model (DeepSeek/Compatible) as primary path
```

文件范围（预期）：
- `core/src/main/kotlin/.../ondevice/*.kt`（新增或修改）
- `core/src/main/kotlin/.../provider/*.kt`（修改）
- `app/src/main/kotlin/.../settings/*.kt`（修改）

### Commit B: 测试与 Proof 文档更新
```
docs(ondevice): add Stage 8A-3 device proof and follow-up pack

- Add real device test sheet (55+ items)
- Add proof pack checklist (17 screenshots)
- Add ondevice demo script (6-8 min)
- Add ondevice error copy library (20 errors)
- Add 30 post-bridge follow-up issues
- Add proof pack builder script
- Add tomorrow work order
- Update INDEX.md with Stage 8A-3 section
```

文件范围：
- `docs/testing/stage8a3_*.md`（新增）
- `docs/competition/stage8a3_*.md`（新增）
- `docs/product/stage8a3_*.md`（新增）
- `docs/issues/stage8a3_*.md`（新增）
- `scripts/proof/build_stage8_ondevice_proof_pack.ps1`（新增）
- `docs/INDEX.md`（更新）

---

## 禁止事项

1. ❌ **不提交 AAR** — `app/libs/*.aar` 在 `.gitignore` 中，永远不 `git add`
2. ❌ **不新增危险权限** — 不添加 `MANAGE_EXTERNAL_STORAGE`、`WRITE_EXTERNAL_STORAGE`
3. ❌ **不展示外部模型增强** — 复赛展示路径不出现 DeepSeek/Compatible 作为主路径
4. ❌ **不绕过 validators** — 不跳过现有的 safety / redaction / format validator
5. ❌ **不修改已有接口签名** — 如果现有 Provider 接口不支持端侧参数，新建扩展接口而非改旧接口
6. ❌ **不硬编码路径/密钥** — 所有配置通过运行时注入或 config 读取
7. ❌ **不声称端侧已真机跑通** — 如实记录测试结果

---

## 联系与回退

- 如果 bridge 实现中遇到 SDK API 与文档不符 → 停止，用 javap 确认实际签名，调整后继续
- 如果真机测试全部失败 → 检查 AAR 版本、设备兼容性；不要强行 hack
- 如果桥接代码引入编译错误 → 先确认所有 import 和依赖正确，再考虑 fallback 到 stub
