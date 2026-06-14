param()

$ErrorActionPreference = "Continue"

Write-Host "== Stage 8A-4 Post-Bridge Static Check =="
Write-Host "Read-only, WARN-only. No Gradle. No file changes. Does not read config.local.json."

function Ok($Message) { Write-Host "[OK] $Message" -ForegroundColor Green }
function Warn($Message) { Write-Host "[WARN] $Message" -ForegroundColor Yellow }
function Fail($Message) { Write-Host "[FAIL] $Message" -ForegroundColor Red }

# Allow scanning app/src and core/src (read-only, no edits)
# Also scan docs for documentation references
$sourceRoots = @("app/src", "core/src")
$docRoots = @("docs")
$allRoots = $sourceRoots + $docRoots

# ---- Helper: get scannable files ----
function Get-ScanFiles($Roots, $Extensions) {
    $items = New-Object System.Collections.Generic.List[string]
    foreach ($root in $Roots) {
        if (-not (Test-Path -LiteralPath $root)) { continue }
        Get-ChildItem -LiteralPath $root -Recurse -File -ErrorAction SilentlyContinue |
            Where-Object { $_.Extension -in $Extensions -or $Extensions.Count -eq 0 } |
            ForEach-Object { [void]$items.Add($_.FullName) }
    }
    return $items
}

# ---- config.local.json check ----
Write-Host "`n-- config.local.json --"
if (Test-Path -LiteralPath "config.local.json") {
    Warn "config.local.json exists locally. Content was NOT read (Test-Path only)."
} else {
    Ok "config.local.json not present."
}

# ---- 1. Direct import com.vivo.llmsdk ----
Write-Host "`n-- 1. Direct import com.vivo.llmsdk --"
$srcFiles = Get-ScanFiles $sourceRoots @(".kt", ".java")
$importHits = @()
foreach ($file in $srcFiles) {
    $matches = Select-String -LiteralPath $file -Pattern "import com\.vivo\.llmsdk" -ErrorAction SilentlyContinue
    foreach ($m in $matches) {
        $importHits += "$($m.Path):$($m.LineNumber): $($m.Line.Trim())"
    }
}
if ($importHits.Count -eq 0) {
    Ok "No direct import of com.vivo.llmsdk. (Reflection bridge is correct.)"
} else {
    foreach ($h in $importHits) { Fail "Direct SDK import: $h" }
    Fail "$($importHits.Count) direct import(s) found. Must use reflection bridge, not direct imports."
}

# ---- 2. onComplete(LlmStats) old signature ----
Write-Host "`n-- 2. onComplete(LlmStats) old signature check --"
$oldSigPatterns = @("onComplete\s*\(\s*LlmStats", "onComplete\s*\(\s*\w+\s+\w+", "onComplete\s*\(\s*[^)]+\)")
$docAndSrcFiles = Get-ScanFiles ($sourceRoots + $docRoots) @(".kt", ".java", ".md")
$oldSigHits = 0
foreach ($file in $docAndSrcFiles) {
    foreach ($pat in $oldSigPatterns) {
        $matches = Select-String -LiteralPath $file -Pattern $pat -ErrorAction SilentlyContinue
        foreach ($m in $matches) {
            $oldSigHits++
            if ($oldSigHits -le 20) {
                Warn "$($m.Path):$($m.LineNumber): onComplete() may have unexpected params: $($m.Line.Trim())"
            }
        }
    }
}
if ($oldSigHits -eq 0) {
    Ok "No onComplete(LlmStats) old signature detected."
} else {
    Warn "$oldSigHits hit(s). If any are in production code (not docs), this is a BLOCKER."
}

