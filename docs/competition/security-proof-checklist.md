# Security Proof Checklist

本清单用于真机测试、证明材料整理、GitHub 提交前检查。不得包含真实 AppID/AppKEY/API key。

## 1. Git 不追踪

每次提交前运行：

```powershell
git ls-files config.local.json local.properties secrets.properties .env .env.* *.jks *.keystore *.apk *.aab app/build core/build build .gradle
```

期望：无输出。

禁止追踪：

- `config.local.json`
- `local.properties`
- `secrets.properties`
- `.env`
- `.env.*`
- `*.jks`
- `*.keystore`
- `*.apk`
- `*.aab`
- `app/build`
- `core/build`
- `build`
- `.gradle`

## 2. 禁止泄漏位置

以下位置不得出现真实密钥、完整 prompt/messages、vendor response body 或 reasoning_content：

- README
- docs
- Issues
- screenshots
- logs
- exports
- history store
- learning store
- redacted proof logs
- demo video
- PPT / poster
- architecture notes
- reviewer README

## 3. 每次真机测试前

- AppID/AppKEY 只进入本地 debug import 或本地 git-ignored 配置。
- 不截图密钥输入框。
- 不截图完整 key。
- 不把完整 key 发给任何 AI、Issue、聊天记录或 proof 文档。
- 不提交 `config.local.json`。
- 不把 request body、完整课程正文、厂商原始响应体放入日志或导出。
- 若曾经在公开位置暴露 key，立即重置 AppKEY。

## 4. 导出文件检查

对 Markdown / HTML / TXT 导出逐个检查：

- 不含 key。
- 不含完整 AppID/AppKEY/API key。
- 不含 Authorization header。
- 不含 prompt/messages。
- 不含 vendor response body。
- 不含 reasoning_content。
- 不含完整 redacted log。
- 只包含学习业务数据：课程名、知识点、证据、微测、复习任务、课程库、薄弱点、MindMap、白名单视频搜索链接。

## 5. 日志检查

允许的安全字段：

- provider
- status
- latency_ms
- request_profile
- timeout_ms
- network_subtype
- model
- max_tokens
- response_content_length
- json_extracted
- validation_error_type
- fallback_used
- reasoning_present
- reasoning_length

禁止字段或内容：

- 明文 AppID/AppKEY/API key。
- Authorization header。
- app_id 明文值。
- 完整 prompt/messages。
- 课程全文。
- request body。
- response body。
- reasoning_content 全文。

## 6. 发布/答辩前

1. 重置任何曾经暴露过的 AppKEY。
2. 跑 PowerShell secrets scan：

   ```powershell
   scripts\secrets_scan\secrets_scan.ps1
   ```

3. 跑 bash secrets scan：

   ```bash
   bash scripts/secrets_scan/secrets_scan.sh
   ```

4. 检查 GitHub Actions：Android CI 和 Secrets Scan 应通过。
5. 检查所有 proof 截图：不含密钥、不含本地隐私路径、不含账户隐私。
6. 检查 demo video：不展示 debug import 明文、不展示完整 key。
7. 检查 README / docs / PPT / poster：只写占位符和安全说明。

## 7. Proof 材料允许包含

- masked credential status，例如 credential_present=true、appId=masked、appKey=masked。
- BlueLM diagnostic 的安全结果，例如 provider=BLUELM、status=OK、http_status=200。
- redacted analysis logs。
- 导出文件的目录摘要，例如 `exports/<file>`，不要暴露个人本机绝对路径。
- 截图中的课程内容应使用公开示例或已授权课堂文本。
