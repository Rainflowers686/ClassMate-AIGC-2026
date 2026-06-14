# Stage 8A-7 Device Test Proof Naming Convention

> 端侧 BlueLM 3B 真机测试 proof 截图/录屏命名规范。
> 用于云真机测试后整理 proof 材料，确保命名一致、可追溯。

---

## 命名格式

```
{PROOF_TYPE}_{YYYYMMDD}_{SEQ}[_OPTIONAL_NOTE].png
```

| 字段 | 说明 | 示例 |
|------|------|------|
| `PROOF_TYPE` | 标准 proof 类型（见下表） | `SDK_PRESENT` |
| `YYYYMMDD` | 测试日期 | `20260606` |
| `SEQ` | 两位序号，从 01 开始 | `01` |
| `OPTIONAL_NOTE` | 可选备注（如设备简称） | `X300Pro` |

完整示例：
```
SDK_PRESENT_20260606_01.png
SETTINGS_TEXT_INIT_SUCCESS_20260606_02.png
SETTINGS_MULTIMODAL_CALLVIT_SUCCESS_20260606_07.png
```

---

## Proof 类型清单

### 1. SDK_PRESENT — AAR 文件存在

| 项目 | 内容 |
|------|------|
| **推荐文件名** | `SDK_PRESENT_{DATE}_01.png` |
| **截图内容** | 终端/cmd 中 `ls -la app/libs/llm-sdk-release.aar` 的输出，或文件管理器中 AAR 文件属性 |
| **不能暴露** | 其他 app/libs/ 目录下的文件列表（如有）；完整绝对路径 |
| **备注** | 仅证明 AAR 文件本地存在。不暴露 AAR 内部类名 |

### 2. AAR_GITIGNORED — AAR 被 gitignore 忽略

| 项目 | 内容 |
|------|------|
| **推荐文件名** | `AAR_GITIGNORED_{DATE}_01.png` |
| **截图内容** | `git check-ignore -v app/libs/llm-sdk-release.aar` 输出 |
| **不能暴露** | 仓库完整路径；`.gitignore` 中除 AAR 行外的其他行 |
| **备注** | 证明 AAR 不入版本控制 |

### 3. JAVAP_MULTIMODAL — javap 验证多模态签名

| 项目 | 内容 |
|------|------|
| **推荐文件名** | `JAVAP_MULTIMODAL_{DATE}_01.png` |
| **截图内容** | `javap -classpath classes.jar com.vivo.llmsdk.LlmManager` 输出，显示 `callVit(byte[], int, int)` 和 `public int callVit` |
| **不能暴露** | classes.jar 完整路径；jar 内部完整类列表 |
| **备注** | 仅截取 callVit 相关 2-3 行即可。在 `stage8a2_sdk_preflight.ps1` 的 temp 目录中执行 |

### 4. SETTINGS_TEXT_INIT_SUCCESS — 纯文本初始化成功

| 项目 | 内容 |
|------|------|
| **推荐文件名** | `SETTINGS_TEXT_INIT_SUCCESS_{DATE}_01.png` |
| **截图内容** | Settings 端侧模型区域，"初始化成功"状态文案 |
| **不能暴露** | 设备序列号、IMEI、MAC 地址；config.local.json 密钥 |
| **备注** | 设备截图。如无法真机截图，使用云真机平台截图功能 |

### 5. SETTINGS_TEXT_GENERATE_SUCCESS — 纯文本生成成功

| 项目 | 内容 |
|------|------|
| **推荐文件名** | `SETTINGS_TEXT_GENERATE_SUCCESS_{DATE}_01.png` |
| **截图内容** | Settings 端侧模型区域，input 框含固定问题，output 框含 AI 回复文本 |
| **不能暴露** | 同上；AI 回复中可能包含的敏感信息 |
| **备注** | 确保 input/output 文本完整可见。如文本过长可分 2 张截图 |

### 6. SETTINGS_MULTIMODAL_CALLVIT_SUCCESS — 多模态 VIT 编码成功

| 项目 | 内容 |
|------|------|
| **推荐文件名** | `SETTINGS_MULTIMODAL_CALLVIT_SUCCESS_{DATE}_01.png` |
| **截图内容** | Settings 多模态 diagnostic 区域，显示 callVit 返回码 0 |
| **不能暴露** | 同上；内置 2×2 测试图的原始字节内容 |
| **备注** | 如 callVit 失败，用 `SETTINGS_MULTIMODAL_CALLVIT_FAILED_{DATE}_01.png` |

### 7. SETTINGS_MULTIMODAL_GENERATE_SUCCESS — 多模态生成成功

| 项目 | 内容 |
|------|------|
| **推荐文件名** | `SETTINGS_MULTIMODAL_GENERATE_SUCCESS_{DATE}_01.png` |
| **截图内容** | Settings 多模态 diagnostic 区域，callVit 成功后 generate 的输出文本 |
| **不能暴露** | 同上 |
| **备注** | 仅在 callVit 返回 0 后才测试此项。失败用 `SETTINGS_MULTIMODAL_GENERATE_FAILED_{DATE}_01.png` |

