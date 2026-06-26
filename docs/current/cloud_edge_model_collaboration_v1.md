# BlueLM Cloud + Edge Model Collaboration

## 中文摘要（当前真实状态）

路由为 云端蓝心(BlueLM) → 端侧 3B → 本地基础整理(LOCAL_RULE)，每个结果带来源与是否兜底，诚实分级。云端就绪需真机凭据；端侧就绪需模型目录 + 全文件权限。云端失败自动尝试端侧；端侧失败（如权限缺失）且有可用输入时，自动进入本地基础整理，产出真实知识点 / 题目 / 证据，而非空安全占位。安全占位仅限输入为空 / 安全审核拒绝 / 本地规则也失败 / 示例课。端侧多模态保持「诊断 / 待验证」，不写成完整可用。

---

Date: 2026-06-21

No credential, endpoint, request body, or `config.local.json` content is recorded.

## Routing Contract

ClassMate uses this route for learning assets:

```text
BLUE_LM_CLOUD -> EDGE_3B -> LOCAL_RULE
```

| Route | Product role | Fallback rule |
| --- | --- | --- |
| BLUE_LM_CLOUD | Organize classroom material into summary, knowledge points, quiz, review plan, and grounded Ask results. | If unavailable, try edge 3B for basic explanation and review guidance. |
| EDGE_3B | Weak-network, offline, and privacy-sensitive fallback for summary, quiz draft, review advice, and export draft. | If device resources are unavailable, local rules still publish a minimum study loop. |
| LOCAL_RULE | Guaranteed minimum path for evidence, quiz templates, review queue, diagnosis, and study pack export. | Never blocks the user from continuing. |

## CapabilityPlan Fields

Each plan step should explain:

- `primaryModelRoute`
- `fallbackModelRoute`
- reason
- output surface
- user-visible benefit
- risk level
- whether confirmation is required

User-facing copy should describe learning outcomes, not internal technical plumbing.

## Current User-Visible Benefits

- Text/material input: cloud model organizes class highlights; edge/local fallback keeps summary, quiz, and review usable.
- OCR/image input: image text becomes evidence; manual confirmation remains available.
- Document input: document snippets and page hints become traceable evidence.
- Audio input: transcript becomes timeline evidence; dialect mode marks uncertain segments.
- WrongBook/Review: similarity and mastery signals adjust review priority.
- Export: study pack includes cloud/edge/local route note without internal provider details.
