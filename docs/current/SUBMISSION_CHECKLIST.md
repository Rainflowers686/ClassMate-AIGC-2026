# Submission Checklist

## 提交前

- [ ] `git status --short` 无非预期文件。
- [ ] 不包含 `config.local.json`。
- [ ] 不包含 AAR/APK/AAB/字体/OfficialDemos。
- [ ] README 和 docs/current 指向 1.14.2。
- [ ] 官方能力矩阵不夸大。
- [ ] 演示脚本不把 fallback 说成官方成功。

## 验证

- [ ] `git diff --check`
- [ ] `:core:test`
- [ ] `:app:testDebugUnitTest`
- [ ] `:app:assembleDebug`
- [ ] `current_preflight.ps1`
- [ ] `cloud_device_precheck.ps1`

## 材料

- [ ] APK 文件名含版本和 commit。
- [ ] 录屏覆盖 3 分钟主链路。
- [ ] 截图包含资料篮、知识结构、证据、微测、反馈、导出。
- [ ] 答辩叙事说明官方能力边界。
