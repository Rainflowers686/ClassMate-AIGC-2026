# Developer Setup - ClassMate 1.14.2

## 环境

- Windows + PowerShell。
- JDK/Android Gradle 环境与项目现有 Gradle wrapper 匹配。
- Android 设备或云真机用于最终验证。

## 配置

- `config.local.json` 可存在于本机，但不得提交、不得打印内容。
- 官方能力需要 AppID/AppKey/Authorization 等配置；文档只允许写字段名和脱敏说明。
- 端侧模型依赖目标设备、SDK/AAR、模型目录和权限。

## 常用命令

```powershell
.\gradlew.bat :core:test --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
powershell -ExecutionPolicy Bypass -File scripts\qa\current_preflight.ps1
powershell -ExecutionPolicy Bypass -File scripts\qa\cloud_device_precheck.ps1
```

## 常见失败

| 失败 | 处理 |
| --- | --- |
| 蓝心未配置 | 使用端侧/本地 fallback，配置后再验证 |
| 官方 TTS 未配置 | 使用系统 TTS 或文稿 |
| 官方 ASR 未配置 | 使用系统 ASR、录音保存、手动转写 |
| 系统 ASR 不可用 | 提示安装/启用语音服务或手动转写 |
| OCR 失败 | 手动补充失败段 |
| 没有微测题 | 检查本地 fallback 是否生成 answerable quiz |
| 导出失败 | 改用 HTML/Text 或内部备份，不覆盖普通版 |
