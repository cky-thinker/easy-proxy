[CmdletBinding()]
param(
    [string]$FixtureRoot = '',
    [string]$ManifestRoot = '',
    [switch]$Force
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
if ([string]::IsNullOrWhiteSpace($FixtureRoot)) {
    $FixtureRoot = Join-Path $repoRoot 'docs\tests\data\fixtures'
}
if ([string]::IsNullOrWhiteSpace($ManifestRoot)) {
    $ManifestRoot = Join-Path $repoRoot 'docs\tests\data\manifests'
}
$FixtureRoot = [IO.Path]::GetFullPath($FixtureRoot)
$ManifestRoot = [IO.Path]::GetFullPath($ManifestRoot)
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$utf8Bom = New-Object System.Text.UTF8Encoding($true)

function Write-LfText {
    param(
        [string]$Path,
        [string]$Text,
        [System.Text.Encoding]$Encoding = $utf8NoBom
    )

    New-Item -ItemType Directory -Path (Split-Path $Path -Parent) -Force | Out-Null
    $normalized = $Text.Replace("`r`n", "`n").Replace("`r", "`n")
    [IO.File]::WriteAllText($Path, $normalized, $Encoding)
}

function New-DeterministicBytes {
    param(
        [string]$Seed,
        [int]$Length
    )

    if ($Length -lt 0) {
        throw 'Length 不能为负数。'
    }
    if ($Length -eq 0) {
        return [byte[]]::new(0)
    }

    $sha256 = [Security.Cryptography.SHA256]::Create()
    try {
        $seedBytes = [Text.Encoding]::UTF8.GetBytes("easy-proxy-fixture-v1|$Seed")
        $digest = $sha256.ComputeHash($seedBytes)
    }
    finally {
        $sha256.Dispose()
    }

    $bytes = New-Object byte[] $Length
    $initial = [Math]::Min($digest.Length, $Length)
    [Array]::Copy($digest, 0, $bytes, 0, $initial)
    $filled = $initial
    while ($filled -lt $Length) {
        $copyLength = [Math]::Min($filled, $Length - $filled)
        [Array]::Copy($bytes, 0, $bytes, $filled, $copyLength)
        $filled += $copyLength
    }
    return $bytes
}

function Write-Fixture {
    param(
        [string]$RelativePath,
        [byte[]]$Bytes,
        [string]$Purpose
    )

    $fullPath = Join-Path $FixtureRoot ($RelativePath.Replace('/', [IO.Path]::DirectorySeparatorChar))
    New-Item -ItemType Directory -Path (Split-Path $fullPath -Parent) -Force | Out-Null

    $needsWrite = $true
    if (Test-Path -LiteralPath $fullPath -PathType Leaf) {
        $existing = [IO.File]::ReadAllBytes($fullPath)
        $same = $existing.Length -eq $Bytes.Length
        if ($same) {
            for ($index = 0; $index -lt $Bytes.Length; $index++) {
                if ($existing[$index] -ne $Bytes[$index]) {
                    $same = $false
                    break
                }
            }
        }
        if ($same) {
            $needsWrite = $false
        }
        elseif (-not $Force) {
            throw "固定测试数据内容与生成规则不一致：$RelativePath。确认需要重建后使用 -Force。"
        }
    }

    if ($needsWrite) {
        $temporaryPath = "$fullPath.tmp-$PID"
        [IO.File]::WriteAllBytes($temporaryPath, $Bytes)
        Move-Item -LiteralPath $temporaryPath -Destination $fullPath -Force
    }

    $hash = [Security.Cryptography.SHA256]::Create()
    try {
        $digest = $hash.ComputeHash($Bytes)
    }
    finally {
        $hash.Dispose()
    }

    [pscustomobject]@{
        RelativePath = $RelativePath
        SizeBytes = $Bytes.Length
        Sha256 = ([BitConverter]::ToString($digest)).Replace('-', '').ToLowerInvariant()
        Purpose = $Purpose
    }
}

New-Item -ItemType Directory -Path $FixtureRoot -Force | Out-Null
New-Item -ItemType Directory -Path $ManifestRoot -Force | Out-Null

$fixtures = New-Object System.Collections.Generic.List[object]
$fixtures.Add((Write-Fixture -RelativePath 'small-1k.bin' -Bytes (New-DeterministicBytes -Seed 'small-1k' -Length 1024) -Purpose 'Small response latency, status and throughput'))
for ($index = 1; $index -le 1000; $index++) {
    $name = '{0:D4}.bin' -f $index
    $relativePath = "distinct/$name"
    $fixtures.Add((Write-Fixture -RelativePath $relativePath -Bytes (New-DeterministicBytes -Seed "distinct/$name" -Length 1024) -Purpose "Unique response ownership check $name"))
}
$fixtures.Add((Write-Fixture -RelativePath 'large-10m.bin' -Bytes (New-DeterministicBytes -Seed 'large-10m' -Length (10 * 1024 * 1024)) -Purpose 'Large response integrity and throughput'))
$fixtures.Add((Write-Fixture -RelativePath 'slow-2s.bin' -Bytes (New-DeterministicBytes -Seed 'slow-2s' -Length (1024 * 1024)) -Purpose 'Slow response isolation; Nginx limit_rate produces about two seconds'))

$manifestRows = New-Object System.Collections.Generic.List[string]
$manifestRows.Add('relative_path,size_bytes,sha256,purpose')
foreach ($fixture in $fixtures) {
    $escapedPurpose = '"' + $fixture.Purpose.Replace('"', '""') + '"'
    $manifestRows.Add("$($fixture.RelativePath),$($fixture.SizeBytes),$($fixture.Sha256),$escapedPurpose")
}

$shaRows = New-Object System.Collections.Generic.List[string]
foreach ($fixture in $fixtures) {
    $shaRows.Add("$($fixture.Sha256)  data/fixtures/$($fixture.RelativePath)")
}

$summaryRows = New-Object System.Collections.Generic.List[string]
$summaryRows.Add('# Easy Proxy 固定测试数据')
$summaryRows.Add('')
$summaryRows.Add('这些文件由 `docs/tests/scripts/common/New-TestFixtures.ps1` 确定性生成，禁止在测试运行中修改。')
$summaryRows.Add('')
$summaryRows.Add('| 路径 | 大小（字节） | SHA-256 | 用途 |')
$summaryRows.Add('| --- | ---: | --- | --- |')
foreach ($fixture in $fixtures) {
    $summaryRows.Add("| ``$($fixture.RelativePath)`` | $($fixture.SizeBytes) | ``$($fixture.Sha256)`` | $($fixture.Purpose) |")
}
$summaryRows.Add('')
$summaryRows.Add('重新生成前先确认差异原因，并使用 `-Force`。生成后执行逐项哈希校验。')

Write-LfText -Path (Join-Path $ManifestRoot 'fixtures.csv') -Text (($manifestRows -join "`n") + "`n")
Write-LfText -Path (Join-Path $ManifestRoot 'fixtures.sha256') -Text (($shaRows -join "`n") + "`n")
Write-LfText -Path (Join-Path $ManifestRoot 'fixtures.md') -Text (($summaryRows -join "`n") + "`n")

$distinctHashes = @($fixtures | Where-Object { $_.RelativePath -like 'distinct/*' } | Select-Object -ExpandProperty Sha256)
if ($distinctHashes.Count -ne 1000) {
    throw "distinct fixture 数量错误：$($distinctHashes.Count)"
}
if (@($distinctHashes | Sort-Object -Unique).Count -ne 1000) {
    throw 'distinct fixture 哈希存在重复。'
}

Write-Output "FIXTURE_ROOT=$FixtureRoot"
Write-Output "FIXTURE_COUNT=$($fixtures.Count)"
Write-Output "DISTINCT_COUNT=$($distinctHashes.Count)"
Write-Output "MANIFEST=$ManifestRoot\fixtures.csv"
Write-Output "SHA256SUMS=$ManifestRoot\fixtures.sha256"