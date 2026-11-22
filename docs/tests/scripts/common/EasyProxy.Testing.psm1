Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
$script:TestsRoot = Join-Path $script:RepoRoot 'docs\tests'
$script:Utf8NoBom = [Text.UTF8Encoding]::new($false)
$script:Utf8Bom = [Text.UTF8Encoding]::new($true)

function Get-EasyProxyRepoRoot {
    return $script:RepoRoot
}

function Get-EasyProxyTestsRoot {
    return $script:TestsRoot
}

function ConvertTo-LfText {
    param([AllowEmptyString()][string]$Text)
    return $Text.Replace("`r`n", "`n").Replace("`r", "`n")
}

function Write-LfText {
    param(
        [Parameter(Mandatory)][string]$Path,
        [AllowEmptyString()][string]$Text,
        [ValidateSet('Utf8NoBom', 'Utf8Bom')][string]$Encoding = 'Utf8NoBom'
    )

    $parent = Split-Path -Path $Path -Parent
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
    $selectedEncoding = if ($Encoding -eq 'Utf8Bom') { $script:Utf8Bom } else { $script:Utf8NoBom }
    [IO.File]::WriteAllText($Path, (ConvertTo-LfText -Text $Text), $selectedEncoding)
}

function ConvertTo-SafeText {
    param([AllowNull()][AllowEmptyString()][string]$Text)

    if ($null -eq $Text) { return '' }
    $safe = $Text
    $safe = [regex]::Replace($safe, '(?im)^(\s*(?:token|password|encryptedPassword|jwtSecret|certPassword|SERVER_JWT_SECRET|SERVER_CERT_PASSWORD|ACR_PASSWORD)\s*[:=]\s*)(.+)$', '$1<REDACTED>')
    $safe = [regex]::Replace($safe, '(?im)(Authorization\s*:\s*Bearer\s+)[^\s]+', '$1<REDACTED>')
    $safe = [regex]::Replace($safe, '(?i)("(?:token|password|encryptedPassword|jwtSecret|certPassword)"\s*:\s*")[^"]*(")', '$1<REDACTED>$2')
    $safe = [regex]::Replace($safe, '(?i)(https?://[^/\s:@]+:)[^/\s@]+(@)', '$1<REDACTED>$2')
    $safe = [regex]::Replace($safe, '(?i)([?&](?:token|password|access_token)=)[^&\s]+', '$1<REDACTED>')
    return $safe
}

function Write-EasyProxyEvidence {
    param(
        [Parameter(Mandatory)][string]$Path,
        [AllowEmptyString()][string]$Text,
        [switch]$Append,
        [switch]$NoSanitize,
        [ValidateSet('Utf8NoBom', 'Utf8Bom')][string]$Encoding = 'Utf8NoBom'
    )

    $value = if ($NoSanitize) { $Text } else { ConvertTo-SafeText -Text $Text }
    $parent = Split-Path -Path $Path -Parent
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
    $selectedEncoding = if ($Encoding -eq 'Utf8Bom') { $script:Utf8Bom } else { $script:Utf8NoBom }
    $normalized = ConvertTo-LfText -Text $value
    if ($Append -and (Test-Path -LiteralPath $Path -PathType Leaf)) {
        [IO.File]::AppendAllText($Path, $normalized, $selectedEncoding)
    }
    else {
        [IO.File]::WriteAllText($Path, $normalized, $selectedEncoding)
    }
}

