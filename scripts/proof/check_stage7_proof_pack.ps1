param(
    [Parameter(Mandatory = $true)]
    [string]$Path
)

$ErrorActionPreference = "Continue"
$warnings = New-Object System.Collections.Generic.List[string]
$failures = New-Object System.Collections.Generic.List[string]

function Section($name) { Write-Host "`n== $name ==" }
function Pass($msg) { Write-Host "[PASS] $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow; $warnings.Add($msg) | Out-Null }
function Fail($msg) { Write-Host "[FAIL] $msg" -ForegroundColor Red; $failures.Add($msg) | Out-Null }
function Exists($p) { return Test-Path -LiteralPath $p }

Section "Stage 7 Proof Pack Checker"
Write-Host "Read-only. Does not run Gradle. Does not read config.local.json."

if (!(Exists $Path)) {
    Fail "Proof pack path not found: $Path"
} else {
    Pass "Proof pack path exists."
}

if ($failures.Count -eq 0) {
    Section "Required files"
    $required = @(
        "README.md",
        "00_status\git_status.txt",
        "00_status\local_artifacts.txt",
        "03_security\security_summary.txt",
        "04_competition\stage7_judge_qna_50.md",
        "04_competition\stage7_feature_matrix_for_reviewers.md",
        "01_demo\stage7_final_demo_script.md"
    )
    foreach ($file in $required) {
        $full = Join-Path $Path $file
        if (Exists $full) { Pass $file } else { Fail "Missing $file" }
    }

    Section "Forbidden copied artifacts"
    $files = Get-ChildItem -LiteralPath $Path -Recurse -Force -File -ErrorAction SilentlyContinue
    $badFiles = $files | Where-Object {
        $_.Name -in @("config.local.json", "local.properties", "secrets.properties") -or
        $_.Extension -in @(".apk", ".aab", ".jks", ".keystore") -or
        $_.FullName -match "(\\|/)(build|\.gradle)(\\|/)"
    }
    if ($badFiles) {
        $badFiles | ForEach-Object { Fail "Forbidden artifact copied: $($_.FullName)" }
    } else {
        Pass "No APK/build/local credential artifacts copied."
    }

    Section "Sensitive text warnings"
    $terms = @(
        ("Author" + "ization"),
        ("Bear" + "er"),
        ("app" + "Key"),
        ("api" + "Key"),
        ("App" + "KEY"),
        ("app" + "_id"),
        ("reasoning" + "_content"),
        ("pro" + "mpt"),
        ("mes" + "sages")
    )
    $hitCount = 0
    foreach ($term in $terms) {
        $matches = $files | Where-Object { $_.Extension -in @(".md", ".txt", ".ps1") } |
            Select-String -Pattern $term -SimpleMatch -ErrorAction SilentlyContinue
        if ($matches) {
            Warn "Term hit: $term"
            $matches | Select-Object -First 20 | ForEach-Object {
                $hitCount++
                $line = $_.Line.Trim()
                if ($line.Length -gt 160) { $line = $line.Substring(0, 160) + "..." }
                Write-Host "$($_.Path):$($_.LineNumber): $line"
            }
        }
    }
    if ($hitCount -eq 0) { Pass "No sensitive text hits." }
}

Section "Summary"
if ($failures.Count -gt 0) {
    Write-Host "FAIL: $($failures.Count) failure(s), $($warnings.Count) warning(s)." -ForegroundColor Red
    exit 1
}
if ($warnings.Count -gt 0) {
    Write-Host "WARN: 0 failure(s), $($warnings.Count) warning(s)." -ForegroundColor Yellow
    exit 0
}
Write-Host "PASS: proof pack looks ready." -ForegroundColor Green

