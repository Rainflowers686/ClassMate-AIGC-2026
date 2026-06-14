# Stage 7 Proof Scripts Usage

## 推荐使用顺序

1. 状态快照：
   ```powershell
   .\scripts\qa\stage7_status_snapshot.ps1
   ```
2. 演示前检查：
   ```powershell
   .\scripts\qa\stage7_demo_preflight.ps1
   ```
3. 生成演示文件夹：
   ```powershell
   .\scripts\demo\make_stage7_demo_folder.ps1
   ```
4. 生成 proof pack：
   ```powershell
   .\scripts\proof\build_stage7_proof_pack.ps1
   ```
5. 校验 proof pack：
   ```powershell
   .\scripts\proof\check_stage7_proof_pack.ps1 -Path proof_out\stage7_proof_xxx
   ```

## build_stage7_proof_pack.ps1

- 输出：`proof_out/stage7_proof_yyyyMMdd_HHmmss/`
- 用途：归档复赛脚本、测试计划、安全摘要、QA 脚本。
- 不会复制：APK 本体、本地配置、build 目录、`.gradle`。
- 不会读取：`config.local.json` 内容。
- 可选参数：`-DryRun`、`-Zip`、`-Open`。

## check_stage7_proof_pack.ps1

- 输入：`-Path proof_out\stage7_proof_xxx`
- 用途：检查 proof pack 必要文件、误复制构建产物和敏感文本风险。
- 输出：PASS / WARN / FAIL。
- 默认只读，不删除文件。

## make_stage7_demo_folder.ps1

- 输出：`demo_out/stage7_demo_yyyyMMdd_HHmmss/`
- 用途：整理演示旁白、PPT 大纲、测试输入、截图清单和 DO_NOT_SHOW。
- 不复制 APK，不读取配置。
- 可选参数：`-DryRun`、`-Open`。

## stage7_status_snapshot.ps1

- 输出当前分支、最近 commit、工作区、APK 元数据、qwen guard 和权限摘要。
- 不运行 Gradle。

## stage7_demo_preflight.ps1

- 检查工作区、GitHub Actions、APK、secrets scan 和关键演示文档。
- 如果构建正被 Stage 7C 半成品阻塞，只记录为 WARN/FAIL，不要在文档脚本阶段修生产代码。

## 使用提醒

- 这些脚本不替代 Gradle 测试和真机测试。
- 演示前仍需确认 APK 是最新可安装版本。
- 所有截图都要避开密钥输入、本地配置、完整日志和内部模型交互。

