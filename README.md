# ClassMate 1.14.2

> 面向 AIGC 全国计算机大赛的证据绑定学习闭环 Android App。

ClassMate 把真实课堂资料变成可复习、可练习、可回溯、可导出的学习资产。它不是通用聊天 App，也不是单一 OCR、转写或刷题工具；主线是：

```text
资料输入 -> 知识结构 -> 证据绑定 -> 微测 -> 反馈 -> 复习闭环 -> AI 精修导出
```

当前候选版本：

| 项 | 状态 |
| --- | --- |
| 版本 | `1.14.2 / versionCode 115` |
| 最新核心提交 | `7473fb1 fix(product): repair final real-device import and quiz blockers` |
| 分支 | `feature/audio-official-loop-hardening-v1` |
| 定位 | 多模态课堂学习闭环 App |
| 演示状态 | 核心链路最终候选版；官方网络能力仍需真实 AppKey 与真机验证 |

## 当前可演示主链路

1. 导入课堂资料：文本、Markdown、图片、拍照、TXT/MD、多文件、PDF 页文本、录音或转写。
2. 生成知识结构：课程主题、知识点、重点解释、易错点。
3. 绑定证据：文本、图片、文档、音频转写都作为 EvidenceAsset 回溯。
4. 做微测：题目来自本节课资料；无可用题时有本地兜底题，不出现空页面。
5. 学习反馈：答题后进入错题、薄弱点、已复习/需复核状态和复习计划。
6. 外部学习入口：B 站/浏览器搜索是 Intent 搜索入口，不是 API 推荐算法。
7. 导出：普通导出始终可用；AI 精修导出由用户主动触发，失败不覆盖普通版。

## 官方能力真实状态

ClassMate 使用官方能力时坚持“能用则用，失败不阻断，状态不夸大”。

| 能力 | 当前状态 | fallback |
| --- | --- | --- |
| 蓝心大模型 HTTP | 已接入学习分析、精修导出、反馈增强；需配置验证真实网络 | 端侧模型或本地规则 |
| 官方长语音转写 1739 HTTP | 任务流代码接入；需 AppKey 与真机验证 | 系统 ASR、录音保存、手动转写 |
| 官方实时 ASR WebSocket | 协议底座接入；流式真机体验待验证 | 系统 SpeechRecognizer、录音保存 |
| 官方 TTS WebSocket | 已按官方 WebSocket 协议代码接入；不是“缺协议不能做” | 系统 TTS、听背文稿 |
| 系统 ASR/TTS | 已接入；依赖设备服务 | 手动转写 / 文稿 |
| 端侧 3B | optional fallback；依赖机型、模型文件和权限 | 云端或本地规则 |
| 文本向量/相似度/查询改写 | 官方 runtime/fallback 结构存在；真实 official used 需配置验证 | 本地 lexical / similarity |
| 外部搜索 | 浏览器 Intent | 无 API 伪装 |

完整矩阵见 [docs/current/OFFICIAL_CAPABILITY_MATRIX_1_14_2.md](docs/current/OFFICIAL_CAPABILITY_MATRIX_1_14_2.md)。
旧版 18 项能力横向自检仍保留为兼容入口：[docs/current/official_18_capability_l3_readiness.md](docs/current/official_18_capability_l3_readiness.md)。该文档与当前 README 的共同口径是：官方配置缺失时回到本地基础整理；图片生成、视频生成、同声传译等实验性入口默认关闭，不影响主学习闭环。

## 重要边界

- 不声称官方 ASR/TTS 已 100% 真机跑通；真实网络成功依赖 AppKey、权限、设备和接口状态。
- 不把端侧模型说成所有手机可用；它是 optional fallback。
- 不把浏览器搜索包装成推荐 API。
- 不把本地 fallback 冒充蓝心结果。
- 导出内容经过 SafeExportText 清理，不包含密钥、内部状态、provider trace 或 raw id。

## 构建与验证

在仓库根目录运行：

```powershell
.\gradlew.bat :core:test --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
powershell -ExecutionPolicy Bypass -File scripts\qa\current_preflight.ps1
powershell -ExecutionPolicy Bypass -File scripts\qa\cloud_device_precheck.ps1
```

打包候选 APK：

```powershell
cd "D:\Edge Download\AIGC\ClassMate"
git pull
.\gradlew.bat clean :app:assembleDebug --no-daemon
$commit = git rev-parse --short HEAD
Copy-Item "app\build\outputs\apk\debug\app-debug.apk" "ClassMate-debug-v1.14.2-$commit.apk"
explorer .
```

不要提交 `config.local.json`、AAR、APK、AAB、字体、密钥或 OfficialDemos。

## 文档入口

- 当前总索引：[docs/current/DOCUMENT_INDEX.md](docs/current/DOCUMENT_INDEX.md)
- 最终状态报告：[docs/current/FINAL_STATUS_1_14_2.md](docs/current/FINAL_STATUS_1_14_2.md)
- 官方能力矩阵：[docs/current/OFFICIAL_CAPABILITY_MATRIX_1_14_2.md](docs/current/OFFICIAL_CAPABILITY_MATRIX_1_14_2.md)
- 真机问题修复矩阵：[docs/current/REAL_DEVICE_FIX_MATRIX_1_14_2.md](docs/current/REAL_DEVICE_FIX_MATRIX_1_14_2.md)
- 真机测试手册：[docs/current/REAL_DEVICE_TEST_MANUAL_1_14_2.md](docs/current/REAL_DEVICE_TEST_MANUAL_1_14_2.md)
- 演示脚本：[docs/current/DEMO_SCRIPT_1_14_2.md](docs/current/DEMO_SCRIPT_1_14_2.md)
- 答辩叙事：[docs/current/DEFENSE_NARRATIVE.md](docs/current/DEFENSE_NARRATIVE.md)
- 构建发布：[docs/current/BUILD_AND_RELEASE.md](docs/current/BUILD_AND_RELEASE.md)
- 安全与密钥：[docs/current/PRIVACY_SECURITY_AND_SECRETS.md](docs/current/PRIVACY_SECURITY_AND_SECRETS.md)
