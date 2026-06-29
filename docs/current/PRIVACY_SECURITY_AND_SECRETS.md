# Privacy, Security and Secrets

## 密钥边界

- 不提交 `config.local.json`。
- 不输出真实 AppKey、Authorization、Bearer 或 token。
- 文档只能出现字段名和脱敏说明。
- QA 脚本只检查 presence，不读取配置内容。

## 文件边界

- 不提交 OfficialDemos。
- 不提交 AAR/APK/AAB。
- 不提交字体文件。
- 不提交 keystore/key。

## UI 与导出边界

- 普通用户页面不显示 provider trace、raw id、内部状态。
- 导出经过 SafeExportText。
- 本地 fallback 不冒充蓝心。
- 官方能力未验证不冒充已成功。

## 官方能力边界

- 官方 TTS/ASR 已代码接入，但真实网络成功需 AppKey、权限、设备和接口状态。
- 端侧 3B 依赖设备和模型目录，不是所有手机可用。
- 外部搜索是浏览器 Intent，不是 API。
