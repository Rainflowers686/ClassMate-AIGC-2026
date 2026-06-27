# ClassMate 真机复测清单 v1

> 版本：见 `app/build.gradle.kts`（当前 1.4.3 / versionCode 97）。设置 / 关于 / 诊断页通过 `BuildConfig` 自动读取，复测时先核对显示版本一致。

给队友真机回归用，按主流程走一遍即可，约 15 分钟。

## 主流程
首页 → 导入资料 → 课程详情 → 查看证据 → 做题 → 复习 → 导出。每一步都应能继续，不卡死、不崩溃。

## 重点测试点

1. **录音文件**
   - 开始/停止课堂录音；成功后应显示文件名、时长、大小，并能「导出录音」分享、「删除」。
   - 录音失败 / 时长为 0 / 无权限时，不应显示「录音成功」，且不生成 AUDIO 证据；提示重试或导入字幕/转写稿。

2. **OCR 确认**
   - 图片/拍照学习输入后进入草稿，可「检查识别结果」并编辑确认；确认前不并入正式资料。

3. **证据三态**
   - 复习页 / 错题 / 诊断 / 任务的证据入口分别出现：强=「查看证据」、弱=「证据待核对」、无=「暂无可回溯证据」。
   - 弱关联证据点开后，证据详情顶部应有「关联较弱，请结合原文核对」提示。

4. **题目答案与解析**
   - 每题有正确答案、每选项解析、总解析、考查知识点；无坏题（缺答案/缺选项）。

5. **导出文件**
   - 导出 PDF / Word(HTML) / Markdown / TXT；内容为复习版（摘要/知识点/微测含答案解析/错题/复习计划/证据索引）。
   - 不应出现英文 debug、provider 状态、密钥、raw id、`安全占位`；弱证据标注「证据待核对」。

6. **课程删除**
   - 首页 / 课程详情删除课程后，首页、历史、复习列表不再显示该课程；旧证据不被新课程复用。

7. **B 站搜索**
   - 「B站搜讲解」能用知识点关键词跳转搜索，不内嵌爬取。

8. **i18n 切换**
   - 设置切换中文/English/跟随系统：已迁移文案（证据三态、帮助弹窗、导航、首页/导入/历史/微测部分）应跟随语言；技术短码（BlueLM、qwen3.5-plus、JSON）保持英文。
   - 说明：部分页面文案仍为中文硬编码，尚未全量 i18n（见下）。

9. **帮助弹窗**
   - 转写页、导入页录音卡、复习页有「?」帮助入口；内容为中文/英文产品说明，无 provider/pipeline/debug。

## 不应出现的 debug token（普通学习页 / 导出）
`Semantic index`、`Tool steps`、`Import report`、`ASR Long job`、`PDF page`、`Transcript timeline`、`topHit`、`provider trace`、`SafetyPlaceholder`、`MIME`、`assetId`、裸 `kp_/q_/ev_`、enum `.name` 直接展示。
（开发者诊断页可保留技术字段，与普通页面隔离。）

## 已知未完成（诚实记录）
- 全局 i18n 未完成：仅主流程部分文案 + 证据三态 + 帮助弹窗已跟随语言；多数页面仍为中文硬编码。
- 帮助弹窗暂覆盖转写页 / 导入页 / 复习页 / 导出；其余页面后续补。
- 证据为词面弱校验，非真正语义匹配。

## 非前端工程回归点
1. **Evidence ownership**：同一个 evidenceId 必须能在当前 L3 snapshot 的 evidence/assets 中解析；跨课程、示例课或缺失 asset 的证据只能显示“证据待核对/暂无证据”，不能作为强证据。
2. **Deletion consistency**：删除课程后，history、learning snapshot、review queue、wrong book、app 私有导出草稿、evidence assets、录音缓存都不应留下坏引用；重复删除不崩溃。
3. **Recording lifecycle**：0 字节录音和失败录音不生成 AUDIO evidence；删除录音或删除课程会清理 app 私有录音文件；重启后缺失文件应降级显示，不冒充可播放证据。
4. **Export safety**：PDF / Word(HTML) / Markdown / TXT / Study Pack 均需经过 SafeExportText；不得出现 AppKey、Authorization、config.local.json、provider/debug token、裸 `kp_/q_/ev_`。
5. **Quiz completeness**：练习、导入题、错题、导出前的题目必须有可用选项、正确答案、解析和证据状态；坏题不得进入 WrongBook 或导出。
6. **Transcript parser**：SRT/VTT/TXT、BOM、畸形时间轴、空文件都应返回可理解状态；视频 metadata 只保留文件信息，不生成假转写证据。
7. **i18n domain copy**：普通用户可见的 evidence/recording/export/quiz/transcript/deletion 状态应使用中文或已接入语言资源，不把 errorCode/provider code 当主文案。
