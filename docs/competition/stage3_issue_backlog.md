# Stage 3 下一步 GitHub Issues 草案

用途：后续通过 GitHub Issues 批量创建。  
注意：ASR/OCR/端侧模型等均写为调研或接入准备，不写成已实现。

## 1. 真机 smoke test Stage 3 APK

- type: proof / test
- priority: high
- labels: `type/proof`, `type/test`, `stage/v1.1`, `priority/high`
- suitable owner: Human
- acceptance criteria:
  - 安装最新 debug APK。
  - 跑完 Stage 3 真机 smoke checklist。
  - 记录通过/失败项和截图。
  - 不截图真实密钥。

## 2. 拍摄复赛演示视频

- type: proof
- priority: high
- labels: `type/proof`, `type/ui-ux`, `stage/v1.3`, `priority/high`
- suitable owner: Human
- acceptance criteria:
  - 按 device recording shot list 完成 5-7 分钟录屏。
  - 配音覆盖项目定位、BlueLM、证据链、微测、复习、Live、Export。
  - 视频不出现密钥、私人通知、本地隐私路径。

## 3. 截图 proof：BlueLM diagnostic + timeline + review + live

- type: proof
- priority: high
- labels: `type/proof`, `type/ui-ux`, `stage/v1.3`, `priority/high`
- suitable owner: Human
- acceptance criteria:
  - 截 BlueLM diagnostic masked 结果。
  - 截 Knowledge Timeline。
  - 截 Evidence / Quiz / Review / Weakness Hub。
  - 截 Live Companion 手动课堂。
  - 所有截图不含真实 key。

## 4. 重置曾暴露过的 vivo AppKEY

- type: security
- priority: blocker
- labels: `type/security`, `priority/blocker`, `stage/v1.3`
- suitable owner: Human
- acceptance criteria:
  - 确认是否曾在聊天、截图、Issue、日志、proof 中暴露真实 key。
  - 若暴露，立即重置 AppKEY。
  - 更新本机 debug import 配置。
  - 不把新 key 发给任何 AI 或远端文档。

## 5. Focus 全屏 UI 统一二轮 polish

- type: ui-ux
- priority: medium
- labels: `type/ui-ux`, `priority/medium`, `stage/v1.1`
- suitable owner: Claude
- acceptance criteria:
  - Home / Import / Timeline / Evidence / Quiz / Review / History / Settings 视觉密度统一。
  - 保持 Focus 为默认主题。
  - 不改 provider 主链路。
  - 真机截图无明显文字溢出。

## 6. Flow 白噪音真实本地音频资源评估

- type: research
- priority: medium
- labels: `type/research`, `type/ui-ux`, `priority/medium`, `stage/v1.2`
- suitable owner: Later
- acceptance criteria:
  - 调研可合法使用的本地短音频资源。
  - 评估 Android 播放实现和权限需求。
  - 明确版权和体积风险。
  - 未实现前继续标注 Flow ambience visual-only。

## 7. ASR 接入调研

- type: research
- priority: high
- labels: `type/research`, `type/feature`, `priority/high`, `stage/v1.2`
- suitable owner: Human
- acceptance criteria:
  - 调研 vivo 长语音转写/实时短语音识别接入条件。
  - 明确权限、隐私、费用、配额和真机限制。
  - 输出接入方案：ASR -> TranscriptSegment -> Live Companion。
  - 不在调研阶段申请录音权限。

## 8. OCR 接入调研

- type: research
- priority: medium
- labels: `type/research`, `type/feature`, `priority/medium`, `stage/v1.2`
- suitable owner: Human
- acceptance criteria:
  - 调研 vivo 通用 OCR 接口和安全要求。
  - 明确图片导入权限和数据流。
  - 输出接入方案：OCR -> ImportDraft -> existing analysis。
  - 未接入前继续保持占位文案。

## 9. 文本向量/相似度接入调研

- type: research
- priority: medium
- labels: `type/research`, `type/feature`, `priority/medium`, `stage/v1.2`
- suitable owner: Codex
- acceptance criteria:
  - 调研 vivo 文本向量/相似度能力。
  - 设计知识点去重、相似课程聚合、复习任务合并方案。
  - 明确不存敏感 prompt/messages。

## 10. 端侧 3B 模型接入调研

- type: research
- priority: medium
- labels: `type/research`, `type/feature`, `priority/medium`, `stage/v1.3`
- suitable owner: Later
- acceptance criteria:
  - 调研端侧 3B 模型可用性、设备要求和 SDK 约束。
  - 输出离线隐私兜底方案。
  - 明确与 LocalFallback 的关系。

## 11. README/ROADMAP 同步 Stage 3

- type: docs
- priority: medium
- labels: `type/docs`, `priority/medium`, `stage/v1.3`
- suitable owner: Codex
- acceptance criteria:
  - README 更新 Stage 3 已实现能力。
  - ROADMAP 区分已完成/规划中/暂缓。
  - 不声称 ASR/OCR/端侧已完成。
  - 不包含真实 key。

## 12. Release checklist for semifinal submission

- type: release
- priority: high
- labels: `type/docs`, `type/proof`, `priority/high`, `stage/v1.3`
- suitable owner: Human
- acceptance criteria:
  - APK、代码包、PPT、海报、演示视频、截图、redacted logs、架构说明准备完毕。
  - secrets scan 和 GitHub Actions 通过。
  - proof 材料不含密钥、隐私路径、账号信息。

## 13. Claude audit after Stage 3 UI/product polish

- type: audit
- priority: high
- labels: `type/proof`, `type/security`, `status/needs-claude-audit`, `priority/high`
- suitable owner: Claude
- acceptance criteria:
  - 审计 Stage 3 UI 是否误导 ASR/OCR 已完成。
  - 审计日志/导出/截图安全。
  - 审计证据链、微测和复习闭环可解释性。
  - 不在 audit 中修改代码。

## 14. Export proof file sensitive-word sweep

- type: security
- priority: high
- labels: `type/security`, `type/proof`, `priority/high`
- suitable owner: Codex
- acceptance criteria:
  - 对 md/html/txt 导出文件搜索 key、Authorization、prompt/messages、reasoning_content。
  - 记录搜索命令和结果。
  - 发现问题则阻止提交 proof。

## 15. Course Library title normalization follow-up

- type: feature
- priority: low
- labels: `type/feature`, `type/ui-ux`, `priority/low`
- suitable owner: Later
- acceptance criteria:
  - 增强课程标题归一规则。
  - 不调用模型分类。
  - 删除 History 不影响 LearningStore。
