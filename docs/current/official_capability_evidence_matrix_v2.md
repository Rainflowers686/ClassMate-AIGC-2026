# Official Capability Evidence Matrix v2

> 状态：已同步到 `1.14.2 / versionCode 115`。完整当前事实源见 [OFFICIAL_CAPABILITY_MATRIX_1_14_2.md](OFFICIAL_CAPABILITY_MATRIX_1_14_2.md)。

## 证据口径

| 能力 | 代码/测试证据 | 当前结论 |
| --- | --- | --- |
| 蓝心大模型 | `core/provider/*`、`CourseAnalyzerTest`、`AppViewModel.startAnalysis` | 已进入学习分析和 AI 精修导出；需配置验证 |
| 官方 TTS WebSocket | `core/official/ws/OfficialTtsWsProtocol.kt`、`OfficialTtsWsSessionTest`、`app/asr/OfficialTtsProvider.kt` | 已代码接入，需 AppKey 真机验证 |
| 官方实时 ASR WebSocket | `core/official/ws/OfficialAsrWsProtocol.kt`、`OfficialRealtimeAsrSessionTest` | 协议底座已接入，需真机验证 |
| 官方长语音转写 | `VivoAsrProvider` 合约和 app ASR job/fallback 路径 | 代码路径存在，需配置验证 |
| 系统 ASR/TTS | `AndroidSpeechRecognizerClient`、`AndroidLocalTtsPlayer`、相关测试 | fallback 可用但设备依赖 |
| OCR | `CaptureGatewayTest`、`OcrFallbackFlowTest`、`OcrImportAssemblerTest` | 图片输入进入证据；失败可手动补充 |
| 导出安全 | `SafeExportTextRedactionTest`、`LearningExportEngineTest`、`ExportCenterTest` | 导出清理密钥、内部状态和 raw id |
| Practice answerable gate | `PracticeAnswerableGateTest`、`QuizAnswerNormalizerTest`、`VariantQuizParserTest` | 坏题不进入主练习 |

## 当前边界

- 以上“代码/测试证据”不等于官方网络已在真机成功。
- smoke pass 与 App runtime used 必须分开写。
- 任何 AppKey、Authorization、Bearer 只允许作为字段名/脱敏说明，不允许写入真实值。
