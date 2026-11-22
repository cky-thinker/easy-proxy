Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Security.Cryptography.ProtectedData

function Get-EasyProxySecretStorePath {
    param([Parameter(Mandatory)][string]$RunId)

    if ($RunId -notmatch '^\d{8}-\d{6}(?:-retry\d{2})?$') {
        throw "运行编号格式错误：$RunId"
    }
    if ([string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
        throw 'LOCALAPPDATA 未设置，无法创建仓库外秘密存储。'
    }
    return Join-Path $env:LOCALAPPDATA "EasyProxy\$RunId\secrets.dpapi.json"
}

function New-EasyProxyRandomText {
    param([int]$ByteCount = 32)

    $bytes = New-Object byte[] $ByteCount
    [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    return ([Convert]::ToBase64String($bytes)).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

function New-EasyProxySecretMaterial {
    param(
        [Parameter(Mandatory)][string]$RunId,
        [switch]$Force
    )

    $path = Get-EasyProxySecretStorePath -RunId $RunId
    if (Test-Path -LiteralPath $path -PathType Leaf) {
        if (-not $Force) {
            return Read-EasyProxySecretMaterial -RunId $RunId
        }
        throw "秘密存储已存在，拒绝覆盖：$path"
    }

    $tokenBytes = New-Object byte[] 16
    [Security.Cryptography.RandomNumberGenerator]::Fill($tokenBytes)
    $material = [ordered]@{
        RunId          = $RunId
        CreatedAt      = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss zzz')
        AdminUsername  = 'admin'
        AdminPassword  = (New-EasyProxyRandomText -ByteCount 32)
        DbUsername     = 'easyproxy_' + (New-EasyProxyRandomText -ByteCount 8).ToLowerInvariant().Replace('-', 'x').Replace('_', 'y')
        DbPassword     = (New-EasyProxyRandomText -ByteCount 32)
        JwtSecret      = (New-EasyProxyRandomText -ByteCount 48)
        CertPassword   = (New-EasyProxyRandomText -ByteCount 32)
        ClientToken    = ([BitConverter]::ToString($tokenBytes)).Replace('-', '').ToLowerInvariant()
    }

    $json = $material | ConvertTo-Json -Compress
    $plain = [Text.Encoding]::UTF8.GetBytes($json)
    $entropy = [Text.Encoding]::UTF8.GetBytes("EasyProxy-$RunId")
    $protected = [Security.Cryptography.ProtectedData]::Protect(
        $plain,
        $entropy,
        [Security.Cryptography.DataProtectionScope]::CurrentUser
    )
    $parent = Split-Path -Path $path -Parent
    New-Item -ItemType Directory -Path $parent -Force | Out-Null
    [IO.File]::WriteAllBytes($path, $protected)
    return [pscustomobject]$material
}

function Read-EasyProxySecretMaterial {
    param([Parameter(Mandatory)][string]$RunId)

    $path = Get-EasyProxySecretStorePath -RunId $RunId
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "缺少仓库外秘密存储：$path"
    }
    $protected = [IO.File]::ReadAllBytes($path)
    $entropy = [Text.Encoding]::UTF8.GetBytes("EasyProxy-$RunId")
    $plain = [Security.Cryptography.ProtectedData]::Unprotect(
        $protected,
        $entropy,
        [Security.Cryptography.DataProtectionScope]::CurrentUser
    )
    try {
        return [Text.Encoding]::UTF8.GetString($plain) | ConvertFrom-Json
    }
    finally {
        [Array]::Clear($plain, 0, $plain.Length)
    }
}

function Get-EasyProxySecretMaterial {
    param(
        [Parameter(Mandatory)][string]$RunId,
        [switch]$ForceNew
    )

    $path = Get-EasyProxySecretStorePath -RunId $RunId
    if (Test-Path -LiteralPath $path -PathType Leaf) {
        if ($ForceNew) { throw "秘密存储已存在，拒绝覆盖：$path" }
        return Read-EasyProxySecretMaterial -RunId $RunId
    }
    return New-EasyProxySecretMaterial -RunId $RunId
}

Export-ModuleMember -Function Get-EasyProxySecretStorePath, Get-EasyProxySecretMaterial, New-EasyProxySecretMaterial, Read-EasyProxySecretMaterial