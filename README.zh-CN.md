# ClassMate

*围绕课程资料、证据关联微测和复习安排构建的 Android 学习流程。*

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white) ![Candidate v1.14.18](https://img.shields.io/badge/Candidate-v1.14.18-6574CD) ![Course project](https://img.shields.io/badge/Status-Course%20project-687078)



**导航：**[项目状态](#项目状态) · [构建](#构建) · [项目文档](#项目文档)

[English](README.md) | [简体中文](README.zh-CN.md)

ClassMate 是一个 Android 学习流程项目，把课程资料整理成带证据关联的题目、反馈和复习计划。

~~~text
资料导入 → 学科知识 → 证据绑定 → 微测 → 反馈 → 复习计划 → 导出
~~~

## 项目状态

仓库记录的候选版本为 1.14.18（versionCode 131）。当前记录的重点是小测完成后的学习流程：先保存结果并清理练习状态，再进入复习页。项目文档注明，复习页导航修复仍需完成真机确认。

依赖服务商的 LLM、OCR、语音输入和语音输出，需要有效的本地配置、网络及设备权限。服务不可用时，本地兜底结果应与服务商输出明确区分。不要发布凭据，也不要把本地兜底描述成服务商结果。

## 构建

在 Windows 上从仓库根目录执行：

~~~powershell
.\gradlew.bat :core:test --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
~~~

Debug APK 输出到 app/build/outputs/apk/debug/app-debug.apk。

## 项目文档

- [当前文档索引](docs/current/DOCUMENT_INDEX.md)
- [1.14.18 变更记录](docs/current/CHANGELOG_1_14_18.md)
- [真机测试手册](docs/current/REAL_DEVICE_TEST_MANUAL_1_14_18.md)
- [构建与发布指南](docs/current/BUILD_AND_RELEASE_GUIDE_1_14_18.md)

本地配置、真实凭据和生成的发布包不应进入源码库。准备公开包前请先阅读项目安全说明。
