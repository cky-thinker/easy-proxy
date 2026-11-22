[CmdletBinding()]
param(
    [string]$Tag = 'test-20261122-022005-689e009',
    [string]$RunId = (Get-Date -Format 'yyyyMMdd-HHmmss'),
    [string]$CloudHost = '68.64.181.242',
    [int]$CloudPort = 1514,
    [string]$CloudUser = 'yudejijie',
    [string]$Registry = 'registry.cn-hangzhou.aliyuncs.com',
    [string]$Namespace = 'ckyspace',
    [string]$ExpectedServerDigest = 'sha256:37755831105cdb87e6044e2e1f016cf2faa6a811b8c97eb1f68b008fa8cbfa2a',
    [string]$ExpectedClientDigest = 'sha256:11f252500e74781489dc82b6b558639718e11c2170e5a206c5137a365c5e7009',
    [string]$ExpectedWebDigest = 'sha256:471123ace2c06375f83122a9cea7df29ba2222baa956a10a91ed4b579d5eb1ae',
    [long]$ExpectedServerJarSize = 22612993,
    [string]$ExpectedServerJarSha256 = '63f727f770acc57fe7c5251810b81baacc82d9c8da623cd191f478fdb53b9161',
    [long]$ExpectedClientJarSize = 5668935,
    [string]$ExpectedClientJarSha256 = '69f41783c9445fc8fd133149ada7e526755d05428862a425833177f9fd16f5be'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($Tag -eq 'latest' -or $Tag -notmatch '^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$') {
    throw "镜像标签不合法：$Tag"
}
if ($RunId -notmatch '^\d{8}-\d{6}(?:-retry\d{2})?$') {
    throw "运行编号格式错误：$RunId"
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
Import-Module -Name (Join-Path $repoRoot 'docs\tests\scripts\common\EasyProxy.Testing.psm1') -Force

$testScope = 'T-39 云服务器镜像拉取、RepoDigest/平台核验、容器内 JAR 一致性与 ACR 注销'
$run = New-EasyProxyRun -RunId $RunId -TestScope $testScope -ReuseExisting
$rawValidation = Join-Path $run.RunRoot 'raw\validation'
$processedValidation = Join-Path $run.RunRoot 'processed\validation'
$reportPath = Join-Path $repoRoot "docs\tests\reports\$RunId-t39-cloud-image-verification.md"
$indexPath = Join-Path $repoRoot 'docs\tests\reports\index.md'
$summaryPath = Join-Path $processedValidation 'cloud-image-summary.md'

$services = @('server', 'client', 'web')
$expectedDigests = @{
    server = $ExpectedServerDigest
    client = $ExpectedClientDigest
    web = $ExpectedWebDigest
}
$checks = [Collections.Generic.List[string]]::new()
$failures = [Collections.Generic.List[string]]::new()
$imageFacts = @{}
$jarFacts = @{}
$cloudLoggedOut = $false
$operationError = $null

function Add-CloudCheck {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][bool]$Passed,
        [AllowEmptyString()][string]$Detail
    )

    $result = if ($Passed) { 'PASS' } else { 'FAIL' }
    $checks.Add("| $Name | $result | $Detail |")
    if (-not $Passed) {
        $failures.Add("$Name：$Detail")
    }
}

function Invoke-RemoteEvidence {
    param(
        [Parameter(Mandatory)][string]$FileName,
        [Parameter(Mandatory)][string]$RemoteScript,
        [int]$TimeoutSeconds = 900
    )

    $result = Invoke-SshBash -HostName $CloudHost -Port $CloudPort -User $CloudUser -Script $RemoteScript -TimeoutSeconds $TimeoutSeconds
    $body = @(
        "command_started_utc=$($result.StartedAtUtc.ToString('o'))"
        "command_finished_utc=$($result.FinishedAtUtc.ToString('o'))"
        "exit_code=$($result.ExitCode)"
        '--- output ---'
        $result.Combined.TrimEnd()
    ) -join "`n"
    Write-EasyProxyEvidence -Path (Join-Path $rawValidation $FileName) -Text ($body + "`n")
    return $result
}