### 8. FALLBACK_LOCAL_RULE — 端侧失败后 LocalRule 兜底

| 项目 | 内容 |
|------|------|
| **推荐文件名** | `FALLBACK_LOCAL_RULE_{DATE}_01.png` |
| **截图内容** | 端侧 init 失败或不可用时，LocalRule 模式下的回复界面 |
| **不能暴露** | 同上 |
| **备注** | 需能看到 LocalRule 标识（如 `[LocalRule]` 前缀） |

### 9. NO_DANGEROUS_STORAGE_PERMISSION — 无危险存储权限

| 项目 | 内容 |
|------|------|
| **推荐文件名** | `NO_DANGEROUS_STORAGE_PERMISSION_{DATE}_01.png` |
| **截图内容** | 系统设置 → 应用 → ClassMate → 权限页面，或 App Info 页面 |
| **不能暴露** | 其他应用的权限信息；设备序列号 |
| **备注** | 证明未申请 MANAGE_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE |

### 10. QWEN_GUARD — enable_thinking 守卫

| 项目 | 内容 |
|------|------|
| **推荐文件名** | `QWEN_GUARD_{DATE}_01.png` |
| **截图内容** | `grep -r "enable_thinking" app/src core/src` 输出 |
| **不能暴露** | 实际密钥或配置值 |
| **备注** | 终端截图，证明 enable_thinking 引用均使用 false |

### 11. SECRETS_SCAN_OK — 密钥扫描通过

| 项目 | 内容 |
|------|------|
| **推荐文件名** | `SECRETS_SCAN_OK_{DATE}_01.png` |
| **截图内容** | `powershell scripts/secrets_scan/secrets_scan.ps1` 输出，"No secrets found" 或等效通过信息 |
| **不能暴露** | 扫描命中的具体密钥内容（如果有的话—那就不叫通过了） |
| **备注** | 如扫描有命中，不截图此项，改填 bug triage |

### 12. CI_ANDROID_OK — CI 编译通过（如有）

| 项目 | 内容 |
|------|------|
| **推荐文件名** | `CI_ANDROID_OK_{DATE}_01.png` |
| **截图内容** | GitHub Actions / CI 平台中 Android build job 的通过状态 |
| **不能暴露** | CI 日志中的密钥、环境变量 |
| **备注** | 如无 CI 则跳过此项 |

### 13. CLOUD_DEVICE_FAILURE_FALLBACK — 云真机失败记录

| 项目 | 内容 |
|------|------|
| **推荐文件名** | `CLOUD_DEVICE_FAILURE_{ERROR_CODE}_{DATE}_01.png` |
| **截图内容** | 云真机上失败时的完整界面 + logcat 窗口 |
| **不能暴露** | 设备标识信息 |
| **备注** | 失败也是一种 proof — 证明诚实度。用于答辩"端侧进度的诚实陈述" |

---

## 录屏命名（如有）

```
{PROOF_TYPE}_{YYYYMMDD}_VIDEO_{SEQ}[_OPTIONAL_NOTE].mp4
```

**录屏内容建议**（30-60 秒）：
1. SDK 纯文本端到端：打开 Settings → init → 输入问题 → 看到输出 → 完成。
2. 多模态 diagnostic：开启 multimodal → callVit → generate。
3. 断网 proof：开启飞行模式 → 云端不可用 → 端侧仍可生成。

---

## 禁止事项

1. ❌ 截图暴露 AppID / AppKey / 任何密钥。
2. ❌ 截图暴露设备序列号、IMEI、Wi-Fi MAC。
3. ❌ 截图暴露个人账户信息（如已登录）。
4. ❌ 文件名包含中文或特殊字符（用 ASCII + 数字）。
5. ❌ 截图中出现 `config.local.json` 文件内容。
6. ❌ 用成功的旧截图冒充新测试（时间戳会暴露）。

---

## 文件组织

```
qa_out/stage8a7_logs/
├── screenshots/
│   ├── SDK_PRESENT_20260606_01.png
│   ├── AAR_GITIGNORED_20260606_02.png
│   ├── SETTINGS_TEXT_INIT_SUCCESS_20260606_03.png
│   ├── SETTINGS_TEXT_GENERATE_SUCCESS_20260606_04.png
│   ├── SETTINGS_MULTIMODAL_CALLVIT_SUCCESS_20260606_05.png
│   ├── SETTINGS_MULTIMODAL_GENERATE_SUCCESS_20260606_06.png
│   ├── FALLBACK_LOCAL_RULE_20260606_07.png
│   └── ...
├── logcat_ondevice_20260606_143022.txt
├── logcat_ondevice_20260606_143501.txt
└── screenshots_20260606_143022/  (from adb pull)
```
