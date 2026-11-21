[CmdletBinding()]
param(
    [string]$Registry = 'registry.cn-hangzhou.aliyuncs.com',
    [string]$Namespace = 'ckyspace',
    [Parameter(Mandatory = $true)]
    [string]$Tag,
    [string]$WslDistro = 'Ubuntu-22.04',
    [Parameter(Mandatory = $true)]
    [string]$RunId,
    [string]$ExpectedServerImageId = 'sha256:ef7c15eb46646f298c725e472d1000f0a4abae40002b6f86ac336e85a5fdb189',
    [string]$ExpectedClientImageId = 'sha256:04afaabd71a1c7e3dd9eb8b38b07c20197d0d1fcfe8ae22b0dfb94ce569c4c43',
    [string]$ExpectedWebImageId = 'sha256:21afbd85c2dd069a8975f46ab5685e65a3677dbcb99c6cc24786b9358538053e'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($Tag -eq 'latest') {
    throw '禁止以 latest 作为验收镜像标签。'
}
if ($Tag -notmatch '^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$') {
    throw "镜像标签不合法：$Tag"
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
$runRoot = Join-Path $repoRoot "docs\tests\runs\$RunId"
$registryLogRoot = Join-Path $runRoot 'raw\registry'
$validationLogRoot = Join-Path $runRoot 'raw\validation'
$summaryPath = Join-Path $runRoot 'processed\validation\acr-push-summary.md'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$script:LastNativeExitCode = 0

function Write-LfText {
    param([string]$Path, [string]$Text)
    New-Item -ItemType Directory -Path (Split-Path $Path -Parent) -Force | Out-Null
    $normalized = $Text.Replace("`r`n", "`n").Replace("`r", "`n")
    [IO.File]::WriteAllText($Path, $normalized, $utf8)
}

function Invoke-CapturedProcess {
    param(
        [string]$FileName,
        [string]$Arguments,
        [string]$LogPath,
        [switch]$AllowFailure
    )

    New-Item -ItemType Directory -Path (Split-Path $LogPath -Parent) -Force | Out-Null
    $process = $null
    try {
        $startInfo = New-Object System.Diagnostics.ProcessStartInfo
        $startInfo.FileName = $FileName
        $startInfo.Arguments = $Arguments
        $startInfo.UseShellExecute = $false
        $startInfo.CreateNoWindow = $true
        $startInfo.RedirectStandardOutput = $true
        $startInfo.RedirectStandardError = $true

        $process = New-Object System.Diagnostics.Process
        $process.StartInfo = $startInfo
        [void]$process.Start()
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        $process.WaitForExit()
        $stdout = $stdoutTask.Result
        $stderr = $stderrTask.Result
        $script:LastNativeExitCode = $process.ExitCode

        $logText = @($stdout, $stderr) -join "`n"
        Write-LfText -Path $LogPath -Text ($logText.TrimEnd() + "`n")
        if (-not [string]::IsNullOrWhiteSpace($stdout)) {
            Write-Host $stdout.TrimEnd()
        }
        if (-not [string]::IsNullOrWhiteSpace($stderr)) {
            Write-Host $stderr.TrimEnd()
        }
        if (-not $AllowFailure -and $script:LastNativeExitCode -ne 0) {
            throw "命令失败，退出码 $($script:LastNativeExitCode)，日志：$LogPath"
        }

        [pscustomobject]@{
            ExitCode = $script:LastNativeExitCode
            StandardOutput = $stdout
            StandardError = $stderr
        }
    }
    finally {
        if ($null -ne $process) {
            $process.Dispose()
        }
    }
}

function ConvertTo-PlainTextOnce {
    param([Security.SecureString]$SecureString)

    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureString)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

function Invoke-WslDockerLogin {
    param([string]$UserName, [Security.SecureString]$SecurePassword)

    $password = ConvertTo-PlainTextOnce -SecureString $SecurePassword
    $process = $null
    try {
        $startInfo = New-Object System.Diagnostics.ProcessStartInfo
        $startInfo.FileName = 'wsl.exe'
        $startInfo.Arguments = "-d $WslDistro -- docker login --username `"$UserName`" --password-stdin `"$Registry`""
        $startInfo.UseShellExecute = $false
        $startInfo.CreateNoWindow = $true
        $startInfo.RedirectStandardInput = $true
        $startInfo.RedirectStandardOutput = $true
        $startInfo.RedirectStandardError = $true

        $process = New-Object System.Diagnostics.Process
        $process.StartInfo = $startInfo
        [void]$process.Start()
        $process.StandardInput.WriteLine($password)
        $process.StandardInput.Close()
        $stdout = $process.StandardOutput.ReadToEnd()
        $stderr = $process.StandardError.ReadToEnd()
        $process.WaitForExit()

        if ($process.ExitCode -ne 0) {
            throw "ACR 登录失败，退出码 $($process.ExitCode)。原始认证输出未写入日志。"
        }

        $loginText = @(
            'ACR_LOGIN=success'
            'credential_logged=no'
            'password_transport=stdin'
            "registry=$Registry"
        ) -join "`n"
        Write-LfText -Path (Join-Path $registryLogRoot 'acr-login.txt') -Text ($loginText + "`n")
    }
    finally {
        $password = $null
        if ($null -ne $process) {
            $process.Dispose()
        }
    }
}

if (-not (Test-Path -LiteralPath $runRoot -PathType Container)) {
    throw "运行目录不存在：$runRoot"
}
if (-not (Get-Command wsl.exe -ErrorAction SilentlyContinue)) {
    throw '未找到 wsl.exe。'
}

New-Item -ItemType Directory -Path $registryLogRoot -Force | Out-Null
New-Item -ItemType Directory -Path $validationLogRoot -Force | Out-Null

$services = @('server', 'client', 'web')
$expectedIds = @{
    server = $ExpectedServerImageId
    client = $ExpectedClientImageId
    web = $ExpectedWebImageId
}
$verifiedIds = @{}

Push-Location $repoRoot
try {
    $trackedChanges = (& git status --porcelain --untracked-files=no 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw '无法读取 Git 状态。'
    }
    if ($trackedChanges -ne '') {
        throw "检测到未提交的已跟踪修改，拒绝推送不可追溯镜像：`n$trackedChanges"
    }

    $commit = (& git rev-parse HEAD).Trim()
    $branch = (& git branch --show-current).Trim()
    $metadata = @(
        "run_id=$RunId"
        "branch=$branch"
        "commit=$commit"
        "registry=$Registry"
        "namespace=$Namespace"
        "tag=$Tag"
        'platform=linux/amd64'
        'action=push-existing-images-no-rebuild'
        'credential_logged=no'
    ) -join "`n"
    Write-LfText -Path (Join-Path $validationLogRoot 'push-metadata.txt') -Text ($metadata + "`n")

    foreach ($service in $services) {
        $reference = "$Registry/$Namespace/easy-proxy-$service`:$Tag"
        $logPath = Join-Path $validationLogRoot "preflight-image-$service.log"
        $result = Invoke-CapturedProcess -FileName 'wsl.exe' -LogPath $logPath -Arguments (
            "-d $WslDistro -- docker image inspect `"$reference`" --format `"{{.Id}}|{{.Os}}|{{.Architecture}}`""
        )
        $fact = $result.StandardOutput.Trim()
        $parts = $fact -split '\|'
        if ($parts.Count -ne 3) {
            throw "无法解析本地镜像事实：$reference -> $fact"
        }
        if ($parts[0] -ne $expectedIds[$service] -or $parts[1] -ne 'linux' -or $parts[2] -ne 'amd64') {
            throw "本地镜像与归档基线不一致：$reference -> $fact"
        }
        $verifiedIds[$service] = $parts[0]
    }

    $userName = Read-Host '请输入 ACR 用户名'
    if ([string]::IsNullOrWhiteSpace($userName)) {
        throw 'ACR 用户名不能为空。'
    }
    $securePassword = Read-Host '请输入 ACR 密码（输入不回显）' -AsSecureString
    if ($securePassword.Length -eq 0) {
        throw 'ACR 密码不能为空。'
    }

    $loginAttempted = $false
    $operationError = $null
    try {
        $loginAttempted = $true
        Invoke-WslDockerLogin -UserName $userName -SecurePassword $securePassword
        Write-Host 'ACR 登录成功，开始推送已归档的现有镜像。'

        foreach ($service in $services) {
            $reference = "$Registry/$Namespace/easy-proxy-$service`:$Tag"
            $pushLog = Join-Path $registryLogRoot "push-existing-$service.log"
            Invoke-CapturedProcess -FileName 'wsl.exe' -LogPath $pushLog -Arguments (
                "-d $WslDistro -- docker push `"$reference`""
            ) | Out-Null
            Write-Host "已推送 $service：$reference"
        }

        $verifyScript = Join-Path $PSScriptRoot 'verify-acr-images.ps1'
        $verifyOutput = (& $verifyScript -Registry $Registry -Namespace $Namespace -Tag $Tag -WslDistro $WslDistro -RunId $RunId 2>&1 | Out-String).Trim()
        $verifyExitCode = $LASTEXITCODE
        Write-LfText -Path (Join-Path $registryLogRoot 'verify-acr-console.log') -Text ($verifyOutput + "`n")
        Write-Host $verifyOutput
        if ($verifyExitCode -ne 0) {
            throw 'ACR 镜像清单验证失败。'
        }
    }
    catch {
        $operationError = $_
    }
    finally {
        $securePassword = $null
        $userName = $null
        if ($loginAttempted) {
            $logoutLog = Join-Path $registryLogRoot 'acr-logout.txt'
            Invoke-CapturedProcess -FileName 'wsl.exe' -LogPath $logoutLog -AllowFailure -Arguments (
                "-d $WslDistro -- docker logout `"$Registry`""
            ) | Out-Null

            $logoutVerifyLog = Join-Path $registryLogRoot 'acr-logout-verify.log'
            $wslLogoutScript = '/mnt/f/workspace/my/easy-proxy/docs/tests/scripts/common/verify-acr-logged-out.sh'
            $wslLogoutEvidence = "/mnt/f/workspace/my/easy-proxy/docs/tests/runs/$RunId/raw/validation/acr-logout.txt"
            $logoutVerify = Invoke-CapturedProcess -FileName 'wsl.exe' -LogPath $logoutVerifyLog -AllowFailure -Arguments (
                "-d $WslDistro -- bash `"$wslLogoutScript`" `"$wslLogoutEvidence`""
            )
            if ($logoutVerify.ExitCode -ne 0 -and $null -eq $operationError) {
                $operationError = 'ACR 注销复核失败：未确认 ACR_AUTH_ENTRY=no。'
            }
        }
    }

    if ($null -ne $operationError) {
        throw $operationError
    }

    $rows = foreach ($service in $services) {
        $reference = "$Registry/$Namespace/easy-proxy-$service`:$Tag"
        "| $service | ``$reference`` | ``$($verifiedIds[$service])`` | 已推送并通过远端清单验证 |"
    }
    $summary = @(
        '# ACR 现有镜像推送摘要'
        ''
        "运行编号：``$RunId``"
        "Git 提交：``$commit``"
        "固定标签：``$Tag``"
        ''
        '| 服务 | 镜像引用 | 本地镜像 ID | 结果 |'
        '| --- | --- | --- | --- |'
    ) + @($rows) + @(
        ''
        '操作方式：仅推送 WSL 中已归档的现有镜像，不重新构建。'
        ''
        '凭据处理：密码仅经 stdin 传递，未写入命令日志或文件。'
        ''
        '远端 RepoDigest 与平台见 `processed/validation/acr-image-summary.md`。'
        ''
        '凭据清理：`raw/validation/acr-logout.txt` 必须包含 `ACR_AUTH_ENTRY=no`。'
    )
    Write-LfText -Path $summaryPath -Text (($summary -join "`n") + "`n")
    Write-Host "推送摘要：$summaryPath"
}
finally {
    Pop-Location
}