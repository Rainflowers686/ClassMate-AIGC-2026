> 状态：历史/参考材料，可能包含旧版本事实或阶段性问题。当前 1.14.2 / versionCode 115 状态请见 [FINAL_STATUS_1_14_2.md](FINAL_STATUS_1_14_2.md) 与 [DOCUMENT_INDEX.md](DOCUMENT_INDEX.md)。

# 蓝心云端大模型 — vivo 真机排查指南 (v1)

本文面向真机调试：用户已在「设置 → AI 模型配置 → 蓝心大模型」填写并保存 AppID/AppKey，但课堂分析仍显示
`BLUELM:NETWORK` 或看起来没接上云端。先读这一页再发问题给开发者。

> 安全红线：**任何截图/日志都不要包含 AppKey、Authorization 明文。** App 内所有诊断都是脱敏的（只显示掩码 AppID、host、错误码）。

---

## 1. 配置保存后到底生效到哪里？

| 能力 | 凭据来源 | 保存后是否立即生效 |
|---|---|---|
| 课堂分析 / 知识点 / 微测 / 解析 / 复习 / 诊断（主 BlueLM 对话） | 设置里保存的 AppID/AppKey（app 私有 `classmate_model_config.json`） | **立即**（保存即重建 active bundle） |
| OCR / 长语音转写（capture） | 同上（capture 现在也回退读取该文件） | **需重启 App**（capture/official 网关在启动时构建） |
| Query Rewrite / Embedding / Text Similarity（official 检索） | 同上 | 见 §6（当前真机默认仍走本地兜底，原因诚实写在 §6） |

注意：`config.local.json` 是**开发机**用的文件，真机进程工作目录里通常不存在，所以真机上**设置里保存的那份就是唯一凭据来源**。

确认是否真的保存成功：
- 设置页「当前状态」应显示「已配置」，且「测试配置（readiness）」提示 `配置就绪（READY）`。
- 若提示 `AppKey 看起来是掩码（***）`：说明你把界面上显示的掩码当成 key 又保存了一次。请重新输入**完整** AppKey（完整 key 永远不显示，掩码不会被保存）。

---

## 2. `BLUELM:NETWORK` 现在如何细分

错误码已经带上传输子类型，便于定位（在「课堂分析失败」面板的「云端蓝心：」一行）：

| 错误码 | 含义 | 通常原因 |
|---|---|---|
| `BLUELM:CONFIG_MISSING` | 没读到可用凭据 | 未配置 / 占位符 / 掩码 key（不是网络问题） |
| `BLUELM:NETWORK:DNS` | 域名解析失败 | 无网络 / DNS / 域名写错 |
| `BLUELM:NETWORK:TLS` | TLS/证书握手失败 | 代理抓包、系统时间错误、证书问题 |
| `BLUELM:NETWORK:CONNECT` | 连接被拒/不可达 | 防火墙、端口、网络隔离 |
| `BLUELM:NETWORK:WRITE` / `:READ` | 连上后读写中断 | 网络抖动、超时前断开 |
| `BLUELM:SOCKET_TIMEOUT` | 读超时 | 网络慢 / 服务无响应 |
| `BLUELM:UNAUTHORIZED:401` / `:403` | 鉴权失败 | AppKey/AppID 不匹配、未开通该能力 |
| `BLUELM:APP_ID_HEADER_MISSING` | app_id 头缺失/为空 | AppID 没填或被掩码 |
| `BLUELM:RATE_LIMITED:429` | 限流 | 调用过快/配额 |
| `BLUELM:EMPTY_RESPONSE` / `:PARSE_ERROR` | 2xx 但无可用文本/解析失败 | 模型名错误、响应结构异常 |

---

## 3. CONFIG_REQUIRED / AUTH_FAILED / NETWORK_FAILED / SCHEMA_FAILED 怎么区分

