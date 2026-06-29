# Official Tool Productization Matrix

> 状态：已同步到 `1.14.2 / versionCode 115`。本文件保留产品化视角，协议细节见 [OFFICIAL_CAPABILITY_MATRIX_1_14_2.md](OFFICIAL_CAPABILITY_MATRIX_1_14_2.md)。

| 能力 | 学习闭环位置 | 用户价值 | fallback | 演示建议 |
| --- | --- | --- | --- | --- |
| 蓝心大模型 | 知识结构、微测、反馈、AI 精修导出 | 把资料整理成可复习资产 | 端侧/本地规则 | 配置可用时演示 |
| OCR | 图片资料 -> OCR_IMAGE evidence | 课件/题图进学习闭环 | 手动补充文字 | 必演示，允许 fallback |
| ASR Long / Realtime | 录音 -> AUDIO evidence / transcript | 课堂语音转学习资料 | 系统 ASR/手动转写 | 演示 fallback，官方待验证 |
| TTS | 听背音频 / 文稿 | 复习材料可听读 | 系统 TTS/文稿 | 演示系统 TTS 或文稿 |
| Retrieval | 证据排序/相似题/检索 | 让复习和证据更相关 | 本地 lexical/similarity | 不作为官方 used 夸大 |
| Function calling | 工具链规划 | 受控编排 OCR/ASR/导出 | 本地 orchestrator | 诊断说明即可 |
| 端侧 3B | 弱网/隐私 fallback | 无云端时仍可生成草稿 | 本地规则 | 有目标机再演示 |

## 1.14.2 结论

主演示应聚焦“资料 -> 证据 -> 微测 -> 反馈 -> 复习 -> 精修导出”。官方能力作为增强和 fallback 矩阵说明，不把待验证能力包装成已完成。

## v1.7 官方检索 runtime 注入证据

检索三能力在 1.7 后不再默认绑定 ConfigMissing 桩。生产 App 入口通过 `OfficialRuntimeGatewayFactory.production()` 构建 gateway，并保留以下 adapter 映射：

- Query Rewrite：`OFFICIAL_RUNTIME_READY / VALIDATION_PENDING`，`VivoQueryRewriteProvider -> VivoQueryRewriteLearningProvider`。
- Embedding：`OFFICIAL_RUNTIME_READY / VALIDATION_PENDING`，`VivoEmbeddingProvider -> VivoEmbeddingLearningProvider`。
- Text Similarity：`OFFICIAL_RUNTIME_READY / VALIDATION_PENDING`，`VivoTextSimilarityProvider -> VivoTextSimilarityLearningProvider`。

以上表示 official-first app runtime 已具备注入路径；真实 `OFFICIAL_RUNTIME_USED` 仍依赖 AppKey、设备、网络和真机验证。失败或未配置时继续 local fallback，不中断学习闭环。
