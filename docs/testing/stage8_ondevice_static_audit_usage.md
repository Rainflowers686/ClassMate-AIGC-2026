# Stage 8A 静态审计脚本使用说明

脚本：

```powershell
.\scripts\qa\stage8_ondevice_static_audit.ps1
```

## 它做什么

- 不运行 Gradle。
- 不修改文件。
- 不读取 `config.local.json` 内容，只检查是否存在。
- 检查端侧 BlueLM 3B 相关 SDK/类名/模型目录引用。
- 检查 qwen3.5-plus 关闭思考 guard。
- 检查 Manifest 权限关键词。
- 检查 forbidden tracked files。
- 扫描可能泄漏密钥、完整模型交互或夸大宣传的文本。

## 它不做什么

- 不替代真机 smoke。
- 不替代 `:core:test` / `:app:testDebugUnitTest` / `:app:assembleDebug`。
- 不验证 SDK 真能运行。
- 不判断模型质量。

## 推荐使用时机

1. Claude 完成 Stage 8A 生产代码后。
2. 真机 smoke 前。
3. 复赛 proof 打包前。
4. 每次准备截图或导出报告前。

## 如何处理 WARN

- SDK 类名缺失：检查生产接线是否真的完成。
- 端侧模型目录缺失：真机确认 `/sdcard/1225` 是否存在。
- Manifest 权限命中：确认是否必要，尤其是全盘存储或录音权限。
- 敏感词命中：人工判断是否是文档安全说明、脚本扫描词，还是实际泄漏。
- 夸大宣传命中：改为“实验模式 / 待接入 / unavailable”。
