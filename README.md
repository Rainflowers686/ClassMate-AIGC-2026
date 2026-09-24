# ClassMate

*An Android study workflow for course materials, evidence-linked micro quizzes, and review.*

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white) ![Candidate v1.14.18](https://img.shields.io/badge/Candidate-v1.14.18-6574CD) ![Course project](https://img.shields.io/badge/Status-Course%20project-687078)



**Guide:** [Project status](#project-status) · [Build](#build) · [Project notes](#project-notes)

[English](README.md) | [简体中文](README.zh-CN.md)

ClassMate is an Android study-workflow project that turns course material into a sequence of evidence-linked questions, feedback and review planning.

~~~text
material input → subject knowledge → evidence binding → micro quiz → feedback → review plan → export
~~~

## Project status

The repository documents candidate version 1.14.18 (versionCode 131). The recorded focus is the study flow after a quiz: save the result, clear practice state, then navigate to Review. The final Review-navigation fix still needs the real-device confirmation recorded in the project docs.

Provider-backed LLM, OCR, speech input and speech output depend on valid local configuration, network access and device permissions. When those services are unavailable, the app should keep their output distinct from local fallback behavior. Do not publish credentials or describe a fallback as provider output.

## Build

From the repository root on Windows:

~~~powershell
.\gradlew.bat :core:test --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
~~~

The debug APK is written to app/build/outputs/apk/debug/app-debug.apk.

## Project notes

- [Current document index](docs/current/DOCUMENT_INDEX.md)
- [Version 1.14.18 changelog](docs/current/CHANGELOG_1_14_18.md)
- [Real-device test manual](docs/current/REAL_DEVICE_TEST_MANUAL_1_14_18.md)
- [Build and release guide](docs/current/BUILD_AND_RELEASE_GUIDE_1_14_18.md)

Keep local configuration, real credentials and generated release packages out of source control. See the project safety notes before preparing a public package.
