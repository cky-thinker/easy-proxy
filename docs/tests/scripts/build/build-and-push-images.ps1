[CmdletBinding()]
param(
    [string]$Registry = 'registry.cn-hangzhou.aliyuncs.com',
    [string]$Namespace = 'ckyspace',
    [string]$Tag = '',
    [string]$WslDistro = 'Ubuntu-22.04',
    [string]$RunId = (Get-Date -Format 'yyyyMMdd-HHmmss'),
    [switch]$AllowFailedTests,
    [switch]$SkipPush
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
$runRoot = Join-Path $repoRoot "docs\tests\runs\$RunId"
$buildLogRoot = Join-Path $runRoot 'raw\build'
$registryLogRoot = Join-Path $runRoot 'raw\registry'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$script:LastNativeExitCode = 0

function Write-LfText {
    param([string]$Path, [string]$Text)
    New-Item -ItemType Directory -Path (Split-Path $Path -Parent) -Force | Out-Null
    $normalized = $Text.Replace("`r`n", "`n").Replace("`r", "`n")
    [IO.File]::WriteAllText($Path, $normalized, $utf8)
}

function Invoke-LoggedNative {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$LogPath,
        [switch]$AllowFailure
    )

    New-Item -ItemType Directory -Path (Split-Path $LogPath -Parent) -Force | Out-Null
    & $FilePath @Arguments 2>&1 | Tee-Object -FilePath $LogPath | Out-Host
    $script:LastNativeExitCode = $LASTEXITCODE
    if (-not $AllowFailure -and $script:LastNativeExitCode -ne 0) {
        throw "命令失败，退出码 $script:LastNativeExitCode，日志：$LogPath"
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

        $logText = @(
            'ACR_LOGIN=success'
            'credential_logged=no'
            'password_transport=stdin'
            "registry=$Registry"
        ) -join "`n"
        Write-LfText -Path (Join-Path $registryLogRoot 'acr-login.txt') -Text ($logText + "`n")
        Write-Host 'ACR 登录成功；未记录用户名或密码。'
    }
    finally {
        $password = $null
        if ($null -ne $process) {
            $process.Dispose()
        }
    }
}

New-Item -ItemType Directory -Path $buildLogRoot -Force | Out-Null
New-Item -ItemType Directory -Path $registryLogRoot -Force | Out-Null

