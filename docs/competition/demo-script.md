# Demo script (复赛展示)

A tight ~3-minute path that shows the differentiation, screenshot-ready at each step.

1. **Home** — one line: "证据式课堂理解 + 自适应微测复习". Tap **一键体验示例课（级数）**.
2. **Analyze** — the product flow (not an engineering log): 读取 → 蓝心理解 → 知识点抽取 →
   证据校验 → 微测生成 → 复习计划. *Screenshot.*
3. **Knowledge timeline** — 8 real concepts (级数收敛与发散, p 级数, …), each with importance /
   difficulty and an amber **evidence** snippet. Note the provenance chip. *Screenshot.*
4. **Evidence detail** — tap a knowledge point: the original paragraph with the cited span
   highlighted, and "为什么这个知识点成立". This is the trust story. *Screenshot.*
5. **Quiz** — answer an `ERROR_ANALYSIS` question (通项趋于零 ⇒ 收敛？). After answering: correct
   option, per-option rationale, explanation, and the cited evidence. *Screenshot.*
6. **Review plan** — steps with 做什么 / 为什么 / 预计几分钟, bound to knowledge points; basis chips
   show importance / difficulty / 错题 / 反馈. *Screenshot.*
7. **Feedback** — tap "已掌握" or "证据不对", return to Review, hit refresh: the plan re-orders.
   This is the closed loop. *Screenshot.*
8. **Settings** — switch theme (Focus → Vitality → Flow) live; show the redacted log lines and the
   "keys never in repo" panel. *Screenshot.*

## Talking points

- "蓝心大模型是主路径" — show Settings provider order; explain fallback only catches failures.
- "每个结论可追溯" — the evidence highlight is the same data the validator checks.
- "微测服务学习" — point at the question *types*, not text matching.
- "安全" — redacted logs + placeholder-only example config.
