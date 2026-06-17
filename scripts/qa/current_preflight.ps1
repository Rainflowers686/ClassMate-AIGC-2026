param(
    [switch]$Quick,
    [switch]$SkipAssemble,
    [switch]$NoSecrets,
    [switch]$VerboseLog
)

$ErrorActionPreference = "Continue"

$script:Total = 0
$script:Passed = 0
$script:Failed = 0
$script:Skipped = 0
$script:Failures = New-Object System.Collections.Generic.List[string]

function Write-Header($Name) {
    Write-Host ""
    Write-Host "== $Name ==" -ForegroundColor Cyan
}

function Pass($Message) {
    $script:Total++
    $script:Passed++
    Write-Host "PASS: $Message" -ForegroundColor Green
}

function Fail($Message) {
    $script:Total++
    $script:Failed++
    $script:Failures.Add($Message) | Out-Null
    Write-Host "FAIL: $Message" -ForegroundColor Red
}

function Skip($Message) {
    $script:Total++
    $script:Skipped++
    Write-Host "SKIP: $Message" -ForegroundColor Yellow
}

function Show-Command($Command) {
    if ($VerboseLog) {
        Write-Host "CMD: $Command" -ForegroundColor DarkGray
    }
}

function Run-Step($Name, [scriptblock]$Action) {
    Write-Header $Name
    try {
        & $Action
    } catch {
        Fail "$Name threw: $($_.Exception.Message)"
    }
}

function Check-Command($Name, $Command) {
    Show-Command $Command
    Invoke-Expression $Command
    if ($LASTEXITCODE -eq 0) {
        Pass $Name
    } else {
        Fail "$Name failed with exit code $LASTEXITCODE"
    }
}

function Check-NoTrackedFiles($Name, [string[]]$Patterns) {
    Show-Command "git ls-files -- <patterns>"
    $tracked = @(git ls-files -- $Patterns 2>$null)
    if ($tracked.Count -eq 0) {
        Pass $Name
    } else {
        $tracked | ForEach-Object { Write-Host "TRACKED: $_" -ForegroundColor Red }
        Fail "$Name found forbidden tracked files"
    }
}

function Check-GitIgnored($Name, $Path) {
    Show-Command "git check-ignore -v $Path"
    $result = git check-ignore -v $Path 2>$null
    if ($LASTEXITCODE -eq 0 -and $result) {
        $result | ForEach-Object { Write-Host $_ }
        Pass $Name
    } else {
        Fail "$Name did not match gitignore"
    }
}

function Check-NotTracked($Name, $Path) {
    Show-Command "git ls-files --error-unmatch $Path"
    $result = git ls-files --error-unmatch $Path 2>$null
    if ($LASTEXITCODE -eq 0 -or $result) {
        $result | ForEach-Object { Write-Host "TRACKED: $_" -ForegroundColor Red }
        Fail "$Name is tracked"
    } else {
        Pass "$Name is not tracked"
    }
}

function Get-TextFiles([string[]]$Roots) {
    $allowedExt = @(".kt", ".java", ".kts", ".xml", ".md", ".yml", ".yaml", ".properties", ".json")
    $files = New-Object System.Collections.Generic.List[string]
    foreach ($root in $Roots) {
        if (-not (Test-Path -LiteralPath $root)) { continue }
        $item = Get-Item -LiteralPath $root
        if ($item.PSIsContainer) {
            Get-ChildItem -LiteralPath $root -Recurse -File -ErrorAction SilentlyContinue |
                Where-Object { $allowedExt -contains $_.Extension.ToLowerInvariant() } |
                ForEach-Object { [void]$files.Add($_.FullName) }
        } else {
            if ($allowedExt -contains $item.Extension.ToLowerInvariant()) {
                [void]$files.Add($item.FullName)
            }
        }
    }
    return $files
}

function U($Escaped) {
    return [regex]::Replace($Escaped, "\\u([0-9a-fA-F]{4})", {
        param($Match)
        return [string][char][Convert]::ToInt32($Match.Groups[1].Value, 16)
    })
}

function Is-AllowedNegation($Line) {
    $negationPattern = (U "\u4e0d|\u4e0d\u5f97|\u4e0d\u8981|\u7981\u6b62|\u975e") + "|not|never|must not|should not"
    return ($Line -match $negationPattern)
}

