# Stage 8A-2 真实端侧 SDK 真机 Smoke 计划

本文用于明天 Claude 完成真实 SDK bridge 后进行真机 smoke。当前计划不声称端侧模型已经跑通，也不声称多模态已经进入完整学习链路。

## A. 静态检查

| 检查项 | 操作 | 预期结果 | 失败时截图 | 是否阻塞复赛演示 | fallback 策略 |
|---|---|---|---|---|---|
| Git 状态 | `git status --short` | AAR 不出现在 tracked 或 untracked 列表 | 终端截图 | 是 | 先修忽略或移除暂存 |
| AAR 忽略 | `git check-ignore -v app\libs\llm-sdk-release.aar` | 命中 `.gitignore` 的 `app/libs/*.aar` | 终端截图 | 是 | 修 `.gitignore` |
| AAR 存在 | `Get-Item app\libs\llm-sdk-release.aar` | 大小约 1.6 MB，时间符合记录 | 终端截图 | 是 | 重新复制 AAR |
| javap 多模态字段 | 运行 preflight 脚本 | `LlmConfig.multimodal` 存在 | preflight 输出 | 是 | 重新核对 AAR 版本 |
| javap callVit | 运行 preflight 脚本 | `LlmManager.callVit(byte[], int, int)` 存在 | preflight 输出 | 是 | 重新核对 AAR 版本 |
| javap onComplete | 运行 preflight 脚本 | `TokenCallback.onComplete()` 无参数 | preflight 输出 | 是 | bridge 改为新签名 |
| native libs | 检查 AAR 内 `jni` 或 `lib` | 存在 native 库 | preflight 输出 | 是 | 核对 SDK 包 |
| arm64-v8a | 检查 AAR 解包目录 | 存在 arm64-v8a | preflight 输出 | 是 | 确认目标设备 ABI |
| 直接导入检查 | 扫描 app/core | 不出现直接导入 SDK 包名 | preflight 输出 | 是 | 改为 reflection bridge |
| qwen guard | 扫描云端请求构造 | qwen3.5-plus 关闭思考逻辑仍在 | preflight 输出 | 是 | 回滚误改 |

## B. App 内 Settings 测试

| 检查项 | 操作 | 预期结果 | 失败时截图 | 是否阻塞复赛演示 | fallback 策略 |
|---|---|---|---|---|---|
| SDK_PRESENT | 打开 Settings 端侧 SDK 卡片 | 显示 SDK present / unavailable | Settings 卡片 | 是 | LocalRule 兜底 |
| modelPath | 查看模型路径 | 显示当前模型目录，不拍私密路径 | Settings 卡片 | 是 | 允许用户调整路径 |
| 纯文本 init | 点击纯文本 init | 返回 ok 或短错误码 | 诊断卡片 | 是 | unavailable -> LocalRule |
| 纯文本 generate | 输入短问题生成 | 返回短文本摘要 | 诊断结果 | 是 | LocalRule |
| interrupt | 生成中点击中断 | 生成停止，UI 不冻结 | 录屏 | 否 | release 后重试 |
| release | 点击释放或离开页面 | 无崩溃，状态清楚 | 诊断状态 | 是 | 重启 App |
| 多模态 init | 选择多模态诊断 | `multimodal=true` init 成功或短错误 | 诊断卡片 | 是 | 标记多模态 unavailable |
| Bitmap/RGB 转换 | 使用测试图片执行转换 | ARGB_8888 转 RGB byte array | 诊断摘要 | 是 | 显示转换失败 |
| callVit | 执行视觉编码 | ret == 0 或短错误码 | 诊断结果 | 是 | 不进入多模态 generate |
| 多模态 generate | callVit 成功后生成 | 返回短回答摘要 | 诊断结果 | 是 | fallback 到文本/LocalRule |
| LocalRule fallback | 禁用 SDK 或模型目录 | LocalRule 可用且标注清楚 | provider path | 否 | 继续演示核心学习链路 |

## C. vivo 云真机风险

| 风险 | 操作 | 预期结果 | 失败时截图 | 是否阻塞复赛演示 | fallback 策略 |
|---|---|---|---|---|---|
| `/sdcard/1225` 不存在 | 诊断模型目录 | 显示目录缺失 | Settings | 是 | 允许手动路径配置 |
| 模型路径不是默认值 | 检查设备模型包 | UI 支持调整或显示实际路径 | 设备信息 | 是 | 记录真实路径 |
| 需要模型配置文件 | 检查是否需要 `bluelm_mtk_llm_config.json` | 缺失时短错误 | 诊断卡片 | 是 | 补齐模型包 |
| 多模态目录差异 | 检查是否为 `/sdcard/bluelm_v_3b` | 可配置或显示缺失 | 诊断卡片 | 是 | 单独配置多模态路径 |
| 权限不足 | 拒绝或缺失存储权限 | 友好提示，不崩溃 | 权限提示 | 是 | LocalRule |
| APU runtime 不可用 | init 返回硬件错误 | 短错误标签 | 诊断卡片 | 是 | LocalRule |
| native libs 加载失败 | 启动诊断 | 显示 native load error | 诊断卡片 | 是 | LocalRule |
| init 错误码 | 触发 init 失败 | 显示错误码和短描述 | 诊断卡片 | 是 | LocalRule |
| VIT 错误码 | callVit 返回非 0 | 显示 VIT 短错误 | 诊断卡片 | 是 | 跳过多模态 |

## D. proof 注意事项

- 不拍模型密钥输入框。
- 不拍完整日志。
- 不拍本地私密目录或账号信息。
- 不把端侧 unavailable 包装成成功。
- 不声称多模态已经完整进入 Timeline/Ask/Quiz/Review/Report，除非有真实链路证据。