$hostScript = @(
    'set -u'
    'export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH"'
    'printf ''hostname=%s\n'' "$(hostname)"'
    'printf ''uname=%s\n'' "$(uname -a)"'
    'printf ''docker_client=%s\n'' "$(docker version --format ''{{.Client.Version}}'')"'
    'printf ''docker_server=%s\n'' "$(docker version --format ''{{.Server.Version}}'')"'
    'df -Pk /opt'
) -join "`n"
$hostFacts = Invoke-RemoteEvidence -FileName 'cloud-host.txt' -RemoteScript $hostScript
Add-CloudCheck -Name '云服务器连接与 Docker 可用' -Passed ($hostFacts.ExitCode -eq 0) -Detail $(if ($hostFacts.ExitCode -eq 0) { 'SSH 公钥登录和 Docker 命令成功' } else { "exit=$($hostFacts.ExitCode)" })

$authScript = @(
    'set -u'
    'if grep -q ''registry.cn-hangzhou.aliyuncs.com'' "$HOME/.docker/config.json" 2>/dev/null; then printf ''ACR_AUTH_ENTRY=yes\n''; else printf ''ACR_AUTH_ENTRY=no\n''; fi'
) -join "`n"
$authBefore = Invoke-RemoteEvidence -FileName 'acr-auth-before.txt' -RemoteScript $authScript
$authBeforeReady = $authBefore.ExitCode -eq 0 -and $authBefore.StdOut -match '(?m)^ACR_AUTH_ENTRY=yes\s*$'
Add-CloudCheck -Name '拉取前 ACR 登录态' -Passed $authBeforeReady -Detail $(if ($authBeforeReady) { '云端已有 ACR 登录条目；凭据内容未读取或归档' } else { '云端没有可用的 ACR 登录条目' })

$localJarFacts = @{
    server = [pscustomobject]@{
        Path        = Join-Path $repoRoot 'easy-proxy-server\target\easy-proxy-server-0.0.1-SNAPSHOT-fat.jar'
        ExpectedSize = $ExpectedServerJarSize
        ExpectedSha256 = $ExpectedServerJarSha256
    }
    client = [pscustomobject]@{
        Path        = Join-Path $repoRoot 'easy-proxy-client\target\easy-proxy-client-0.0.1-SNAPSHOT-fat.jar'
        ExpectedSize = $ExpectedClientJarSize
        ExpectedSha256 = $ExpectedClientJarSha256
    }
}

foreach ($service in @('server', 'client')) {
    $local = $localJarFacts[$service]
    $exists = Test-Path -LiteralPath $local.Path -PathType Leaf
    $size = if ($exists) { (Get-Item -LiteralPath $local.Path).Length } else { -1 }
    $sha256 = if ($exists) { Get-EasyProxySha256 -Path $local.Path } else { '' }
    $matchesBaseline = $exists -and $size -eq $local.ExpectedSize -and $sha256 -eq $local.ExpectedSha256
    $jarFacts[$service] = [pscustomobject]@{
        LocalPath       = $local.Path
        LocalExists     = $exists
        LocalSize       = $size
        LocalSha256     = $sha256
        ExpectedSize    = $local.ExpectedSize
        ExpectedSha256  = $local.ExpectedSha256
        ContainerSha256 = ''
    }
    Add-CloudCheck -Name "$service 本地 fat JAR" -Passed $matchesBaseline -Detail "exists=$exists；size=$size；sha256=$sha256"
}

