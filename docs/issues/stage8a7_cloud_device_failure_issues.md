# Stage 8A-7 Cloud Device Failure Issues — 20 Pre-Written GitHub Issue Drafts

> 云真机端侧 BlueLM 3B 测试失败场景的预写 Issue 草案。
> 云真机排到后，如遇对应的失败类型，复制对应 issue 到 GitHub。
> 当前状态：bridge 代码已完成，**不代表真机已跑通或已遇到这些问题**。

---

## Issue 1: Model path not found

**标题**: `[OnDevice] Model files not found at configured path on cloud device`

**复现步骤**:
1. 在云真机上安装 APK。
2. 进入 Settings → 端侧模型区域。
3. 观察模型路径显示。
4. 检查 `/sdcard/1225/` 是否真实存在模型文件。

**预期**: 模型路径下存在 `.bin` / `.param` / `.json` 等模型文件。

**实际**: 目录不存在或为空。

**初步原因**: 模型文件需预置到设备，云真机可能为空白镜像。

**验收标准**:
- [ ] 确认模型文件路径在云真机上的约定。
- [ ] 确认云真机是否支持 `/sdcard/` 持久化写入。
- [ ] 如有预置方案，更新 runbook。

---

## Issue 2: Init timeout

**标题**: `[OnDevice] LlmManager.init() times out on cloud device`

**复现步骤**:
1. 确认 AAR 存在且模型路径正确。
2. 点击 Settings 端侧 init 按钮。
3. 等待 > 15 秒。

**预期**: init 在 10 秒内完成回调。

**实际**: init 无回调或超时。

**初步原因**: Native 库加载缓慢、模型加载超时、NPU 初始化卡住。

**验收标准**:
- [ ] 确认 timeout 阈值是否需要调整（SDK 侧 vs 应用侧）。
- [ ] 确认云真机芯片型号和 NPU 驱动版本。
- [ ] logcat 中有 native 加载日志。

---

## Issue 3: Native load failed

**标题**: `[OnDevice] Native library (.so) load failed — UnsatisfiedLinkError`

**复现步骤**:
1. 在云真机上安装 APK。
2. 进入 Settings → 端侧模型区域。
3. 点击初始化。

**预期**: native `.so` 正常加载。

**实际**: `UnsatisfiedLinkError` 或 native crash。

**初步原因**: AAR 中仅有 arm64-v8a `.so`，云真机可能为 x86 或 armeabi-v7a。

**验收标准**:
- [ ] 确认云真机 CPU ABI（`adb shell getprop ro.product.cpu.abi`）。
- [ ] 如非 arm64-v8a，确认是否需要 extra ABI 的 AAR。
- [ ] 更新 AAR metadata 记录设备兼容性。

---

## Issue 4: NPU/APU unavailable

**标题**: `[OnDevice] NPU/APU acceleration unavailable on cloud device`

**复现步骤**:
1. 云真机上运行端侧 init。
2. 观察是否 fallback 到 CPU。
3. 记录推理速度。

**预期**: NPU/APU 可用，或 CPU fallback 速度可接受。

**实际**: CPU 推理速度极慢（单 token > 5 秒）或 NPU 完全不可用。

**初步原因**: 云真机芯片可能不支持 NPU，或驱动版本不匹配。

**验收标准**:
- [ ] 记录云真机芯片型号和 NPU 支持情况。
- [ ] 确认 CPU-only 推理是否在可接受范围。
- [ ] 答辩口径中注明 NPU 依赖。

---

## Issue 5: callVit failed

**标题**: `[OnDevice] LlmManager.callVit() returns non-zero on cloud device`

**复现步骤**:
1. 端侧 init 成功。
2. multimodal = true。
3. 点击 callVit（内置 2×2 Bitmap）。
4. 观察返回码。

**预期**: `callVit` 返回 0。

**实际**: 返回非 0 值。

**初步原因**: VIT encoder 未加载、Bitmap 格式不兼容、native VIT 库缺失。

**验收标准**:
- [ ] 确认 AAR 中是否包含 VIT native 库。
- [ ] 确认 Bitmap RGB_565 格式是否被 SDK 接受。
- [ ] 记录返回码和 logcat。

---

## Issue 6: Generate returns no token

**标题**: `[OnDevice] generate() returns with no tokens and no error`

**复现步骤**:
1. init 成功。
2. 输入纯文本 prompt。
3. 点击 generate。

**预期**: TokenCallback.onToken() 被调用至少一次。

**实际**: 无 token 输出，也无 onError 回调。

**初步原因**: prompt 为空或格式不被 SDK 接受、模型未完全加载、内部空响应。

**验收标准**:
- [ ] 确认 SDK 对 prompt 格式的要求。
- [ ] 测试不同长度和内容的 prompt。
- [ ] 确认 TokenCallback 回调链完整。

---

## Issue 7: onComplete not called

**标题**: `[OnDevice] TokenCallback.onComplete() never called after generate finishes`

**复现步骤**:
1. init + generate 成功。
2. 观察最后一个 token 后是否触发 onComplete。

**预期**: onComplete() 在最后一个 token 后被调用。

