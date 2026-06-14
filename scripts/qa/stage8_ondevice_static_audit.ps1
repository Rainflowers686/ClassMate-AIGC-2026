param()

$ErrorActionPreference = "Continue"

Write-Host "== Stage 8A OnDevice BlueLM Static Audit =="
Write-Host "WARN-only. No Gradle. No file changes. Does not read config.local.json."

function Ok($Message) { Write-Host "[OK] $Message" -ForegroundColor Green }
function Warn($Message) { Write-Host "[WARN] $Message" -ForegroundColor Yellow }

function ShouldSkip($Path) {
    $p = $Path -replace '/', '\'
    if ($p -match '(^|\\)scripts\\secrets_scan\\') { return $true }
    if ($p -ieq "scripts\qa\stage8_ondevice_static_audit.ps1") { return $true }
    if ($p -ieq "config.local.json") { return $true }
    if ($p -ieq "local.properties") { return $true }
    if ($p -ieq "secrets.properties") { return $true }
    if ($p -match '(^|\\)\.env(\.|$)') { return $true }
    if ($p -match '(^|\\)(app|core)\\build\\') { return $true }
    if ($p -match '(^|\\)\.gradle\\') { return $true }
    return $false
}

function Get-ScanFiles($Roots) {
    $items = New-Object System.Collections.Generic.List[string]
    foreach ($root in $Roots) {
        if (-not (Test-Path -LiteralPath $root)) { continue }
        Get-ChildItem -LiteralPath $root -Recurse -File -ErrorAction SilentlyContinue |
            Where-Object { -not (ShouldSkip $_.FullName) } |
            ForEach-Object { [void]$items.Add($_.FullName) }
    }
    return $items
}

function Search-Terms {
    param(
        [string]$Title,
        [string[]]$Terms,
        [string[]]$Roots,
        [int]$Limit = 80
    )
    Write-Host "`n-- $Title --"
    $files = Get-ScanFiles $Roots
    if ($files.Count -eq 0) {
        Warn "No files found."
        return
    }
    $hits = 0
    foreach ($term in $Terms) {
        foreach ($file in $files) {
            $matches = Select-String -LiteralPath $file -Pattern $term -SimpleMatch -ErrorAction SilentlyContinue
            foreach ($m in $matches) {
                $hits++
                if ($hits -le $Limit) {
                    $line = $m.Line.Trim()
                    if ($line.Length -gt 180) { $line = $line.Substring(0, 180) + "..." }
                    Warn "$($m.Path):$($m.LineNumber): $line"
                }
            }
        }
    }
    if ($hits -eq 0) { Ok "No hits." } else { Warn "$hits hit(s). Review manually; output capped at $Limit." }
}

function Search-Regex {
    param(
        [string]$Title,
        [string[]]$Patterns,
        [string[]]$Roots,
        [int]$Limit = 80
    )
    Write-Host "`n-- $Title --"
    $files = Get-ScanFiles $Roots
    $hits = 0
    foreach ($pattern in $Patterns) {
        foreach ($file in $files) {
            $matches = Select-String -LiteralPath $file -Pattern $pattern -ErrorAction SilentlyContinue
            foreach ($m in $matches) {
                $hits++
                if ($hits -le $Limit) {
                    $line = $m.Line.Trim()
                    if ($line.Length -gt 180) { $line = $line.Substring(0, 180) + "..." }
                    Warn "$($m.Path):$($m.LineNumber): $line"
                }
            }
        }
    }
    if ($hits -eq 0) { Ok "No hits." } else { Warn "$hits hit(s). Review manually; output capped at $Limit." }
}

Write-Host "`n-- config.local.json --"
if (Test-Path -LiteralPath "config.local.json") {
    Warn "config.local.json exists locally. Content was not read."
} else {
    Ok "config.local.json not present."
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
    foreach ($item in $tracked) { Warn "Tracked forbidden path: $item" }
}

Search-Terms "On-device SDK references" @(
    "llm-sdk-release.aar",
    "LlmManager",
    "LlmConfig",
    "/sdcard/1225",
    "OnDeviceBlueLM",
    "OnDevice",
    "LocalProviderChain"
) @("app/src", "core/src", "docs")

Search-Terms "Model routing and external demo mentions" @(
    "DeepSeek",
    "Compatible",
    "demo_compatible",
    "external model",
    "custom model"
) @("app/src", "core/src", "docs")

$sensitiveTerms = @(
    ("Author" + "ization"),
    ("Bear" + "er"),
    ("app" + "Key"),
    ("api" + "Key"),
    ("App" + "KEY"),
    ("app" + "_id"),
    ("pro" + "mpt"),
    ("mes" + "sages"),
    ("reasoning" + "_content"),
    ("vendor" + " body"),
    ("raw" + " response")
)
Search-Terms "Sensitive text and raw-model-interaction risk" $sensitiveTerms @("app/src", "core/src", "docs", "scripts")

function U {
    param([int[]]$Codes)
    return -join ($Codes | ForEach-Object { [char]$_ })
}

$real = U @(0x771F, 0x5B9E)
$done = U @(0x5B8C, 0x6210)
$supported = U @(0x652F, 0x6301)
$connected = U @(0x63A5, 0x5165)
$voiceprintSupported = U @(0x58F0, 0x7EB9, 0x8BC6, 0x522B, 0x5DF2, 0x652F, 0x6301)
$autoSpeakerSupported = U @(0x81EA, 0x52A8, 0x8BF4, 0x8BDD, 0x4EBA, 0x5206, 0x6BB5, 0x5DF2, 0x652F, 0x6301)
$noiseDone = U @(0x5DF2, 0x5B9E, 0x73B0, 0x5E95, 0x566A, 0x5904, 0x7406)
$crawl = U @(0x722C, 0x53D6)
$siteB = U @(0x7AD9)
$douyin = U @(0x6296, 0x97F3)
$xiaohongshu = U @(0x5C0F, 0x7EA2, 0x4E66)

