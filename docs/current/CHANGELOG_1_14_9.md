# ClassMate 1.14.9 / versionCode 122 变更记录

## 版本定位

1.14.9 是 1.14.8 BlueLM 主链路恢复后的真机回归收口版，重点修复练习完成自动退出、微测入口数据源分叉、课堂强调词污染复习/总结，以及 qwen3.5-plus 官方参数映射和正式请求超时策略。

## 本轮修复

- 练习完成后停留在练习页，显示完成摘要，由用户主动选择继续练习、查看解析、返回课程或重新生成微测。
- 课程详情与知识点时间线的微测入口统一进入 Practice 主流程，不再跳旧 Quiz 页面。
- 复习计划、课程总结、相关知识点和微测题继续只接收 evidence-bound subject knowledge points；新增“牛顿第二定律”真机噪声样例守卫。
- BlueLM 主模型底层按 qwen3.5-plus 官方 OpenAI 风格接口请求；用户页面只展示“蓝心 / 蓝心大模型”。
- 三档模式映射：
  - 快速：`reasoning_effort=low`，`enable_thinking=false`，正式超时约 5 分钟。
  - 均衡：`reasoning_effort=medium`，`enable_thinking=false`，正式超时约 6 分钟。
  - 专业：UI 可称 Max/专业，API 发送 `reasoning_effort=high`，`enable_thinking=true`，正式超时约 10 分钟。
- qwen 返回中的 `reasoning_content` 只作为协议元数据忽略，不展示给普通用户，不写入导出。
- dry-run 和 `provider_live_smoke.ps1` 仍保持短超时，用于诊断，不作为主流程开关。

## 风险

- BlueLM、官方 ASR/TTS/OCR 的真实网络成功仍依赖 AppID/AppKey、接口权限、网络和服务端状态。
- 本地 fallback 保留为学习闭环兜底，但不会标注为 BlueLM 成功。
- 仍需真机确认长等待取消、弱网下 fallback 保留和不同入口微测题数量一致性。
