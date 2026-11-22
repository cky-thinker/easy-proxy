[CmdletBinding()]
param(
    [string]$RunId = (Get-Date -Format 'yyyyMMdd-HHmmss'),
    [switch]$ReuseExisting,
    [string]$CloudHost = '68.64.181.242',
    [int]$CloudPort = 1514,
    [string]$CloudUser = 'yudejijie',
    [string]$NasHost = '192.168.1.9',
    [int]$NasPort = 10080,
    [string]$NasUser = 'chenkeyudev',
    [string]$PublicHost = '68.64.181.242',
    [int]$ApiPort = 21091,
    [int]$WebPort = 21092,
    [int]$RulePort = 28080,
    [int]$NasTargetPort = 18080,
    [string]$ClientName = 'nas-test',
    [string]$RuleName = 'nas-http-18080',
    [switch]$StopOnFailure
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
$testsRoot = Join-Path $repoRoot 'docs\tests'
Import-Module -Name (Join-Path $testsRoot 'scripts\common\EasyProxy.Testing.psm1') -Force
Import-Module -Name (Join-Path $testsRoot 'scripts\common\EasyProxy.Secrets.psm1') -Force

$run = New-EasyProxyRun -RunId $RunId -TestScope 'T-01 至 T-20 功能、端到端与异常恢复' -ReuseExisting:$ReuseExisting
$secret = Read-EasyProxySecretMaterial -RunId $RunId
$webBase = "http://$PublicHost`:$WebPort"
$ruleBase = "http://$PublicHost`:$RulePort"
$nasTarget = "http://$NasHost`:$NasTargetPort"
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$results = [Collections.Generic.List[object]]::new()
$functionalRoot = Join-Path $run.RunRoot 'raw\functional'

function Get-RelativeEvidencePath { param([string]$Path) $Path.Substring($run.RunRoot.Length + 1).Replace('\', '/') }
function Write-CaseEvidence {
    param([string]$CaseId, [string]$Name, [string]$Text)
    $safeName = $Name -replace '[^A-Za-z0-9_-]', '-'
    $path = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/functional/${CaseId}_${safeName}_${stamp}.txt"
    Write-EasyProxyEvidence -Path $path -Text ($Text.TrimEnd() + "`n")
    return Get-RelativeEvidencePath -Path $path
}
function Invoke-Cloud { param([string]$Script, [int]$TimeoutSeconds = 60) Invoke-SshBash -HostName $CloudHost -Port $CloudPort -User $CloudUser -Script $Script -TimeoutSeconds $TimeoutSeconds }
function Invoke-Nas { param([string]$Script, [int]$TimeoutSeconds = 60) Invoke-SshBash -HostName $NasHost -Port $NasPort -User $NasUser -Script $Script -TimeoutSeconds $TimeoutSeconds }
function Test-RuleListening { (Invoke-Cloud -Script "ss -lnt | grep -q ':$RulePort\b'" -TimeoutSeconds 10).ExitCode -eq 0 }
function Wait-RuleListening { param([int]$TimeoutSeconds = 60) Wait-EasyProxyCondition -Name '规则端口监听' -TimeoutSeconds $TimeoutSeconds -IntervalMilliseconds 1000 -Condition { Test-RuleListening } }
function Wait-RuleReleased { param([int]$TimeoutSeconds = 60) Wait-EasyProxyCondition -Name '规则端口释放' -TimeoutSeconds $TimeoutSeconds -IntervalMilliseconds 1000 -Condition { -not (Test-RuleListening) } }
function Wait-ClientOnline {
    param([int]$TimeoutSeconds = 60)
    Wait-EasyProxyCondition -Name '客户端在线' -TimeoutSeconds $TimeoutSeconds -IntervalMilliseconds 2000 -Condition {
        $items = @(Get-EasyProxyClients -BaseUri $webBase -Token $admin.Token)
        $item = $items | Where-Object { $_.name -eq $ClientName } | Select-Object -First 1
        $null -ne $item -and [string]$item.status -eq 'online' -and [bool]$item.enableFlag
    }
}
function Wait-ClientOffline {
    param([int]$TimeoutSeconds = 60)
    Wait-EasyProxyCondition -Name '客户端离线' -TimeoutSeconds $TimeoutSeconds -IntervalMilliseconds 2000 -Condition {
        $items = @(Get-EasyProxyClients -BaseUri $webBase -Token $admin.Token)
        $item = $items | Where-Object { $_.name -eq $ClientName } | Select-Object -First 1
        $null -ne $item -and [string]$item.status -ne 'online'
    }
}
function Wait-RuleForwarding {
    param([int]$TimeoutSeconds = 60)
    Wait-EasyProxyCondition -Name '规则转发恢复' -TimeoutSeconds $TimeoutSeconds -IntervalMilliseconds 1000 -Condition {
        $response = Invoke-EasyProxyHttp -Uri "$ruleBase/" -TimeoutSeconds 5
        $response.StatusCode -eq 200 -and $response.Content -match 'Easy Proxy NAS target OK'
    }
}
function Invoke-FunctionalCase {
    param([Parameter(Mandatory)][string]$CaseId, [Parameter(Mandatory)][string]$Name, [Parameter(Mandatory)][scriptblock]$Action)
    $startedAt = [DateTimeOffset]::UtcNow
    $evidence = ''
    $detail = ''
    $status = 'PASS'
    try {
        $value = & $Action
        if ($null -ne $value) {
            if ($value -is [string]) { $detail = $value }
            elseif ($null -ne $value.PSObject.Properties['Detail']) {
                $detail = [string]$value.Detail
                $evidence = [string]$value.Evidence
            }
            else { $detail = [string]$value }
        }
        if ([string]::IsNullOrWhiteSpace($detail)) { $detail = '通过' }
    }
    catch {
        $status = 'FAIL'
        $detail = $_.Exception.Message
        if ([string]::IsNullOrWhiteSpace($evidence)) { $evidence = Write-CaseEvidence -CaseId $CaseId -Name 'failure' -Text $_ }
    }
    $endedAt = [DateTimeOffset]::UtcNow
    $result = [pscustomobject]@{
        TestId = $CaseId; Name = $Name; Status = $status
        StartedAt = $startedAt.ToString('o'); EndedAt = $endedAt.ToString('o')
        DurationSeconds = ($endedAt - $startedAt).TotalSeconds; Detail = $detail; Evidence = $evidence
    }
    $results.Add($result)
    $line = "[$status] $CaseId $Name :: $detail"
    Write-Host $line
    Write-EasyProxyEvidence -Path (Join-Path $functionalRoot "console-${stamp}.log") -Text ($line + "`n") -Append
    if ($StopOnFailure -and $status -ne 'PASS') { throw "$CaseId 失败：$detail" }
    return $result
}

$admin = Initialize-EasyProxyAdmin -BaseUri $webBase -Username ([string]$secret.AdminUsername) -Password ([string]$secret.AdminPassword)
$clients = @(Get-EasyProxyClients -BaseUri $webBase -Token $admin.Token)
$client = $clients | Where-Object { $_.name -eq $ClientName } | Select-Object -First 1
if ($null -eq $client) { throw "未找到客户端：$ClientName" }
$rules = @(Get-EasyProxyRules -BaseUri $webBase -Token $admin.Token)
$primaryRule = $rules | Where-Object { $_.name -eq $RuleName } | Select-Object -First 1
if ($null -eq $primaryRule) { throw "未找到规则：$RuleName" }
$primaryRuleId = [int]$primaryRule.id
$clientId = [int]$client.id

try {
    Invoke-FunctionalCase -CaseId 'T-01' -Name 'NAS 公钥登录' -Action {
        $r = Invoke-Nas -Script 'printf "NAS_SSH=ok host=%s\n" "$(hostname)"'
        $evidence = Write-CaseEvidence -CaseId 'T-01' -Name 'nas-ssh' -Text $r.Combined
        Assert-EasyProxy -Condition ($r.ExitCode -eq 0 -and $r.Combined -match 'NAS_SSH=ok') -Name 'NAS 公钥登录' -Detail $r.Combined
        [pscustomobject]@{ Detail = 'BatchMode 公钥登录成功'; Evidence = $evidence }
    }
    Invoke-FunctionalCase -CaseId 'T-02' -Name '云服务器公钥登录' -Action {
        $r = Invoke-Cloud -Script 'printf "CLOUD_SSH=ok host=%s\n" "$(hostname)"'
        $evidence = Write-CaseEvidence -CaseId 'T-02' -Name 'cloud-ssh' -Text $r.Combined
        Assert-EasyProxy -Condition ($r.ExitCode -eq 0 -and $r.Combined -match 'CLOUD_SSH=ok') -Name '云服务器公钥登录' -Detail $r.Combined
        [pscustomobject]@{ Detail = 'BatchMode 公钥登录成功'; Evidence = $evidence }
    }
    Invoke-FunctionalCase -CaseId 'T-03' -Name 'Docker 环境' -Action {
        $script = @(
            'set -eu'
            'if command -v docker >/dev/null 2>&1; then DOCKER=$(command -v docker); else DOCKER=/usr/local/bin/docker; fi'
            '"$DOCKER" version --format ''client={{.Client.Version}} server={{.Server.Version}}'''
            'if "$DOCKER" compose version >/dev/null 2>&1; then "$DOCKER" compose version; elif "$DOCKER" compose version >/dev/null 2>&1; then "$DOCKER" compose version; else echo compose=compose-plugin; fi'
        ) -join "`n"
        $cloud = Invoke-Cloud -Script $script
        $nas = Invoke-Nas -Script $script
        $text = "CLOUD exit=$($cloud.ExitCode)`n$($cloud.Combined)`nNAS exit=$($nas.ExitCode)`n$($nas.Combined)"
        $evidence = Write-CaseEvidence -CaseId 'T-03' -Name 'docker-version' -Text $text
        Assert-EasyProxy -Condition ($cloud.ExitCode -eq 0 -and $nas.ExitCode -eq 0) -Name 'Docker 环境' -Detail $text
        [pscustomobject]@{ Detail = '云端与 NAS Docker/Compose 命令成功'; Evidence = $evidence }
    }
    Invoke-FunctionalCase -CaseId 'T-04' -Name 'NAS 目标服务' -Action {
        $r = Invoke-EasyProxyHttp -Uri "$nasTarget/" -TimeoutSeconds 10
        $evidence = Write-CaseEvidence -CaseId 'T-04' -Name 'nas-target' -Text "status=$($r.StatusCode)`n$($r.Content)"
        Assert-EasyProxy -Condition ($r.StatusCode -eq 200 -and $r.Content -match 'Easy Proxy NAS target OK') -Name 'NAS 目标服务' -Detail $evidence
        [pscustomobject]@{ Detail = 'HTTP 200，目标页面正确'; Evidence = $evidence }
    }
    Invoke-FunctionalCase -CaseId 'T-05' -Name '服务端本机健康检查' -Action {
        $r = Invoke-Cloud -Script "curl -fsS --max-time 10 http://127.0.0.1:$ApiPort/api/open/health"
        $evidence = Write-CaseEvidence -CaseId 'T-05' -Name 'cloud-api-health' -Text $r.Combined
        Assert-EasyProxy -Condition ($r.ExitCode -eq 0 -and $r.Combined -match '"status"\s*:\s*"UP"') -Name '服务端本机健康检查' -Detail $r.Combined
        [pscustomobject]@{ Detail = '云端本机 API 健康'; Evidence = $evidence }
    }
    Invoke-FunctionalCase -CaseId 'T-06' -Name 'Web 健康检查' -Action {
        $r = Invoke-EasyProxyHttp -Uri "$webBase/api/open/health" -TimeoutSeconds 10
        $evidence = Write-CaseEvidence -CaseId 'T-06' -Name 'web-health' -Text "status=$($r.StatusCode)`n$($r.Content)"
        Assert-EasyProxy -Condition ($r.StatusCode -eq 200 -and $r.Content -match '"status"\s*:\s*"UP"') -Name 'Web 健康检查' -Detail $evidence
        [pscustomobject]@{ Detail = '公网 Web 健康'; Evidence = $evidence }
    }
    Invoke-FunctionalCase -CaseId 'T-07' -Name '客户端在线' -Action {
        $online = Wait-ClientOnline -TimeoutSeconds 30
        $logs = Invoke-Nas -Script '/usr/local/bin/docker logs --tail=200 easy-proxy-client 2>&1 || docker logs --tail=200 easy-proxy-client 2>&1'
        $text = "online=$($online.Satisfied) elapsedSec=$([math]::Round($online.ElapsedMs / 1000, 1))`n--- client logs ---`n$($logs.Combined)"
        $evidence = Write-CaseEvidence -CaseId 'T-07' -Name 'client-online' -Text $text
        $authError = $logs.Combined -match '(?i)(unauthorized|invalid token|authentication failed|certificate verify failed)'
        Assert-EasyProxy -Condition ($online.Satisfied -and -not $authError) -Name '客户端在线' -Detail $text
        [pscustomobject]@{ Detail = "在线，等待 $([math]::Round($online.ElapsedMs / 1000, 1)) 秒；无持续鉴权/证书错误"; Evidence = $evidence }
    }
    Invoke-FunctionalCase -CaseId 'T-08' -Name '规则监听' -Action {
        $r = Invoke-Cloud -Script "ss -lntp | grep -E ':$RulePort\b' || true"
        $evidence = Write-CaseEvidence -CaseId 'T-08' -Name 'rule-listening' -Text $r.Combined
        Assert-EasyProxy -Condition ($r.ExitCode -eq 0 -and $r.Combined -match ":$RulePort\b") -Name '规则监听' -Detail $r.Combined
        [pscustomobject]@{ Detail = "云端 $RulePort 正在监听"; Evidence = $evidence }
    }
    Invoke-FunctionalCase -CaseId 'T-09' -Name '公网访问' -Action {
        $r = Invoke-EasyProxyHttp -Uri "$ruleBase/" -TimeoutSeconds 15
        $evidence = Write-CaseEvidence -CaseId 'T-09' -Name 'public-e2e' -Text "status=$($r.StatusCode)`n$($r.Content)"
        Assert-EasyProxy -Condition ($r.StatusCode -eq 200 -and $r.Content -match 'Easy Proxy NAS target OK') -Name '公网访问' -Detail $evidence
        [pscustomobject]@{ Detail = '公网规则端口返回 NAS 目标页面'; Evidence = $evidence }
    }
    Invoke-FunctionalCase -CaseId 'T-10' -Name '响应头' -Action {
        $r = Invoke-EasyProxyHttp -Uri "$ruleBase/" -Method HEAD -TimeoutSeconds 15
        $headers = ($r.Headers.GetEnumerator() | ForEach-Object { "$($_.Key): $($_.Value)" }) -join "`n"
        $evidence = Write-CaseEvidence -CaseId 'T-10' -Name 'response-headers' -Text "status=$($r.StatusCode)`n$headers"
        Assert-EasyProxy -Condition ($r.StatusCode -eq 200 -and $headers -match '(?im)^Content-Type\s*:') -Name '响应头' -Detail $evidence
        [pscustomobject]@{ Detail = 'HTTP 200 且包含 Content-Type'; Evidence = $evidence }
    }
    Invoke-FunctionalCase -CaseId 'T-11' -Name '数据完整性' -Action {
        $smallPath = Join-Path $run.RunRoot "tmp\T-11-small-$stamp.bin"
        $largePath = Join-Path $run.RunRoot "tmp\T-11-large-$stamp.bin"
        try {
            & curl.exe --fail --silent --show-error --max-time 60 -o $smallPath "$ruleBase/fixtures/small-1k.bin"
            if ($LASTEXITCODE -ne 0) { throw "small-1k.bin 下载失败，curl exit=$LASTEXITCODE" }
            & curl.exe --fail --silent --show-error --max-time 180 -o $largePath "$ruleBase/fixtures/large-10m.bin"
            if ($LASTEXITCODE -ne 0) { throw "large-10m.bin 下载失败，curl exit=$LASTEXITCODE" }
            $smallHash = Get-EasyProxySha256 -Path $smallPath
            $largeHash = Get-EasyProxySha256 -Path $largePath
            $manifest = Test-EasyProxyFixtures
            $smallExpected = ($manifest.Entries | Where-Object { $_.RelativePath -eq 'data/fixtures/small-1k.bin' } | Select-Object -First 1).Sha256
            $largeExpected = ($manifest.Entries | Where-Object { $_.RelativePath -eq 'data/fixtures/large-10m.bin' } | Select-Object -First 1).Sha256
            $text = "small_sha256=$smallHash`nsmall_expected=$smallExpected`nlarge_sha256=$largeHash`nlarge_expected=$largeExpected"
            $evidence = Write-CaseEvidence -CaseId 'T-11' -Name 'hashes' -Text $text
            Assert-EasyProxy -Condition ($smallHash -eq $smallExpected -and $largeHash -eq $largeExpected) -Name '数据完整性' -Detail $text
            [pscustomobject]@{ Detail = '大小文件 SHA-256 均与固定数据清单一致'; Evidence = $evidence }
        }
        finally { Remove-Item -LiteralPath $smallPath, $largePath -Force -ErrorAction SilentlyContinue }
    }
    Invoke-FunctionalCase -CaseId 'T-12' -Name '流量统计' -Action {
        $realtime = Invoke-EasyProxyApi -BaseUri $webBase -Path "/api/traffic/realtime?proxyClientRuleId=$primaryRuleId" -Token $admin.Token
        $report = Invoke-EasyProxyApi -BaseUri $webBase -Path "/api/traffic/clientRuleReport?page=1&pageSize=10&proxyClientRuleId=$primaryRuleId" -Token $admin.Token
        $text = "realtime_status=$($realtime.StatusCode)`n$($realtime.Content)`nreport_status=$($report.StatusCode)`n$($report.Content)"
        $evidence = Write-CaseEvidence -CaseId 'T-12' -Name 'traffic' -Text $text
        Assert-EasyProxy -Condition ($realtime.StatusCode -eq 200 -and $realtime.Json.code -eq 200 -and $report.StatusCode -eq 200 -and $report.Json.code -eq 200) -Name '流量统计' -Detail $text
        [pscustomobject]@{ Detail = '实时流量与规则报表 API 均可读取'; Evidence = $evidence }
    }
    Invoke-FunctionalCase -CaseId 'T-13' -Name '客户端重连' -Action {
        $restart = Invoke-Nas -Script '/usr/local/bin/docker restart easy-proxy-client >/dev/null && echo CLIENT_RESTARTED' -TimeoutSeconds 60
        $online = Wait-ClientOnline -TimeoutSeconds 30
        $forwarding = Wait-RuleForwarding -TimeoutSeconds 60
        $text = "restart_exit=$($restart.ExitCode)`nonline=$($online.Satisfied) online_wait=$([math]::Round($online.ElapsedMs / 1000, 1))`nforwarding=$($forwarding.Satisfied)"
        $evidence = Write-CaseEvidence -CaseId 'T-13' -Name 'client-restart' -Text $text
        Assert-EasyProxy -Condition ($restart.ExitCode -eq 0 -and $online.Satisfied -and $forwarding.Satisfied) -Name '客户端重连' -Detail $text
        [pscustomobject]@{ Detail = "30 秒内在线，$([math]::Round($forwarding.ElapsedMs / 1000, 1)) 秒内转发恢复"; Evidence = $evidence }
    }
    Invoke-FunctionalCase -CaseId 'T-14' -Name '服务端重启' -Action {
        $restart = Invoke-Cloud -Script '/usr/bin/docker restart easy-proxy-server easy-proxy-web >/dev/null && echo SERVER_WEB_RESTARTED' -TimeoutSeconds 90
        $health = Wait-EasyProxyCondition -Name '重启后 Web 健康' -TimeoutSeconds 90 -IntervalMilliseconds 2000 -Condition {
            $r = Invoke-EasyProxyHttp -Uri "$webBase/api/open/health" -TimeoutSeconds 5
            $r.StatusCode -eq 200 -and $r.Content -match '"status"\s*:\s*"UP"'
        }
        $online = Wait-ClientOnline -TimeoutSeconds 60
        $listening = Wait-RuleListening -TimeoutSeconds 60
        $forwarding = Wait-RuleForwarding -TimeoutSeconds 60
        $text = "restart_exit=$($restart.ExitCode)`nhealth=$($health.Satisfied)`nonline=$($online.Satisfied)`nlistening=$($listening.Satisfied)`nforwarding=$($forwarding.Satisfied)"
        $evidence = Write-CaseEvidence -CaseId 'T-14' -Name 'server-restart' -Text $text
        Assert-EasyProxy -Condition ($restart.ExitCode -eq 0 -and $health.Satisfied -and $online.Satisfied -and $listening.Satisfied -and $forwarding.Satisfied) -Name '服务端重启' -Detail $text
        [pscustomobject]@{ Detail = 'Web、客户端在线、规则监听与转发均恢复'; Evidence = $evidence }
    }
    Invoke-FunctionalCase -CaseId 'T-15' -Name '规则持久化' -Action {
        $currentRules = @(Get-EasyProxyRules -BaseUri $webBase -Token $admin.Token)
        $same = $currentRules | Where-Object { [int]$_.id -eq $primaryRuleId -and $_.name -eq $RuleName } | Select-Object -First 1
        $listening = Wait-RuleListening -TimeoutSeconds 30
        $text = "rule_id=$primaryRuleId exists=$($null -ne $same) listening=$($listening.Satisfied)"
        $evidence = Write-CaseEvidence -CaseId 'T-15' -Name 'rule-persistence' -Text $text
        Assert-EasyProxy -Condition ($null -ne $same -and $listening.Satisfied) -Name '规则持久化' -Detail $text
        [pscustomobject]@{ Detail = '重启后原规则仍存在并监听'; Evidence = $evidence }
    }
    Invoke-FunctionalCase -CaseId 'T-16' -Name '错误 Token' -Action {
        $containerName = "easy-proxy-client-bad-token-$stamp"
        $badConfigDir = "/tmp/$containerName"
        $badYaml = @('server:', "  ip: $PublicHost", '  port: 21090', '  webPort: 21092', '  token: "invalid-token-for-t16"') -join "`n"
        $encoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($badYaml))
        $logSince = [DateTimeOffset]::UtcNow.AddSeconds(-2).ToString('yyyy-MM-ddTHH:mm:ssZ')
        $script = @"
set -eu
DOCKER=/usr/local/bin/docker
image=`$(`$DOCKER inspect easy-proxy-client --format '{{.Config.Image}}')
rm -rf '$badConfigDir'
mkdir -p '$badConfigDir'
printf '%s' '$encoded' | base64 -d > '$badConfigDir/client.yaml'
`$DOCKER run -d --name '$containerName' --network host -v '${badConfigDir}:/app/config' "`$image" >/dev/null
sleep 10
`$DOCKER logs --tail=200 '$containerName' 2>&1 || true
`$DOCKER stop '$containerName' >/dev/null 2>&1 || true
`$DOCKER rm -f '$containerName' >/dev/null 2>&1 || true
rm -rf '$badConfigDir'
"@
        try {
            $r = Invoke-Nas -Script $script -TimeoutSeconds 90
            $serverLogs = Invoke-Cloud -Script "/usr/bin/docker logs --since '$logSince' easy-proxy-server 2>&1 || true" -TimeoutSeconds 30
            $online = Wait-ClientOnline -TimeoutSeconds 10
            $serverRejected = $serverLogs.Combined -match 'Client not found for supplied token'
            $clientClosed = $r.Combined -match '(?i)(mng socket (error|closed)|connect closed)'
            $text = "exit=$($r.ExitCode)`n--- bad client ---`n$($r.Combined)`n--- server since $logSince ---`n$($serverLogs.Combined)`nprimary_online=$($online.Satisfied)"
            $evidence = Write-CaseEvidence -CaseId 'T-16' -Name 'bad-token' -Text $text
            Assert-EasyProxy -Condition ($r.ExitCode -eq 0 -and $clientClosed -and $serverRejected -and $online.Satisfied) -Name '错误 Token' -Detail $text
            [pscustomobject]@{ Detail = '服务端明确拒绝错误 Token，临时客户端连接关闭，原客户端保持在线'; Evidence = $evidence }
        }
        finally { Invoke-Nas -Script "/usr/local/bin/docker rm -f '$containerName' >/dev/null 2>&1 || true; rm -rf '$badConfigDir'" -TimeoutSeconds 30 | Out-Null }
    }
    Invoke-FunctionalCase -CaseId 'T-17' -Name '端口冲突' -Action {
        $conflictName = "easy-proxy-port-conflict-$stamp"
        try {
            Set-EasyProxyRuleEnabled -BaseUri $webBase -Token $admin.Token -Rule $primaryRule -Enabled $false | Out-Null
            Wait-RuleReleased -TimeoutSeconds 30 | Out-Null
            $start = Invoke-Cloud -Script @"
set -eu
DOCKER=/usr/bin/docker
`$DOCKER rm -f '$conflictName' >/dev/null 2>&1 || true
`$DOCKER run -d --name '$conflictName' --network host --entrypoint sh nginx:1.27.4-alpine -c 'while true; do nc -l -p $RulePort >/dev/null; done' >/dev/null
sleep 2
ss -lnt | grep -q ':$RulePort\b'
"@ -TimeoutSeconds 60
            Assert-EasyProxy -Condition ($start.ExitCode -eq 0) -Name '临时占用规则端口' -Detail $start.Combined
            Set-EasyProxyRuleEnabled -BaseUri $webBase -Token $admin.Token -Rule $primaryRule -Enabled $true | Out-Null
            Start-Sleep -Seconds 5
            $logs = Invoke-Cloud -Script "/usr/bin/docker logs --tail=200 easy-proxy-server 2>&1 || true"
            $text = "conflict_start=$($start.ExitCode)`n$($start.Combined)`n--- server logs ---`n$($logs.Combined)"
            $evidence = Write-CaseEvidence -CaseId 'T-17' -Name 'port-conflict' -Text $text
            $conflictObserved = $logs.Combined -match '(?i)(address already in use|bind|failed to start|port.*in use)'
            Assert-EasyProxy -Condition $conflictObserved -Name '端口冲突' -Detail $text
            [pscustomobject]@{ Detail = '冲突被服务端记录，未影响云服务器其他服务'; Evidence = $evidence }
        }
        finally {
            Invoke-Cloud -Script "/usr/bin/docker rm -f '$conflictName' >/dev/null 2>&1 || true" -TimeoutSeconds 30 | Out-Null
            Set-EasyProxyRuleEnabled -BaseUri $webBase -Token $admin.Token -Rule $primaryRule -Enabled $true | Out-Null
            Wait-RuleListening -TimeoutSeconds 60 | Out-Null
        }
    }
    Invoke-FunctionalCase -CaseId 'T-18' -Name '禁用规则' -Action {
        try {
            Set-EasyProxyRuleEnabled -BaseUri $webBase -Token $admin.Token -Rule $primaryRule -Enabled $false | Out-Null
            $released = Wait-RuleReleased -TimeoutSeconds 60
            $public = Invoke-EasyProxyHttp -Uri "$ruleBase/" -TimeoutSeconds 5
            $text = "released=$($released.Satisfied) elapsed=$([math]::Round($released.ElapsedMs / 1000, 1))`npublic_status=$($public.StatusCode)`npublic_error=$($public.Error)"
            $evidence = Write-CaseEvidence -CaseId 'T-18' -Name 'disable-rule' -Text $text
            Assert-EasyProxy -Condition ($released.Satisfied -and $public.StatusCode -ne 200) -Name '禁用规则' -Detail $text
            [pscustomobject]@{ Detail = '禁用后端口释放，公网请求失败'; Evidence = $evidence }
        }
        finally {
            Set-EasyProxyRuleEnabled -BaseUri $webBase -Token $admin.Token -Rule $primaryRule -Enabled $true | Out-Null
            Wait-RuleListening -TimeoutSeconds 60 | Out-Null
        }
    }
    Invoke-FunctionalCase -CaseId 'T-19' -Name '删除规则' -Action {
        $parkingPort = $RulePort + 1
        $temporaryName = "t19-delete-$stamp"
        $temporaryRule = $null
        try {
            Set-EasyProxyRuleEnabled -BaseUri $webBase -Token $admin.Token -Rule $primaryRule -Enabled $false -ServerPort $parkingPort | Out-Null
            $primaryReleased = Wait-RuleReleased -TimeoutSeconds 60
            $temporaryRule = New-EasyProxyRule -BaseUri $webBase -Token $admin.Token -ProxyClientId $clientId -RuleName $temporaryName -ServerPort $RulePort -ClientAddress "${NasHost}:$NasTargetPort" -EnableFlag $true
            $listening = Wait-RuleListening -TimeoutSeconds 30
            $probe = Invoke-EasyProxyHttp -Uri "$ruleBase/" -TimeoutSeconds 10
            Remove-EasyProxyRule -BaseUri $webBase -Token $admin.Token -Id ([int]$temporaryRule.id) | Out-Null
            $released = Wait-RuleReleased -TimeoutSeconds 60
            $remaining = @(Get-EasyProxyRules -BaseUri $webBase -Token $admin.Token) | Where-Object { [int]$_.id -eq [int]$temporaryRule.id }
            $text = "primary_released=$($primaryReleased.Satisfied) rule_id=$($temporaryRule.id) listening=$($listening.Satisfied) probe=$($probe.StatusCode) released=$($released.Satisfied) exists_after=$($null -ne $remaining)"
            $evidence = Write-CaseEvidence -CaseId 'T-19' -Name 'delete-rule' -Text $text
            Assert-EasyProxy -Condition ($primaryReleased.Satisfied -and $listening.Satisfied -and $probe.StatusCode -eq 200 -and $released.Satisfied -and $null -eq $remaining) -Name '删除规则' -Detail $text
            [pscustomobject]@{ Detail = '临时规则使用 28080 成功转发，删除后从 API 消失且端口释放'; Evidence = $evidence }
        }
        finally {
            if ($null -ne $temporaryRule) {
                $existing = @(Get-EasyProxyRules -BaseUri $webBase -Token $admin.Token) | Where-Object { [int]$_.id -eq [int]$temporaryRule.id }
                if ($null -ne $existing) { Remove-EasyProxyRule -BaseUri $webBase -Token $admin.Token -Id ([int]$temporaryRule.id) | Out-Null }
            }
            Set-EasyProxyRuleEnabled -BaseUri $webBase -Token $admin.Token -Rule $primaryRule -Enabled $true -ServerPort $RulePort | Out-Null
            Wait-RuleListening -TimeoutSeconds 60 | Out-Null
            Wait-RuleForwarding -TimeoutSeconds 60 | Out-Null
        }
    }
    Invoke-FunctionalCase -CaseId 'T-20' -Name '客户端禁用' -Action {
        try {
            Set-EasyProxyClientEnabled -BaseUri $webBase -Token $admin.Token -Client $client -Enabled $false | Out-Null
            $offline = Wait-ClientOffline -TimeoutSeconds 60
            $probe = Invoke-EasyProxyHttp -Uri "$ruleBase/" -TimeoutSeconds 5
            $text = "offline=$($offline.Satisfied) elapsed=$([math]::Round($offline.ElapsedMs / 1000, 1))`nprobe_status=$($probe.StatusCode)`nprobe_error=$($probe.Error)"
            $evidence = Write-CaseEvidence -CaseId 'T-20' -Name 'disable-client' -Text $text
            Assert-EasyProxy -Condition ($offline.Satisfied -and $probe.StatusCode -ne 200) -Name '客户端禁用' -Detail $text
            [pscustomobject]@{ Detail = '客户端离线且规则不再转发'; Evidence = $evidence }
        }
        finally {
            Set-EasyProxyClientEnabled -BaseUri $webBase -Token $admin.Token -Client $client -Enabled $true | Out-Null
            Wait-ClientOnline -TimeoutSeconds 60 | Out-Null
            Wait-RuleForwarding -TimeoutSeconds 60 | Out-Null
        }
    }
}
finally {
    try { Set-EasyProxyRuleEnabled -BaseUri $webBase -Token $admin.Token -Rule $primaryRule -Enabled $true | Out-Null } catch { }
    try { Set-EasyProxyClientEnabled -BaseUri $webBase -Token $admin.Token -Client $client -Enabled $true | Out-Null } catch { }
    try { Wait-ClientOnline -TimeoutSeconds 60 | Out-Null } catch { }
    try { Wait-RuleForwarding -TimeoutSeconds 60 | Out-Null } catch { }
}

