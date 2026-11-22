[CmdletBinding()]
param(
    [string]$Registry = 'registry.cn-hangzhou.aliyuncs.com',
    [string]$Namespace = 'ckyspace',
    [Parameter(Mandatory = $true)]
    [string]$Tag,
    [string]$WslDistro = 'Ubuntu-22.04',
    [string]$RunId = (Get-Date -Format 'yyyyMMdd-HHmmss'),
    [string]$ExpectedServerDigest = '',
    [string]$ExpectedClientDigest = '',
    [string]$ExpectedWebDigest = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($Tag -eq 'latest') {
    throw '禁止以 latest 作为验收镜像标签。'
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
$registryRoot = Join-Path $repoRoot "docs\tests\runs\$RunId\raw\registry"
$summaryPath = Join-Path $repoRoot "docs\tests\runs\$RunId\processed\validation\acr-image-summary.md"
New-Item -ItemType Directory -Path $registryRoot -Force | Out-Null
New-Item -ItemType Directory -Path (Split-Path $summaryPath -Parent) -Force | Out-Null

$expected = @{
    server = $ExpectedServerDigest
    client = $ExpectedClientDigest
    web = $ExpectedWebDigest
}
$rows = New-Object System.Collections.Generic.List[string]
$failed = $false

foreach ($service in @('server', 'client', 'web')) {
    $reference = "$Registry/$Namespace/easy-proxy-$service`:$Tag"
    $textOutput = "/mnt/f/workspace/my/easy-proxy/docs/tests/runs/$RunId/raw/registry/imagetools-$service.txt"
    $jsonOutput = "/mnt/f/workspace/my/easy-proxy/docs/tests/runs/$RunId/raw/registry/imagetools-$service.json"
    $localText = Join-Path $registryRoot "imagetools-$service.txt"
    $localJson = Join-Path $registryRoot "imagetools-$service.json"

    & wsl.exe -d $WslDistro -- bash /mnt/f/workspace/my/easy-proxy/docs/tests/scripts/build/inspect-acr-image.sh $reference $textOutput $jsonOutput
    if ($LASTEXITCODE -ne 0) {
        throw "无法查询 ACR 镜像清单：$reference"
    }

    $inspection = Get-Content -LiteralPath $localJson -Raw | ConvertFrom-Json
    $digest = [string]$inspection.manifest.digest
    $architecture = [string]$inspection.image.architecture
    $operatingSystem = [string]$inspection.image.os

    if ($digest -notmatch '^sha256:[0-9a-f]{64}$') {
        throw "镜像摘要格式无效：$reference -> $digest"
    }
    if ($architecture -ne 'amd64' -or $operatingSystem -ne 'linux') {
        throw "镜像平台不符合 linux/amd64：$reference -> $operatingSystem/$architecture"
    }
    if ($expected[$service] -ne '' -and $digest -ne $expected[$service]) {
        throw "RepoDigest 不匹配：$reference 期望 $($expected[$service])，实际 $digest"
    }

    $rows.Add("| $service | ``$reference`` | ``$digest`` | ``$operatingSystem/$architecture`` | 通过 |")
    Write-Host "已验证 $service $digest"
}

$markdown = ((@(
    '# ACR 镜像清单验证'
    ''
    "运行编号：``$RunId``"
    ''
    '| 服务 | 镜像引用 | RepoDigest | 平台 | 判定 |'
    '| --- | --- | --- | --- | --- |'
) + @($rows) + @(
    ''
    '验证命令：`docs/tests/scripts/build/verify-acr-images.ps1`'
    ''
    '判定：三套镜像均存在，标签固定，平台为 `linux/amd64`，且可读取 RepoDigest。'
)) -join "`n")

$utf8 = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllText($summaryPath, $markdown + "`n", $utf8)
Write-Host "验证摘要：$summaryPath"