$overclaimPatterns = @(
    "$real\s*ASR\s*($done|$supported|$connected)",
    "$real\s*OCR\s*($done|$supported|$connected)",
    $voiceprintSupported,
    $autoSpeakerSupported,
    $noiseDone,
    "$crawl\s*B$siteB",
    "$crawl\s*$douyin",
    "$crawl\s*$xiaohongshu"
)
Search-Regex "Overclaim and platform crawling phrases" $overclaimPatterns @("app/src", "core/src", "docs")

Write-Host "`n-- Manifest permissions --"
$manifest = "app/src/main/AndroidManifest.xml"
if (Test-Path -LiteralPath $manifest) {
    foreach ($perm in @(
        "android.permission.INTERNET",
        "android.permission.RECORD_AUDIO",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_EXTERNAL_STORAGE",
        "mediatek.permission.ACCESS_APU_SYS"
    )) {
        $hit = Select-String -LiteralPath $manifest -Pattern $perm -SimpleMatch -ErrorAction SilentlyContinue
        if ($hit) { Warn "Manifest contains $perm" } else { Ok "Manifest does not contain $perm" }
    }
} else {
    Warn "AndroidManifest not found."
}

Write-Host "`n-- qwen guard --"
Search-Terms "qwen3.5-plus / enable_thinking guard" @("qwen3.5-plus", "enable_thinking") @("app/src", "core/src")

# --- Stage 8A-2 additions: TokenCallback / callVit / multimodal / gitignore checks ---

Write-Host "`n-- Stage 8A-2: TokenCallback.onComplete() no-param check --"
$tcPatterns = @("onComplete\s*\(\s*LlmStats", "onComplete\s*\(\s*\w+\s+\w+", "onComplete\s*\(\s*[^)]+\)")
$tcFiles = @(Get-ChildItem -Path app/src,core/src,docs -Recurse -Include *.kt,*.java,*.md -ErrorAction SilentlyContinue |
    Where-Object { -not (ShouldSkip $_.FullName) })
$tcHits = 0
foreach ($file in $tcFiles) {
    foreach ($pat in $tcPatterns) {
        $matches = Select-String -LiteralPath $file.FullName -Pattern $pat -ErrorAction SilentlyContinue
        foreach ($m in $matches) {
            $tcHits++
            if ($tcHits -le 20) {
                Warn "$($m.Path):$($m.LineNumber): onComplete() may have unexpected parameters: $($m.Line.Trim())"
            }
        }
    }
}
if ($tcHits -eq 0) {
    Ok "No onComplete() parameter misuse detected. (Official SDK: onComplete() takes no parameters, no LlmStats.)"
} else {
    Warn "$tcHits hit(s). Verify onComplete() signature matches official SDK (no params)."
}

Write-Host "`n-- Stage 8A-2: callVit / multimodal interface check --"
$vitTerms = @("callVit", "multimodal", "VIT", "VitEncoder", "vision.encode")
$vitFiles = @(Get-ChildItem -Path app/src,core/src,docs -Recurse -Include *.kt,*.java,*.md -ErrorAction SilentlyContinue |
    Where-Object { -not (ShouldSkip $_.FullName) })
$vitHits = 0
foreach ($term in $vitTerms) {
    foreach ($file in $vitFiles) {
        $matches = Select-String -LiteralPath $file.FullName -Pattern $term -SimpleMatch -ErrorAction SilentlyContinue
        foreach ($m in $matches) {
            $vitHits++
            if ($vitHits -le 30) {
                $line = $m.Line.Trim()
                if ($line.Length -gt 180) { $line = $line.Substring(0, 180) + "..." }
                Write-Host "  [INFO] $($m.Path):$($m.LineNumber): $line" -ForegroundColor Gray
            }
        }
    }
}
if ($vitHits -eq 0) {
    Warn "No callVit / multimodal references found. Expected if ondevice multimodal is not yet wired."
} else {
    Ok "$vitHits callVit/multimodal reference(s) found. Confirm they match official SDK signatures."
}

Write-Host "`n-- Stage 8A-2: app/libs/*.aar gitignore check --"
$aarDir = "app\libs"
if (Test-Path -LiteralPath $aarDir) {
    $aarFiles = Get-ChildItem -LiteralPath $aarDir -Filter *.aar -ErrorAction SilentlyContinue
    foreach ($aar in $aarFiles) {
        $relPath = $aar.FullName.Substring((Get-Location).Path.Length + 1)
        $ignored = git check-ignore -q $relPath 2>$null
        if ($LASTEXITCODE -eq 0) {
            Ok "$relPath is git-ignored (safe)."
        } else {
            Warn "$relPath is NOT git-ignored! Check .gitignore for app/libs/*.aar rule."
        }
    }
    if ($aarFiles.Count -eq 0) {
        Write-Host "  [INFO] No .aar files found in app/libs." -ForegroundColor Gray
    }
} else {
    Write-Host "  [INFO] app/libs directory not found." -ForegroundColor Gray
}

Write-Host "`n== Summary =="
Write-Host "WARN-only audit complete. Review WARN entries manually before rematch proof."