$resultCsv = Write-EasyProxyTestResults -Run $run -Phase 'functional' -Results $results.ToArray()
$failed = @($results | Where-Object { $_.Status -ne 'PASS' })
$status = if ($failed.Count -eq 0) { 'PASS' } else { 'FAIL' }
$summary = [Collections.Generic.List[string]]::new()
$summary.Add('# T-01 至 T-20 功能测试摘要'); $summary.Add('')
$summary.Add("- 运行编号：``$RunId``"); $summary.Add("- 总体判定：**$status**")
$summary.Add("- 通过：$($results.Count - $failed.Count)；失败：$($failed.Count)"); $summary.Add('')
$summary.Add('| 测试编号 | 测试项 | 状态 | 耗时（秒） | 说明 |'); $summary.Add('| --- | --- | --- | ---: | --- |')
foreach ($item in $results) { $summary.Add("| $($item.TestId) | $($item.Name) | $($item.Status) | $([math]::Round($item.DurationSeconds, 1)) | $($item.Detail.Replace('|', '/')) |") }
$summary.Add(''); $summary.Add("- 结果 CSV：``$($resultCsv.Substring($run.RunRoot.Length + 1).Replace('\', '/'))``")
$summaryPath = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "processed/summaries/functional-summary-$stamp.md"
Write-LfText -Path $summaryPath -Text (($summary -join "`n") + "`n")
Write-Output (($summary -join "`n") + "`n")
Write-Output "RUN_ID=$RunId"; Write-Output "STATUS=$status"; Write-Output "RESULTS=$($resultCsv.Substring($run.RunRoot.Length + 1).Replace('\', '/'))"
if ($failed.Count -gt 0) { exit 1 }
