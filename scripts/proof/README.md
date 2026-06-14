# Proof Scripts

## build_stage7_proof_pack.ps1

用途：把 Stage 7 复赛演示、测试、竞争定位、功能矩阵和 QA 脚本整理到本地 proof 目录，方便答辩前归档和检查。

基本用法：

```powershell
.\scripts\proof\build_stage7_proof_pack.ps1
```

常用参数：

```powershell
.\scripts\proof\build_stage7_proof_pack.ps1 -DryRun
.\scripts\proof\build_stage7_proof_pack.ps1 -Zip
.\scripts\proof\build_stage7_proof_pack.ps1 -Open
.\scripts\proof\build_stage7_proof_pack.ps1 -Zip -Open
```

## 输出结构

默认输出：

```text
proof_out/stage7_proof_yyyyMMdd_HHmmss/
  00_status/
  01_demo/
  02_testing/
  03_security/
  04_competition/
  05_scripts/
  README.md
```

## 安全边界

- 不复制本地凭据文件。
- 不复制 APK 本体，只记录 APK 路径、大小和时间。
- 不读取 `config.local.json` 内容，只记录是否存在。
- 不复制 `app/build`、`core/build`、`.gradle`、本地配置或构建产物。
- 缺失文档只输出 WARN，不阻止打包。

## 不替代什么

这个 proof pack 适合复赛 proof 归档，不替代：

- GitHub Actions。
- 本地 Gradle 测试。
- 真机 smoke test。
- 导出报告人工检查。
- 最终提交前的安全扫描。