function Check-PatternAbsent($Name, [string[]]$Roots, [string[]]$Patterns, [switch]$AllowNegatedContext) {
    $files = Get-TextFiles $Roots
    $hits = New-Object System.Collections.Generic.List[string]
    foreach ($file in $files) {
        foreach ($pattern in $Patterns) {
            $matches = Select-String -LiteralPath $file -Pattern $pattern -ErrorAction SilentlyContinue
            foreach ($m in $matches) {
                $line = $m.Line.Trim()
                if ($AllowNegatedContext -and (Is-AllowedNegation $line)) { continue }
                if ($line.Length -gt 180) { $line = $line.Substring(0, 180) + "..." }
                [void]$hits.Add("$($m.Path):$($m.LineNumber): $line")
            }
        }
    }

    if ($hits.Count -eq 0) {
        Pass $Name
    } else {
        $hits | Select-Object -First 40 | ForEach-Object { Write-Host $_ -ForegroundColor Red }
        if ($hits.Count -gt 40) { Write-Host "... plus $($hits.Count - 40) more hit(s)" -ForegroundColor Red }
        Fail "$Name found forbidden pattern(s)"
    }
}

function Check-PatternPresent($Name, [string[]]$Paths, $Pattern, $MinimumCount) {
    $hits = New-Object System.Collections.Generic.List[string]
    foreach ($path in $Paths) {
        if (-not (Test-Path -LiteralPath $path)) { continue }
        $matches = Select-String -LiteralPath $path -Pattern $Pattern -ErrorAction SilentlyContinue
        foreach ($m in $matches) {
            [void]$hits.Add("$($m.Path):$($m.LineNumber): $($m.Line.Trim())")
        }
    }

    if ($hits.Count -ge $MinimumCount) {
        $hits | ForEach-Object { Write-Host $_ }
        Pass $Name
    } else {
        $hits | ForEach-Object { Write-Host $_ -ForegroundColor Yellow }
        Fail "$Name expected at least $MinimumCount hit(s), found $($hits.Count)"
    }
}

Write-Host "ClassMate current Stage10 preflight" -ForegroundColor Cyan
if ($Quick) { Write-Host "Mode: Quick" } else { Write-Host "Mode: Full" }
Write-Host "SkipAssemble: $SkipAssemble"
Write-Host "NoSecrets: $NoSecrets"

Run-Step "Repository status" {
    Show-Command "git branch --show-current"
    $branch = git branch --show-current
    Write-Host "Branch: $branch"
    if ($LASTEXITCODE -eq 0) { Pass "git branch" } else { Fail "git branch failed" }

    Show-Command "git status --short"
    $status = git status --short
    if ($status) { $status | ForEach-Object { Write-Host $_ } } else { Write-Host "Working tree clean." }
    if ($LASTEXITCODE -eq 0) { Pass "git status" } else { Fail "git status failed" }

    Write-Host "config.local.json presence only:"
    if (Test-Path -LiteralPath "config.local.json") {
        Write-Host "config.local.json exists; content was not read." -ForegroundColor Yellow
    } else {
        Write-Host "config.local.json not present."
    }
}

Run-Step "Whitespace diff check" {
    Check-Command "git diff --check" "git diff --check"
}

Run-Step "Forbidden tracked files" {
    Check-NoTrackedFiles "No forbidden private/generated files tracked" @(
        "config.local.json",
        "local.properties",
        "secrets.properties",
        ".env",
        ".env.*",
        "*.jks",
        "*.keystore",
        "*.apk",
        "*.aab",
        "app/build",
        "core/build",
        "build",
        ".gradle",
        ".codex_work",
        ".codex_work/*",
        ".vscode",
        ".vscode/*"
    )
}

Run-Step "Local SDK artifact policy" {
    Check-GitIgnored "app/libs/llm-sdk-release.aar is gitignored" "app\libs\llm-sdk-release.aar"
    Check-NotTracked "app/libs/llm-sdk-release.aar" "app\libs\llm-sdk-release.aar"
    Check-NotTracked "config.local.json" "config.local.json"
}

Run-Step "Direct SDK import guard" {
    Check-PatternAbsent "No direct import com.vivo.llmsdk" @("app\src\main", "core\src\main") @(
        "^\s*import\s+com\.vivo\.llmsdk"
    )
}

