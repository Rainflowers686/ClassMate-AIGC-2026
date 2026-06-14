# Stage 8B 端侧多模态进入学习主链 — 架构设计

> 当前状态：AAR 已确认支持 `multimodal` / `callVit`，生产 bridge 待 Claude 接入，真机待验证。
> 本文定义从 Settings diagnostic 到完整学习闭环的渐进路线。
> 不声称端侧真机已跑通，不声称多模态已完整接入学习闭环。

---

## 1. 当前状态

| 项目 | 状态 |
|------|------|
| AAR 包含 `LlmConfig.multimodal` | ✅ javap 已确认 |
| AAR 包含 `LlmManager.callVit(byte[],int,int)` | ✅ javap 已确认 |
| 生产 reflection bridge | ❌ 待 Claude 接入 |
| Settings 多模态 diagnostic | ❌ 待 bridge 后 |
| MaterialBundle 主链接入图片理解 | ❌ 本文规划 |
| 真机端侧多模态验证 | ❌ 待 bridge 后 |

---

## 2. 目标链路

```
课件截图 / 板书照片 / 题目图片 / PDF 页面截图
        │
        ▼
   Bitmap (ARGB_8888)
        │
        ▼
   RGB byte[] 转换 (width × height × 3)
        │
        ▼
   callVit(rgbBytes, width, height)  → 返回 0 = 成功
        │
        ▼
   多模态 prompt 模板 (文本 + image token)
        │
        ▼
   generate(multimodalPrompt, callback)  → 流式 token 输出
        │
        ▼
   Image Understanding Text (结构化文本)
        │
        ▼
   ImageUnderstandingSource (来源标注)
        │
        ▼
   MaterialBundle (资料融合)
        │
        ▼
   CourseAnalyzer / Ask / Quiz / Review / StudyReport
```

---

## 3. 新来源类型建议

在 `MaterialBundle` / `ImageUnderstandingSource` 中引入以下枚举值：

| 来源类型 | 说明 | 输入格式 |
|----------|------|----------|
| `SLIDE_IMAGE_UNDERSTANDING` | 课件 PPT 截图理解 | Bitmap (端侧 VIT) |
| `BLACKBOARD_IMAGE_UNDERSTANDING` | 板书/白板照片理解 | Bitmap (端侧 VIT) |
| `EXERCISE_IMAGE_UNDERSTANDING` | 练习题/试卷截图理解 | Bitmap (端侧 VIT) |
| `PDF_PAGE_IMAGE_UNDERSTANDING` | PDF 页面截图理解 | Bitmap (端侧 VIT) |

每个来源类型携带：
- `sourceLabel: String` — 用户可见标签（如"课件截图·端侧理解"）
- `originalImageHash: String` — 原图哈希（不存原图，但可追溯）
- `vitModelVersion: String` — VIT 模型版本
- `understandingTimestamp: Long` — 理解生成时间
- `confidenceLevel: ImageUnderstandingConfidence` — 置信度（LOW/MEDIUM/HIGH）

---

## 4. 为什么不能直接把模型回答当知识点

端侧图像理解的输出**不能**直接等同于 OCR 原文或教材知识点：

| 问题 | 防护措施 |
|------|----------|
| 模型可能"幻觉"图片中不存在的内容 | **Evidence marker**：标注"端侧图像理解生成文本，非原始教材文字" |
| 理解结果可能是错误的/不准确的 | **Validator**：格式校验 + 置信度阈值检查 |
| 结果可能包含不当内容 | **Redaction**：敏感信息脱敏 pipeline |
| 无法追溯理解来源 | **Source backlink**：保留 originalImageHash，用户可回溯到原始图片 |
| 与 OCR/手动输入混淆 | **Source label**：明确区分 `IMAGE_UNDERSTANDING` vs `OCR_TEXT` vs `MANUAL_INPUT` |

---

## 5. Prompt 设计

### 5.1 课件截图提取

```
你是一个课堂课件分析助手。请分析这张课件截图的文字内容。

要求：
1. 提取课件中可见的文字内容（标题、要点、公式）
2. 按原始层级结构组织（标题 > 子标题 > 要点）
3. 不要编造图片中不存在的文字
4. 如果公式/图表无法完整识别，标注"[端侧理解：此部分可能不完整]"
5. 输出格式：Markdown
```

### 5.2 板书结构化

```
你是一个板书内容整理助手。请分析这张板书照片。

要求：
1. 提取板书中的所有文字内容
2. 识别板书的结构（标题、分栏、步骤编号等）
3. 对模糊不清的部分标注"[板书此处模糊]"
4. 不要猜测板书之外的背景内容
5. 输出格式：结构化文本（标题 + 要点列表）
```

### 5.3 题目解释

```
你是一个习题分析助手。请分析这道题目的内容。

要求：
1. 识别题目类型（选择/填空/简答/计算）
2. 提取题目正文
3. 如果包含公式/图表，描述其内容（不要尝试精确渲染）
4. 给出解题思路提示（非完整答案）
5. 标注"端侧理解：此解释来自图像分析，建议结合原始题目核对"
```

### 5.4 图表解释

```
你是一个图表解读助手。请分析这张图表。

要求：
1. 描述图表类型（柱状图/折线图/流程图/示意图等）
2. 提取图表的坐标轴标签和数据趋势
3. 解释图表传达的核心信息
4. 标注"端侧理解：图表数据为视觉估计，可能存在偏差"
```

### 5.5 公式识别失败时的兜底

```
你识别到图片中包含数学/物理公式。端侧 VIT 对复杂公式的识别精度有限。

建议：
1. 将图片上传至云端 BlueLM 进行精确公式识别（需要网络）
2. 手动输入公式（使用 LaTeX 编辑器）
3. 使用此公式的参考文本替代（如果已知公式名称）
```

---

## 6. 安全边界

| 边界 | 要求 |
|------|------|
| 不上传图片 | 端侧 callVit + generate 全链路本地，无网络传输 |
| 不保存原图 | 理解完成后释放 Bitmap，除非用户主动保存到资料库 |
| 不导出本地路径 | StudyReport/Export 中不包含 `/sdcard/...` 等路径 |
| 不导出 raw prompt | prompt 模板不包含在导出报告中 |
| 不伪装为原始教材文字 | 始终标注 sourceLabel，区分"端侧理解"和"原始文本" |
| 不绕过 validators | 图像理解输出经过与文本输入相同的 validation pipeline |
| 不泄漏图片内容到日志 | 日志仅记录 callVit 返回码和理解文本长度，不记录内容 |

---

## 7. 分阶段路线

| 阶段 | 目标 | 产出 | 依赖 |
|------|------|------|------|
| **8B-1** | Settings 多模态 diagnostic | callVit + generate 在 Settings 诊断页面可用 | Stage 8A-2 bridge 完成 |
| **8B-2** | 图片理解文本预览 | 用户可预览理解结果，可编辑 | 8B-1 完成 |
| **8B-3** | ImageUnderstandingSource 接口 | 抽象接口定义 + 端侧实现 | 8B-2 完成 |
| **8B-4** | MaterialBundle 接入 | 图片理解结果进入资料篮 | 8B-3 完成 |
| **8B-5** | Ask/Quiz/Report 使用图像来源 | 学习主链路可引用图像理解结果 | 8B-4 完成 |
| **8B-6** | 真机性能与隐私 proof | 性能基准、截图、评委材料 | 8B-5 + 真机环境 |