- **CONFIG_REQUIRED**（配置问题，非网络）：`CONFIG_MISSING` / 掩码 key / AppID 缺失。先回设置补全。
- **AUTH_FAILED**：`UNAUTHORIZED:401/403` 或 `APP_ID_HEADER_MISSING`。AppID/AppKey 不匹配，或该 AppKey 未开通对应能力。
- **NETWORK_FAILED**：`NETWORK:*` / `SOCKET_TIMEOUT`。换网络、关代理、校正系统时间后重试。
- **SCHEMA_FAILED**：`PARSE_ERROR` / `EMPTY_RESPONSE`。多为模型名或响应结构问题，记录 `http_status` 与 `request_id_name_used`。

---

## 4. 怎么确认云端真的接上了（而不是悄悄走了本地/端侧）

1. 开发包里用「测试蓝心连通性（debug 诊断）」：成功会返回 `status=OK`、`http_status=200`，且**不**落任何课程数据。
2. 跑一次真实课堂分析，看结果页「来源」：
   - `云端蓝心` = 真的走了云端 BlueLM；
   - `端侧蓝心` = 云端失败后端侧 3B 接管；
   - `安全占位` = 两者都不可用（不会伪造知识点）。
3. App 不会把网络失败显示成云端成功——失败就是失败，错误码如 §2。

---

## 5. 端侧模型正常但云端不通

- 端侧（/sdcard/1225 模型 + 全文件权限）正常 ≠ 云端就绪：两者凭据/通道完全独立。
- 若分析「来源」恒为 `端侧蓝心`，而你期望云端：按 §2 看云端错误码——多半是 `CONFIG_MISSING`（真机重启后再试）、`UNAUTHORIZED`（key 不匹配）或 `NETWORK:*`。
- 路线始终是 `云端蓝心 → 端侧蓝心 → 安全占位`，UI 如实显示最终路线。

---

## 6. 当前真机已知边界（诚实说明，不夸大）

- **主对话模型**（课堂分析及其派生的知识点/微测/解析/复习/诊断）在真机保存凭据后即可走云端 BlueLM。
- **OCR/长语音 capture** 现在也会回退读取设置里保存的凭据（**重启 App 后**生效）。
- **official 检索（Query Rewrite / Embedding / Text Similarity）**：网关已注入真实适配器，但在 App 内它当前由主线程的 L3 快照流程驱动；为避免主线程网络（会崩溃），本轮**未**打开它在真机的实时联网，仍走本地兜底并如实标注。把该流程迁到 IO 线程后即可开启——这是明确的后续项，不是已完成项。
- 这些状态在 5 级诊断里能看到：`OFFICIAL_RUNTIME_USED` 才是真用了云端；`LOCAL_FALLBACK_USED` / `OFFICIAL_RUNTIME_NOT_CONFIGURED` 都是兜底。

---

## 7. 课程领域识别错了（例如机械被识别成大学物理）

- 现在领域识别基于内容证据词打分：机械（齿轮/轴承/应力/应变/材料力学…）不会再被算成大学物理；信号弱时回落「通用课堂」并提示确认。
- 手动改：导入页「科目 / 术语表」卡片里，**直接在「自定义课程 / 学科名」输入框填写任意课程名**（机械原理、医学、法学、经管…），不再只能选内置六项。
- 导入页会显示「上次识别：X（置信度 Y%，可在上方修改课程名）」，据此确认或修改。

---

## 8. 自定义术语表

- 任意课程都会在分析时按内容**动态生成术语**（不依赖内置六个学科）。
- 内置六个学科只是**起始术语包**；在「自定义课程 / 学科名」里输入新学科即可。
- 动态术语会进入分析的提示词上下文，帮助知识点抽取/解析更贴合本学科。

---

## 9. 发给开发者时：可以发 / 不能发

可以发：
- 错误码全文（如 `BLUELM:NETWORK:TLS`、`BLUELM:UNAUTHORIZED:401`）；
- 诊断面板的脱敏行（`http_status`、`stage`、`subtype`、`latency_ms`、掩码 AppID、host）；
- 结果页「来源」标签、导入页「上次识别」行。

**不能发**：
- AppKey / Authorization 明文、完整 AppID；
- 含密钥的 `config.local.json` 或 `classmate_model_config.json` 原文；
- 长段用户原始课堂文本。
