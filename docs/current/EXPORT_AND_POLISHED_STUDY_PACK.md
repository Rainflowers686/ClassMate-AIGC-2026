# Export and Polished Study Pack

## 普通导出

- 快速生成。
- 不依赖蓝心长等待。
- 始终可用。
- 使用当前 L3 snapshot：标题、摘要、知识点、微测、答案解析、复习计划、证据索引。

## AI 精修导出

- 用户主动点击。
- 使用深度/Max 长超时策略。
- 生成 `PolishedStudyPack`。
- 同一份 markdown 驱动 PDF、HTML、Word 兼容 HTML、Markdown、Text。
- 失败不覆盖普通导出；会保留已有草稿或本地学习包。

## 精修内容结构

1. 课程标题。
2. 资料来源摘要。
3. 核心知识结构。
4. 重点解释。
5. 证据摘录。
6. 易错点。
7. 考前速记。
8. 自测题。
9. 复习计划。
10. 薄弱点。
11. 下一步建议。

## 安全

- 所有导出经过 SafeExportText。
- 不输出真实密钥、Authorization、Bearer 值。
- 不输出 provider trace、BuildConfig、raw id、内部状态。
- 弱证据标为“证据待核对”，无证据标为“暂无可回溯证据”。