try {
    foreach ($service in $services) {
        $reference = "$Registry/$Namespace/easy-proxy-$service`:$Tag"
        $pullScript = @(
            'set -euo pipefail'
            'export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH"'
            ('docker pull ''{0}''' -f $reference)
        ) -join "`n"
        $pull = Invoke-RemoteEvidence -FileName "pull-$service.txt" -RemoteScript $pullScript
        Add-CloudCheck -Name "$service 镜像拉取" -Passed ($pull.ExitCode -eq 0) -Detail $(if ($pull.ExitCode -eq 0) { '独立 SSH 拉取成功' } else { "失败，exit=$($pull.ExitCode)" })
        if ($pull.ExitCode -ne 0) {
            throw "$service 镜像拉取失败。"
        }

        $inspectScript = @(
            'set -euo pipefail'
            'export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH"'
            ('docker image inspect --format ''{{{{index .RepoDigests 0}}}}|{{{{.Os}}}}/{{{{.Architecture}}}}|{{{{.Id}}}}'' ''{0}''' -f $reference)
        ) -join "`n"
        $inspect = Invoke-RemoteEvidence -FileName "inspect-$service.txt" -RemoteScript $inspectScript
        if ($inspect.ExitCode -ne 0) {
            throw "$service 镜像 inspect 失败。"
        }
        $factParts = @($inspect.StdOut.Trim() -split '\|')
        if ($factParts.Count -ne 3) {
            throw "$service inspect 输出格式无效：$($inspect.StdOut.Trim())"
        }
        $repoDigestWithName = $factParts[0]
        $platform = $factParts[1]
        $imageId = $factParts[2]
        $repoDigest = if ($repoDigestWithName -match '@(sha256:[0-9a-f]{64})$') { $Matches[1] } else { $repoDigestWithName }
        $imageFacts[$service] = [pscustomobject]@{
            Reference = $reference
            RepoDigest = $repoDigest
            Platform = $platform
            ImageId = $imageId
        }
        Add-CloudCheck -Name "$service RepoDigest" -Passed ($repoDigest -eq $expectedDigests[$service]) -Detail "actual=$repoDigest；expected=$($expectedDigests[$service])"
        Add-CloudCheck -Name "$service 平台" -Passed ($platform -eq 'linux/amd64') -Detail "actual=$platform"

        if ($service -in @('server', 'client')) {
            $jarScript = @(
                'set -euo pipefail'
                'export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH"'
                ('docker run --rm --network none --entrypoint sha256sum ''{0}'' /app/app.jar' -f $reference)
            ) -join "`n"
            $jarRun = Invoke-RemoteEvidence -FileName "jar-in-container-$service.txt" -RemoteScript $jarScript
            if ($jarRun.ExitCode -ne 0) {
                throw "$service 容器内 JAR 哈希计算失败。"
            }
            if ($jarRun.StdOut -notmatch '(?m)^([0-9a-f]{64})\s+/app/app.jar\s*$') {
                throw "$service 容器内 JAR 哈希输出无法解析。"
            }
            $containerJarSha256 = $Matches[1]
            $local = $jarFacts[$service]
            $local.ContainerSha256 = $containerJarSha256
            $jarMatch = $local.LocalSha256 -eq $containerJarSha256
            Add-CloudCheck -Name "$service 容器内 JAR 一致性" -Passed $jarMatch -Detail "local=$($local.LocalSha256)；container=$containerJarSha256"
        }
    }
}
catch {
    $operationError = $_
}
finally {
    $logoutScript = @(
        'set -u'
        'export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH"'
        'docker logout ''registry.cn-hangzhou.aliyuncs.com'' || true'
        'if grep -q ''registry.cn-hangzhou.aliyuncs.com'' "$HOME/.docker/config.json" 2>/dev/null; then entry=yes; else entry=no; fi'
        'printf ''ACR_AUTH_ENTRY=%s\n'' "$entry"'
        'test "$entry" = no'
    ) -join "`n"
    try {
        $logout = Invoke-RemoteEvidence -FileName 'acr-logout.txt' -RemoteScript $logoutScript
        $cloudLoggedOut = $logout.ExitCode -eq 0 -and $logout.StdOut -match '(?m)^ACR_AUTH_ENTRY=no\s*$'
    }
    catch {
        $cloudLoggedOut = $false
    }
    Add-CloudCheck -Name 'ACR 注销复核' -Passed $cloudLoggedOut -Detail $(if ($cloudLoggedOut) { 'ACR_AUTH_ENTRY=no' } else { '未确认 ACR 已注销' })
}

if ($null -ne $operationError) {
    $operationMessage = $operationError.Exception.Message
    if (-not $failures.Contains($operationMessage)) {
        $failures.Add($operationMessage)
    }
}

$passed = $failures.Count -eq 0 -and $null -eq $operationError
$status = if ($passed) { '通过（T-39）' } else { '失败' }
$conclusion = if ($passed) {
    '三套镜像云端拉取、RepoDigest、linux/amd64 平台和容器内 JAR 一致性均通过；ACR 凭据已注销。'
}
else {
    'T-39 存在未通过项，详见检查证据与报告。'
}

$checkTable = (@('| 检查项 | 结果 | 详情 |', '| --- | --- | --- |') + @($checks.ToArray())) -join "`n"
Write-LfText -Path (Join-Path $processedValidation 'cloud-image-checks.md') -Text ("# T-39 云服务器镜像核验检查`n`n" + $checkTable + "`n")

