# Stage 7 Submission Package Checklist

## 1. APK

- [ ] Debug APK 路径已记录：`app/build/outputs/apk/debug/app-debug.apk`
- [ ] APK 大小和生成时间已记录。
- [ ] BuildInfo 页面可展示版本、构建类型、commit。
- [ ] 是否需要 release APK：待比赛最终提交规则确认。
- [ ] 如果需要 release APK，先补签名与安全检查流程，不临时混入本地凭据。

## 2. 演示视频

- [ ] 8-10 分钟主视频。
- [ ] 2 分钟快速版。
- [ ] 必拍：Official BlueLM 路径。
- [ ] 必拍：多源导入和资料篮。
- [ ] 必拍：Timeline / Evidence。
- [ ] 必拍：Ask This Lesson 三类结果。
- [ ] 必拍：Quiz / Review / 需要多练。
- [ ] 必拍：Export Center 保存和分享。
- [ ] 必拍：Settings 能力路线图和诚实边界。

## 3. 证明截图

- [ ] BlueLM diagnostic OK。
- [ ] Timeline / Evidence。
- [ ] Ask grounded / partial / not_found。
- [ ] Quiz 题目和解释。
- [ ] Review 任务和薄弱点。
- [ ] 需要多练或 Practice Search。
- [ ] Export Center 格式列表。
- [ ] StudyReport 打印效果。
- [ ] ASR experimental 文案。
- [ ] GitHub Actions。
- [ ] Secrets scan。

## 4. 文档

- [ ] README 或项目说明。
- [ ] 评审版功能矩阵。
- [ ] 安全说明。
- [ ] 竞品对比。
- [ ] 未来路线。
- [ ] 真机回归计划。
- [ ] 评委问答。
- [ ] Proof 截图清单。

## 5. 禁止提交或展示

- [ ] `config.local.json`。
- [ ] 本地应用凭据或完整密钥。
- [ ] 截图中的密钥明文。
- [ ] 内部模型输入、消息体或推理字段。
- [ ] `build/`、`.gradle/`、APK/AAB 构建产物。
- [ ] 私人账号、私密路径、真实学生隐私材料。

## 6. 最后 30 分钟检查流程

1. 运行轻量 QA 脚本和 secrets scan。
2. 确认 GitHub Actions 最新 run 状态。
3. 确认 APK 可安装并可启动。
4. 跑 BlueLM diagnostic。
5. 用标准样例跑一遍导入到导出的主链路。
6. 检查导出报告不含本地凭据、内部模型交互或调试原始内容。
7. 检查截图目录，删除任何包含密钥、账号、私密路径的图片。
8. 生成 proof pack，保留 zip 和目录。
9. 最后确认提交包不包含本地配置或构建缓存。

