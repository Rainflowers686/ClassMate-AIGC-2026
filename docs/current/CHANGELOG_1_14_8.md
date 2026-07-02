# ClassMate 1.14.8 / versionCode 121

提交目标：`fix(product): restore BlueLM as primary AI provider`

## 修复内容

- BlueLM 不再只是 dry-run 诊断入口。课程分析、微测生成、反馈后重总结/替换题、弱点变式、学习增强和 AI 精修导出在配置 Ready 时先尝试 BlueLM。
- 新增 `BlueLMProviderResolver`，统一 Missing / Incomplete / Invalid / Ready 判定，正式请求、dry-run、增强入口和测试 hook 使用同源配置。
- 微测 BlueLM 返回结果必须通过学科知识点过滤、evidence quote 绑定和 answerable quiz 校验；不合格题不进入推荐练习。
- 反馈后本地即时优化仍保留，同时 BlueLM 可用时追加同知识点/证据的修正提示并刷新当前 snapshot。
- 官方 ASR 主路线保持不依赖系统 SpeechRecognizer：官方实时 ASR -> 官方长语音转写 -> 手动转写；系统 ASR 仅可选 fallback。

## 风险与真机验证

- 真实 BlueLM 成功仍依赖 AppID/AppKey、网络、接口权限和服务状态。
- 本轮不读取、不提交、不打印 `config.local.json` 或任何真实密钥。
- `provider_live_smoke.ps1` 无本地配置时输出 SKIP；配置存在时应检查分类和脱敏输出。

## 验证重点

1. 保存 BlueLM 配置后，课程分析和微测会优先尝试 BlueLM。
2. BlueLM 失败时保留本地整理结果，并显示失败分类/重试路径。
3. 微测题必须绑定知识点和 evidence，不再显示课堂强调词题。
4. 反馈后当前页面能看到本地即时修正；BlueLM 可用时追加云端优化提示。
5. 导出内容不得出现 raw id、provider trace 或密钥。
