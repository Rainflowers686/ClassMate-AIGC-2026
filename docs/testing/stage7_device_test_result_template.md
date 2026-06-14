# Stage 7 Device Test Result Template

状态枚举：PASS / FAIL / BLOCKED / SKIP

| 编号 | 模块 | 测试项 | 操作 | 预期 | 实际 | 状态 | 截图文件名 | 日志/备注 | 优先级 | 是否影响复赛演示 |
|---:|---|---|---|---|---|---|---|---|---|---|
| 1 | 安装 | APK 安装 | 安装 debug APK | 安装成功 |  |  |  |  | P0 | 是 |
| 2 | 安装 | App 启动 | 点击图标 | 进入首页 |  |  |  |  | P0 | 是 |
| 3 | 安装 | BuildInfo | 打开 Settings Build | 版本/commit 可见 |  |  |  |  | P1 | 否 |
| 4 | 设置 | Provider profile | 打开 Settings | 三路径分层可见 |  |  |  |  | P0 | 是 |
| 5 | 设置 | BlueLM 配置状态 | 查看 Official BlueLM | masked 状态正常 |  |  |  |  | P0 | 是 |
| 6 | 设置 | Compatible Demo | 查看配置 | 明确展示增强 |  |  |  |  | P1 | 否 |
| 7 | 设置 | LocalFallback | 查看配置 | 兜底可见 |  |  |  |  | P0 | 是 |
| 8 | BlueLM | Diagnostic | 点击测试连接 | OK 或短错误 |  |  |  |  | P0 | 是 |
| 9 | BlueLM | 长文本分析 | 导入高数样例 | Timeline 生成 |  |  |  |  | P0 | 是 |
| 10 | BlueLM | fallback 标注 | 断网或 local_only | 来源标注清楚 |  |  |  |  | P1 | 否 |
| 11 | Markdown | UTF-8 | 导入中文 md | 文本正常 |  |  |  |  | P0 | 是 |
| 12 | Markdown | BOM | 导入 BOM md | 不乱码 |  |  |  |  | P1 | 否 |
| 13 | Markdown | 表格 | 导入表格 md | 提取可读文本 |  |  |  |  | P1 | 否 |
| 14 | Markdown | 公式 | 导入公式 md | 不崩溃 |  |  |  |  | P1 | 否 |
| 15 | OCR | 课件 OCR | 粘贴课件 OCR | 加入资料篮 |  |  |  |  | P0 | 是 |
| 16 | OCR | 板书 OCR | 粘贴板书 OCR | 来源为板书 |  |  |  |  | P0 | 是 |
| 17 | OCR | PDF OCR | 粘贴讲义 OCR | 来源为讲义/PDF |  |  |  |  | P1 | 否 |
| 18 | OCR | 空文本 | 空 OCR 点击加入 | 被阻止或提示 |  |  |  |  | P1 | 否 |
| 19 | OCR | 来源 marker | 分析 OCR 资料 | Evidence 显示来源 |  |  |  |  | P0 | 是 |
| 20 | SRT/VTT | SRT 导入 | 导入 SRT | 时间戳保留 |  |  |  |  | P1 | 是 |
| 21 | SRT/VTT | VTT 导入 | 导入 VTT | 时间戳保留 |  |  |  |  | P1 | 是 |
| 22 | SRT/VTT | TXT 转写稿 | 粘贴转写稿 | 可加入资料篮 |  |  |  |  | P1 | 否 |
| 23 | Transcript | 改 speaker | 修改标签 | 保存成功 |  |  |  |  | P1 | 否 |
| 24 | Transcript | 合并段落 | 合并两段 | 文本顺序正确 |  |  |  |  | P1 | 否 |
| 25 | Transcript | 删除段落 | 删除一段 | 不崩溃 |  |  |  |  | P2 | 否 |
| 26 | Live 手动 | 开始课堂 | 输入标题开始 | 计时和状态正常 |  |  |  |  | P0 | 是 |
| 27 | Live 手动 | 添加片段 | 手动追加 | 片段数增加 |  |  |  |  | P0 | 是 |
| 28 | Live 手动 | 暂停继续 | 暂停再继续 | 状态正确 |  |  |  |  | P1 | 否 |
| 29 | Live 手动 | 结束生成 | 结束并分析 | 进入 Timeline |  |  |  |  | P0 | 是 |
| 30 | Live ASR | 可用提示 | 打开 ASR 实验 | 边界文案清楚 |  |  |  |  | P1 | 是 |
| 31 | Live ASR | 权限拒绝 | 拒绝麦克风 | 不崩溃，可手动 |  |  |  |  | P1 | 否 |
| 32 | Live ASR | 系统不可用 | 不支持设备 | 显示 fallback 提示 |  |  |  |  | P1 | 否 |
| 33 | Timeline | 总览 | 打开时间线 | 知识点列表可见 |  |  |  |  | P0 | 是 |
| 34 | Timeline | 来源 | 查看来源 marker | 来源清楚 |  |  |  |  | P0 | 是 |
| 35 | Evidence | quote | 打开证据 | 能追溯原文 |  |  |  |  | P0 | 是 |
| 36 | Evidence | 校验 | 查看结果 | 无伪造证据 |  |  |  |  | P0 | 是 |
| 37 | Ask | grounded | 问有证据问题 | grounded |  |  |  |  | P0 | 是 |
| 38 | Ask | partial | 问部分依据问题 | partial |  |  |  |  | P1 | 是 |
| 39 | Ask | not_found | 问无依据问题 | 不胡编 |  |  |  |  | P0 | 是 |
| 40 | Ask | OCR 来源 | 问 OCR 相关问题 | 引用 OCR 来源 |  |  |  |  | P1 | 否 |
| 41 | Quiz | 进入微测 | 点击 Quiz | 题目可见 |  |  |  |  | P0 | 是 |
| 42 | Quiz | 答对 | 选择正确项 | 显示解释 |  |  |  |  | P0 | 是 |
| 43 | Quiz | 答错 | 选择错误项 | 显示证据解释 |  |  |  |  | P0 | 是 |
| 44 | Review | 今日任务 | 打开 Review | 任务出现 |  |  |  |  | P0 | 是 |
| 45 | Review | 薄弱点 | 产生错题 | 薄弱点出现 |  |  |  |  | P1 | 是 |
| 46 | Review | 需要多练 | 标记需要多练 | 任务原因更新 |  |  |  |  | P1 | 是 |
| 47 | Practice | 入口 | 打开 Practice | 若未完成则 BLOCKED |  |  |  |  | P0 | 是 |
| 48 | Practice | 错题模式 | 进入错题练习 | 可答题或待测 |  |  |  |  | P1 | 否 |
| 49 | Practice | 搜索 | 打开练习搜索 | 只外部搜索 |  |  |  |  | P1 | 是 |
| 50 | Course Library | 搜索 | 搜课程/知识点 | 结果收窄 |  |  |  |  | P1 | 是 |
| 51 | Course Library | 筛选 | 切换 BlueLM/兜底 | 结果正确 |  |  |  |  | P1 | 是 |
| 52 | Course Library | 排序 | 切换排序 | 顺序变化 |  |  |  |  | P2 | 否 |
| 53 | Course Detail | 打开课程 | 点课程卡 | 进入详情 |  |  |  |  | P0 | 是 |
| 54 | Export | PDF | 导出 PDF | 文件可打开 |  |  |  |  | P0 | 是 |
| 55 | Export | Markdown | 导出 md | 笔记可读 |  |  |  |  | P1 | 否 |
| 56 | Export | 分享 | 系统分享 | Sharesheet 出现 |  |  |  |  | P0 | 是 |
| 57 | StudyReport | 打印 | 打开报告 | 内容结构完整 |  |  |  |  | P0 | 是 |
| 58 | Safety | 导出脱敏 | 检查报告 | 无敏感配置和内部模型内容 |  |  |  |  | P0 | 是 |
| 59 | UI | 小屏 | 云真机检查 | 按钮不溢出 |  |  |  |  | P1 | 是 |
| 60 | i18n | 中文 | 全流程扫屏 | 用户文案中文为主 |  |  |  |  | P2 | 否 |

