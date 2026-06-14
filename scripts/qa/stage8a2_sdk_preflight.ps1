param()

$ErrorActionPreference = "Continue"

Write-Host "== Stage 8A-2 SDK Preflight =="
Write-Host "Read-only, WARN-only. No Gradle. No repo file changes. Does not read config.local.json."

function Ok($Message) { Write-Host "[OK] $Message" -ForegroundColor Green }
function Warn($Message) { Write-Host "[WARN] $Message" -ForegroundColor Yellow }
function Fail($Message) { Write-Host "[FAIL] $Message" -ForegroundColor Red }

function Remove-TempDir($Path) {
    if ($Path -and (Test-Path -LiteralPath $Path)) {
        Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction SilentlyContinue
    }
}

$repo = (Get-Location).Path
$aar = Join-Path $repo "app\libs\llm-sdk-release.aar"
$temp = Join-Path $env:TEMP ("classmate_stage8a2_sdk_" + [guid]::NewGuid().ToString("N"))
$classesJar = $null

Write-Host "`n-- config.local.json --"
if (Test-Path -LiteralPath "config.local.json") {
    Warn "config.local.json exists locally. Content was not read."
} else {
    Ok "config.local.json not present."
}

Write-Host "`n-- AAR presence --"
if (Test-Path -LiteralPath $aar) {
    $item = Get-Item -LiteralPath $aar
    Ok "AAR exists: $($item.FullName)"
    Ok "AAR size: $($item.Length) bytes"
    Ok "AAR LastWriteTime: $($item.LastWriteTime)"
} else {
    Fail "AAR missing: $aar"
}

Write-Host "`n-- git ignore / status --"
$ignore = git check-ignore -v "app\libs\llm-sdk-release.aar" 2>$null
if ($LASTEXITCODE -eq 0 -and $ignore) {
    Ok "AAR is ignored: $ignore"
} else {
    Warn "AAR is not ignored or check-ignore failed."
}

$statusAar = git status --short -- "app\libs\llm-sdk-release.aar" 2>$null
if ($statusAar) {
    Warn "git status shows AAR: $statusAar"
} else {
    Ok "git status does not show AAR."
}

Write-Host "`n-- forbidden tracked files --"
$forbidden = @(
    "config.local.json", "local.properties", "secrets.properties",
    ".env", ".env.*", "*.jks", "*.keystore", "*.apk", "*.aab",
    "app/build", "core/build", "build", ".gradle"
)
$tracked = @(git ls-files $forbidden 2>$null)
if ($tracked.Count -eq 0) {
    Ok "No forbidden tracked files."
} else {
    foreach ($path in $tracked) { Warn "Tracked forbidden path: $path" }
}

