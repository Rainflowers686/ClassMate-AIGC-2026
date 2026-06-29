# ClassMate Security

当前安全策略见：

- [docs/current/PRIVACY_SECURITY_AND_SECRETS.md](docs/current/PRIVACY_SECURITY_AND_SECRETS.md)
- [docs/current/SAFE_EXPORT_POLICY.md](docs/current/SAFE_EXPORT_POLICY.md)

## 核心规则

- 不提交 `config.local.json`。
- 不提交 AppKey、Authorization、Bearer、token 或任何真实密钥值。
- 不提交 AAR/APK/AAB、字体、OfficialDemos、keystore。
- QA 脚本只检查配置 presence，不读取配置内容。
- 导出内容经过 SafeExportText，不能包含真实密钥、内部状态、provider trace 或 raw id。
- 本地 fallback 不冒充蓝心；官方能力未验证不冒充已成功。

## 报告问题

如发现泄密风险或导出安全问题，提交 issue 时只使用脱敏截图/日志，不贴真实密钥或用户隐私内容。
