[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$RunId,
    [string]$RunRoot = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($RunId -notmatch '^\d{8}-\d{6}(?:-retry\d{2})?$') {
    throw "运行编号格式错误：$RunId"
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
if ([string]::IsNullOrWhiteSpace($RunRoot)) {
    $RunRoot = Join-Path $repoRoot "docs\tests\runs\$RunId"
}
$resolvedRunRoot = (Resolve-Path -LiteralPath $RunRoot).Path
$expectedParent = [IO.Path]::GetFullPath((Join-Path $repoRoot 'docs\tests\runs')).TrimEnd('\', '/')
$actualParent = [IO.Path]::GetDirectoryName($resolvedRunRoot).TrimEnd('\', '/')
if (-not $actualParent.Equals($expectedParent, [StringComparison]::OrdinalIgnoreCase)) {
    throw "运行目录必须位于 docs/tests/runs 下：$resolvedRunRoot"
}
if ((Split-Path -Leaf $resolvedRunRoot) -ne $RunId) {
    throw "RunId 与目录名不一致：$RunId / $resolvedRunRoot"
}

$outputPath = Join-Path $resolvedRunRoot 'SHA256SUMS.txt'
$files = @(Get-ChildItem -LiteralPath $resolvedRunRoot -Recurse -File |
    Where-Object { $_.FullName -ne $outputPath } |
    Sort-Object FullName)

$rows = foreach ($file in $files) {
    $stream = [IO.File]::OpenRead($file.FullName)
    try {
        $sha = [Security.Cryptography.SHA256]::Create()
        try {
            $hash = ([BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-', '').ToLowerInvariant()
        }
        finally {
            $sha.Dispose()
        }
    }
    finally {
        $stream.Dispose()
    }
    $relative = $file.FullName.Substring($resolvedRunRoot.Length + 1).Replace('\', '/')
    "$hash  $relative"
}

[IO.File]::WriteAllText($outputPath, (($rows -join "`n") + "`n"), [Text.UTF8Encoding]::new($false))
Write-Output $outputPath