function Invoke-EasyProxyProcess {
    param(
        [Parameter(Mandatory)][string]$FilePath,
        [string[]]$ArgumentList = @(),
        [AllowNull()][string]$InputText = $null,
        [int]$TimeoutSeconds = 120
    )

    $resolvedFilePath = $FilePath
    if (-not [IO.Path]::IsPathRooted($FilePath) -and $FilePath -notmatch '[\\/]') {
        $command = Get-Command -Name $FilePath -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($null -eq $command) {
            throw "未找到可执行文件：$FilePath"
        }
        $resolvedFilePath = $command.Source
    }

    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $resolvedFilePath
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.RedirectStandardInput = $null -ne $InputText
    $startInfo.CreateNoWindow = $true
    $startInfo.StandardOutputEncoding = $script:Utf8NoBom
    $startInfo.StandardErrorEncoding = $script:Utf8NoBom
    foreach ($argument in $ArgumentList) {
        [void]$startInfo.ArgumentList.Add([string]$argument)
    }

    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    $startedAt = [DateTimeOffset]::UtcNow
    try {
        if (-not $process.Start()) {
            throw "无法启动进程：$FilePath"
        }
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        if ($startInfo.RedirectStandardInput) {
            $process.StandardInput.Write($InputText)
            $process.StandardInput.Close()
        }
        if (-not $process.WaitForExit([Math]::Max(1, $TimeoutSeconds) * 1000)) {
            try { $process.Kill($true) } catch { }
            throw "进程超时（${TimeoutSeconds}s）：$resolvedFilePath"
        }
        $process.WaitForExit()
        $stdout = $stdoutTask.GetAwaiter().GetResult()
        $stderr = $stderrTask.GetAwaiter().GetResult()
        return [pscustomobject]@{
            FilePath      = $resolvedFilePath
            Arguments     = @($ArgumentList)
            ExitCode      = $process.ExitCode
            StdOut        = $stdout
            StdErr        = $stderr
            Combined      = (($stdout, $stderr | Where-Object { -not [string]::IsNullOrEmpty($_) }) -join "`n")
            StartedAtUtc  = $startedAt
            FinishedAtUtc = [DateTimeOffset]::UtcNow
            TimedOut      = $false
        }
    }
    finally {
        $process.Dispose()
    }
}

function Invoke-LocalCommand {
    param(
        [Parameter(Mandatory)][string]$FilePath,
        [string[]]$ArgumentList = @(),
        [AllowNull()][string]$InputText = $null,
        [int]$TimeoutSeconds = 120
    )
    return Invoke-EasyProxyProcess -FilePath $FilePath -ArgumentList $ArgumentList -InputText $InputText -TimeoutSeconds $TimeoutSeconds
}

function Invoke-SshBash {
    param(
        [Parameter(Mandatory)][string]$HostName,
        [Parameter(Mandatory)][int]$Port,
        [Parameter(Mandatory)][string]$User,
        [Parameter(Mandatory)][string]$Script,
        [int]$TimeoutSeconds = 300,
        [string]$SshExecutable = 'ssh'
    )

    $destination = "$User@$HostName"
    $arguments = @(
        '-o', 'BatchMode=yes',
        '-o', 'StrictHostKeyChecking=accept-new',
        '-o', 'ConnectTimeout=10',
        '-T',
        '-p', [string]$Port,
        $destination,
        'bash', '-s'
    )
    return Invoke-EasyProxyProcess -FilePath $SshExecutable -ArgumentList $arguments -InputText $Script -TimeoutSeconds $TimeoutSeconds
}

function Invoke-EasyProxyHttp {
    param(
        [Parameter(Mandatory)][string]$Uri,
        [ValidateSet('GET', 'POST', 'PUT', 'DELETE', 'HEAD')][string]$Method = 'GET',
        [AllowNull()][object]$Body = $null,
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 30
    )

    $requestHeaders = @{}
    foreach ($key in $Headers.Keys) { $requestHeaders[$key] = $Headers[$key] }
    $parameters = @{
        Uri                = $Uri
        Method             = $Method
        Headers            = $requestHeaders
        TimeoutSec         = $TimeoutSeconds
        NoProxy            = $true
        SkipHttpErrorCheck = $true
        ErrorAction        = 'Stop'
    }
    if ($null -ne $Body) {
        if ($Body -is [string]) {
            $parameters['Body'] = $Body
            $parameters['ContentType'] = 'application/json; charset=utf-8'
        }
        else {
            $parameters['Body'] = ($Body | ConvertTo-Json -Depth 20 -Compress)
            $parameters['ContentType'] = 'application/json; charset=utf-8'
        }
    }

    $startedAt = [DateTimeOffset]::UtcNow
    try {
        $response = Invoke-WebRequest @parameters
        $content = if ($response.Content -is [byte[]]) {
            [Text.Encoding]::UTF8.GetString($response.Content)
        }
        else {
            [string]$response.Content
        }
        $json = $null
        if (-not [string]::IsNullOrWhiteSpace($content)) {
            try { $json = $content | ConvertFrom-Json -Depth 30 } catch { }
        }
        return [pscustomobject]@{
            Uri           = $Uri
            Method        = $Method
            StatusCode    = [int]$response.StatusCode
            Headers       = $response.Headers
            Content       = $content
            Json          = $json
            Success       = $true
            Error         = $null
            ElapsedMs     = ([DateTimeOffset]::UtcNow - $startedAt).TotalMilliseconds
        }
    }
    catch {
        return [pscustomobject]@{
            Uri           = $Uri
            Method        = $Method
            StatusCode    = 0
            Headers       = @{}
            Content       = ''
            Json          = $null
            Success       = $false
            Error         = $_.Exception.Message
            ElapsedMs     = ([DateTimeOffset]::UtcNow - $startedAt).TotalMilliseconds
        }
    }
}