# ---- 3. callVit / multimodal presence check ----
Write-Host "`n-- 3. callVit / multimodal reference scan --"
$vitTerms = @("callVit", "multimodal", "VitEncoder", "vision.encode")
$vitHits = 0
foreach ($term in $vitTerms) {
    foreach ($file in $docAndSrcFiles) {
        $matches = Select-String -LiteralPath $file -Pattern $term -SimpleMatch -ErrorAction SilentlyContinue
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
    Warn "No callVit / multimodal references found. Expected in bridge files and docs."
} else {
    Ok "$vitHits callVit / multimodal reference(s) found. Verify callVit only used in diagnostic paths."
}

# ---- 4. AAR gitignored ----
Write-Host "`n-- 4. AAR gitignore verification --"
$aarDir = "app\libs"
if (Test-Path -LiteralPath $aarDir) {
    $aarFiles = Get-ChildItem -LiteralPath $aarDir -Filter *.aar -ErrorAction SilentlyContinue
    foreach ($aar in $aarFiles) {
        $relPath = $aar.FullName.Substring((Get-Location).Path.Length + 1)
        $ignored = git check-ignore -q $relPath 2>$null
        if ($LASTEXITCODE -eq 0) {
            Ok "$relPath is git-ignored (safe)."
        } else {
            Fail "$relPath is NOT git-ignored! Check .gitignore for app/libs/*.aar rule."
        }
    }
    if ($aarFiles.Count -eq 0) {
        Warn "No .aar files found in app/libs. (Expected: llm-sdk-release.aar should be present locally.)"
    }
} else {
    Warn "app/libs directory not found."
}

# ---- 5. Forbidden tracked files ----
Write-Host "`n-- 5. Forbidden tracked files --"
$forbidden = @(
    "config.local.json", "local.properties", "secrets.properties",
    ".env", ".env.*", "*.jks", "*.keystore", "*.apk", "*.aab",
    "app/build", "core/build", "build", ".gradle"
)
$tracked = @(git ls-files $forbidden 2>$null)
if ($tracked.Count -eq 0) {
    Ok "No forbidden tracked files."
} else {
    foreach ($item in $tracked) { Fail "Tracked forbidden path: $item" }
}

# ---- 6. qwen guard ----
Write-Host "`n-- 6. qwen enable_thinking guard --"
$qwenTerms = @("qwen3.5-plus", "enable_thinking")
$qwenHits = 0
foreach ($term in $qwenTerms) {
    foreach ($file in $srcFiles) {
        $matches = Select-String -LiteralPath $file -Pattern $term -SimpleMatch -ErrorAction SilentlyContinue
        foreach ($m in $matches) {
            $qwenHits++
            if ($qwenHits -le 20) {
                Warn "$($m.Path):$($m.LineNumber): qwen guard term: $($m.Line.Trim())"
            }
        }
    }
}
if ($qwenHits -eq 0) {
    Ok "No qwen3.5-plus / enable_thinking in source files."
} else {
    Warn "$qwenHits hit(s). Verify enable_thinking=false is preserved where intended."
}

# ---- 7. ProviderResolver file changes ----
Write-Host "`n-- 7. ProviderResolver integrity --"
$resolverFiles = @(Get-ChildItem -Path core/src -Recurse -Include *.kt -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match "Provider|Resolver|Chain" } |
    Where-Object { $_.Name -notmatch "Test" })
if ($resolverFiles.Count -gt 0) {
    Write-Host "  [INFO] Provider-related files found:" -ForegroundColor Gray
    $resolverFiles | ForEach-Object { Write-Host "    $($_.FullName)" -ForegroundColor Gray }
    Write-Host "  [INFO] Verify via git diff that bridge does not alter ProviderResolver chain order."
} else {
    Warn "No Provider/Resolver files found in core/src. (May need different naming convention.)"
}

# ---- 8. Validator file changes ----
Write-Host "`n-- 8. Validator integrity --"
$validatorFiles = @(Get-ChildItem -Path core/src -Recurse -Include *.kt -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match "Validat|Safety|Redact|Format" } |
    Where-Object { $_.Name -notmatch "Test" })
if ($validatorFiles.Count -gt 0) {
    Write-Host "  [INFO] Validator-related files found:" -ForegroundColor Gray
    $validatorFiles | ForEach-Object { Write-Host "    $($_.FullName)" -ForegroundColor Gray }
    Write-Host "  [INFO] Verify via git diff that bridge does not weaken validators."
} else {
    Warn "No validator/safety files found in core/src. (May need different naming convention.)"
}

# ---- 9. Manifest dangerous storage permission ----
Write-Host "`n-- 9. Manifest dangerous storage permissions --"
$manifest = "app/src/main/AndroidManifest.xml"
if (Test-Path -LiteralPath $manifest) {
    $dangerPerms = @("MANAGE_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE")
    foreach ($perm in $dangerPerms) {
        $hit = Select-String -LiteralPath $manifest -Pattern $perm -SimpleMatch -ErrorAction SilentlyContinue
        if ($hit) {
            Fail "Manifest contains dangerous permission: $perm at $($hit.Path):$($hit.LineNumber)"
        } else {
            Ok "Manifest does NOT contain $perm."
        }
    }
    # READ_EXTERNAL_STORAGE — warn if present
    $readHit = Select-String -LiteralPath $manifest -Pattern "READ_EXTERNAL_STORAGE" -SimpleMatch -ErrorAction SilentlyContinue
    if ($readHit) {
        Warn "Manifest contains READ_EXTERNAL_STORAGE at $($readHit.Path):$($readHit.LineNumber). Verify it's justified."
    } else {
        Ok "Manifest does NOT contain READ_EXTERNAL_STORAGE."
    }
} else {
    Warn "AndroidManifest.xml not found at $manifest."
}

# ---- 10. Settings no DeepSeek/Compatible main path ----
Write-Host "`n-- 10. Settings external model main path check --"
$extModelTerms = @("DeepSeek", "Compatible Demo", "外部模型增强", "external model")
$extHits = 0
foreach ($term in $extModelTerms) {
    foreach ($file in $srcFiles) {
        $matches = Select-String -LiteralPath $file -Pattern $term -SimpleMatch -ErrorAction SilentlyContinue
        foreach ($m in $matches) {
            $extHits++
            if ($extHits -le 20) {
                $line = $m.Line.Trim()
                if ($line.Length -gt 180) { $line = $line.Substring(0, 180) + "..." }
                Warn "$($m.Path):$($m.LineNumber): external model term '$term': $line"
            }
        }
    }
}
if ($extHits -eq 0) {
    Ok "No DeepSeek / Compatible / external model references in source files."
} else {
    Warn "$extHits hit(s). Confirm these are NOT in competition display paths (architecture reserves are OK)."
}

