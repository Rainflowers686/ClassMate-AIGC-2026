# Stage 8A-2 Real SDK Bridge GitHub Issues Backlog

以下为后续可复制到 GitHub Issues 的任务草案。所有任务都不能提交 AAR，不能写真实密钥，不能改坏 Official BlueLM 云端主路径。

## 1. Optional AAR Gradle include

- 目标：本地存在 AAR 时接入，缺失时仍可编译。
- 文件范围建议：app Gradle 配置、app/libs 检测文档。
- 验收标准：无 AAR 环境不崩；有 AAR 环境可加载。
- 不做事项：不提交 AAR。
- 风险：误把本地 SDK 变成 CI 必需依赖。

## 2. Reflection RealVivoOnDeviceLlmBridge

- 目标：用 reflection 包装真实 SDK。
- 文件范围建议：app ondevice bridge。
- 验收标准：缺类返回 unavailable，有类可 init。
- 不做事项：不直接导入 SDK 包。
- 风险：反射签名写错。

## 3. TokenCallback onComplete signature

- 目标：适配无参完成回调。
- 文件范围建议：端侧 callback adapter。
- 验收标准：javap 和测试都确认无参。
- 不做事项：不写旧版 stats 参数。
- 风险：运行时 callback 不触发。

## 4. Text generation diagnostic

- 目标：Settings 可测纯文本 init/generate。
- 文件范围建议：Settings 诊断、bridge。
- 验收标准：返回短输出或短错误。
- 不做事项：不进入长文本主分析。
- 风险：UI 线程阻塞。

## 5. Multimodal input template

- 目标：生成官方图片理解输入模板。
- 文件范围建议：ondevice template helper。
- 验收标准：模板包含 image marker 和用户问题。
- 不做事项：不用于普通文本任务。
- 风险：模板拼接错误。

## 6. BitmapToRgb converter

- 目标：ARGB_8888 转 RGB byte array。
- 文件范围建议：app ondevice image helper。
- 验收标准：输出长度等于 width * height * 3。
- 不做事项：不记录图片内容或路径。
- 风险：通道顺序错误。

## 7. callVit diagnostic

- 目标：Settings 可测 callVit。
- 文件范围建议：bridge、Settings card。
- 验收标准：ret == 0 才允许继续 generate。
- 不做事项：不伪装 VIT 成功。
- 风险：错误码映射不足。

## 8. Model path settings

- 目标：显示或配置模型路径。
- 文件范围建议：Settings、model config repository。
- 验收标准：默认路径可见，缺失时明确提示。
- 不做事项：不拍私密路径 proof。
- 风险：路径不适配云真机。

## 9. init/generate timeout

- 目标：端侧 init/generate 有超时保护。
- 文件范围建议：ondevice controller。
- 验收标准：超时后可 fallback。
- 不做事项：不无限等待。
- 风险：设备慢导致误判。

## 10. interrupt/release lifecycle

- 目标：支持中断和释放。
- 文件范围建议：bridge、ViewModel。
- 验收标准：连续调用不崩。
- 不做事项：不泄漏 native 资源。
- 风险：并发状态机错误。

## 11. LocalProviderChain fallback

- 目标：OnDevice -> LocalRule 可见。
- 文件范围建议：core ondevice chain。
- 验收标准：provider path 清楚。
- 不做事项：不把 LocalRule 包装成 AI 成功。
- 风险：fallback 标签误导。

## 12. Settings SDK card

- 目标：展示 SDK present/unavailable。
- 文件范围建议：Settings。
- 验收标准：可截图 proof。
- 不做事项：不展示敏感配置。
- 风险：文案夸大。

## 13. Settings multimodal card

- 目标：展示多模态 init/callVit/generate 诊断。
- 文件范围建议：Settings。
- 验收标准：未接通时显示待验证。
- 不做事项：不声称完整学习链路完成。
- 风险：评委误解能力边界。

## 14. No direct import test

- 目标：确认 app/core 无直接 SDK 包导入。
- 文件范围建议：QA script 或 unit test。
- 验收标准：扫描通过。
- 不做事项：不依赖 AAR 让 CI 失败。
- 风险：反射之外出现硬依赖。

## 15. No AAR tracked test

- 目标：确认 AAR 不被 Git 追踪。
- 文件范围建议：QA script。
- 验收标准：forbidden tracked 输出为空。
- 不做事项：不复制 AAR 到 proof pack。
- 风险：误提交二进制 SDK。

## 16. Manifest permission guard

- 目标：检查没有新增不必要危险权限。
- 文件范围建议：QA script。
- 验收标准：权限变更需明确说明。
- 不做事项：不默认加全盘存储权限。
- 风险：复赛安全解释成本上升。

## 17. Cloud BlueLM guard

- 目标：云端 Official BlueLM 不受端侧改动影响。
- 文件范围建议：provider tests。
- 验收标准：云端配置和诊断仍通过。
- 不做事项：不改主链路顺序。
- 风险：端侧实验破坏主路径。

## 18. qwen enable_thinking=false guard

- 目标：保留 qwen3.5-plus 关闭思考字段。
- 文件范围建议：core provider tests。
- 验收标准：请求构造仍包含 guard。
- 不做事项：不泛化给所有模型。
- 风险：长文本分析再次超时。

## 19. Real device smoke

- 目标：X300 Pro / vivo 云真机执行 smoke。
- 文件范围建议：docs/testing。
- 验收标准：记录设备、APK、SDK、模型目录、结果。
- 不做事项：不把失败包装成成功。
- 风险：云真机权限或模型目录差异。

## 20. Future MaterialBundle image source

- 目标：规划图片理解进入 MaterialBundle。
- 文件范围建议：core material 未来任务。
- 验收标准：有 ImageUnderstandingSource 设计。
- 不做事项：Stage 8A-2 不接主链路。
- 风险：证据定位和来源标注不足。
