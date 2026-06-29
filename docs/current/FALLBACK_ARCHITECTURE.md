# Fallback Architecture

## 模型 fallback

```text
BlueLM cloud -> on-device 3B -> local rule
```

- 蓝心云端：主分析、反馈增强、AI 精修导出。
- 端侧 3B：optional fallback，依赖设备、模型目录、权限。
- 本地规则：最低可用闭环，保证有资料时不空白。

## 语音 fallback

```text
Official ASR Long / Realtime WS -> Android SpeechRecognizer -> recording saved + manual transcript
Official TTS WS -> Android TextToSpeech -> script/text only
```

- 官方 ASR/TTS 需要 AppKey 和真机验证。
- 系统 ASR/TTS 设备依赖。
- 手动转写和文稿不是失败，而是保证学习闭环不中断的 fallback。

## OCR fallback

```text
Official OCR -> on-device/image draft where available -> manual image text
```

失败图片不阻断成功图片；用户确认前不进入学习闭环。

## Quiz fallback

```text
Model-generated quiz -> repaired answerable quiz -> local basic quiz
```

坏题不进入 Practice、WrongBook 或导出；无题不展示空链路。

## Export fallback

```text
AI polished pack -> existing polished draft or local study pack -> HTML/Text safe export
```

普通导出始终可用；AI 精修失败不覆盖普通版。