**实际**: 有 token 输出但 onComplete 从未触发。

**初步原因**: SDK 内部状态机未正确到达完成态、reflection bridge 中方法映射错误。

**验收标准**:
- [ ] 确认 reflection bridge 中 TokenCallback 接口定义与 AAR 匹配。
- [ ] 确认 onComplete() 无参签名（非 onComplete(LlmStats)）。
- [ ] 测试多次 generate 确认是偶现还是必现。

---

## Issue 8: onError without error code

**标题**: `[OnDevice] onError callback invoked without meaningful error code or message`

**复现步骤**:
1. 制造一个错误场景（如错误模型路径）。
2. 点击 init。
3. 观察 onError 回调参数。

**预期**: onError 传入具体错误码和错误信息。

**实际**: 错误码为 0 或 null，或信息为空字符串。

**初步原因**: SDK 内部异常未正确映射到错误码。

**验收标准**:
- [ ] 确认 SDK 错误码枚举。
- [ ] 在 reflection bridge 中映射所有已知错误码。
- [ ] UI 给用户显示"未知错误"而非空白。

---

## Issue 9: Settings preview shows empty state

**标题**: `[OnDevice] Settings ondevice panel shows blank or empty state when SDK is present`

**复现步骤**:
1. AAR 存在于 app/libs。
2. 打开 Settings → 端侧模型区域。

**预期**: 显示 SDK 状态、模型路径、操作按钮。

**实际**: 空白页面或仅部分 UI 渲染。

**初步原因**: UI state 初始化逻辑问题、recomposition 未触发。

**验收标准**:
- [ ] 确认 Settings UI state 初始化时机。
- [ ] 确认 reflection 类加载未抛未捕获异常。
- [ ] Composable 预览截图验证。

---

## Issue 10: Fallback UI not shown

**标题**: `[OnDevice] LocalRule fallback indicator not displayed when end-side model fails`

**复现步骤**:
1. 制造端侧模型不可用场景。
2. 发起提问。

**预期**: UI 显示"当前使用本地规则兜底"。

**实际**: 回复出现但无 LocalRule 标识，用户无法区分。

**初步原因**: fallback 标识 UI 未在 ProviderChain 切换时正确更新。

**验收标准**:
- [ ] 端侧不可用时 ProviderChain 正确 fallback。
- [ ] LocalRule 回复有视觉标识（前缀或颜色）。
- [ ] UI 状态与 ProviderChain 状态同步。

---

## Issue 11: Bridge crashes the app UI

**标题**: `[OnDevice] Bridge reflection call causes unhandled crash (Activity not found / NPE)`

**复现步骤**:
1. Settings 端侧区域操作。
2. 点击 init / generate / callVit 任意按钮。

**预期**: 操作正常完成或显示错误提示。

**实际**: App 闪退。

**初步原因**: reflection 调用中未捕获的异常、null 返回值未检查、主线程阻塞。

**验收标准**:
- [ ] 所有 reflection 调用包裹 try-catch。
- [ ] 异常在 UI 层降级为错误提示而非 crash。
- [ ] 主线程操作确认异步。

---

## Issue 12: Release lifecycle issue

**标题**: `[OnDevice] LlmManager.release() not called on Activity destroy, potential native memory leak`

**复现步骤**:
1. 在 Settings 端侧完成一次 init + generate。
2. 退出 Settings 或旋转屏幕。
3. 多次重复。

**预期**: release() 在适当时机调用，native 内存释放。

**实际**: 多次操作后 native 内存累积或 crash。

**初步原因**: Lifecycle observer 未正确绑定、release 时机错误。

**验收标准**:
- [ ] release() 在 Activity/Fragment onDestroy 时调用。
- [ ] 多次 init/generate/release 循环无内存增长。
- [ ] 屏幕旋转不导致多个 LlmManager 实例。

---

## Issue 13: Repeated init causes state corruption

**标题**: `[OnDevice] Calling init() twice without release() causes undefined behavior`

**复现步骤**:
1. 点击 init 一次（成功）。
2. 不点 release，直接再点 init。
3. 观察状态。

**预期**: 第二次 init 被拦截（提示"已初始化"）或正常运行。

**实际**: 状态混乱、crash 或回调重复。

**初步原因**: SDK 不支持重复 init，bridge 未做幂等守卫。

**验收标准**:
- [ ] bridge 层在 init 前检查当前状态。
- [ ] 如已初始化，拦截并提示 or 自动 release 后重新 init。
- [ ] UI 按钮在已初始化状态下禁用。

---

## Issue 14: APK ABI mismatch on cloud device

**标题**: `[OnDevice] APK only bundles arm64-v8a .so, cloud device is different ABI`

**复现步骤**:
1. 在非 arm64-v8a 云真机上安装 APK。
2. 尝试端侧 init。

**预期**: Native 正常加载或明确提示 ABI 不支持。

**实际**: `UnsatisfiedLinkError`。

**初步原因**: APK 仅含 arm64-v8a 的 `.so`。

