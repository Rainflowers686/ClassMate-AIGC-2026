# Real Device Test Manual - 1.14.8 / versionCode 121

## A. BlueLM 主链路

1. 在开发者设置保存官方 BlueLM AppID/AppKey。
2. 运行官方服务 dry-run。
3. 预期：BlueLM 显示成功或明确失败分类；界面不显示密钥值。
4. 导入一段课堂文本或 OCR 文本并生成课程。
5. 预期：可看到 BlueLM 尝试痕迹/云端来源；失败时保留本地整理版并可重试。

## B. 微测生成

1. 打开课程详情或知识点页面，进入微测。
2. 预期：题干围绕学科知识点；答案详解包含知识点与 evidence 摘录。
3. 若 BlueLM 不可用，预期进入本地 evidence 题或资料不足空态，不硬造无关题。

## C. 反馈即时优化

1. 对一个微测题反馈“题目不准确”。
2. 预期：当前页面立即标记/替换，且 BlueLM 可用时会追加云端修正提示；失败时本地即时优化仍保留。
3. 对知识点反馈不准确，预期当前知识点进入需复核或更新摘要。

## D. AI 精修导出

1. 在课程详情点击 AI 精修导出。
2. 预期：BlueLM 配置 Ready 时先尝试云端精修；失败后仍保留本地 polished fallback。
3. 导出内容不得出现 raw id、provider trace、AppKey、Authorization 值。

## E. 官方 ASR 主路线

1. 未配置官方 ASR 时录音。
2. 预期：录音保存可用，界面提示可粘贴转写；不默认弹出系统 SpeechRecognizer 失败。
3. 配置官方 ASR 后录音 10 秒并停止。
4. 预期：出现官方长语音转写入口；失败时显示分类，不生成假 transcript。
