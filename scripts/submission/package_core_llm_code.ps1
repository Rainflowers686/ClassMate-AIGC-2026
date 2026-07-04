param(
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$repo = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$out = Join-Path $repo "_ai_outputs\core_llm_code_package"

$include = @(
    "core\src\main\kotlin\com\classmate\core\provider",
    "core\src\main\kotlin\com\classmate\core\ai",
    "core\src\main\kotlin\com\classmate\core\official",
    "core\src\main\kotlin\com\classmate\core\analysis",
    "core\src\main\kotlin\com\classmate\core\export",
    "app\src\main\java\com\classmate\app\platform",
    "app\src\main\java\com\classmate\app\data\BlueLMHttpTransport.kt",
    "app\src\main\java\com\classmate\app\l3",
    "app\src\main\java\com\classmate\app\diagnostics",
    "scripts\qa\provider_live_smoke.ps1",
    "docs\current\CORE_LLM_CODE_PACKAGE_GUIDE_1_14_18.md",
    "docs\current\OFFICIAL_INTERFACE_REFERENCE_INDEX.md",
    "docs\current\CLAUDE_OFFICIAL_INTERFACE_LOOKUP_GUIDE.md"
)

$deny = @(
    "config.local.json",
    "\.apk$",
    "\.aab$",
    "\.aar$",
    "\.ttf$",
    "\.otf$",
    "\\build\\",
    "\\.gradle\\",
    "OfficialDemos",
    "Authorization",
    "Bearer ",
    "SECRET",
    "secret",
    "token"
)

function Is-Denied([string]$path) {
    foreach ($pattern in $deny) {
        if ($path -match $pattern) { return $true }
    }
    return $false
}

Write-Host "ClassMate core LLM code package"
Write-Host "Repo: $repo"
Write-Host "Output: $out"
if ($DryRun) { Write-Host "Mode: dry-run" }

$files = New-Object System.Collections.Generic.List[string]
foreach ($rel in $include) {
    $path = Join-Path $repo $rel
    if (-not (Test-Path $path)) {
        Write-Host "SKIP missing: $rel"
        continue
    }
    $item = Get-Item $path
    if ($item.PSIsContainer) {
        Get-ChildItem $item.FullName -Recurse -File |
            Where-Object { -not (Is-Denied $_.FullName) } |
            ForEach-Object { $files.Add($_.FullName) }
    } else {
        if (-not (Is-Denied $item.FullName)) { $files.Add($item.FullName) }
    }
}

$files = $files | Sort-Object -Unique
Write-Host "Files: $($files.Count)"
if ($DryRun) {
    $files | ForEach-Object { Write-Host ("  " + $_.Substring($repo.Path.Length + 1)) }
    exit 0
}

if (Test-Path $out) { Remove-Item -LiteralPath $out -Recurse -Force }
New-Item -ItemType Directory -Force -Path $out | Out-Null

foreach ($file in $files) {
    $rel = $file.Substring($repo.Path.Length + 1)
    $dest = Join-Path $out $rel
    New-Item -ItemType Directory -Force -Path (Split-Path $dest -Parent) | Out-Null
    Copy-Item -LiteralPath $file -Destination $dest
}

$readme = Join-Path $out "README_PACKAGE.txt"
@"
ClassMate core LLM code package
Version: 1.14.18 / versionCode 131

This package contains source and documentation only.
It excludes config.local.json, build outputs, APK/AAB/AAR, OfficialDemos, fonts, and credential material.

Review before zipping:
  Select-String -Path "$out\**\*" -Pattern "AppKey|Authorization|Bearer|SECRET|secret|token" -CaseSensitive:$false
"@ | Set-Content -Path $readme -Encoding UTF8

Write-Host "DONE: $out"
