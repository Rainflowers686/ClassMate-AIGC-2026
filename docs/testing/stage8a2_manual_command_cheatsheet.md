# Stage 8A-2 Manual Command Cheatsheet

> 浣庨闄╃淮鎶ゆ湡閫熸煡銆備笉瑙︾ app/src / core/src / Gradle / Manifest / AAR銆?> 鏈€鍚庝竴娆℃洿鏂帮細2026-06-05

---

## 1. 鏋勫缓 Demo SDK AAR

鍦ㄥ畼鏂?BlueLM demo 婧愮爜鐩綍涓嬶紙鍙﹁鎻愪緵锛屼笉鍦ㄦ浠撳簱锛夛細

```bash
# 鍋囪 demo 椤圭洰鏍圭洰褰曚负 $DEMO_ROOT
cd $DEMO_ROOT

# 濡傛灉 demo 浣跨敤 Gradle wrapper
./gradlew :llm-sdk:assembleRelease

# 鎴栫洿鎺ヨ皟鐢?module 鏋勫缓
./gradlew :llm-sdk:bundleReleaseAar
```

杈撳嚭 AAR 閫氬父浣嶄簬锛?```
$DEMO_ROOT/llm-sdk/build/outputs/aar/llm-sdk-release.aar
```

> **娉ㄦ剰**锛氭浠撳簱涓嶅寘鍚?demo 婧愮爜锛屼篃涓嶅寘鍚鏋勫缓鐨?AAR銆侫AR 鐢卞紑鍙戣€呬粠瀹樻柟娓犻亾鑾峰彇骞惰嚜琛屾斁缃€?
---

## 2. 淇?local.properties

```bash
# Windows (PowerShell)
@"
sdk.dir=C\:\\Users\\Rain\\AppData\\Local\\Android\\Sdk
ndk.dir=C\:\\Users\\Rain\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125
"@ | Out-File -FilePath local.properties -Encoding utf8

# 鎴栫洿鎺ョ紪杈?notepad local.properties
```

> **娉ㄦ剰**锛歚local.properties` 宸插垪鍏?`.gitignore`锛屼笉浼氭彁浜ゃ€傝矾寰勪緷鏈満瀹為檯 SDK 浣嶇疆濉啓銆?
---

## 3. 澶嶅埗 AAR 鍒?ClassMate

```bash
# 浠?demo 鏋勫缓杈撳嚭澶嶅埗鍒?ClassMate
cp $DEMO_ROOT/llm-sdk/build/outputs/aar/llm-sdk-release.aar \
   "D:\Edge Download\AIGC\ClassMate\app\libs\llm-sdk-release.aar"

# 鎴?PowerShell
Copy-Item "$DEMO_ROOT\llm-sdk\build\outputs\aar\llm-sdk-release.aar" `
           "D:\Edge Download\AIGC\ClassMate\app\libs\llm-sdk-release.aar"
```

楠岃瘉鏀剧疆姝ｇ‘锛?```bash
Test-Path "D:\Edge Download\AIGC\ClassMate\app\libs\llm-sdk-release.aar"
# 搴旇緭鍑?True
```

---

## 4. javap 妫€鏌ュ懡浠?
妫€鏌?AAR 鍐呯殑绫诲拰鏂规硶绛惧悕锛堜笉杩愯 Gradle锛夛細

```bash
# 瑙ｅ帇 AAR
cd "D:\Edge Download\AIGC\ClassMate\app\libs"
mkdir _aar_inspect
cd _aar_inspect
jar xf ../llm-sdk-release.aar

# 妫€鏌?classes.jar 涓殑鍏抽敭绫?javap -classpath classes.jar com.vivo.llmsdk.LlmConfig
javap -classpath classes.jar com.vivo.llmsdk.LlmManager
javap -classpath classes.jar com.vivo.llmsdk.TokenCallback

# 纭鍏抽敭鏂规硶绛惧悕
# LlmConfig.multimodal 瀛楁
# LlmManager.callVit(byte[], int, int) 鏂规硶
# TokenCallback.onToken(String) / onComplete() / onError(int, String)

# 娓呯悊
cd ../..
rm -rf _aar_inspect
```

鏈熸湜杈撳嚭绀轰緥锛堜粎渚涘弬鑰冿紝瀹為檯浠ュ畼鏂?SDK 涓哄噯锛夛細
```
public class com.vivo.llmsdk.TokenCallback {
  public void onToken(java.lang.String);
  public void onComplete();          // 鏃犲弬鏁?鈥?鍏抽敭妫€鏌ョ偣
  public void onError(int, java.lang.String);
}

public class com.vivo.llmsdk.LlmManager {
  public int callVit(byte[], int, int);
}

public class com.vivo.llmsdk.LlmConfig {
  public boolean multimodal;
}
```

---

## 5. git check-ignore 鍛戒护

纭 AAR 琚?`.gitignore` 瑕嗙洊锛堥槻姝㈣鎻愪氦锛夛細

