# Stage 7 Risk Register

| 编号 | 风险描述 | 概率 | 影响 | 规避方案 | 应急方案 | 责任模块 | 是否影响复赛 |
|---:|---|---|---|---|---|---|---|
| 1 | Stage 7C Review/Practice 编译未完成 | 高 | 高 | 等 Claude 完成后再构建 | 演示跳过 Practice，标记待测 | Review/Practice | 是 |
| 2 | 系统 ASR 不可用 | 中 | 中 | 准备手动 Live 和字幕稿 | 改用手动片段 | Live ASR | 部分 |
| 3 | RECORD_AUDIO 权限被拒 | 中 | 中 | 提前说明实验模式 | 使用手动转写 | Live ASR | 否 |
| 4 | BlueLM 网络失败 | 中 | 高 | 演示前跑 diagnostic | 使用已生成样例或 LocalFallback | Provider | 是 |
| 5 | BlueLM 长文本超时 | 中 | 高 | 使用标准样例和合适模型 | 缩短文本或展示缓存记录 | Provider | 是 |
| 6 | qwen guard 被回退 | 低 | 高 | 跑脚本检查 | 恢复 guard 后重测 | Provider | 是 |
| 7 | Markdown 乱码 | 中 | 中 | 使用标准样例 | 改用粘贴文本 | Import | 部分 |
| 8 | Markdown 表格清洗差 | 中 | 低 | 选择清晰样例 | 说明为文本化导入 | Import | 否 |
| 9 | OCR 错字导致知识点偏差 | 中 | 中 | 使用预备 OCR 样例 | 人工改正 OCR 文本 | OCR | 部分 |
| 10 | OCR 来源 marker 不明显 | 中 | 中 | 提前选含 OCR 的课程 | 展示资料篮来源 | MaterialBundle | 部分 |
| 11 | SRT/VTT 时间戳格式不兼容 | 中 | 中 | 使用标准样例 | 改用 TXT 转写稿 | Transcript | 部分 |
| 12 | Transcript Editor 操作复杂 | 中 | 低 | 只展示核心操作 | 跳过高级编辑 | Transcript | 否 |
| 13 | Ask 证据漏召回 | 中 | 高 | 使用问题库 | 选择 grounded 样例 | Ask/Evidence | 是 |
| 14 | Ask 对无依据问题胡编 | 中 | 高 | 使用 not_found 样例测试 | 现场说明边界并跳过 | Ask | 是 |
| 15 | Evidence quote 定位失败 | 中 | 高 | 使用标准长课文本 | 改用 LocalFallback 结果说明 | Evidence | 是 |
| 16 | Quiz 题目质量不稳定 | 中 | 中 | 使用样例课 | 选择质量较好的题展示 | Quiz | 部分 |
| 17 | Review 任务未生成 | 中 | 高 | 先完成一次分析 | 展示历史课程任务 | Review | 是 |
| 18 | 需要多练入口不可用 | 中 | 中 | 等 Stage 7C 稳定 | 展示文档和搜索词 | Practice | 部分 |
| 19 | Practice Search 打开第三方页面暴露账号 | 中 | 中 | 不登录平台，先截图面板 | 只复制搜索词 | Practice Search | 部分 |
| 20 | Course Library 筛选误导 | 中 | 低 | 用多课程样例测试 | 只展示搜索和课程详情 | Course Library | 否 |
| 21 | Course Detail 空状态不清楚 | 中 | 低 | 选择有记录课程 | 返回 Timeline | Course Detail | 否 |
| 22 | PDF 中文渲染不理想 | 中 | 中 | 同时准备 HTML/Markdown | 演示 HTML 报告 | Export | 部分 |
| 23 | 下载目录保存失败 | 中 | 中 | 使用系统 Save As | 改系统分享 | Export | 部分 |
| 24 | Export 分享失败 | 中 | 中 | 检查接收 App | 保存到文件 | Export | 部分 |
| 25 | 导出报告过长 | 中 | 低 | 展示目录和关键页 | 切换 Markdown | StudyReport | 否 |
| 26 | 误提交密钥 | 低 | 高 | 跑 secrets scan 和 git ls-files | 立即重置密钥 | Security | 是 |
| 27 | 截图泄漏 key | 中 | 高 | 不拍 debug import | 删除截图并重置密钥 | Proof | 是 |
| 28 | 截图泄漏本地路径 | 中 | 中 | 使用样例文件名 | 裁剪或重拍 | Proof | 部分 |
| 29 | UI 按钮溢出 | 中 | 中 | 小屏 smoke | 换横屏或选关键页 | UI | 部分 |
| 30 | 中文文案不完整 | 中 | 低 | 跑 text audit | 现场避开问题页 | i18n | 否 |
| 31 | Settings 路线图夸大未实现能力 | 低 | 高 | 使用 Stage 7 文案 | 现场口头澄清 | Settings | 是 |
| 32 | 云真机限制文件选择 | 中 | 中 | 准备粘贴文本方案 | 只演示粘贴/示例 | Device | 部分 |
| 33 | 云真机网络不稳定 | 中 | 高 | 提前跑 diagnostic | 使用本地真机或缓存记录 | Device | 是 |
| 34 | GitHub Actions 最新 run 失败 | 中 | 高 | 提前检查失败原因 | 展示上一个稳定 run 并说明 | CI | 是 |
| 35 | proof pack 缺文件 | 中 | 低 | 跑 proof checker | 手动补文档 | Proof | 否 |
| 36 | demo folder 缺素材 | 低 | 低 | 跑 DryRun | 手动从 docs 复制 | Demo | 否 |

