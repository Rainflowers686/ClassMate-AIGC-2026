# Stage 7 Commit Split Plan

不要在当前混合工作区直接一把提交。先等 Claude 的生产代码稳定，再按逻辑拆分。

## 1. Stage 7C Practice commit

- 推荐 commit message：`feat(practice): add adaptive practice session and need-more-practice loop`
- 应包含文件范围：`app/src` 中 Practice/Review 相关、`core/src` 中 practice/learning/exporting 相关、对应测试。
- 不应包含文件范围：competition docs、proof scripts、Course Library docs。
- 提交前检查：
  ```powershell
  .\gradlew.bat :core:test
  .\gradlew.bat :app:testDebugUnitTest
  .\gradlew.bat :app:assembleDebug
  scripts\secrets_scan\secrets_scan.ps1
  git diff --check
  ```

## 2. Stage 7D Course Library commit

- 推荐 commit message：`feat(library): add course search filters and onboarding guide`
- 应包含文件范围：History/Home/Settings/Course Library UI、对应 focused tests、`docs/product/course_library_ia_notes.md`、`docs/testing/stage7_course_library_smoke.md`。
- 不应包含文件范围：Review/Practice/LearningStore、provider、validators。
- 提交前检查：
  ```powershell
  .\gradlew.bat :app:testDebugUnitTest
  .\gradlew.bat :app:assembleDebug
  scripts\secrets_scan\secrets_scan.ps1
  git diff --check
  ```

## 3. Stage 7E/7F Demo docs commit

- 推荐 commit message：`docs(competition): add stage 7 demo proof and QA scripts`
- 应包含文件范围：Stage 7 final demo script、judge Q&A、competitor positioning、feature matrix、screenshot list、QA scripts、regression plan。
- 不应包含文件范围：app/core production code。
- 提交前检查：
  ```powershell
  git diff --check
  git status --short docs scripts
  scripts\secrets_scan\secrets_scan.ps1
  ```

## 4. Stage 7G Proof pack commit

- 推荐 commit message：`docs(proof): add stage 7 competition proof pack generator`
- 应包含文件范围：`scripts/proof/build_stage7_proof_pack.ps1`、`scripts/proof/README.md`、submission checklist、demo narration assets、INDEX Stage 7G entries。
- 不应包含文件范围：proof_out 输出目录、zip、APK、build。
- 提交前检查：
  ```powershell
  scripts\proof\build_stage7_proof_pack.ps1 -DryRun
  git diff --check
  scripts\secrets_scan\secrets_scan.ps1
  ```

## 5. Stage 7H Demo/Issues docs commit

- 推荐 commit message：`docs(demo): add stage 7 presentation assets and proof automation`
- 应包含文件范围：slide outline、image prompt pack、video script、README draft、release notes、architecture one-pager、issues backlog、labels/milestones、commit split plan、demo folder maker、proof checker、test templates、risk register。
- 不应包含文件范围：app/core/test/Gradle/Manifest。
- 提交前检查：
  ```powershell
  scripts\proof\build_stage7_proof_pack.ps1 -DryRun
  scripts\demo\make_stage7_demo_folder.ps1 -DryRun
  git diff --check
  scripts\secrets_scan\secrets_scan.ps1
  ```

## 注意

- 不要提交 `proof_out/` 或 `demo_out/`。
- 不要提交本地配置文件。
- 如果 build 失败来自 Stage 7C 半成品，先完成 Stage 7C，再提交 docs。
- 每个 commit 都应能独立回滚。