```bash
cd "D:\Edge Download\AIGC\ClassMate"

# 妫€鏌?AAR 鏄惁琚拷鐣?git check-ignore -v app/libs/llm-sdk-release.aar
# 鏈熸湜杈撳嚭绫讳技: .gitignore:24:app/libs/*.aar app/libs/llm-sdk-release.aar

# 妫€鏌?.so 鏄惁琚拷鐣?git check-ignore -v app/libs/arm64-v8a/libllm.so
# 鏈熸湜杈撳嚭绫讳技: .gitignore:25:app/libs/**/*.so    app/libs/arm64-v8a/libllm.so

# 妫€鏌ユ晱鎰熼厤缃枃浠舵槸鍚﹁蹇界暐
git check-ignore -v config.local.json
git check-ignore -v local.properties
git check-ignore -v secrets.properties
```

---

## 6. Secrets Scan 鍛戒护

```bash
cd "D:\Edge Download\AIGC\ClassMate"

# 杩愯 secrets 鎵弿锛圥owerShell锛?powershell -ExecutionPolicy Bypass -File scripts\secrets_scan\secrets_scan.ps1

# 杩愯闈欐€佸璁?powershell -ExecutionPolicy Bypass -File scripts\qa\stage8_ondevice_static_audit.ps1

# 鎵嬪姩 grep 妫€鏌ユ晱鎰熻瘝锛堜笉璇?config.local.json 鍐呭锛?git ls-files -- '*.kt' '*.kts' '*.json' '*.xml' '*.md' |
  Select-String -Pattern '(?i)(apiKey|appKey|app_id|Bearer|Authorization|secret|token)' |
  Where-Object { $_.Path -notmatch 'secrets_scan' -and $_.Path -notmatch 'config\.local' }
```

> 绂佹璇嶆鏌ヨ鏄庯紙浠ヤ笅涓烘娴嬫ā寮忕ず渚嬶紝涓嶆槸鐪熷疄瀵嗛挜锛夛細
> - `"appKey": "***"` 鈥?妫€娴嬫ā寮?`appKey`
> - `"apiKey": "***"` 鈥?妫€娴嬫ā寮?`apiKey`
> - `Authorization: Bearer ***` 鈥?妫€娴嬫ā寮?`Bearer`
> - 涓婅堪妯″紡鐢ㄤ簬闈欐€佹壂鎻忥紝鑻ュ湪婧愮爜涓嚭鐜板嵆瑙﹀彂 WARN

---

## 7. Qwen Guard 妫€鏌ュ懡浠?
```bash
cd "D:\Edge Download\AIGC\ClassMate"

# 妫€鏌?core/src 鍜?app/src 涓槸鍚︽畫鐣?qwen3.5-plus 鎴?enable_thinking
rg -n "qwen3\.5-plus|enable_thinking" app/src core/src --type-add 'src:*.kt' -t src 2>$null

# 濡傛灉 rg 涓嶅彲鐢紝鐢?PowerShell Select-String
Get-ChildItem -Path app/src,core/src -Recurse -Include *.kt -ErrorAction SilentlyContinue |
  Select-String -Pattern "qwen3\.5-plus|enable_thinking" |
  ForEach-Object { "$($_.Path):$($_.LineNumber): $($_.Line.Trim())" }
```

鏈熸湜锛氭棤鍖归厤锛堟垨浠呭湪宸茬煡鐨?guard 璇存槑鏂囨。涓嚭鐜帮級銆?
---

## 8. 涓嶈鎻愪氦 AAR 鐨勬彁閱?
```
!!! 閲嶈 !!!

app/libs/*.aar 宸插湪 .gitignore 绗?24 琛屽垪鍏ュ拷鐣ャ€?鍗充娇浣犳湰鍦版湁 AAR锛実it status 涔熶笉浼氭樉绀哄畠銆?濡傛灉浣犵湅鍒?AAR 鍑虹幇鍦?git status 涓紝璇风珛鍗筹細

1. 妫€鏌?.gitignore 鏄惁鍖呭惈 app/libs/*.aar
2. 濡傛灉涔嬪墠宸茶拷韪細git rm --cached app/libs/llm-sdk-release.aar
3. 纭 git check-ignore 鐢熸晥
4. 姘歌繙涓嶈 git add -f app/libs/*.aar

鍘熷洜锛?- AAR 鏄緵搴斿晢浜岃繘鍒讹紝涓嶅簲鍏ヤ粨搴?- 鍙兘鍖呭惈鏈叕寮€鐨勬巿鏉冧俊鎭?- 浠撳簱浠呬繚鐣欏鎺ヤ唬鐮侊紙Seam/Provider锛夛紝AAR 鐢卞紑鍙戣€呮湰鍦版彁渚?```

---

## 9. 蹇€熻嚜妫€娓呭崟

鍦ㄨ瘎瀹℃垨鎻愪氦鍓嶆墽琛岋細

- [ ] `git check-ignore -v app/libs/llm-sdk-release.aar` 杈撳嚭棰勬湡琛?- [ ] `powershell -ExecutionPolicy Bypass -File scripts\secrets_scan\secrets_scan.ps1` 閫氳繃
- [ ] `powershell -ExecutionPolicy Bypass -File scripts\qa\stage8_ondevice_static_audit.ps1` 鏃犳柊 WARN
- [ ] `javap` 纭 TokenCallback.onComplete() 鏃犲弬鏁?- [ ] 鏃?`qwen3.5-plus` / `enable_thinking` 娈嬬暀
- [ ] `config.local.json` 鏈杩借釜
- [ ] 娌℃湁 `git add -f` 寮哄埗娣诲姞蹇界暐鏂囦欢