Push-Location $repoRoot
try {
    if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
        throw '未找到 git。'
    }
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        throw '未找到 mvn。WSL 构建镜像时仍需在 Windows 管理机完成 Maven 打包。'
    }
    if (-not (Get-Command wsl.exe -ErrorAction SilentlyContinue)) {
        throw '未找到 wsl.exe。'
    }

    $trackedChanges = (& git status --porcelain --untracked-files=no 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw '无法读取 Git 状态。'
    }
    if ($trackedChanges -ne '') {
        throw "检测到未提交的已跟踪源码修改，拒绝构建不可追溯镜像：`n$trackedChanges"
    }

    $commit = (& git rev-parse HEAD).Trim()
    $branch = (& git branch --show-current).Trim()
    if ($Tag -eq '') {
        $prefix = if ($AllowFailedTests) { 'exploratory' } else { 'test' }
        $Tag = "$prefix-$(Get-Date -Format 'yyyyMMdd-HHmmss')-$($commit.Substring(0, 7))"
    }

    if ($Tag -notmatch '^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$') {
        throw "镜像标签不合法：$Tag"
    }
    if ($Tag -eq 'latest') {
        throw '禁止使用 latest 作为测试或验收镜像标签。'
    }
    if ($AllowFailedTests -and -not $Tag.StartsWith('exploratory-', [StringComparison]::Ordinal)) {
        throw '允许单元测试失败时，标签必须以 exploratory- 开头。'
    }

    $metadata = @(
        "run_id=$RunId"
        "branch=$branch"
        "commit=$commit"
        "registry=$Registry"
        "namespace=$Namespace"
        "tag=$Tag"
        'platform=linux/amd64'
        "allow_failed_tests=$($AllowFailedTests.IsPresent.ToString().ToLowerInvariant())"
        "skip_push=$($SkipPush.IsPresent.ToString().ToLowerInvariant())"
    ) -join "`n"
    Write-LfText -Path (Join-Path $buildLogRoot 'build-metadata.txt') -Text ($metadata + "`n")

    Invoke-LoggedNative -FilePath 'mvn' -Arguments @('-B', '-ntp', 'clean', 'test') -LogPath (Join-Path $buildLogRoot 'maven-clean-test.log') -AllowFailure
    $testExitCode = $script:LastNativeExitCode
    if ($testExitCode -ne 0) {
        if (-not $AllowFailedTests) {
            throw '单元测试失败，停止构建。仅在明确标记为探索性验证时可使用 -AllowFailedTests。'
        }
        Write-Warning '单元测试失败；按 -AllowFailedTests 继续。产物只能标记为探索性镜像。'
    }

    Invoke-LoggedNative -FilePath 'mvn' -Arguments @('-B', '-ntp', 'package', '-DskipTests') -LogPath (Join-Path $buildLogRoot 'maven-package-skip-tests.log')

    $buildHelper = '/mnt/f/workspace/my/easy-proxy/docs/tests/scripts/build/build-push-image.sh'
    $services = @('server', 'client', 'web')
    if ($SkipPush) {
        foreach ($service in $services) {
            $logPath = Join-Path $buildLogRoot "build-load-$service.log"
            Invoke-LoggedNative -FilePath 'wsl.exe' -Arguments @(
                '-d', $WslDistro, '--', 'bash', $buildHelper,
                $Registry, $Namespace, $Tag, $service, 'load'
            ) -LogPath $logPath
        }
    }
    else {
        $userName = Read-Host '请输入 ACR 用户名'
        if ([string]::IsNullOrWhiteSpace($userName)) {
            throw 'ACR 用户名不能为空。'
        }
        $securePassword = Read-Host '请输入 ACR 密码（输入不回显）' -AsSecureString
        if ($securePassword.Length -eq 0) {
            throw 'ACR 密码不能为空。'
        }

        $loginAttempted = $false
        try {
            $loginAttempted = $true
            Invoke-WslDockerLogin -UserName $userName -SecurePassword $securePassword

            foreach ($service in $services) {
                $logPath = Join-Path $buildLogRoot "build-push-$service.log"
                Invoke-LoggedNative -FilePath 'wsl.exe' -Arguments @(
                    '-d', $WslDistro, '--', 'bash', $buildHelper,
                    $Registry, $Namespace, $Tag, $service, 'push'
                ) -LogPath $logPath
            }

            $verifyScript = Join-Path $PSScriptRoot 'verify-acr-images.ps1'
            & $verifyScript -Registry $Registry -Namespace $Namespace -Tag $Tag -WslDistro $WslDistro -RunId $RunId
            if ($LASTEXITCODE -ne 0) {
                throw 'ACR 镜像清单验证失败。'
            }
        }
        finally {
            $securePassword = $null
            $userName = $null
            if ($loginAttempted) {
                $logoutLog = Join-Path $registryLogRoot 'acr-logout.txt'
                Invoke-LoggedNative -FilePath 'wsl.exe' -Arguments @(
                    '-d', $WslDistro, '--', 'docker', 'logout', $Registry
                ) -LogPath $logoutLog -AllowFailure
                $logoutScript = Join-Path $repoRoot 'docs/tests/scripts/common/verify-acr-logged-out.sh'
                $wslLogoutScript = '/mnt/f/workspace/my/easy-proxy/docs/tests/scripts/common/verify-acr-logged-out.sh'
                $wslLogoutEvidence = "/mnt/f/workspace/my/easy-proxy/docs/tests/runs/$RunId/raw/validation/acr-logout.txt"
                Invoke-LoggedNative -FilePath 'wsl.exe' -Arguments @(
                    '-d', $WslDistro, '--', 'bash', $wslLogoutScript, $wslLogoutEvidence
                ) -LogPath (Join-Path $registryLogRoot 'acr-logout-verify.log')
                Write-Host "凭据清理脚本：$logoutScript"
            }
        }
    }

    Write-Host "构建任务完成。运行目录：$runRoot"
    Write-Host "镜像标签：$Tag"
}
finally {
    Pop-Location
}