# Claude Stage 8B Multimodal Pipeline Prompt

> 这是写给 Claude/Opus 的**未来**任务提示词。
> **前提条件**：Stage 8A-2 reflection bridge 已完成、真机纯文本生成已通过、Settings 多模态 diagnostic 已验证。
> **不现在执行**。

---

## 前提确认

在开始 Stage 8B 工作前，确认以下条件已满足：

- [ ] Stage 8A-2 reflection bridge 已合并
- [ ] `./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug` 全部通过
- [ ] 真机 Settings 多模态 diagnostic 已运行：callVit 返回 0，generate 有输出
- [ ] `scripts/qa/stage8a4_post_bridge_static_check.ps1` 所有 BLOCKER 通过
- [ ] 阅读完成 `docs/architecture/stage8b_multimodal_learning_pipeline.md`

---

## 目标

把端侧多模态输出接入 MaterialBundle，实现最小闭环：

```
图片 → callVit → 多模态 generate → ImageUnderstandingSource → MaterialBundle → Timeline
```

---

## 实现要求

### 安全边界

1. **不上传图片**：callVit + generate 全链路本地，无网络请求。
2. **不保存原图**：理解完成后释放 Bitmap，除非用户显式选择保存。
3. **不导出本地路径**：`/sdcard/...` 等路径不进入 Export pipeline。
4. **不绕过 validators**：图像理解结果走与文本输入相同的 validation pipeline。
5. **不伪装为 OCR 原文**：每个图像理解结果必须有 `sourceLabel`。
6. **source label 区分**：4 种来源类型使用不同 label。

### 接口设计

参考 `docs/architecture/stage8b_multimodal_learning_pipeline.md` 第 3 节的新来源类型。

### 导出安全

StudyReport / Export 中图像理解结果标注为"端侧图像理解·实验性"，与 OCR 原文明确区分。

---

## 最小闭环实现（先做）

1. 图片 → RGB 转换 → callVit
2. 多模态 prompt + generate → 理解文本
3. 创建 `ImageUnderstandingSource`（含 sourceLabel, originalImageHash）
4. 加入 MaterialBundle（作为新的素材来源类型）
5. Timeline 显示图像理解条目

---

## 验证命令

```bash
# 编译
./gradlew :core:test
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug

# 静态检查
powershell -ExecutionPolicy Bypass -File scripts/qa/stage8a4_post_bridge_static_check.ps1
powershell -ExecutionPolicy Bypass -File scripts/secrets_scan/secrets_scan.ps1

# 真机验证
# 1. Settings 多模态 diagnostic 通过
# 2. 导入图片 → 查看理解结果 → 加入资料篮
# 3. Timeline 中出现图像理解条目
# 4. Ask 可以引用图像理解来源
# 5. StudyReport 显示"端侧图像理解"来源
```

---

## 禁止事项

- ❌ 不 commit AAR
- ❌ 不新增危险存储权限
- ❌ 不展示 DeepSeek/Compatible 作为主路径
- ❌ 不绕过 validators
- ❌ 不将端侧理解结果标注为"OCR 原文"
- ❌ 不声称多模态已完整学习闭环完成
