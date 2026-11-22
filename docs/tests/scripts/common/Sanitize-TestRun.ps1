[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$RunId
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($RunId -notmatch '^\d{8}-\d{6}(?:-retry\d{2})?$') {
    throw "运行编号格式错误：$RunId"
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
$runRoot = (Resolve-Path -LiteralPath (Join-Path $repoRoot "docs\tests\runs\$RunId")).Path
$expectedRoot = [IO.Path]::GetFullPath((Join-Path $repoRoot 'docs\tests\runs')).TrimEnd('\', '/')
$actualRoot = [IO.Path]::GetDirectoryName($runRoot).TrimEnd('\', '/')
if (-not $actualRoot.Equals($expectedRoot, [StringComparison]::OrdinalIgnoreCase) -or
    (Split-Path -Leaf $runRoot) -ne $RunId) {
    throw "运行目录必须位于 docs/tests/runs/$RunId：$runRoot"
}

Import-Module -Name (Join-Path $PSScriptRoot 'EasyProxy.Testing.psm1') -Force
$utf8 = [Text.UTF8Encoding]::new($false, $true)
$textExtensions = @(
    '.md', '.ps1', '.psm1', '.sh', '.yml', '.yaml', '.csv', '.json', '.txt',
    '.log', '.conf', '.properties', '.xml', '.example', '.toml', '.ini'
)

$changedFiles = [Collections.Generic.List[string]]::new()
$scannedFiles = 0
foreach ($file in @(Get-ChildItem -LiteralPath $runRoot -Recurse -File | Sort-Object FullName)) {
    if ($file.Name -eq 'SHA256SUMS.txt') { continue }
    if ($textExtensions -notcontains $file.Extension.ToLowerInvariant()) { continue }
    $scannedFiles++
    $bytes = [IO.File]::ReadAllBytes($file.FullName)
    $hasBom = $bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF
    $text = $utf8.GetString($bytes)
    if ($hasBom) { $text = $text.TrimStart([char]0xFEFF) }
    $safe = ConvertTo-SafeText -Text $text
    if ($safe -ne $text) {
        $encoding = if ($hasBom) { [Text.UTF8Encoding]::new($true) } else { [Text.UTF8Encoding]::new($false) }
        [IO.File]::WriteAllText($file.FullName, $safe, $encoding)
        $changedFiles.Add($file.FullName.Substring($runRoot.Length + 1).Replace('\', '/'))
    }
}

$residualPatterns = @(
    '-----BEGIN [A-Z ]*PRIVATE KEY-----',
    '(?im)\bParameters\s*:\s*(?!<REDACTED>)\S',
    '(?im)\b[A-Za-z0-9_-]*(?:token|password|secret)\b\s*[:=]\s*(?!<REDACTED>|["'']?<REDACTED>)[^\s,;}\]]{8,}',
    '(?i)Authorization\s*:\s*(?:Bearer|Basic)\s+(?!<REDACTED>)[^\s]+',
    '(?i)https?://[^/\s:@]+:(?!<REDACTED>)[^/\s@]+@',
    '(?i)\$2[abxy]\$[0-9]{2}\$[./A-Za-z0-9]{53}',
    '(?i)eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}'
)
$residualFiles = [Collections.Generic.List[string]]::new()
$residualCount = 0
foreach ($file in @(Get-ChildItem -LiteralPath $runRoot -Recurse -File | Sort-Object FullName)) {
    if ($file.Name -eq 'SHA256SUMS.txt') { continue }
    if ($textExtensions -notcontains $file.Extension.ToLowerInvariant()) { continue }
    $bytes = [IO.File]::ReadAllBytes($file.FullName)
    $text = $utf8.GetString($bytes).TrimStart([char]0xFEFF)
    foreach ($pattern in $residualPatterns) {
        $count = [regex]::Matches($text, $pattern).Count
        if ($count -gt 0) {
            $residualCount += $count
            $relative = $file.FullName.Substring($runRoot.Length + 1).Replace('\', '/')
            if (-not $residualFiles.Contains($relative)) { $residualFiles.Add($relative) }
        }
    }
}

if ($residualCount -gt 0) {
    throw "脱敏后仍有 $residualCount 项敏感特征，涉及：$($residualFiles -join ', ')（不输出明文）"
}

& (Join-Path $PSScriptRoot 'New-RunChecksums.ps1') -RunId $RunId | Out-Null
Write-Output "RUN_ID=$RunId"
Write-Output "SCANNED_FILES=$scannedFiles"
Write-Output "SANITIZED_FILES=$($changedFiles.Count)"
Write-Output "SANITIZED_PATHS=$($changedFiles -join ',')"
Write-Output 'RESIDUAL_SECRETS=0'
Write-Output 'ACR_AUTH_ENTRY=no'