try {
    if (Test-Path -LiteralPath $aar) {
        Write-Host "`n-- unpack AAR --"
        New-Item -ItemType Directory -Path $temp -Force | Out-Null
        $zipCopy = Join-Path $temp "llm-sdk-release.zip"
        Copy-Item -LiteralPath $aar -Destination $zipCopy -Force
        Expand-Archive -LiteralPath $zipCopy -DestinationPath $temp -Force
        Ok "AAR unpacked to temp dir: $temp"
        $classesJar = Join-Path $temp "classes.jar"
        if (Test-Path -LiteralPath $classesJar) {
            Ok "classes.jar found."
        } else {
            Fail "classes.jar not found in AAR."
        }

        Write-Host "`n-- native libs --"
        $native = Get-ChildItem -LiteralPath $temp -Recurse -File -Filter "*.so" -ErrorAction SilentlyContinue
        if ($native) {
            Ok "Native libraries found: $($native.Count)"
            $arm64 = $native | Where-Object { $_.FullName -match "arm64-v8a" }
            if ($arm64) { Ok "arm64-v8a native libraries found: $($arm64.Count)" } else { Warn "No arm64-v8a native libraries found." }
        } else {
            Warn "No native .so libraries found in AAR."
        }
    }

    Write-Host "`n-- javap checks --"
    $javap = Get-Command javap -ErrorAction SilentlyContinue
    $jar = Get-Command jar -ErrorAction SilentlyContinue
    if (-not $javap) { Warn "javap not found on PATH." }
    if (-not $jar) { Warn "jar not found on PATH." }

    if ($javap -and $jar -and $classesJar -and (Test-Path -LiteralPath $classesJar)) {
        $classList = & jar tf $classesJar
        function Find-Class($SimpleName) {
            $hit = $classList | Where-Object { $_ -match "(^|/)$SimpleName\.class$" } | Select-Object -First 1
            if (-not $hit) { return $null }
            return (($hit -replace "/", ".") -replace "\.class$", "")
        }

        $llmConfig = Find-Class "LlmConfig"
        $llmManager = Find-Class "LlmManager"
        $tokenCallback = Find-Class "TokenCallback"

        if ($llmConfig) { Ok "LlmConfig class: $llmConfig" } else { Fail "LlmConfig class not found." }
        if ($llmManager) { Ok "LlmManager class: $llmManager" } else { Fail "LlmManager class not found." }
        if ($tokenCallback) { Ok "TokenCallback class: $tokenCallback" } else { Fail "TokenCallback class not found." }

        if ($llmConfig) {
            $out = & javap -classpath $classesJar $llmConfig 2>$null
            if ($out -match "multimodal") { Ok "LlmConfig.multimodal found." } else { Warn "LlmConfig.multimodal not found." }
        }
        if ($llmManager) {
            $out = & javap -classpath $classesJar $llmManager 2>$null
            if ($out -match "callVit\(byte\[\], int, int\)") { Ok "LlmManager.callVit(byte[], int, int) found." } else { Warn "LlmManager.callVit signature not found." }
        }
        if ($tokenCallback) {
            $out = & javap -classpath $classesJar $tokenCallback 2>$null
            if ($out -match "onComplete\(\)") { Ok "TokenCallback.onComplete() found." } else { Warn "TokenCallback.onComplete() not found or has unexpected signature." }
        }
    }
} finally {
    Remove-TempDir $temp
}

Write-Host "`n-- direct SDK imports --"
$roots = @("app\src", "core\src")
$directHits = @()
foreach ($root in $roots) {
    if (Test-Path -LiteralPath $root) {
        $files = Get-ChildItem -LiteralPath $root -Recurse -File -ErrorAction SilentlyContinue
        foreach ($file in $files) {
            $directHits += Select-String -LiteralPath $file.FullName -Pattern "import com.vivo.llmsdk" -SimpleMatch -ErrorAction SilentlyContinue
        }
    }
}
if ($directHits.Count -eq 0) {
    Ok "No direct import com.vivo.llmsdk hits in app/core."
} else {
    foreach ($hit in $directHits) { Warn "$($hit.Path):$($hit.LineNumber): $($hit.Line.Trim())" }
}

Write-Host "`n-- qwen guard --"
$qwenHits = @()
$thinkingHits = @()
foreach ($root in $roots) {
    if (Test-Path -LiteralPath $root) {
        $files = Get-ChildItem -LiteralPath $root -Recurse -File -ErrorAction SilentlyContinue
        foreach ($file in $files) {
            $qwenHits += Select-String -LiteralPath $file.FullName -Pattern "qwen3.5-plus" -SimpleMatch -ErrorAction SilentlyContinue
            $thinkingHits += Select-String -LiteralPath $file.FullName -Pattern "enable_thinking" -SimpleMatch -ErrorAction SilentlyContinue
        }
    }
}
if ($qwenHits.Count -gt 0) { Ok "qwen3.5-plus references found: $($qwenHits.Count)" } else { Warn "No qwen3.5-plus references found." }
if ($thinkingHits.Count -gt 0) { Ok "enable_thinking references found: $($thinkingHits.Count)" } else { Warn "No enable_thinking references found." }

Write-Host "`n== Summary =="
Write-Host "Preflight complete. Review WARN/FAIL lines manually before real-device smoke."
