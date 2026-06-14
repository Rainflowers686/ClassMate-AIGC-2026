# Stage 5 Device Helper 使用说明

这个辅助脚本用于明天真机测试时减少重复命令。它不会默认安装、启动、拉文件或清理文件；无参数时只显示帮助。

## 推荐顺序

先运行轻量预检查：

```powershell
.\scripts\qa\stage5_preflight.ps1
```

查看 APK 和分支信息：

```powershell
.\scripts\qa\stage5_device_helper.ps1 -Info
```

查找 adb：

```powershell
.\scripts\qa\stage5_device_helper.ps1 -FindAdb
```

检查设备：

```powershell
.\scripts\qa\stage5_device_helper.ps1 -Devices
```

轻量组合检查：

```powershell
.\scripts\qa\stage5_device_helper.ps1 -AllLight
```

安装 APK：

```powershell
.\scripts\qa\stage5_device_helper.ps1 -Install
```

启动 App：

```powershell
.\scripts\qa\stage5_device_helper.ps1 -Launch
```

查看导出：

```powershell
.\scripts\qa\stage5_device_helper.ps1 -ListExports
```

优先使用 App 内 Export Center 的“保存到文件”或“分享”功能。`run-as` 只作为调试辅助；部分云真机会禁用或限制它。

如确实需要把 app 私有导出调试拉到本地：

```powershell
.\scripts\qa\stage5_device_helper.ps1 -PullExports
```

清理本地 `qa_exports`：

```powershell
.\scripts\qa\stage5_device_helper.ps1 -CleanQaExports
```

## 云真机 adb 不通怎么办

- 直接用云真机控制台上传并安装 APK。
- 使用 App 内 Export Center，把文件保存到云真机可见目录，或用系统分享面板导出。
- 截图时只拍模型状态、诊断结果、Timeline、Evidence、Ask、Quiz、Review、Course Detail 和 Export 成功结果。
- 不要拍摄真实模型凭据、调试导入明文、本地配置文件或账号隐私。

## 注意事项

- 脚本不读取 `config.local.json` 内容。
- 脚本不保存 device log 到仓库。
- `-Logcat` 只打印实时观察提示，是否执行由测试者手动决定。
- 如果 adb 不存在，安装 Android SDK platform-tools，并确认 `adb.exe` 在 PATH 或常见 SDK 目录中。
- 导出文件 proof 优先来自用户可见目录或系统分享，不依赖 app 私有目录。

