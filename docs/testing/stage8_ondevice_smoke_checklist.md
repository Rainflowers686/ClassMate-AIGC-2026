# Stage 8A 端侧 BlueLM 3B 真机 Smoke 清单

本清单用于 X300 Pro / vivo 云真机或实体机验收。当前为测试计划，不代表端侧 SDK 已经接通。

| # | 检查项 | 操作 | 预期结果 | 失败排查 | 是否阻塞复赛演示 | 截图建议 |
|---|---|---|---|---|---|---|
| 1 | APK 安装 | 安装当前 debug APK | App 可启动 | 检查签名、存储、系统版本 | 是 | 桌面图标或安装成功页 |
| 2 | 设备环境 | 记录设备型号 | 明确是否为目标 vivo 设备 | 云真机配置页 | 是 | 设备信息，避免账号隐私 |
| 3 | 模型目录 | 检查 `/sdcard/1225` 是否存在 | 存在则继续；不存在显示 unavailable | 模型包路径或权限 | 是 | Settings 诊断 |
| 4 | SDK AAR | 确认 `llm-sdk-release.aar` 已被工程引用 | 依赖可解析 | 检查本地 SDK 文件 | 是 | 依赖状态，不拍私密路径 |
| 5 | native library | 启动后执行诊断 | 加载成功或给出短错误 | ABI、so 路径、设备能力 | 是 | 诊断卡片 |
| 6 | Settings 入口 | 打开端侧模型诊断 | 显示 available / unavailable / error | provider 注册或 UI 状态 | 是 | 脱敏诊断 |
| 7 | init 成功 | 点击初始化测试 | 后台执行，UI 不冻结 | 模型目录、native、线程 | 是 | 状态变化 |
| 8 | init 失败 | 移除模型目录或禁用 SDK | 显示 unavailable，不崩溃 | 异常捕获和映射 | 是 | 错误提示 |
| 9 | generate 短输入 | 输入短句执行生成 | 有短输出或明确错误码 | 回调、线程、模型状态 | 是 | 只拍短输出摘要 |
| 10 | TokenCallback | 观察增量和完成回调 | 有增量 token 和完成状态 | 回调生命周期 | 是 | 安全输出摘要 |
| 11 | onError | 模拟缺模型或无权限 | 显示短错误标签 | 错误码映射 | 是 | 错误标签 |
| 12 | interrupt | 生成中点击中断 | 生成停止，UI 可继续 | interrupt 线程和状态机 | 否 | 中断状态 |
| 13 | release | 退出或切 provider | 资源释放，无崩溃 | native release | 是 | 诊断状态 |
| 14 | 弱网 | 限制网络后测端侧短输入 | 端侧可本地运行；云端失败不影响端侧 | 是否误走云端 | 是 | provider path |
| 15 | 离线 | 飞行模式下测端侧 | 可用则生成；不可用则 LocalRule | 链路选择 | 是 | provider path |
| 16 | SDK 不存在 | 不放入 SDK | unavailable，不伪装可用 | 依赖探测 | 是 | unavailable |
| 17 | 模型目录缺失 | 不提供 `/sdcard/1225` | 友好提示目录缺失 | 目录探测 | 是 | 目录提示 |
| 18 | 权限拒绝 | 拒绝存储相关权限 | 友好提示，不崩溃 | 权限分支 | 是 | 权限提示 |
| 19 | 不保存完整输入 | 执行生成后查日志和导出 | 只保留短标签、长度、状态 | redaction policy | 是 | 搜索结果 |
| 20 | 不保存完整输出 | 检查日志、报告、导出 | 不含模型完整输出或内部推理字段 | export safety | 是 | 搜索结果 |
| 21 | 不泄漏密钥 | 执行 StudyReport/PDF/分享 | 不含真实应用标识和密钥 | secrets scan / text audit | 是 | 搜索结果 |
| 22 | 云端主路径 | 切回 Official BlueLM 诊断 | 云端主路径仍可用或给真实短错误 | ProviderResolver | 是 | Official BlueLM 卡片 |
| 23 | LocalRule fallback | 禁用 SDK 与网络 | LocalRule 可用且标注清楚 | LocalProviderChain | 否 | fallback path |
| 24 | Ask 端侧兜底 | 对已分析课程提问 | 有证据则回答；无证据拒答 | evidence mapping | 是 | Ask 结果 |
| 25 | Report 建议 | 生成学习报告建议 | 只基于结构化结果 | 是否引入模型原始交互 | 否 | 报告段落 |
| 26 | Practice 建议 | 对薄弱点生成建议 | 标注 provider path，不改规则状态 | Review/Practice 分层 | 否 | 建议卡片 |
| 27 | 长输入保护 | 输入长课堂文本请求端侧 | 不默认走端侧长文本主分析 | profile gating | 是 | profile 标签 |
| 28 | UI 卡顿 | 连续 init/generate/release | UI 无 ANR | 后台线程 | 是 | 录屏 |
| 29 | 重启后状态 | 重启 App | 诊断状态可重新获取 | persisted state | 否 | Settings |
| 30 | 截图安全 | 准备 proof 截图 | 不拍密钥、私有路径、完整日志 | proof checklist | 是 | proof 文件 |

## 记录模板

| 日期 | 设备 | APK | SDK 状态 | 模型目录 | 结论 | 负责人 |
|---|---|---|---|---|---|---|
| TODO | TODO | TODO | TODO | TODO | TODO | TODO |

## 诚实边界

- 如果 SDK、设备或模型目录不可用，只能写 unavailable / 待接入。
- 真机 smoke 不等于长期稳定性测试。
- 不展示真实密钥、本地私密路径、账号信息或完整模型交互。
