# ClassMate secrets scan (Windows local). Mirrors secrets_scan.sh.
# Run from anywhere: powershell -ExecutionPolicy Bypass -File scripts\secrets_scan\secrets_scan.ps1
$status = 0
Write-Output "== ClassMate secrets scan =="

$tracked = git ls-files
$forbidden = @("config.local.json", "secrets.properties", ".env", "keystore.properties", "signing.properties")
foreach ($f in $tracked) {
    if (($forbidden -contains $f) -or ($f -match '\.(jks|keystore)$') -or ($f -match '^\.env')) {
        Write-Output "ERROR forbidden tracked file: $f"
        $status = 1
    }
}

$placeholder = 'YOUR_|REPLACE_ME|CHANGEME|PLACEHOLDER|EXAMPLE|XXXX'
$fields = 'appId|appKey|apiKey|app_id|app_key|api_key|secret|token'
$pattern = '"(' + $fields + ')"\s*:\s*"([^"]+)"'

$files = git ls-files -- '*.json' '*.kt' '*.kts' '*.xml' '*.properties' '*.md' '*.yml' '*.yaml'
foreach ($file in $files) {
    if (-not (Test-Path $file)) { continue }
    # Test fixtures intentionally hold synthetic, real-looking credentials; skip test sources.
    if ($file -match '(^|/)src/test/') { continue }
    $found = Select-String -Path $file -Pattern $pattern -AllMatches -ErrorAction SilentlyContinue
    foreach ($line in $found) {
        foreach ($m in $line.Matches) {
            $val = $m.Groups[2].Value
            if (($val -notmatch $placeholder) -and ($val.Length -ge 10)) {
                Write-Output "ERROR possible secret in $file : $($m.Value)"
                $status = 1
            }
        }
    }
}

if ($status -eq 0) { Write-Output "OK: no secrets or forbidden files detected." }
exit $status
