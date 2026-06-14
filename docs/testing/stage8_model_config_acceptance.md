# Stage 8A 模型配置持久化验收清单

目标：验证 Official BlueLM / qwen3.5-plus 配置可以安全保存、重启后恢复、脱敏显示，并且不会进入日志、导出、截图或 Git。

| # | 验收项 | 操作 | 预期 | 失败排查 |
|---|---|---|---|---|
| 1 | 输入配置 | 在 Settings 输入应用 ID、应用密钥、model | UI 接受配置，密钥不明文回显 | 输入校验、状态绑定 |
| 2 | 保存配置 | 点击保存 | 显示保存成功，provider 可用状态更新 | 配置仓库、加密/私有存储 |
| 3 | 重启恢复 | 完全关闭并重启 App | 当前模型和配置状态仍存在 | 持久化读取 |
| 4 | 掩码显示 | 回到 Settings | 只显示 present/masked，不显示完整密钥 | UI summary |
| 5 | 连接测试 | 点击测试连接 | 显示 provider/status/latency/短错误 | 诊断链路 |
| 6 | 删除配置 | 点击删除并确认 | 当前配置清空，Official BlueLM 显示未配置 | 删除路径 |
| 7 | 删除后重启 | 重启 App | 仍然未配置 | 持久化删除 |
| 8 | 导出安全 | 导出 PDF/HTML/Markdown/TXT | 不含真实密钥、完整模型交互或内部推理字段 | Export sanitizer |
| 9 | 日志安全 | 执行分析和 Ask 后查日志 | 只含短标签、长度、状态 | RedactedLogger |
| 10 | Git 安全 | 执行 forbidden tracked 检查 | 不追踪本地配置、密钥、构建产物 | `.gitignore` / git status |
| 11 | 截图安全 | 拍 Settings proof | 不拍完整密钥输入框 | 演示前清空或遮挡 |
| 12 | StudyReport 安全 | 打开学习报告 | 不含配置详情或完整模型交互 | StudyReport 生成器 |
| 13 | 当前模型标签 | 首页/设置显示当前 provider | 显示 Official BlueLM / qwen3.5-plus 或 LocalRule | provider summary |
| 14 | 复赛模式 | 主设置页 | 不突出 DeepSeek、Compatible Demo 或外部模型增强 | Settings IA |
| 15 | 长期扩展 | 添加模型 API 折叠入口 | 可作为中性产品能力，但不影响复赛主路径 | UI 分组 |

## 复赛推荐展示

- 展示 Official BlueLM/qwen3.5-plus 当前状态。
- 展示连接测试通过或真实短错误。
- 展示密钥只出现 masked/present。
- 不展示完整密钥输入内容。

## 不通过标准

- 重启后配置丢失。
- UI、日志、导出、StudyReport 或截图出现真实密钥。
- 将 Compatible Demo 当作复赛官方主路径。
- 连接测试失败时只显示泛化错误，无法区分配置、网络、权限、限流等短标签。