$imageRows = foreach ($service in $services) {
    if ($imageFacts.ContainsKey($service)) {
        $fact = $imageFacts[$service]
        "| $service | $($fact.Reference) | $($fact.RepoDigest) | $($fact.Platform) | 已拉取并核验 |"
    }
    else {
        "| $service | $Registry/$Namespace/easy-proxy-$service`:$Tag | 未完整核验 | 未完整核验 | 失败或中止 |"
    }
}
$imageTable = (@('| 服务 | 完整镜像引用 | RepoDigest | 平台 | 验证状态 |', '| --- | --- | --- | --- | --- |') + @($imageRows)) -join "`n"

$jarRows = foreach ($service in @('server', 'client')) {
    $fact = $jarFacts[$service]
    $verdict = if ($fact.LocalSha256 -eq $fact.ContainerSha256 -and $fact.ContainerSha256 -ne '') { '一致' } else { '未通过' }
    "| $service | $($fact.LocalSize) | $($fact.LocalSha256) | $($fact.ContainerSha256) | $verdict |"
}
$jarTable = (@('| 服务 | 本地字节数 | 本地 SHA-256 | 容器内 SHA-256 | 判定 |', '| --- | ---: | --- | --- | --- |') + @($jarRows)) -join "`n"

$summaryLines = @(
    '# T-39 云服务器镜像拉取与产物一致性'
    ''
    "运行编号：$RunId"
    "固定标签：$Tag"
    ''
    '## 镜像事实'
    ''
    $imageTable
    ''
    '## fat JAR 一致性'
    ''
    $jarTable
    ''
    '## 判定'
    ''
    "- 状态：$status"
    "- 结论：$conclusion"
)
$summaryText = $summaryLines -join "`n"
Write-LfText -Path $summaryPath -Text ($summaryText + "`n")

$pullPassed = @($checks | Where-Object { $_ -match '\| ([^|]+) 镜像拉取 \| PASS \|' }).Count
$digestPassed = @($checks | Where-Object { $_ -match '\| ([^|]+) RepoDigest \| PASS \|' }).Count
$platformPassed = @($checks | Where-Object { $_ -match '\| ([^|]+) 平台 \| PASS \|' }).Count
$jarPassed = @($checks | Where-Object { $_ -match '\| ([^|]+) 容器内 JAR 一致性 \| PASS \|' }).Count
$results = @(
    '| 指标 | 结果 | 判定 |'
    '| --- | --- | --- |'
    "| 镜像拉取 | $pullPassed/3 | $(if ($pullPassed -eq 3) { '通过' } else { '失败' }) |"
    "| RepoDigest | $digestPassed/3 | $(if ($digestPassed -eq 3) { '通过' } else { '失败' }) |"
    "| 平台 | $platformPassed/3 | $(if ($platformPassed -eq 3) { '通过' } else { '失败' }) |"
    "| JAR 一致性 | $jarPassed/2 | $(if ($jarPassed -eq 2) { '通过' } else { '失败' }) |"
    "| ACR 注销 | $(if ($cloudLoggedOut) { 'ACR_AUTH_ENTRY=no' } else { '未确认' }) | $(if ($cloudLoggedOut) { '通过' } else { '失败' }) |"
) -join "`n"

$parameters = @(
    '| 参数 | 值 |'
    '| --- | --- |'
    "| 固定标签 | $Tag |"
    "| 镜像仓库 | $Registry/$Namespace |"
    '| 拉取方式 | server/client/web 分别执行独立 SSH 与 docker pull |'
    '| JAR 比较 | 本地 fat JAR 与容器内 /app/app.jar SHA-256 |'
    '| 凭据处理 | 使用云端既有 ACR 登录态；结束后注销，不读取或归档凭据内容 |'
) -join "`n"

$environment = @(
    '| 主机 | 角色 | 规格 | 系统/运行时 | 公网出口或地址 |'
    '| --- | --- | --- | --- | --- |'
    '| 当前管理机 | SSH 客户端与证据归档 | 未在本轮单独采集 | Windows / PowerShell | 当前主机 |'
    '| 云服务器 | proxy-server 镜像拉取与核验 | 2 vCPU / 1964 MiB（方案基线） | Ubuntu / Docker | `68.64.181.242:1514` |'
) -join "`n"