**验收标准**:
- [ ] 记录云真机 ABI。
- [ ] 确认是否需要构建多 ABI APK（arm64-v8a + armeabi-v7a）。
- [ ] 如不可行，在 Settings 中显示"当前设备 ABI 不支持端侧模型"。

---

## Issue 15: minSdk version override

**标题**: `[OnDevice] AAR minSdk is 28, verify no conflict with app minSdk`

**复现步骤**:
1. 构建 APK 安装到 Android 版本 < 28 的设备（如有）。

**预期**: 安装失败或端侧模型不可用时有明确提示。

**实际**: 不确定。

**初步原因**: AAR minSdk 28，app minSdk 可能与 AAR 冲突。

**验收标准**:
- [ ] 确认 app minSdk 与 AAR minSdk 一致。
- [ ] 如有差异，确认行为（安装失败？功能降级？）。
- [ ] 在 build.gradle 中确认 minSdk。

---

## Issue 16: Storage path permission issue

**标题**: `[OnDevice] Model path /sdcard/1225/ cannot be read without storage permission on Android ≥13`

**复现步骤**:
1. 在 Android 13+ 设备上安装 APK。
2. 模型文件放在 `/sdcard/1225/`。
3. 端侧 init。

**预期**: SDK 能读取模型文件。

**实际**: 权限拒绝。

**初步原因**: Android 13+ 的 scoped storage 可能限制 `/sdcard/` 路径的读取。

**验收标准**:
- [ ] 确认 Android 13+ 上 `/sdcard/` 权限策略。
- [ ] 确认 SDK 内部如何访问模型文件（Java File API？native fopen？）。
- [ ] 如需要，探索 `MANAGE_EXTERNAL_STORAGE` 的替代方案（SAF、app-private）。

---

## Issue 17: Multimodal prompt format issue

**标题**: `[OnDevice] Multimodal generate() prompt format not accepted by SDK`

**复现步骤**:
1. callVit 成功返回 0。
2. 用 prompt 调用 generate（含图像 embedding）。

**预期**: generate 正常输出 token。

**实际**: generate 返回错误或无 token。

**初步原因**: 多模态 prompt 格式需要特殊前缀/后缀，或需要将 VIT 输出以特定格式传入。

**验收标准**:
- [ ] 确认 SDK 多模态 prompt 格式要求。
- [ ] 在 bridge 中正确构造多模态 prompt。
- [ ] 文档记录 prompt 格式约定。

---

## Issue 18: RGB/Bitmap conversion issue

**标题**: `[OnDevice] Bitmap RGB_565 byte[] conversion produces wrong format for callVit`

**复现步骤**:
1. 创建 2×2 RGB_565 Bitmap。
2. 转换为 byte[]。
3. 传入 callVit。

**预期**: callVit 返回 0。

**实际**: callVit 返回错误，可能因为 byte order 或 pixel format 不对。

**初步原因**: RGB_565 的 byte order（little-endian vs big-endian）或 SDK 期望 ARGB_8888。

**验收标准**:
- [ ] 确认 SDK 期望的 Bitmap 格式。
- [ ] 测试 ARGB_8888 / RGB_565 / NV21 三种格式。
- [ ] 在 bridge 中自动转换为 SDK 期望格式。

---

## Issue 19: Proof screenshot missing

**标题**: `[OnDevice] Required proof screenshots cannot be captured on cloud device`

**复现步骤**:
1. 云真机测试完成。
2. 尝试按 proof 清单截图。

**预期**: 所有关键状态有截图。

**实际**: cloud device remote client 截图功能受限，部分 UI 状态截不到。

**初步原因**: 云真机截图工具限制（无法截系统权限页、logcat 窗口等）。

**验收标准**:
- [ ] 列出无法在云真机上截取的 proof 项。
- [ ] 替代方案：终端截图、本地模拟、平台自带截图功能。
- [ ] 更新 proof 清单标注"云真机无法截图→本地补充"。

---

## Issue 20: Cloud device cannot write/access model files

**标题**: `[OnDevice] Cloud device environment prevents model file deployment to /sdcard/`

**复现步骤**:
1. 云真机分配后，尝试 adb push 模型文件。
2. 尝试在云真机 web 界面上传文件。

**预期**: 模型文件可部署到设备。

**实际**: 云真机不支持文件持久化写入；每次重启镜像重置。

**初步原因**: 云真机为临时镜像，重启后 `/sdcard/` 清空；或上传功能被限制。

**验收标准**:
- [ ] 确认云真机平台的文件持久化策略。
- [ ] 探索是否可以在 APK 中预置小模型（assets/）。
- [ ] 替代方案：使用本地真机替代云真机。
- [ ] 更新 runbook 中云真机模型部署说明。

---

## Issue 适用说明

以上 20 个 Issue 为预写草案。云真机排到后：

1. 遇到对应失败 → 复制 issue 内容 → 填入实际数值 → 在 GitHub 创建 issue。
2. 未遇到的 → 不创建，保留为草案参考。
3. 遇到新的失败类型 → 参考模板格式新增 issue。
4. 所有 issue 标记 label：`ondevice`、`stage8a7`、`cloud-device-test`。
