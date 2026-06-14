# Stage 8A-2 端侧 SDK AAR 构建记录

本文记录本次端侧 LLM 官方 demo SDK AAR 的可复现构建过程。记录只包含工程路径、构建命令、AAR 产物和 javap 验证信息，不包含任何真实密钥。

## 1. 背景

官方 demo 源码最初位于中文路径。Windows + Android Gradle Plugin 对非 ASCII 路径存在兼容限制，导致原始中文路径构建失败。为降低路径兼容风险，本次将源码复制到英文路径后重新构建。

## 2. 源码复制路径

英文路径：

```text
D:\AIGC_SDK\ondevice_llm_demo_src
```

原始中文路径只作为来源，不建议继续作为构建目录。后续复现也应优先使用英文路径。

## 3. local.properties

英文路径下的 `local.properties` 应包含：

```properties
android.overridePathCheck=true
sdk.dir=D:/AAAbiancheng/Android/SDK
```

说明：

- `android.overridePathCheck=true` 用于绕过路径检查。
- `sdk.dir` 指向本机 Android SDK。
- 不在该文件中写入任何应用密钥或模型服务密钥。

## 4. 自动安装的构建组件

构建过程中 Android Gradle Plugin 自动补齐了：

- NDK 25.1.8937393
- CMake 3.22.1

如果复现机器缺少这些组件，可允许 Android SDK 管理器自动安装，或提前手动安装。

## 5. 成功构建命令

在英文源码目录执行：

```powershell
cd D:\AIGC_SDK\ondevice_llm_demo_src
.\gradlew.bat :llm-sdk:assembleRelease
```

本次 Codex 文档整理不运行 Gradle。该命令仅用于后续人工复现。

## 6. AAR 产物

成功产物：

```text
D:\AIGC_SDK\ondevice_llm_demo_src\llm-sdk\build\outputs\aar\llm-sdk-release.aar
```

已记录产物信息：

| 项目 | 值 |
|---|---|
| 大小 | 约 1,602,551 bytes |
| 时间 | 2026/6/5 19:24:13 |

复制到 ClassMate：

```text
D:\Edge Download\AIGC\ClassMate\app\libs\llm-sdk-release.aar
```

## 7. 为什么不提交 AAR

ClassMate 仓库通过 `.gitignore` 忽略本地 AAR：

```text
app/libs/*.aar
```

原因：

- AAR 是本地 SDK 产物，不适合进入 Git 历史。
- 官方 SDK 可能存在授权、版本和设备限制。
- 复赛 proof 只记录路径、大小、时间和静态检查，不复制 AAR 本体。

## 8. javap 复核命令

建议复核流程：

```powershell
$aar = "D:\Edge Download\AIGC\ClassMate\app\libs\llm-sdk-release.aar"
$tmp = Join-Path $env:TEMP "classmate_llm_sdk_check"
Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $tmp | Out-Null
Expand-Archive -Path $aar -DestinationPath $tmp -Force
jar tf (Join-Path $tmp "classes.jar") | Select-String "LlmConfig|LlmManager|TokenCallback"
```

找到完整类名后执行：

```powershell
javap -classpath (Join-Path $tmp "classes.jar") <full.class.Name>
```

本次 javap 已确认：

### LlmConfig

- `modelPath`
- `nPredict`
- `nCtx`
- `nThreads`
- `topK`
- `topP`
- `temperature`
- `npuPower`
- `multimodal`

### LlmManager

- `init(LlmConfig): int`
- `callVit(byte[], int, int): int`
- `generate(String, TokenCallback): void`
- `interrupt(): void`
- `release(): void`

### TokenCallback

- `onToken(String)`
- `onComplete()`
- `onError(int,String)`

注意：新 AAR 的 `TokenCallback.onComplete()` 没有 `LlmStats` 参数。实现 bridge 时不要写旧签名。

## 9. 复现检查

复现完成后应运行：

```powershell
git check-ignore -v app\libs\llm-sdk-release.aar
git status --short
.\scripts\qa\stage8a2_sdk_preflight.ps1
```

预期：

- AAR 存在。
- AAR 被 `.gitignore` 忽略。
- Git 不追踪 AAR。
- javap 能看到 `multimodal`、`callVit` 和无参 `onComplete()`。