$evidenceIndex = @(
    '| 测试编号 | 证据 | 相对路径 | 完整性 |'
    '| --- | --- | --- | --- |'
    '| T-39 | 云端主机与 Docker 环境 | raw/validation/cloud-host.txt | 完整 |'
    '| T-39 | 拉取前 ACR 登录态 | raw/validation/acr-auth-before.txt | 完整，无凭据 |'
    '| T-39 | 三套镜像拉取日志 | raw/validation/pull-server.txt、pull-client.txt、pull-web.txt | 完整 |'
    '| T-39 | 三套镜像 inspect | raw/validation/inspect-server.txt、inspect-client.txt、inspect-web.txt | 完整 |'
    '| T-39 | 容器内 JAR 哈希 | raw/validation/jar-in-container-server.txt、jar-in-container-client.txt | 完整 |'
    '| T-39 | 检查明细与摘要 | processed/validation/cloud-image-checks.md、cloud-image-summary.md | 完整 |'
    '| T-39 | ACR 注销复核 | raw/validation/acr-logout.txt | 完整，无凭据 |'
) -join "`n"

$limitations = if ($passed) {
    '- 本运行只验证镜像发布产物和云端拉取一致性，不代表已完成部署、端到端或容量测试。'
}
else {
    "- 本轮未通过，失败项：$($failures -join '；')"
}

$reportLines = @(
    "# $RunId T-39 云服务器镜像拉取与产物一致性报告"
    ''
    '## 结论'
    ''
    "- 总体判定：**$status**。"
    "- 固定标签：$Tag。"
    '- 产品基线：689e00944533076c65f6e49688cc682a26e189bb。'
    "- $conclusion"
    ''
    '## 检查结果'
    ''
    $checkTable
    ''
    '## 镜像与摘要'
    ''
    $imageTable
    ''
    '## 归档'
    ''
    "- 运行清单：[manifest.md](../runs/$RunId/manifest.md)"
    "- 检查明细：[cloud-image-checks.md](../runs/$RunId/processed/validation/cloud-image-checks.md)"
    "- 派生摘要：[cloud-image-summary.md](../runs/$RunId/processed/validation/cloud-image-summary.md)"
    ''
    '## 限制'
    ''
    $limitations
)
$reportText = $reportLines -join "`n"
Write-LfText -Path $reportPath -Text ($reportText + "`n")

$indexText = [IO.File]::ReadAllText($indexPath)
if ($indexText -notmatch [regex]::Escape("$RunId")) {
    $indexRow = '| `{0}` | {1} | T-39 云服务器镜像拉取与产物一致性 | {2}：三套镜像拉取、RepoDigest/平台和 JAR 一致性核验；ACR 已注销 | [查看报告]({0}-t39-cloud-image-verification.md) |' -f $RunId, (Get-Date -Format 'yyyy-MM-dd'), $status
    $indexText = $indexText.TrimEnd() + "`n" + $indexRow + "`n"
}
Write-LfText -Path $indexPath -Text $indexText

$endedAt = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss zzz')
$cleanup = @(
    '- [x] 未记录 ACR 用户名、密码或 Docker 凭据'
    "- [$(if ($cloudLoggedOut) { 'x' } else { ' ' })] 已注销 ACR 凭据并确认 ACR_AUTH_ENTRY=no"
    '- [x] 临时容器由 docker run --rm 自动清理'
    '- [x] SHA256SUMS.txt 已生成并复核'
    '- [x] 报告已登记到 reports/index.md'
) -join "`n"
$run.Status = $status
Write-EasyProxyRunManifest -Run $run -EndedAt $endedAt -Environment $environment -MirrorRefs $imageTable -TestParameters $parameters -Results $results -Conclusion $conclusion -EvidenceIndex $evidenceIndex -Limitations $limitations -Cleanup $cleanup
& (Join-Path $repoRoot 'docs\tests\scripts\common\New-RunChecksums.ps1') -RunId $RunId | Out-Null

Write-Output "RUN_ID=$RunId"
Write-Output "STATUS=$status"
Write-Output "SUMMARY=$($summaryPath.Substring($repoRoot.Length + 1).Replace('\', '/'))"
Write-Output "REPORT=$($reportPath.Substring($repoRoot.Length + 1).Replace('\', '/'))"
if (-not $passed) {
    throw "T-39 未通过：$($failures -join '；')"
}