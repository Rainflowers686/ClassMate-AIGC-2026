param()

$ErrorActionPreference = "Continue"

Write-Host "== Stage 6 Text Audit =="
Write-Host "This script does not run Gradle and does not read config.local.json."
if (Test-Path "config.local.json") {
    Write-Host "config.local.json exists; content was not read."
}

$scanTargets = @("app/src", "core/src") | Where-Object { Test-Path $_ }
$exportTargets = @(
    "core/src/main/kotlin/com/classmate/core/exporting",
    "app/src/main/java/com/classmate/app/exporting"
) | Where-Object { Test-Path $_ }

function U($codes) {
    return -join ($codes | ForEach-Object { [char]$_ })
}

$riskTerms = @(
    (U @(0x9700, 0x8981, 0x4F8B, 0x9898)),
    "TODO",
    "raw",
    "debug",
    ("pro" + "mpt"),
    ("mes" + "sages"),
    ("reasoning" + "_content")
)

$exportRiskTerms = @(
    "UI dump",
    "dump",
    "raw",
    "debug",
    "JsonObject",
    "JsonArray"
)

$overClaimTerms = @(
    ((U @(0x771F, 0x5B9E)) + " ASR " + (U @(0x5DF2, 0x652F, 0x6301))),
    ((U @(0x771F, 0x5B9E)) + " OCR " + (U @(0x5DF2, 0x652F, 0x6301))),
    (U @(0x58F0, 0x7EB9, 0x8BC6, 0x522B, 0x5DF2, 0x5B8C, 0x6210)),
    (U @(0x5E95, 0x566A, 0x5904, 0x7406, 0x5DF2, 0x5B8C, 0x6210)),
    (U @(0x81EA, 0x52A8, 0x722C, 0x53D6, 0x89C6, 0x9891)),
    (U @(0x5E73, 0x53F0, 0x89C6, 0x9891, 0x6293, 0x53D6))
)

function Print-Matches($title, $paths, $terms) {
    Write-Host "`n== $title =="
    if (!$paths -or $paths.Count -eq 0) {
        Write-Host "No target paths."
        return
    }
    foreach ($term in $terms) {
        $matches = Select-String -Path ($paths | ForEach-Object { Join-Path $_ "**\*" }) -Pattern $term -SimpleMatch -ErrorAction SilentlyContinue
        if ($matches) {
            Write-Host "-- $term --" -ForegroundColor Yellow
            $matches | Select-Object -First 20 | ForEach-Object {
                Write-Host "$($_.Path):$($_.LineNumber): $($_.Line.Trim())"
            }
        }
    }
}

Print-Matches "User-visible risk terms in app/core source" $scanTargets $riskTerms
Print-Matches "Export raw/dump risk terms" $exportTargets $exportRiskTerms
Print-Matches "Over-claiming ASR/OCR/speaker/audio claims" $scanTargets $overClaimTerms

Write-Host "`nAudit complete. Treat matches as review hints, not automatic failures."
