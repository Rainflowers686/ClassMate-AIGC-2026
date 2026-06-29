# Final Smoke Test Checklist

## 命令 smoke

```powershell
git diff --check
.\gradlew.bat :core:test --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
powershell -ExecutionPolicy Bypass -File scripts\qa\current_preflight.ps1
powershell -ExecutionPolicy Bypass -File scripts\qa\cloud_device_precheck.ps1
```

## App smoke

1. 冷启动显示 1.14.2。
2. 图片 OCR 失败项可手动输入。
3. 生成课程有知识结构和微测。
4. 图片题能回图片证据。
5. 答错进入错题和复习。
6. 录音无 ASR 时仍保存并可转写。
7. TTS 不可用时保留文稿。
8. 普通导出和 AI 精修导出都可触发。
9. 导出文件无密钥、内部状态和 raw id。
10. 浏览器搜索不是 API 推荐。
