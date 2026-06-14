# Stage 5 真机完整测试路线

用途：明天真机测试按顺序执行，覆盖 Stage 5 Export Center、多来源导入、Transcript Editor、Ask、Review、Course Detail 和安全 proof。所有截图都应避开本地凭据、调试导入明文和私有配置文件。

| # | 模块 | 操作 | 预期现象 | 截图/录屏位置 | 失败时排查方向 | 复赛 proof |
|---|---|---|---|---|---|---|
| 1 | 安装 APK | 在真机或云真机安装最新 debug APK。 | App 可启动，底部 4 个 tab 为首页、复习、课程库、设置。 | App 首页首屏。 | 检查 APK 是否最新、安装权限、设备 Android 版本。 | 是 |
| 2 | Settings 导入模型配置 | 进入设置，展开调试区，导入本机临时 BlueLM/qwen 配置。 | 只显示 masked / present 状态，不展示明文凭据。 | 设置模型状态区域，避开输入框明文。 | 检查配置 JSON 格式、debug build、是否粘贴完整字段。 | 是 |
| 3 | Test BlueLM connection | 点击 BlueLM 连接测试。 | 诊断显示 OK、HTTP 200、模型名、延迟、短内容预览。 | 诊断结果卡片。 | 检查网络、模型权限、app 端是否直连、请求配置。 | 是 |
| 4 | 首页 Dashboard | 返回首页。 | 首页只显示今日概览、导入、Live、最近一课、复习摘要、模型 chip。 | 首页首屏。 | 检查是否仍有长调试内容或导出大卡片堆叠。 | 是 |
| 5 | 导入文本样例 | 首页点击“导入课堂资料”，选择示例课堂或粘贴文本，进入资料篮。 | 资料篮显示文本来源和字数。 | 选择资料类型、资料篮。 | 检查导入 flow 路由和 courseText 状态。 | 是 |
| 6 | 导入 PPT OCR | 在资料篮选择课件/PPT OCR，粘贴 PPT OCR 文本并加入资料。 | 资料篮新增“课件 OCR”资料项。 | 资料篮 OCR 项。 | 检查空文本 guard、source type 映射、手动 OCR 文案。 | 是 |
| 7 | 导入板书 OCR | 粘贴板书 OCR 文本并加入资料。 | 资料篮新增“板书 OCR”资料项。 | 资料篮 OCR 项。 | 检查 draft kind、MaterialSource 映射。 | 是 |
| 8 | 导入 SRT/VTT/TXT 转写稿 | 粘贴字幕或转写稿文本，保留时间戳。 | 资料篮显示文本资料，时间戳未被删除。 | 文本输入框和资料篮。 | 检查清洗逻辑是否误删时间戳。 | 是 |
| 9 | Transcript Editor | 打开 Transcript Editor，改 speaker、改文本、合并、删除、新增片段。 | 每个操作后片段列表正确更新，不申请录音权限。 | 编辑前后片段列表。 | 检查 session/segment state、按钮启用条件。 | 是 |
| 10 | 生成 Knowledge Timeline | 进入分析设置，填写标题/科目，点击生成。 | 进入分析进度，完成后进入课程详情或 Timeline。 | 分析中与完成页。 | 检查资料是否为空、Provider 状态、fallback 日志。 | 是 |
| 11 | Evidence 来源检查 | 打开知识时间线和证据详情。 | Evidence 可看到课堂文本、课件 OCR、板书 OCR 或字幕来源。 | Evidence 详情页。 | 检查 MaterialBundle marker、EvidenceResolver、UI source label。 | 是 |
| 12 | Ask grounded | 问本节课直接有依据的问题。 | 返回 grounded / 有证据，并显示 quote。 | Ask 回答卡片。 | 检查证据 quote 定位、Ask parser、Provider fallback。 | 是 |
| 13 | Ask partial | 问本节课只有部分依据的问题。 | 返回 partial / 部分依据，不扩展成完整外部知识。 | Ask 回答卡片。 | 检查 groundedness 降级规则。 | 是 |
| 14 | Ask not_found | 问本节课没有依据的问题。 | 返回 not_found / 未找到依据，不胡编。 | Ask 回答卡片。 | 检查无证据拒答逻辑和本地兜底模板。 | 是 |
| 15 | Quiz | 进入微测，答对和答错各一次。 | 显示答案、解释和证据，记录反馈。 | Quiz 答题与解释。 | 检查 quiz state、evidence span、LearningStore 事件。 | 是 |
| 16 | Review | 进入复习 tab。 | 今日任务、薄弱点、即将复习、已移除/已掌握区域清楚，任务按钮横向紧凑。 | Review 首屏和任务卡。 | 检查 LearningStore、ReviewTask、按钮布局。 | 是 |
| 17 | Course Library / Course Detail | 进入课程库，点击课程卡。 | 进入独立课程详情，显示概览和子入口。 | 课程库、课程详情。 | 检查 course key、history 聚合、openCourse 路由。 | 是 |
| 18 | Export Center | 从课程详情打开导出中心。 | 可选择 PDF、Markdown、MindMap、Word 兼容 HTML、Slides HTML。 | 导出中心格式选择。 | 检查 artifact 构建、格式文案是否诚实。 | 是 |
| 19 | 保存到文件 | 选择任一格式，点击保存到文件。 | 系统文件选择器出现，保存成功后提示已保存到用户选择位置。 | 系统文件选择器和成功提示。 | 检查 SAF intent、MIME、默认文件名。 | 是 |
| 20 | 保存到下载目录 | 在文件选择器选择 Downloads 或云真机可见目录。 | 文件管理器可找到导出文件。 | 文件管理器中导出文件。 | 检查用户选择路径、文件写入、文件后缀。 | 是 |
| 21 | 系统分享 | 点击分享。 | 系统分享面板出现，文件为 content URI 授权分享。 | 分享面板，不拍账号隐私。 | 检查 FileProvider、share cache、读权限 flag。 | 是 |
| 22 | 安全脱敏检查 | 打开导出文件，用搜索检查敏感类别。 | 不出现凭据、鉴权头、令牌前缀、模型输入字段、消息数组、供应商原始体、推理内容字段。 | 搜索无命中结果。 | 检查导出模板、日志拼接、history/learning store 数据。 | 是 |
| 23 | 诚实边界检查 | 查看音频、视频、OCR、speaker 相关文案。 | 明确“占位 / 待接入 / 不解析 / 不上传”，不声称真实 ASR/OCR/声纹已完成。 | 导入入口和说明文案。 | 检查 UI copy 和 capability roadmap。 | 是 |

## 失败排查表

| 现象 | 优先排查 |
|---|---|
| BlueLM diagnostic OK 但长分析失败 | 模型输出、超时 profile、max tokens、fallback 日志和 parse 状态。 |
| Evidence 无法定位 OCR 来源 | MaterialBundle marker、sourceType、segment text 是否进入 raw text。 |
| Ask 胡编 | groundedness parser、EvidenceResolver、无证据降级模板。 |
| 导出后找不到文件 | 是否使用“保存到文件”选择了用户可见目录；不要只看 app 私有目录。 |
| 分享失败 | FileProvider authority、paths XML、share cache 文件、系统是否有接收 App。 |
| 导出文件出现敏感类别 | 停止录制 proof，检查 SafeExportText、ExportCenter、history/learning store。 |
| UI 又变成长页面 | 检查是否绕过 Course Detail / Import Flow，把子功能直接堆回 tab root。 |

