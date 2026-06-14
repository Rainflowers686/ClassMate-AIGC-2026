param()

$ErrorActionPreference = "Continue"

function U($codes) {
    return -join ($codes | ForEach-Object { [char]$_ })
}

Write-Host "== Stage 7 Sensitive Text Audit =="
Write-Host "WARN-only. Does not run Gradle. Does not read config.local.json."
if (Test-Path "config.local.json") {
    Write-Host "config.local.json exists; content was not read."
}

$scanRoots = @("app\src\main\java", "core\src\main\kotlin", "docs") | Where-Object { Test-Path $_ }
$ignoreParts = @("scripts\secrets_scan")

$riskTerms = @(
    ("Author" + "ization"),
    ("Bear" + "er"),
    ("app" + "Key"),
    ("api" + "Key"),
    ("app" + "_id"),
    "App" + "KEY",
    ("pro" + "mpt"),
    ("mes" + "sages"),
    ("reasoning" + "_content"),
    ("vendor " + "body"),
    ("raw " + "response"),
    ((U @(0x771F,0x5B9E)) + " ASR " + (U @(0x5DF2,0x5B8C,0x6210))),
    ((U @(0x771F,0x5B9E)) + " OCR " + (U @(0x5DF2,0x5B8C,0x6210))),
    (U @(0x58F0,0x7EB9,0x8BC6,0x522B,0x5DF2,0x652F,0x6301)),
    (U @(0x81EA,0x52A8,0x8BF4,0x8BDD,0x4EBA,0x5206,0x6BB5,0x5DF2,0x652F,0x6301)),
    (U @(0x5DF2,0x5B9E,0x73B0,0x5E95,0x566A,0x5904,0x7406)),
    (U @(0x722C,0x53D6,0x0020,0x0042,0x7AD9)),
    (U @(0x722C,0x53D6,0x6296,0x97F3)),
    (U @(0x722C,0x53D6,0x5C0F,0x7EA2,0x4E66))
)

$files = foreach ($root in $scanRoots) {
    Get-ChildItem -LiteralPath $root -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object {
            $path = $_.FullName
            -not ($ignoreParts | Where-Object { $path.Contains($_) })
        }
}

$hitCount = 0
foreach ($term in $riskTerms) {
    $matches = $files | Select-String -Pattern $term -SimpleMatch -ErrorAction SilentlyContinue
    if ($matches) {
        Write-Host "`n[WARN] Term hit: $term" -ForegroundColor Yellow
        $matches | Select-Object -First 30 | ForEach-Object {
            $hitCount++
            $line = $_.Line.Trim()
            if ($line.Length -gt 180) { $line = $line.Substring(0, 180) + "..." }
            Write-Host "$($_.Path):$($_.LineNumber): $line"
        }
    }
}

Write-Host "`n== Summary =="
if ($hitCount -eq 0) {
    Write-Host "No risk text hits. This is not a substitute for secrets scan." -ForegroundColor Green
} else {
    Write-Host "$hitCount warning hit(s). Review context; remove secrets, internal model text, or over-claiming copy before proof capture." -ForegroundColor Yellow
}

