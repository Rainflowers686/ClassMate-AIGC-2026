# Real Device Fix Matrix - 1.14.8 / versionCode 121

| 真机反馈 | 本轮修复 | 验证方式 | 仍需真机确认 |
| --- | --- | --- | --- |
| 蓝心大模型仍不可用，主功能像是绕过 BlueLM | `BlueLMProviderResolver` 同源解析设置页配置；微测、反馈、弱点变式、精修导出等增强入口配置 Ready 时先调用 `ProviderAskChatClient` | `BlueLMProviderResolverTest`、`BlueLMMainFlowIntegrationTest`、真机保存配置后最小 prompt | 真实 AppID/AppKey、网络、接口权限 |
| 微测题仍可能不是知识点习题 | BlueLM 题目返回后再做学科知识点/evidence 绑定和 answerable 校验；不合格时落回本地 evidence 题或空态 | BlueLM fake provider 生成题进入 CLOUD session；现有 Quiz/Relevance guard | 真机 BlueLM 返回质量 |
| 反馈后只像 toast | 本地即时优化保留；BlueLM 可用时追加同知识点/证据修正提示并刷新 snapshot | `BlueLMMainFlowIntegrationTest.feedbackRefinementUsesBlueLmWhenConfiguredAndRedactsCredentials` | 真机页面刷新体感 |
| ASR 不能继续依赖系统 SpeechRecognizer | 保持 1.14.6/1.14.7 主路线：官方实时 ASR、官方长语音转写、手动转写；系统 ASR 仅可选 fallback | `OfficialAsrRoutePlannerTest`、录音转写 flow tests、真机手册 | 官方 ASR 凭据和录音上传成功 |
| 本地 fallback 被误认为蓝心结果 | BlueLM client 只在 Ready 后创建，失败后 fallback 保留源标签；文档明确 fallback 不冒充 BlueLM | Provider/Export/Debug leak guards | 用户文案现场核对 |

## 不夸大口径

- BlueLM 是 AI 主功能优先 provider，不等于每台真机都已联网成功。
- 无配置时 SKIP 不是失败；有配置但失败时必须看 dry-run 分类。
- 系统 SpeechRecognizer 不是 vivo 官方 ASR。
- 本地学科过滤、题目兜底和导出 fallback 不能标成 BlueLM。
