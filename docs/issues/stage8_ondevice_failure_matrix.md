# Stage 8A 端侧模型失败矩阵

| # | 风险 | 触发条件 | 预期 fallback | 检查方法 | 优先级 |
|---|---|---|---|---|---|
| 1 | SDK AAR 缺失 | 工程未引入 `llm-sdk-release.aar` | unavailable -> LocalRule | 静态审计 + build 日志 | P0 |
| 2 | `LlmManager` 类缺失 | AAR 版本不对 | unavailable | 静态审计 | P0 |
| 3 | `LlmConfig` 参数不兼容 | SDK 版本差异 | 短错误标签 | 真机 init | P0 |
| 4 | native so 缺失 | ABI 不匹配 | unavailable | 启动诊断 | P0 |
| 5 | native load crash | 设备不支持 | 捕获并显示 error | 真机启动 | P0 |
| 6 | 模型目录缺失 | `/sdcard/1225` 不存在 | LocalRule | Settings 诊断 | P0 |
| 7 | 模型文件损坏 | 目录存在但文件不完整 | unavailable | init 错误码 | P0 |
| 8 | 存储权限拒绝 | 用户拒绝 | LocalRule | 权限测试 | P0 |
| 9 | 高风险存储权限 | 误加全盘权限 | 复赛解释或移除 | Manifest 审计 | P0 |
| 10 | APU 权限缺失 | 设备需要但未授权 | unavailable | 真机错误码 | P1 |
| 11 | UI 线程阻塞 | init/generate 在主线程 | 中断并修复 | 卡顿/ANR | P0 |
| 12 | generate 超时 | 模型响应慢 | interrupt -> LocalRule | 超时测试 | P1 |
| 13 | interrupt 不生效 | 状态机错误 | release -> LocalRule | 中断测试 | P1 |
| 14 | release 泄漏 | native 资源未释放 | 重启或禁用端侧 | 多轮测试 | P1 |
| 15 | TokenCallback 乱序 | 并发生成 | 丢弃旧 session | 多次点击 | P1 |
| 16 | onError 未映射 | SDK 返回未知码 | UNKNOWN 短标签 | 错误码测试 | P1 |
| 17 | 输出太长 | 小任务无限生成 | 截断 + fallback | max token 测试 | P1 |
| 18 | 输出非 JSON | Ask/Report 需要结构化 | parser fail -> LocalRule | fake response | P0 |
| 19 | 证据不可定位 | quote 改写 | partial/not_found | EvidenceResolver | P0 |
| 20 | 绕过 validator | 直接展示模型结果 | 禁止合入 | code review | P0 |
| 21 | 密钥泄漏 | 配置进入日志/导出 | 阻塞发布 | secrets scan | P0 |
| 22 | 完整输入泄漏 | 日志保留模型输入 | 阻塞发布 | text audit | P0 |
| 23 | 完整输出泄漏 | 导出保留模型原始输出 | 阻塞发布 | export audit | P0 |
| 24 | 内部推理字段泄漏 | 报告或截图出现 | 阻塞发布 | text audit | P0 |
| 25 | 云端主路径受损 | ProviderResolver 改坏 | 回滚端侧改动 | integration test | P0 |
| 26 | LocalRule 被隐藏 | fallback 显示成 AI 成功 | 修 UI 标签 | UI smoke | P0 |
| 27 | Compatible 抢主路径 | 复赛设置误突出外部 demo | 调整 IA | Settings smoke | P1 |
| 28 | 弱网误判 | 云端失败未切端侧 | LocalProviderChain | 断网测试 | P1 |
| 29 | 设备过热/耗电 | 长时间端侧生成 | 限制 profile | 真机观察 | P2 |
| 30 | 多设备不一致 | 非目标机不支持 | unavailable | 设备矩阵 | P2 |
| 31 | 权限文案误导 | 声称已完全离线 | 修文案 | 文案审计 | P1 |
| 32 | 复赛 proof 泄漏 | 截图拍到私密输入 | 重拍并重置密钥 | proof checklist | P0 |
| 33 | StudyReport 误写端侧原文 | 报告模板拼接错误 | sanitizer | 导出检查 | P0 |
| 34 | Ask 胡编 | 问题超出课堂 | not_found | Ask bank | P0 |
| 35 | Practice 建议无依据 | 薄弱点信息不足 | 只给搜索词 | Practice smoke | P1 |