function Invoke-EasyProxyApi {
    param(
        [Parameter(Mandatory)][string]$BaseUri,
        [Parameter(Mandatory)][string]$Path,
        [ValidateSet('GET', 'POST', 'PUT', 'DELETE', 'HEAD')][string]$Method = 'GET',
        [AllowNull()][object]$Body = $null,
        [AllowNull()][string]$Token = $null,
        [int]$TimeoutSeconds = 30
    )

    $headers = @{}
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers['Authorization'] = "Bearer $Token"
    }
    $uri = $BaseUri.TrimEnd('/') + '/' + $Path.TrimStart('/')
    $response = Invoke-EasyProxyHttp -Uri $uri -Method $Method -Body $Body -Headers $headers -TimeoutSeconds $TimeoutSeconds
    return $response
}

function Assert-EasyProxy {
    param(
        [Parameter(Mandatory)][bool]$Condition,
        [Parameter(Mandatory)][string]$Name,
        [AllowEmptyString()][string]$Detail = ''
    )

    if (-not $Condition) {
        $suffix = if ([string]::IsNullOrWhiteSpace($Detail)) { '' } else { "：$Detail" }
        throw "$Name 失败$suffix"
    }
}

function Wait-EasyProxyCondition {
    param(
        [Parameter(Mandatory)][scriptblock]$Condition,
        [string]$Name = '条件',
        [int]$TimeoutSeconds = 60,
        [int]$IntervalMilliseconds = 1000
    )

    $startedAt = [DateTimeOffset]::UtcNow
    $lastError = $null
    while (([DateTimeOffset]::UtcNow - $startedAt).TotalSeconds -lt $TimeoutSeconds) {
        try {
            $value = & $Condition
            if ($value) {
                return [pscustomobject]@{
                    Satisfied = $true
                    Value     = $value
                    ElapsedMs = ([DateTimeOffset]::UtcNow - $startedAt).TotalMilliseconds
                    LastError = $null
                }
            }
        }
        catch {
            $lastError = $_.Exception.Message
        }
        Start-Sleep -Milliseconds $IntervalMilliseconds
    }
    return [pscustomobject]@{
        Satisfied = $false
        Value     = $null
        ElapsedMs = ([DateTimeOffset]::UtcNow - $startedAt).TotalMilliseconds
        LastError = $lastError
    }
}

