# Design Handoff Summary

本文只根据 `docs/design_refs` 的文件名和少量标题整理交接方向，不复制大段 HTML。

## 已有设计参考

- `classmate_design_system.html`：ClassMate 三主题设计系统参考，包含 token 和组件方向。
- `classmate_focus.html`：Focus 主题参考。
- `classmate_flow.html`：Flow 主题参考。
- `classmate_liquid_glass.html`：Liquid Glass / Vitality 方向参考。

## 主题定位

### Focus

Focus 是默认主题。它应该服务学习、阅读、证据链和复习任务：

- 信息密度适中。
- 证据、知识点、任务状态清晰。
- 适合评审截图和真实学习场景。
- 优先保证 Android Compose 稳定落地。

### Flow

Flow 用于 Live Companion、沉浸课堂和未来白噪音方向：

- 可以局部强化课堂伴学、计时、片段记录和专注状态。
- 当前不能声称已有真实白噪音音频或真实 ASR。
- 适合做 Live 页面局部体验增强，而不是全 App 大改。

### Vitality / Liquid Glass

Vitality / Liquid Glass 是可选活力主题方向：

- 适合作为年轻、轻快、展示增强的视觉备选。
- 不应牺牲学习任务的可读性。
- 不追求重 blur 或高成本玻璃拟态。
- 不照搬 iPhone 外壳，应按 Android Compose 组件和性能约束实现。

## 工程落地原则

1. Focus 先行：默认主题优先保证课程导入、时间线、证据、微测、复习、导出可读。
2. Flow 局部：优先增强 Live Companion、课堂计时、手动片段、专注状态，不做全局重构。
3. Vitality 后置：作为可选主题逐步微调，避免干扰主流程。
4. 不追求重 blur：Android 真机性能和可读性优先。
5. 不照搬手机外壳：避免把页面做成设备壳展示，重点是学习产品本身。
6. Compose 落地：以现有 Material3、cards、buttons、chips、timeline、review task 结构为基础。
7. 不改坏 BlueLM 主链路：视觉调整不得改变 provider resolver、校验器和安全日志。

## 推荐交接顺序

1. 固化 Focus 的信息层级：Home、Import、Timeline、Evidence、Quiz、Review、History、Settings。
2. 在 Flow 中强化 Live Companion：手动/模拟转写说明、计时、片段数、最近片段、生成时间线。
3. 让 Course Library 和 Weakness Hub 更像学习产品，而不是调试列表。
4. 最后再做 Vitality / Liquid Glass 的局部动效和色彩增强。

## 截图优先级

答辩或 proof 截图建议先用 Focus：

- Home
- Import Hub
- Live Companion
- Knowledge Timeline
- Evidence Detail
- Quiz
- Review / Weakness Hub
- History / Course Library
- Settings / Capability Roadmap

Flow 可作为 Live Companion 的补充截图。Vitality / Liquid Glass 可作为主题扩展截图，不作为主证明链路。
