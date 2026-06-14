# Stage 8A 模型 API 管理产品规格

本规格定义 Settings 中“模型 API 管理”的长期产品形态。复赛默认突出 Official BlueLM、OnDevice BlueLM 和 LocalRule；其他模型入口可保留为折叠的长期扩展，不影响复赛主路径表达。

## 页面目标

- 展示当前官方模型路径：Official BlueLM / qwen3.5-plus。
- 支持保存、测试、删除运行态模型配置。
- 支持端侧 BlueLM 3B 诊断。
- 长期支持“添加模型 API”，但复赛默认不突出外部 demo。
- 确保密钥不进入日志、截图、导出、StudyReport 或 Git。

## 字段

| 字段 | 用途 | 安全要求 |
|---|---|---|
| providerType | official_bluelm / ondevice_bluelm / local_rule / custom | 只显示短标签 |
| displayName | UI 名称 | 不含密钥 |
| modelName | 模型名，例如 qwen3.5-plus | 可显示 |
| baseUrl | 自定义模型接口地址 | 复赛模式默认折叠 |
| applicationId | Official BlueLM 应用 ID | 掩码显示 |
| secretPresent | 密钥状态 | 只显示 present/masked |
| capabilityTags | chat/json/local/offline | 可显示 |
| diagnosticStatus | ok/unavailable/error | 短错误标签 |
| isCompetitionOfficial | 是否复赛官方路径 | Official BlueLM 为 true |

## 主页面分组

### 已配置

- Official BlueLM / qwen3.5-plus
- 连接状态
- 测试连接
- 删除配置

### 端侧模型

- OnDevice BlueLM 3B
- SDK 状态
- 模型目录状态
- init/generate 诊断
- unavailable 说明

### 本地兜底

- LocalRule
- 不联网
- 规则型兜底说明

### 长期扩展

- 添加模型 API
- Compatible Demo
- 自定义兼容接口

长期扩展默认折叠，复赛演示不将其作为官方主路径。

## 交互验收

- 保存后重启仍可恢复配置状态。
- 删除后重启仍为空配置。
- 连接测试只显示短结果，不显示完整请求或响应。
- UI 只展示密钥存在状态或掩码。
- 截图模式可以隐藏输入区域。
- 导出和 StudyReport 不包含任何配置详情。

## 失败态

| 场景 | UI 文案 |
|---|---|
| 未配置 | 未配置 Official BlueLM |
| 密钥缺失 | 缺少应用密钥 |
| 连接失败 | 连接失败，查看短错误 |
| 端侧 SDK 缺失 | 端侧 SDK 不可用 |
| 模型目录缺失 | 未找到端侧模型目录 |
| LocalRule 生效 | 当前使用本地规则兜底 |

## 不做

- 不把 DeepSeek、Compatible Demo 或外部模型增强写成复赛官方主路径。
- 不在默认页展示完整接口地址和密钥。
- 不把配置导出到报告、历史、学习状态或 proof。
