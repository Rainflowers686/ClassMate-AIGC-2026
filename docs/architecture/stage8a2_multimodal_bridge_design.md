# Stage 8A-2 端侧多模态 Bridge 设计

本文设计真实端侧 SDK bridge 的多模态接入方式。Stage 8A-2 只建议先做 Settings diagnostic，不直接接入 MaterialBundle 主链路。

## 1. 纯文本与多模态共用 LlmManager

纯文本和多模态都通过同一类 SDK manager 执行：

- 纯文本：`LlmConfig.multimodal = false`
- 多模态：`LlmConfig.multimodal = true`

两者应使用不同 task profile 和诊断卡片，避免用户误以为多模态已经进入正式学习链路。

## 2. 多模态流程

```text
init(multimodal=true)
Bitmap ARGB_8888 -> RGB byte[]
callVit(rgbData, width, height)
if ret == 0: generate(textInput, callback)
else: show VIT short error
```

## 3. Bitmap ARGB_8888 转 RGB byte[]

规则：

- 输入 Bitmap 必须转为 ARGB_8888。
- 遍历像素时丢弃 alpha。
- 每个像素输出 3 个 byte：R、G、B。
- 输出大小应等于 `width * height * 3`。
- 转换失败时只显示短错误，不记录图片内容或本地路径。

## 4. 输入模板

多模态文本应使用官方 demo 形式：

```text
[|Human|]:<im_start><image><im_end>用户问题
[|AI|]:
```

纯文本仍使用普通人机对话模板。不要把图片理解模板用于普通文本任务。

## 5. TokenCallback 签名

新 AAR 的 callback：

- `onToken(String)`
- `onComplete()`
- `onError(int,String)`

注意：`onComplete()` 无参数。不要使用旧版带 stats 参数的签名。

## 6. 为什么用 reflection

- AAR 被 `.gitignore` 忽略，不提交到仓库。
- CI 或其他开发机可能没有本地 AAR。
- 直接导入 SDK 包会让缺 AAR 环境无法编译。
- reflection bridge 可以在 SDK 缺失时返回 unavailable，并保持 app/core 可编译。

## 7. 为什么不直接导入 SDK 包

不直接写 `import com.vivo.llmsdk...` 的原因：

- 本地 AAR 不进 Git。
- 官方 SDK 包名或签名可能随版本变化。
- 复赛前需要保持云端主链路稳定，不让端侧实验影响核心构建。

## 8. 为什么先做 Settings diagnostic

Settings diagnostic 输入小、风险低、可截图证明：

- SDK 是否存在。
- 模型目录是否存在。
- init 是否成功。
- 纯文本 generate 是否可用。
- 多模态 callVit 是否可用。
- unavailable 时是否 LocalRule 兜底。

只有诊断稳定后，才考虑把图片理解结果接入学习链路。

## 9. 后续接入路线

长期路线：

```text
课件截图 / 板书照片 / 题目图片
-> ImageUnderstandingSource
-> MaterialBundle
-> Timeline / Ask / Quiz / Review / Report
```

接入要求：

- 图片理解结果必须有来源 marker。
- 证据引用必须能定位到图片理解来源或文字化片段。
- 不得替代 OCR 结果，只能作为补充。
- 不得绕过现有 validator。

## 10. 本轮不做

- 不做图片 picker 主流程。
- 不申请相册权限。
- 不替代 OCR。
- 不声称多模态已完整完成。
- 不将多模态输出直接写入最终学习结果。