# ---- 11. Wrong package name com.blue.lm.sdk ----
Write-Host "`n-- 11. Wrong SDK package name com.blue.lm.sdk --"
$wrongPkgHits = 0
# Exclude audit and INDEX docs that mention wrong package as a counter-example
$excludePattern = "stage8_ondevice_docs_consistency_audit|INDEX\.md"
foreach ($file in $docAndSrcFiles) {
    if ($file -match $excludePattern) { continue }
    $matches = Select-String -LiteralPath $file -Pattern "com\.blue\.lm\.sdk" -ErrorAction SilentlyContinue
    foreach ($m in $matches) {
        $wrongPkgHits++
        if ($wrongPkgHits -le 20) {
            Fail "$($m.Path):$($m.LineNumber): Wrong package name 'com.blue.lm.sdk' — should be 'com.vivo.llmsdk'"
        }
    }
}
if ($wrongPkgHits -eq 0) {
    Ok "No wrong package name 'com.blue.lm.sdk' found. Correct: com.vivo.llmsdk."
} else {
    Fail "$wrongPkgHits hit(s) of wrong package name. Fix to 'com.vivo.llmsdk'."
}

# ---- 12. Wrong callVit return type void ----
Write-Host "`n-- 12. Wrong callVit return type (void instead of int) --"
$wrongVitHits = 0
# Exclude audit and INDEX docs that mention void callVit as a counter-example
foreach ($file in $docAndSrcFiles) {
    if ($file -match $excludePattern) { continue }
    $matches = Select-String -LiteralPath $file -Pattern "void\s+callVit" -ErrorAction SilentlyContinue
    foreach ($m in $matches) {
        $wrongVitHits++
        if ($wrongVitHits -le 20) {
            Fail "$($m.Path):$($m.LineNumber): Wrong callVit return type 'void' — correct signature is 'public int callVit(byte[], int, int)'"
        }
    }
}
if ($wrongVitHits -eq 0) {
    Ok "No wrong callVit return type. Correct: int callVit(byte[], int, int)."
} else {
    Fail "$wrongVitHits hit(s) of wrong callVit signature. Fix to 'int callVit(byte[], int, int)'."
}

# ---- 13. Over-claim phrase detection ----
Write-Host "-- 13. Over-claim phrase detection (ASCII patterns only) --"
Write-Host "  NOTE: Full CJK over-claim audit is in docs/testing/stage8_ondevice_docs_consistency_audit.md"
Write-Host "  This script checks ASCII-level patterns only (PowerShell CJK handling is unreliable)."

$overClaimPatterns = @(
    "on-device.*fully.*passed",
    "ondevice.*production.*ready",
    "multimodal.*fully.*integrated",
    "multimodal.*complete.*loop",
    "ondevice.*replace.*cloud",
    "commit.*AAR.*repo",
    "commit.*SDK.*binary"
)
$overClaimHits = 0
$docsOnly = Get-ScanFiles @("docs") @(".md")
foreach ($pat in $overClaimPatterns) {
    foreach ($file in $docsOnly) {
        $matches = Select-String -LiteralPath $file -Pattern $pat -ErrorAction SilentlyContinue
        foreach ($m in $matches) {
            $line = $m.Line.Trim()
            if ($line -match "not (claim|represent|yet|fully|complete|replace|commit)" -or
                $line -match "do(n't| not)" -or
                $line -match "disclaimer|warning|caution") {
                continue
            }
            $overClaimHits++
            if ($overClaimHits -le 20) {
                if ($line.Length -gt 180) { $line = $line.Substring(0, 180) + "..." }
                Fail "$($m.Path):$($m.LineNumber): Potential over-claim via English pattern: $line"
            }
        }
    }
}
if ($overClaimHits -eq 0) {
    Ok "No over-claim phrases detected via English patterns."
    Ok "Full CJK audit: see docs/testing/stage8_ondevice_docs_consistency_audit.md (PASSED)."
} else {
    Fail "$overClaimHits over-claim hit(s) found. Review and reword."
}

# ---- Summary ----
Write-Host "`n== Stage 8A-4 Post-Bridge Static Check Summary =="
Write-Host "Review all WARN/FAIL entries above."
Write-Host "Key BLOCKER checks:"
Write-Host "  - Direct import com.vivo.llmsdk (1)"
Write-Host "  - onComplete(LlmStats) old signature (2)"
Write-Host "  - AAR not git-ignored (4)"
Write-Host "  - Forbidden tracked files (5)"
Write-Host "  - Manifest dangerous permissions (9)"
Write-Host "  - Wrong package name com.blue.lm.sdk (11)"
Write-Host "  - Wrong callVit return type void (12)"
Write-Host "  - Over-claim phrases in docs (13)"
Write-Host ""
Write-Host "WARN-only static check complete. No Gradle ran. No files changed."