Run-Step "Forbidden wording guard" {
    $forbiddenWording = @(
        (U "LocalRule\s*\u53ef\u7528"),
        (U "\u672c\u5730\u89c4\u5219\u515c\u5e95"),
        (U "\u89c4\u5219\u667a\u80fd"),
        (U "LocalRule.*\u667a\u80fd"),
        (U "LocalRule.*\u515c\u5e95"),
        (U "\u672c\u5730\u89c4\u5219\u5206\u6790"),
        (U "\u7aef\u4fa7\u7ed3\u679c.*LOCAL_FALLBACK"),
        (U "LOCAL_FALLBACK.*\u7aef\u4fa7"),
        (U "\u591a\u6a21\u6001\u66ff\u4ee3\s*OCR"),
        (U "\u81ea\u52a8\s*OCR\s*\u5b8c\u6210"),
        (U "DeepSeek.*\u590d\u8d5b\u4e3b\u8def\u5f84"),
        (U "Compatible.*\u590d\u8d5b\u4e3b\u8def\u5f84")
    )
    Check-PatternAbsent "No obsolete or exaggerated current-baseline wording" @(
        "README.md",
        "docs\current",
        "app\src\main",
        "core\src\main"
    ) $forbiddenWording -AllowNegatedContext
}

Run-Step "qwen profile-aware thinking guard" {
    $providerFiles = @(
        "core\src\main\kotlin\com\classmate\core\provider\VendorIo.kt",
        "core\src\main\kotlin\com\classmate\core\provider\CloudModelQualityProfile.kt",
        "core\src\main\kotlin\com\classmate\core\provider\BlueLMDiagnostic.kt"
    )
    Check-PatternPresent "qwen3.5-plus model guard is present" $providerFiles "qwen3\.5-plus" 1
    Check-PatternPresent "enable_thinking profile field is present" $providerFiles "enable_thinking" 2
    Check-PatternPresent "thinking support flag is present" $providerFiles "supportsEnableThinking" 1
    Check-PatternPresent "reasoning effort is present" $providerFiles "reasoning_effort" 1
    Check-PatternPresent "deep study enables thinking" $providerFiles "enableThinking\s*=\s*true" 1
}

Run-Step "Secrets scan" {
    if ($NoSecrets) {
        Skip "secrets scan skipped by -NoSecrets"
    } else {
        $scan = "scripts\secrets_scan\secrets_scan.ps1"
        if (Test-Path -LiteralPath $scan) {
            Show-Command $scan
            & $scan
            if ($LASTEXITCODE -eq 0) { Pass "secrets scan" } else { Fail "secrets scan failed with exit code $LASTEXITCODE" }
        } else {
            Fail "secrets scan script missing"
        }
    }
}

Run-Step "Gradle unit tests" {
    if ($Quick) {
        Skip "Gradle tests skipped by -Quick"
    } else {
        Check-Command ":core:test" ".\gradlew.bat :core:test"
        Check-Command ":app:testDebugUnitTest" ".\gradlew.bat :app:testDebugUnitTest"
    }
}

Run-Step "Debug assemble" {
    if ($Quick) {
        Skip "assemble skipped by -Quick"
    } elseif ($SkipAssemble) {
        Skip "assemble skipped by -SkipAssemble"
    } else {
        Check-Command ":app:assembleDebug" ".\gradlew.bat :app:assembleDebug"
    }
}

Write-Header "Summary"
Write-Host "total   : $script:Total"
Write-Host "passed  : $script:Passed" -ForegroundColor Green
if ($script:Failed -eq 0) {
    Write-Host "failed  : $script:Failed" -ForegroundColor Green
} else {
    Write-Host "failed  : $script:Failed" -ForegroundColor Red
}
Write-Host "skipped : $script:Skipped" -ForegroundColor Yellow

if ($script:Failed -gt 0) {
    Write-Host ""
    Write-Host "Failures:" -ForegroundColor Red
    $script:Failures | ForEach-Object { Write-Host "- $_" -ForegroundColor Red }
    exit 1
}

Write-Host "CURRENT PREFLIGHT PASS" -ForegroundColor Green
exit 0