function Get-EasyProxySha256 {
    param([Parameter(Mandatory)][string]$Path)
    $stream = [IO.File]::OpenRead($Path)
    try {
        $sha = [Security.Cryptography.SHA256]::Create()
        try {
            return ([BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-', '').ToLowerInvariant()
        }
        finally { $sha.Dispose() }
    }
    finally { $stream.Dispose() }
}

function Test-EasyProxyFixtures {
    param([string]$FixtureRoot = (Join-Path $script:TestsRoot 'data\fixtures'))

    $root = [IO.Path]::GetFullPath($FixtureRoot)
    $testsRootFull = [IO.Path]::GetFullPath($script:TestsRoot).TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    if (-not ($root + [IO.Path]::DirectorySeparatorChar).StartsWith($testsRootFull, [StringComparison]::OrdinalIgnoreCase)) {
        throw "FixtureRoot 必须位于 docs/tests 下：$root"
    }

    $manifestPath = Join-Path $script:TestsRoot 'data\manifests\fixtures.sha256'
    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
        throw "缺少固定数据清单：$manifestPath"
    }

    $entries = New-Object System.Collections.Generic.List[object]
    $failures = New-Object System.Collections.Generic.List[string]
    foreach ($line in [IO.File]::ReadAllLines($manifestPath)) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        if ($line -notmatch '^([0-9a-f]{64})  (.+)$') {
            $failures.Add("清单行格式错误")
            continue
        }
        $expectedHash = $Matches[1]
        $relative = $Matches[2].Replace('/', [IO.Path]::DirectorySeparatorChar)
        $fullPath = Join-Path $script:TestsRoot $relative
        if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
            $failures.Add("缺少文件：$relative")
            continue
        }
        $actualHash = Get-EasyProxySha256 -Path $fullPath
        if ($actualHash -ne $expectedHash) {
            $failures.Add("哈希不匹配：$relative")
        }
        $entries.Add([pscustomobject]@{
            RelativePath = $relative.Replace('\', '/')
            FullPath     = $fullPath
            Sha256       = $actualHash
            ExpectedHash = $expectedHash
            SizeBytes    = (Get-Item -LiteralPath $fullPath).Length
        })
    }

    $listedRelative = @($entries | ForEach-Object { $_.RelativePath.Replace('data/fixtures/', '') })
    $actualFiles = @(Get-ChildItem -LiteralPath $root -Recurse -File | ForEach-Object {
        $_.FullName.Substring($root.Length + 1).Replace('\', '/')
    })
    foreach ($actual in $actualFiles) {
        if ($listedRelative -notcontains $actual) {
            $failures.Add("未纳入清单：$actual")
        }
    }

    return [pscustomobject]@{
        Passed      = ($failures.Count -eq 0 -and $entries.Count -eq 1003)
        Count       = $entries.Count
        Failures    = $failures.ToArray()
        Entries     = $entries.ToArray()
        Manifest    = $manifestPath
    }
}

function New-EasyProxyRun {
    param(
        [string]$RunId = (Get-Date -Format 'yyyyMMdd-HHmmss'),
        [Parameter(Mandatory)][string]$TestScope,
        [string]$Status = '进行中'
    )

    if ($RunId -notmatch '^\d{8}-\d{6}(?:-retry\d{2})?$') {
        throw "运行编号格式错误：$RunId"
    }
    $runRoot = Join-Path $script:TestsRoot "runs\$RunId"
    if (Test-Path -LiteralPath $runRoot) {
        throw "运行目录已存在，禁止覆盖：$runRoot"
    }

    $relativeDirs = @(
        'raw\functional', 'raw\load', 'raw\health', 'raw\metrics', 'raw\logs',
        'raw\responses', 'raw\docker', 'raw\validation',
        'processed\summaries', 'processed\charts', 'processed\validation',
        'screenshots', 'tmp'
    )
    New-Item -ItemType Directory -Path $runRoot -Force | Out-Null
    foreach ($relativeDir in $relativeDirs) {
        New-Item -ItemType Directory -Path (Join-Path $runRoot $relativeDir) -Force | Out-Null
    }

    $branch = (git -C $script:RepoRoot branch --show-current).Trim()
    $commit = (git -C $script:RepoRoot rev-parse HEAD).Trim()
    $run = [pscustomobject]@{
        RunId     = $RunId
        RunRoot   = $runRoot
        TestScope = $TestScope
        StartedAt = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss zzz')
        Branch    = $branch
        Commit    = $commit
        Status    = $Status
    }
    Write-EasyProxyRunManifest -Run $run
    return $run
}

function Write-EasyProxyRunManifest {
    param(
        [Parameter(Mandatory)][object]$Run,
        [string]$EndedAt = '',
        [string]$Environment = '',
        [string]$TestParameters = '',
        [string]$Results = '',
        [string]$Conclusion = '',
        [string]$MirrorRefs = '',
        [string]$EvidenceIndex = '',
        [string]$Limitations = '',
        [string]$Cleanup = ''
    )

    if ([string]::IsNullOrWhiteSpace($Environment)) {
        $Environment = @(
            '| 主机 | 角色 | 规格 | 系统/运行时 | 公网出口或地址 |',
            '| --- | --- | --- | --- | --- |',
            '| 当前管理机 | 压测源与 SSH 客户端 | 待采集 | Windows / PowerShell | 待采集 |',
            '| WSL Ubuntu-22.04 | 镜像构建 | 待采集 | Docker/Buildx/Compose | 本地 |',
            '| 云服务器 | proxy-server | 待采集 | Linux / Docker | `68.64.181.242` |',
            '| 群晖 NAS | proxy-client | 待采集 | DSM / Docker | `192.168.1.9` |'
        ) -join "`n"
    }
    if ([string]::IsNullOrWhiteSpace($MirrorRefs)) {
        $MirrorRefs = @(
            '| 服务 | 完整镜像引用 | RepoDigest | 平台 | 验证状态 |',
            '| --- | --- | --- | --- | --- |',
            '| server | 未涉及 | - | `linux/amd64` | 未验证 |',
            '| client | 未涉及 | - | `linux/amd64` | 未验证 |',
            '| web | 未涉及 | - | `linux/amd64` | 未验证 |'
        ) -join "`n"
    }
    if ([string]::IsNullOrWhiteSpace($EvidenceIndex)) {
        $EvidenceIndex = @(
            '| 测试编号 | 证据 | 相对路径 | 完整性 |',
            '| --- | --- | --- | --- |',
            '| 待补充 | 待补充 | 待补充 | 待补充 |'
        ) -join "`n"
    }
    if ([string]::IsNullOrWhiteSpace($Cleanup)) {
        $Cleanup = @(
            '- [ ] 临时容器与防火墙规则已清理',
            '- [ ] 敏感字段已脱敏',
            '- [ ] `SHA256SUMS.txt` 已生成并复核',
            '- [ ] 报告已登记到 `reports/index.md`'
        ) -join "`n"
    }

    $manifest = @(
        '# 测试运行清单',
        '',
        '## 基本信息',
        '',
        '| 字段 | 值 |',
        '| --- | --- |',
        "| 运行编号 | ``$($Run.RunId)`` |",
        "| 测试范围 | $($Run.TestScope) |",
        '| 执行人 | Codex 自动化执行 |',
        "| 开始时间 | $($Run.StartedAt) |",
        "| 结束时间 | $EndedAt |",
        "| Git 分支/提交 | ``$($Run.Branch)`` / ``$($Run.Commit)`` |",
        "| 运行状态 | $($Run.Status) |",
        "| 结论 | $Conclusion |",
        '',
        '## 环境',
        '',
        $Environment,
        '',
        '## 镜像',
        '',
        $MirrorRefs,
        '',
        '## 测试参数',
        '',
        $TestParameters,
        '',
        '## 结果摘要',
        '',
        $Results,
        '',
        '## 证据索引',
        '',
        $EvidenceIndex,
        '',
        '## 偏差与限制',
        '',
        $Limitations,
        '',
        '## 清理与脱敏',
        '',
        $Cleanup
    ) -join "`n"
    Write-LfText -Path (Join-Path $Run.RunRoot 'manifest.md') -Text ($manifest + "`n")
}

function New-EasyProxyToken {
    $bytes = New-Object byte[] 16
    [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    return ([BitConverter]::ToString($bytes)).Replace('-', '').ToLowerInvariant()
}

function ConvertTo-EasyProxyEncryptedPassword {
    param(
        [Parameter(Mandatory)][string]$Password,
        [Parameter(Mandatory)][string]$PublicKeyPem
    )

    $rsa = [Security.Cryptography.RSA]::Create()
    try {
        $rsa.ImportFromPem($PublicKeyPem)
        $plainBytes = [Text.Encoding]::UTF8.GetBytes($Password)
        $encrypted = $rsa.Encrypt($plainBytes, [Security.Cryptography.RSAEncryptionPadding]::OaepSHA256)
        return [Convert]::ToBase64String($encrypted)
    }
    finally {
        $rsa.Dispose()
    }
}

function Get-EasyProxyLoginConfig {
    param([Parameter(Mandatory)][string]$BaseUri)
    $response = Invoke-EasyProxyApi -BaseUri $BaseUri -Path '/api/open/loginConfig'
    Assert-EasyProxy -Condition ($response.StatusCode -eq 200 -and $response.Json.code -eq 200 -and -not [string]::IsNullOrWhiteSpace($response.Json.data.passwordPublicKey)) -Name '读取登录配置' -Detail $(if ($response.Success) { $response.Content } else { $response.Error })
    return $response
}

function Initialize-EasyProxyAdmin {
    param(
        [Parameter(Mandatory)][string]$BaseUri,
        [Parameter(Mandatory)][string]$Username,
        [Parameter(Mandatory)][string]$Password,
        [string]$Mobile = '13800000000',
        [string]$Email = 'admin@example.invalid'
    )

    $check = Invoke-EasyProxyApi -BaseUri $BaseUri -Path '/api/open/checkInit'
    Assert-EasyProxy -Condition ($check.StatusCode -eq 200 -and $check.Json.code -eq 200) -Name '检查初始化状态' -Detail $(if ($check.Success) { $check.Content } else { $check.Error })
    $config = Get-EasyProxyLoginConfig -BaseUri $BaseUri
    $encryptedPassword = ConvertTo-EasyProxyEncryptedPassword -Password $Password -PublicKeyPem ([string]$config.Json.data.passwordPublicKey)

    if ([bool]$check.Json.data) {
        $initBody = @{
            username          = $Username
            encryptedPassword = $encryptedPassword
            mobile            = $Mobile
            email             = $Email
        }
        $init = Invoke-EasyProxyApi -BaseUri $BaseUri -Path '/api/open/initUser' -Method POST -Body $initBody
        Assert-EasyProxy -Condition ($init.StatusCode -eq 200 -and $init.Json.code -eq 200) -Name '初始化管理员' -Detail $(if ($init.Success) { $init.Content } else { $init.Error })
    }

    $loginBody = @{
        username          = $username
        encryptedPassword = $encryptedPassword
        captchaId         = ''
        captchaCode       = ''
    }
    $login = Invoke-EasyProxyApi -BaseUri $BaseUri -Path '/api/open/loginUser' -Method POST -Body $loginBody
    Assert-EasyProxy -Condition ($login.StatusCode -eq 200 -and $login.Json.code -eq 200 -and -not [string]::IsNullOrWhiteSpace([string]$login.Json.data.token)) -Name '管理员登录' -Detail $(if ($login.Success) { $login.Content } else { $login.Error })
    return [pscustomobject]@{
        Token    = [string]$login.Json.data.token
        UserInfo = $login.Json.data
    }
}

function Get-EasyProxyClients {
    param([Parameter(Mandatory)][string]$BaseUri, [Parameter(Mandatory)][string]$Token)
    $response = Invoke-EasyProxyApi -BaseUri $BaseUri -Path '/api/proxyClient/all' -Token $Token
    Assert-EasyProxy -Condition ($response.StatusCode -eq 200 -and $response.Json.code -eq 200) -Name '查询客户端列表' -Detail $(if ($response.Success) { $response.Content } else { $response.Error })
    return @($response.Json.data)
}

function Get-EasyProxyRules {
    param([Parameter(Mandatory)][string]$BaseUri, [Parameter(Mandatory)][string]$Token)
    $response = Invoke-EasyProxyApi -BaseUri $BaseUri -Path '/api/proxyClientRule/all' -Token $Token
    Assert-EasyProxy -Condition ($response.StatusCode -eq 200 -and $response.Json.code -eq 200) -Name '查询规则列表' -Detail $(if ($response.Success) { $response.Content } else { $response.Error })
    return @($response.Json.data)
}

function New-EasyProxyClient {
    param(
        [Parameter(Mandatory)][string]$BaseUri,
        [Parameter(Mandatory)][string]$Token,
        [Parameter(Mandatory)][string]$ClientName,
        [string]$ClientToken = (New-EasyProxyToken)
    )
    $body = @{ name = $ClientName; token = $ClientToken; status = 'offline'; enableFlag = $true }
    $response = Invoke-EasyProxyApi -BaseUri $BaseUri -Path '/api/proxyClient' -Method POST -Body $body -Token $Token
    Assert-EasyProxy -Condition ($response.StatusCode -eq 200 -and $response.Json.code -eq 200) -Name '创建客户端' -Detail $(if ($response.Success) { $response.Content } else { $response.Error })
    return $response.Json.data
}

function New-EasyProxyRule {
    param(
        [Parameter(Mandatory)][string]$BaseUri,
        [Parameter(Mandatory)][string]$Token,
        [Parameter(Mandatory)][int]$ProxyClientId,
        [Parameter(Mandatory)][string]$RuleName,
        [Parameter(Mandatory)][int]$ServerPort,
        [Parameter(Mandatory)][string]$ClientAddress,
        [AllowNull()][Nullable[int]]$LimitConn = $null,
        [AllowNull()][Nullable[int]]$LimitRate = $null,
        [bool]$EnableFlag = $true
    )
    $body = @{
        proxyClientId = $ProxyClientId
        name          = $RuleName
        serverPort    = $ServerPort
        clientAddress = $ClientAddress
        limitConn     = $LimitConn
        limitRate     = $LimitRate
        enableFlag    = $EnableFlag
    }
    $response = Invoke-EasyProxyApi -BaseUri $BaseUri -Path '/api/proxyClientRule' -Method POST -Body $body -Token $Token
    Assert-EasyProxy -Condition ($response.StatusCode -eq 200 -and $response.Json.code -eq 200) -Name '创建转发规则' -Detail $(if ($response.Success) { $response.Content } else { $response.Error })
    return $response.Json.data
}

function Set-EasyProxyRuleEnabled {
    param(
        [Parameter(Mandatory)][string]$BaseUri,
        [Parameter(Mandatory)][string]$Token,
        [Parameter(Mandatory)][object]$Rule,
        [Parameter(Mandatory)][bool]$Enabled,
        [string]$RuleName = '',
        [int]$ServerPort = 0,
        [string]$ClientAddress = ''
    )
    $body = @{
        id            = $Rule.id
        proxyClientId = $Rule.proxyClientId
        name          = if ([string]::IsNullOrWhiteSpace($RuleName)) { $Rule.name } else { $RuleName }
        serverPort    = if ($ServerPort -gt 0) { $ServerPort } else { $Rule.serverPort }
        clientAddress = if ([string]::IsNullOrWhiteSpace($ClientAddress)) { $Rule.clientAddress } else { $ClientAddress }
        limitConn     = $Rule.limitConn
        limitRate     = $Rule.limitRate
        enableFlag    = $Enabled
    }
    $response = Invoke-EasyProxyApi -BaseUri $BaseUri -Path '/api/proxyClientRule' -Method PUT -Body $body -Token $Token
    Assert-EasyProxy -Condition ($response.StatusCode -eq 200 -and $response.Json.code -eq 200) -Name '更新规则状态' -Detail $(if ($response.Success) { $response.Content } else { $response.Error })
    return $response.Json.data
}

function Set-EasyProxyClientEnabled {
    param(
        [Parameter(Mandatory)][string]$BaseUri,
        [Parameter(Mandatory)][string]$Token,
        [Parameter(Mandatory)][object]$Client,
        [Parameter(Mandatory)][bool]$Enabled
    )
    $body = @{
        id         = $Client.id
        name       = $Client.name
        token      = $Client.token
        status     = $Client.status
        enableFlag = $Enabled
    }
    $response = Invoke-EasyProxyApi -BaseUri $BaseUri -Path '/api/proxyClient' -Method PUT -Body $body -Token $Token
    Assert-EasyProxy -Condition ($response.StatusCode -eq 200 -and $response.Json.code -eq 200) -Name '更新客户端状态' -Detail $(if ($response.Success) { $response.Content } else { $response.Error })
    return $response.Json.data
}

function Invoke-EasyProxyHealth {
    param([Parameter(Mandatory)][string]$BaseUri, [int]$TimeoutSeconds = 10)
    return Invoke-EasyProxyApi -BaseUri $BaseUri -Path '/api/open/health' -TimeoutSeconds $TimeoutSeconds
}

Export-ModuleMember -Function *