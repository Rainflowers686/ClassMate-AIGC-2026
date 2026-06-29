# Safe Export Policy

## 目标

导出的学习资料应适合打印、分享和提交，但不能泄露工程内部信息或敏感配置。

## 必须清理

- 真实密钥值。
- Authorization / Bearer 真实值。
- `config.local.json` 内容。
- provider trace。
- BuildConfig 内部信息。
- raw id。
- 内部状态枚举。

## 证据状态

| 证据状态 | 导出文案 |
| --- | --- |
| strong | 查看证据 / 引用来源 |
| weak | 证据待核对 |
| missing | 暂无可回溯证据 |

## 导出格式

- PDF：打印版。
- DOCX / Word-compatible：可编辑学习文档。
- HTML：浏览器可打开。
- Markdown：可二次编辑。
- Text：最低兼容。
- Course essence audio script：脚本文本，不伪装成已生成真实音频。

## 测试证据

- `SafeExportTextRedactionTest`
- `LearningExportEngineTest`
- `ExportCenterTest`
- `StudyReport*Test`
