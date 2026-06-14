# Claude Stage 8A OnDevice BlueLM Red-Team Review Prompt

请对 ClassMate Stage 8A 端侧 BlueLM 3B 集成做红队审查。只审查，不修改代码，除非我明确要求修复。

## 背景

- Official BlueLM/qwen3.5-plus 仍是复赛主路径。
- OnDevice BlueLM 3B 是智能兜底和决赛扩展，不替代云端主路径。
- LocalRule 是最终安全兜底。
- Provider 链应保持：Official BlueLM cloud -> OnDeviceBlueLM -> LocalRule；本地链为 OnDeviceBlueLM -> LocalRule。

## 重点审查

1. 是否真实引用端侧 SDK，而不是 fake 接口。
2. 是否有 `llm-sdk-release.aar`、`LlmManager`、`LlmConfig`、`/sdcard/1225` 相关真实接线。
3. init/generate/interrupt/release 是否在后台线程，不阻塞 UI。
4. SDK、模型目录、native、权限不可用时是否显示 unavailable 并 fallback。
5. 是否保留 Official BlueLM 云端主路径和 qwen3.5-plus 关闭思考 guard。
6. 是否没有改坏 ProviderResolver 顺序。
7. 是否没有削弱 ResultValidator / EvidenceValidator / EvidenceResolver。
8. 是否没有记录完整模型输入、完整模型输出、供应商原始响应或内部推理字段。
9. 是否没有把密钥写入日志、导出、StudyReport、截图或 Git。
10. 是否没有把 Compatible Demo 或外部模型增强写成复赛官方主路径。

## 真机 smoke 必查

- X300 Pro 或目标 vivo 云真机。
- `/sdcard/1225` 模型目录。
- Settings 端侧诊断。
- 短输入 generate。
- interrupt/release。
- SDK 缺失、模型目录缺失、权限拒绝。
- 弱网/离线 fallback。

## 输出格式

- Findings first，按严重程度排序。
- 每条包含文件/行号、风险、复现或证据、建议。
- 如果没有发现问题，说明残余风险和仍需真机验证的项目。
- 不输出真实密钥。
