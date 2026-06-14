# 后续 Commit 切分建议

本文仅为整理提交的建议，不执行 git commit。适用于 Claude Code 完成 Stage 3 生产重构后拆分提交。

## 1. Theme tokens and shared components

- 建议 commit message：`feat(ui): add product theme tokens and shared components`
- 包含文件类型：`app/src/main/java/.../ui/design/`、`ui/components/` 中的 token、通用卡片、按钮、状态组件。
- 验证命令：
  - `.\gradlew.bat :app:testDebugUnitTest`
  - `.\gradlew.bat :app:assembleDebug`
- 风险：中。共享组件影响多个页面。
- 是否适合单独回滚：适合。如果组件破坏布局，可单独回滚。

## 2. Productize Live Companion

- 建议 commit message：`feat(live): productize manual classroom companion`
- 包含文件类型：Live 页面、Live 状态模型、Live 相关 ViewModel 接线、Live 单测。
- 验证命令：
  - `.\gradlew.bat :core:test`
  - `.\gradlew.bat :app:testDebugUnitTest`
  - 真机手动：开始、暂停、继续、结束、生成时间线。
- 风险：中。涉及导航和分析入口。
- 是否适合单独回滚：适合。Live 是独立入口，回滚不应影响 BlueLM 主链路。

## 3. Unify Home Review History Settings

- 建议 commit message：`feat(app): unify learning shell across home review history settings`
- 包含文件类型：Home、Review、History、Settings 页面和 AppViewModel 中非 provider 的状态接线。
- 验证命令：
  - `.\gradlew.bat :app:testDebugUnitTest`
  - `.\gradlew.bat :app:assembleDebug`
  - 真机 smoke：四个 tab 来回切换。
- 风险：中高。涉及主导航和多个页面。
- 是否适合单独回滚：部分适合。若拆得更细，回滚更安全。

## 4. Polish Timeline Quiz Ask Lesson

- 建议 commit message：`feat(lesson): add ask-this-lesson and polish timeline quiz flow`
- 包含文件类型：Timeline、Quiz、Ask Lesson core/parser/engine、相关测试。
- 验证命令：
  - `.\gradlew.bat :core:test`
  - `.\gradlew.bat :app:testDebugUnitTest`
  - 真机：Timeline -> Evidence -> Ask -> Quiz。
- 风险：中。Ask Lesson 必须避免伪造证据。
- 是否适合单独回滚：适合。Ask 可以作为独立功能回滚。

## 5. Add Flow Live Experience

- 建议 commit message：`feat(flow): add focused live companion experience`
- 包含文件类型：Flow 主题局部组件、Live 页面视觉增强、少量主题 token。
- 验证命令：
  - `.\gradlew.bat :app:testDebugUnitTest`
  - `.\gradlew.bat :app:assembleDebug`
  - 真机：Focus/Flow/Vitality 切换。
- 风险：中。视觉改动可能影响可读性。
- 是否适合单独回滚：适合。Flow 应保持局部增强。

## 6. Export History Review Completeness

- 建议 commit message：`feat(export): include course library weakness and proof-safe reports`
- 包含文件类型：Export、Course Library、Weakness Hub、History/Review 相关测试。
- 验证命令：
  - `.\gradlew.bat :core:test`
  - `.\gradlew.bat :app:testDebugUnitTest`
  - 导出 md/html/txt 后检查敏感词。
- 风险：中高。导出涉及 proof 安全。
- 是否适合单独回滚：适合。导出功能可独立回滚。

## 7. Docs and Proof Updates

- 建议 commit message：`docs(competition): add demo script qna and acceptance matrix`
- 包含文件类型：`docs/competition/*.md`、`docs/design_refs/*.md`。
- 验证命令：
  - `git diff --check`
  - `scripts\secrets_scan\secrets_scan.ps1`
  - `git ls-files config.local.json local.properties secrets.properties .env .env.* *.jks *.keystore *.apk *.aab app/build core/build build .gradle`
- 风险：低。只影响文档。
- 是否适合单独回滚：适合。

## 推荐顺序

1. 先提交不影响主链路的 docs/proof。
2. 再提交主题 token 和共享组件。
3. 再提交 Live Companion。
4. 再提交 Home/Review/History/Settings 信息结构。
5. 再提交 Timeline/Quiz/Ask。
6. 最后提交 Flow/Vitality 视觉增强。

每个生产代码 commit 后都应确认：

- 不改坏 Official BlueLM / Compatible / LocalFallback 顺序。
- qwen3.5-plus 仍关闭 thinking。
- 不削弱 EvidenceValidator / ResultValidator / EvidenceResolver。
- 不写入真实密钥。
