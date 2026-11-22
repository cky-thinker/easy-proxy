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
    [int]$WebPort = 21092,
    [int]$RulePort = 28080,
    [int]$DurationSeconds = 300,
    [int]$InjectAtSeconds = 120,
    [int]$NetworkBlockSeconds = 30,
    [switch]$Quick,
    [switch]$StopOnFailure
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
$testsRoot = Join-Path $repoRoot 'docs\tests'
Import-Module -Name (Join-Path $testsRoot 'scripts\common\EasyProxy.Testing.psm1') -Force
Import-Module -Name (Join-Path $testsRoot 'scripts\common\EasyProxy.Secrets.psm1') -Force
$run = New-EasyProxyRun -RunId $RunId -TestScope 'T-31 至 T-36 故障注入、恢复与容器健康' -ReuseExisting:$ReuseExisting
$secret = Read-EasyProxySecretMaterial -RunId $RunId
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$webBase = "http://$PublicHost`:$WebPort"
$ruleBase = "http://$PublicHost`:$RulePort"
$fixtureManifest = Join-Path $testsRoot 'data\manifests\fixtures.csv'
$project = Join-Path $testsRoot 'tools\EasyProxy.LoadTester\EasyProxy.LoadTester.csproj'
$dll = Join-Path $testsRoot 'tools\EasyProxy.LoadTester\bin\Release\net8.0\EasyProxy.LoadTester.dll'
$rawLoad = Join-Path $run.RunRoot 'raw\load'
$rawDocker = Join-Path $run.RunRoot 'raw\docker'
$results = [Collections.Generic.List[object]]::new()
if ($Quick) { $DurationSeconds = 20; $InjectAtSeconds = 5; $NetworkBlockSeconds = 3 }
if ($DurationSeconds -lt 10 -or $InjectAtSeconds -lt 1 -or $InjectAtSeconds -ge ($DurationSeconds - 2)) { throw '故障注入时间参数不合法' }

function Invoke-Cloud { param([string]$Script, [int]$TimeoutSeconds = 120) Invoke-SshBash -HostName $CloudHost -Port $CloudPort -User $CloudUser -Script $Script -TimeoutSeconds $TimeoutSeconds }
function Invoke-Nas { param([string]$Script, [int]$TimeoutSeconds = 120) Invoke-SshBash -HostName $NasHost -Port $NasPort -User $NasUser -Script $Script -TimeoutSeconds $TimeoutSeconds }
function Wait-ClientOnline {
    param([int]$TimeoutSeconds = 60)
    Wait-EasyProxyCondition -Name '客户端在线' -TimeoutSeconds $TimeoutSeconds -IntervalMilliseconds 2000 -Condition {
        $items = @(Get-EasyProxyClients -BaseUri $webBase -Token $admin.Token)
        $item = $items | Where-Object { $_.name -eq 'nas-test' } | Select-Object -First 1
        $null -ne $item -and [string]$item.status -eq 'online' -and [bool]$item.enableFlag
    }
}
function Wait-RuleForwarding {
    param([int]$TimeoutSeconds = 60)
    Wait-EasyProxyCondition -Name '数据面恢复' -TimeoutSeconds $TimeoutSeconds -IntervalMilliseconds 1000 -Condition {
        $response = Invoke-EasyProxyHttp -Uri "$ruleBase/" -TimeoutSeconds 5
        $response.StatusCode -eq 200 -and $response.Content -match 'Easy Proxy NAS target OK'
    }
}
function Wait-WebHealth {
    param([int]$TimeoutSeconds = 30)
    Wait-EasyProxyCondition -Name 'Web 健康恢复' -TimeoutSeconds $TimeoutSeconds -IntervalMilliseconds 1000 -Condition {
        $r = Invoke-EasyProxyHttp -Uri "$webBase/api/open/health" -TimeoutSeconds 5
        $r.StatusCode -eq 200 -and $r.Content -match '"status"\s*:\s*"UP"'
    }
}
function Add-FaultResult {
    param([string]$CaseId, [string]$Name, [DateTimeOffset]$StartedAt, [DateTimeOffset]$EndedAt, [bool]$Passed, [string]$Detail, [string]$Evidence)
    $result = [pscustomobject]@{
        TestId = $CaseId; Name = $Name; Status = $(if ($Passed) { 'PASS' } else { 'FAIL' })
        StartedAt = $StartedAt.ToString('o'); EndedAt = $EndedAt.ToString('o')
        DurationSeconds = ($EndedAt - $StartedAt).TotalSeconds; Detail = $Detail; Evidence = $Evidence
    }
    $results.Add($result)
    Write-Host "[$($result.Status)] $CaseId $Name :: $Detail"
    if ($StopOnFailure -and -not $Passed) { throw "$CaseId 失败：$Detail" }
    return $result
}
function Start-LoadProcess {
    param([string]$CaseId, [string]$Label, [Nullable[double]]$MinSuccessRate = $null)
    $prefix = "${CaseId}_${Label}_$stamp"
    $args = @($dll, '--base-url', $ruleBase, '--manifest', $fixtureManifest, '--scenario', 'small', '--concurrency', '100', '--warmup-seconds', '10', '--duration-seconds', [string]$DurationSeconds, '--cooldown-seconds', '20', '--connections', 'keep-alive', '--request-timeout-seconds', '10', '--health-url', "$ruleBase/fixtures/small-1k.bin", '--health-interval-seconds', '1', '--health-stop-seconds', '30', '--output-dir', $rawLoad, '--file-prefix', $prefix, '--no-fail-on-5xx')
    if ($null -ne $MinSuccessRate) { $args += @('--min-success-rate', ([double]$MinSuccessRate).ToString([Globalization.CultureInfo]::InvariantCulture)) }
    $stdout = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/load/$prefix-stdout.txt"
    $stderr = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/load/$prefix-stderr.txt"
    $process = Start-Process -FilePath 'dotnet' -ArgumentList $args -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    return [pscustomobject]@{ CaseId = $CaseId; Label = $Label; Prefix = $prefix; Process = $process; Stdout = $stdout; Stderr = $stderr }
}
function Complete-LoadProcess {
    param([object]$Load, [DateTimeOffset]$StartedAt, [int]$TimeoutSeconds = 420)
    if (-not $Load.Process.WaitForExit($TimeoutSeconds * 1000)) {
        try { $Load.Process.Kill($true) } catch { }
        throw "$($Load.CaseId) 负载进程超时"
    }
    $endedAt = [DateTimeOffset]::UtcNow
    $summaryPath = Join-Path $rawLoad "$($Load.Prefix)-summary.json"
    $summary = $null
    if (Test-Path -LiteralPath $summaryPath -PathType Leaf) { $summary = [IO.File]::ReadAllText($summaryPath) | ConvertFrom-Json -Depth 30 }
    $consolePath = Join-Path $rawLoad "$($Load.Prefix)-console.txt"
    $stdout = if (Test-Path -LiteralPath $Load.Stdout) { [IO.File]::ReadAllText($Load.Stdout) } else { '' }
    $stderr = if (Test-Path -LiteralPath $Load.Stderr) { [IO.File]::ReadAllText($Load.Stderr) } else { '' }
    Write-EasyProxyEvidence -Path $consolePath -Text (@("exit_code=$($Load.Process.ExitCode)", '--- stdout ---', $stdout.TrimEnd(), '--- stderr ---', $stderr.TrimEnd()) -join "`n") 
    $measuredFailures = 0
    $server5xx = 0
    if ($null -ne $summary) {
        $measuredFailures = [int]$summary.measured.totalRequests - [int]$summary.measured.successfulRequests
        $server5xx = [int]$summary.measured.server5xx
    }
    return [pscustomobject]@{
        StartedAt = $StartedAt; EndedAt = $endedAt; ExitCode = $Load.Process.ExitCode
        Summary = $summary; MeasuredFailures = $measuredFailures; Server5xx = $server5xx
        Evidence = $consolePath.Substring($run.RunRoot.Length + 1).Replace('\', '/')
    }
}
function Get-ContainerInspect {
    param([ValidateSet('Cloud','Nas')][string]$Target, [string]$Container)
    $docker = if ($Target -eq 'Cloud') { '/usr/bin/docker' } else { '/usr/local/bin/docker' }
    $script = "if $docker inspect '$Container' >/dev/null 2>&1; then $docker inspect '$Container' --format '{{.State.Status}}|{{.RestartCount}}|{{.State.OOMKilled}}|{{.State.ExitCode}}'; else echo missing; fi"
    if ($Target -eq 'Cloud') { return (Invoke-Cloud -Script $script -TimeoutSeconds 30).Combined.Trim() }
    return (Invoke-Nas -Script $script -TimeoutSeconds 30).Combined.Trim()
}
function Restart-Container {
    param([ValidateSet('Cloud','Nas')][string]$Target, [string]$Container, [int]$TimeoutSeconds = 90)
    if ($Target -eq 'Cloud') { return Invoke-Cloud -Script "/usr/bin/docker restart '$Container' >/dev/null && echo RESTARTED=$Container" -TimeoutSeconds $TimeoutSeconds }
    return Invoke-Nas -Script "/usr/local/bin/docker restart '$Container' >/dev/null && echo RESTARTED=$Container" -TimeoutSeconds $TimeoutSeconds
}

$admin = Initialize-EasyProxyAdmin -BaseUri $webBase -Username ([string]$secret.AdminUsername) -Password ([string]$secret.AdminPassword)
$beforeInspect = [ordered]@{
    'easy-proxy-server' = Get-ContainerInspect -Target Cloud -Container 'easy-proxy-server'
    'easy-proxy-web' = Get-ContainerInspect -Target Cloud -Container 'easy-proxy-web'
    'easy-proxy-client' = Get-ContainerInspect -Target Nas -Container 'easy-proxy-client'
    'easy-proxy-target' = Get-ContainerInspect -Target Nas -Container 'easy-proxy-target'
}
$inspectBeforePath = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/docker/T-36_inspect-before_$stamp.txt"
Write-EasyProxyEvidence -Path $inspectBeforePath -Text (($beforeInspect.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join "`n") -Append

$stopFile = Join-Path $run.RunRoot "tmp\fault-monitor-stop-$stamp"
$monitorStdout = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/logs/fault-monitor-stdout-$stamp.log"
$monitorStderr = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/logs/fault-monitor-stderr-$stamp.log"
$monitorScript = Join-Path $testsRoot 'scripts\monitoring\Start-EasyProxyMonitor.ps1'
$monitorProcess = Start-Process -FilePath ((Get-Command pwsh).Source) -ArgumentList @('-NoProfile', '-File', $monitorScript, '-RunId', $RunId, '-ReuseExisting', '-IntervalSeconds', '5', '-StopFile', $stopFile) -WindowStyle Hidden -RedirectStandardOutput $monitorStdout -RedirectStandardError $monitorStderr -PassThru
Start-Sleep -Seconds 3

try {
    $caseStart = [DateTimeOffset]::UtcNow
    $load = Start-LoadProcess -CaseId 'T-31' -Label 'restart-client'
    Start-Sleep -Seconds $InjectAtSeconds
    $restart = Restart-Container -Target Nas -Container 'easy-proxy-client'
    $clientOnline = Wait-ClientOnline -TimeoutSeconds 30
    $forwarding = Wait-RuleForwarding -TimeoutSeconds 60
    $completed = Complete-LoadProcess -Load $load -StartedAt $caseStart
    $passed = $restart.ExitCode -eq 0 -and $clientOnline.Satisfied -and $forwarding.Satisfied -and $completed.MeasuredFailures -gt 0 -and $null -ne $completed.Summary -and [int]$completed.Summary.measured.hashMismatches -eq 0
    Add-FaultResult -CaseId 'T-31' -Name '压测中重启客户端' -StartedAt $caseStart -EndedAt $completed.EndedAt -Passed $passed -Detail "failures=$($completed.MeasuredFailures) 5xx=$($completed.Server5xx) online=$($clientOnline.Satisfied) onlineWait=$([math]::Round($clientOnline.ElapsedMs/1000,1))s forwarding=$($forwarding.Satisfied) hashMismatch=$($completed.Summary.measured.hashMismatches)" -Evidence $completed.Evidence | Out-Null

    $caseStart = [DateTimeOffset]::UtcNow
    $load = Start-LoadProcess -CaseId 'T-32' -Label 'restart-server'
    Start-Sleep -Seconds $InjectAtSeconds
    $restart = Restart-Container -Target Cloud -Container 'easy-proxy-server'
    $clientOnline = Wait-ClientOnline -TimeoutSeconds 60
    $forwarding = Wait-RuleForwarding -TimeoutSeconds 60
    $completed = Complete-LoadProcess -Load $load -StartedAt $caseStart
    $passed = $restart.ExitCode -eq 0 -and $clientOnline.Satisfied -and $forwarding.Satisfied -and $completed.MeasuredFailures -gt 0 -and $null -ne $completed.Summary -and [int]$completed.Summary.measured.hashMismatches -eq 0
    Add-FaultResult -CaseId 'T-32' -Name '压测中重启服务端' -StartedAt $caseStart -EndedAt $completed.EndedAt -Passed $passed -Detail "failures=$($completed.MeasuredFailures) 5xx=$($completed.Server5xx) online=$($clientOnline.Satisfied) forwarding=$($forwarding.Satisfied) hashMismatch=$($completed.Summary.measured.hashMismatches)" -Evidence $completed.Evidence | Out-Null

    $caseStart = [DateTimeOffset]::UtcNow
    $load = Start-LoadProcess -CaseId 'T-33' -Label 'restart-web' -MinSuccessRate 0.995
    Start-Sleep -Seconds $InjectAtSeconds
    $restart = Restart-Container -Target Cloud -Container 'easy-proxy-web'
    $webHealth = Wait-WebHealth -TimeoutSeconds 30
    $completed = Complete-LoadProcess -Load $load -StartedAt $caseStart
    $successRate = if ($null -ne $completed.Summary) { [double]$completed.Summary.measured.successRate } else { 0 }
    $passed = $restart.ExitCode -eq 0 -and $webHealth.Satisfied -and $successRate -ge 0.995 -and $null -ne $completed.Summary -and [int]$completed.Summary.measured.hashMismatches -eq 0
    Add-FaultResult -CaseId 'T-33' -Name '压测中重启 Web' -StartedAt $caseStart -EndedAt $completed.EndedAt -Passed $passed -Detail "success=$([math]::Round($successRate*100,3))% webHealth=$($webHealth.Satisfied) webWait=$([math]::Round($webHealth.ElapsedMs/1000,1))s hashMismatch=$($completed.Summary.measured.hashMismatches)" -Evidence $completed.Evidence | Out-Null

    $caseStart = [DateTimeOffset]::UtcNow
    $load = Start-LoadProcess -CaseId 'T-34' -Label 'restart-target'
    $clientBefore = Get-ContainerInspect -Target Nas -Container 'easy-proxy-client'
    $serverBefore = Get-ContainerInspect -Target Cloud -Container 'easy-proxy-server'
    Start-Sleep -Seconds $InjectAtSeconds
    $restart = Restart-Container -Target Nas -Container 'easy-proxy-target'
    $forwarding = Wait-RuleForwarding -TimeoutSeconds 30
    $completed = Complete-LoadProcess -Load $load -StartedAt $caseStart
    $clientAfter = Get-ContainerInspect -Target Nas -Container 'easy-proxy-client'
    $serverAfter = Get-ContainerInspect -Target Cloud -Container 'easy-proxy-server'
    $passed = $restart.ExitCode -eq 0 -and $forwarding.Satisfied -and $clientBefore -eq $clientAfter -and $serverBefore -eq $serverAfter -and $completed.MeasuredFailures -gt 0 -and $null -ne $completed.Summary -and [int]$completed.Summary.measured.hashMismatches -eq 0
    Add-FaultResult -CaseId 'T-34' -Name '压测中重启目标服务' -StartedAt $caseStart -EndedAt $completed.EndedAt -Passed $passed -Detail "failures=$($completed.MeasuredFailures) forwarding=$($forwarding.Satisfied) clientInspectUnchanged=$($clientBefore -eq $clientAfter) serverInspectUnchanged=$($serverBefore -eq $serverAfter) hashMismatch=$($completed.Summary.measured.hashMismatches)" -Evidence $completed.Evidence | Out-Null

    $caseStart = [DateTimeOffset]::UtcNow
    $load = Start-LoadProcess -CaseId 'T-35' -Label 'network-block'
    Start-Sleep -Seconds $InjectAtSeconds
    $network = Invoke-Cloud -Script @"
set -eu
cleanup() { sudo -n iptables -D INPUT -p tcp --dport 21090 -j DROP >/dev/null 2>&1 || true; }
trap cleanup EXIT INT TERM
sudo -n iptables -I INPUT 1 -p tcp --dport 21090 -j DROP
echo NETWORK_BLOCKED=21090
sleep $NetworkBlockSeconds
cleanup
trap - EXIT INT TERM
echo NETWORK_RESTORED=21090
"@ -TimeoutSeconds ($NetworkBlockSeconds + 60)
    $clientOnline = Wait-ClientOnline -TimeoutSeconds 60
    $forwarding = Wait-RuleForwarding -TimeoutSeconds 60
    $completed = Complete-LoadProcess -Load $load -StartedAt $caseStart
    $passed = $network.ExitCode -eq 0 -and $network.Combined -match 'NETWORK_RESTORED=21090' -and $clientOnline.Satisfied -and $forwarding.Satisfied -and $completed.MeasuredFailures -gt 0 -and $null -ne $completed.Summary -and [int]$completed.Summary.measured.hashMismatches -eq 0
    Add-FaultResult -CaseId 'T-35' -Name '网络中断与自动重连' -StartedAt $caseStart -EndedAt $completed.EndedAt -Passed $passed -Detail "networkRestore=$($network.Combined -match 'NETWORK_RESTORED=21090') failures=$($completed.MeasuredFailures) online=$($clientOnline.Satisfied) forwarding=$($forwarding.Satisfied) hashMismatch=$($completed.Summary.measured.hashMismatches)" -Evidence $completed.Evidence | Out-Null
}
finally {
    try { Invoke-Cloud -Script 'sudo -n iptables -D INPUT -p tcp --dport 21090 -j DROP >/dev/null 2>&1 || true' -TimeoutSeconds 30 | Out-Null } catch { }
    Write-LfText -Path $stopFile -Text "stop`n"
    if (-not $monitorProcess.WaitForExit(60000)) { try { $monitorProcess.Kill($true) } catch { } }
}
Start-Sleep -Seconds 5

$inspect = [ordered]@{
    'easy-proxy-server' = Get-ContainerInspect -Target Cloud -Container 'easy-proxy-server'
    'easy-proxy-web' = Get-ContainerInspect -Target Cloud -Container 'easy-proxy-web'
    'easy-proxy-client' = Get-ContainerInspect -Target Nas -Container 'easy-proxy-client'
    'easy-proxy-target' = Get-ContainerInspect -Target Nas -Container 'easy-proxy-target'
}
$cloudLogs = Invoke-Cloud -Script '/usr/bin/docker logs --tail=1000 easy-proxy-server 2>&1; /usr/bin/docker logs --tail=1000 easy-proxy-web 2>&1' -TimeoutSeconds 60
$nasLogs = Invoke-Nas -Script '/usr/local/bin/docker logs --tail=1000 easy-proxy-client 2>&1; /usr/local/bin/docker logs --tail=1000 easy-proxy-target 2>&1' -TimeoutSeconds 60
$inspectAfterPath = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/docker/T-36_inspect-after_$stamp.txt"
$logPath = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/logs/T-36_containers_$stamp.log"
Write-EasyProxyEvidence -Path $inspectAfterPath -Text (($inspect.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join "`n") -Append
Write-EasyProxyEvidence -Path $logPath -Text ("--- cloud ---`n$($cloudLogs.Combined)`n--- nas ---`n$($nasLogs.Combined)") -Append
$allLogs = "$($cloudLogs.Combined)`n$($nasLogs.Combined)"
$fatalPattern = '(?i)(outofmemory|out of memory|java\.lang\.OutOfMemoryError|panic:|fatal error|segmentation fault)'
$fatalMatches = [regex]::Matches($allLogs, $fatalPattern).Count
$oomCount = @($inspect.Values | Where-Object { $_ -match '\|(true)\|' }).Count
$missingCount = @($inspect.Values | Where-Object { $_ -eq 'missing' }).Count
$t36Start = [DateTimeOffset]::UtcNow
$t36Passed = $fatalMatches -eq 0 -and $oomCount -eq 0 -and $missingCount -eq 0
Add-FaultResult -CaseId 'T-36' -Name '日志、队列与容器健康' -StartedAt $t36Start -EndedAt ([DateTimeOffset]::UtcNow) -Passed $t36Passed -Detail "fatalMatches=$fatalMatches oom=$oomCount missing=$missingCount containers=$($inspect.Count) logEvidence=raw/logs/T-36_containers_$stamp.log" -Evidence "raw/logs/T-36_containers_$stamp.log" | Out-Null

$resultCsv = Write-EasyProxyTestResults -Run $run -Phase 'fault-injection' -Results $results.ToArray()
$failed = @($results | Where-Object { $_.Status -ne 'PASS' })
$status = if ($failed.Count -eq 0) { 'PASS' } else { 'FAIL' }
$summary = [Collections.Generic.List[string]]::new()
$summary.Add('# T-31 至 T-36 故障注入与恢复摘要'); $summary.Add('')
$summary.Add("- 运行编号：``$RunId``"); $summary.Add("- 总体判定：**$status**")
$summary.Add("- 通过：$($results.Count - $failed.Count)；失败：$($failed.Count)"); $summary.Add('')
$summary.Add('| 测试编号 | 测试项 | 状态 | 耗时（秒） | 说明 |'); $summary.Add('| --- | --- | --- | ---: | --- |')
foreach ($item in $results) { $summary.Add("| $($item.TestId) | $($item.Name) | $($item.Status) | $([math]::Round($item.DurationSeconds, 1)) | $($item.Detail.Replace('|', '/')) |") }
$summary.Add(''); $summary.Add("- 结果 CSV：``$($resultCsv.Substring($run.RunRoot.Length + 1).Replace('\', '/'))``")
$summaryPath = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "processed/summaries/fault-injection-summary-$stamp.md"
Write-LfText -Path $summaryPath -Text (($summary -join "`n") + "`n")
Write-Output (($summary -join "`n") + "`n")
Write-Output "RUN_ID=$RunId"; Write-Output "STATUS=$status"; Write-Output "RESULTS=$($resultCsv.Substring($run.RunRoot.Length + 1).Replace('\', '/'))"
if ($failed.Count -gt 0) { exit 1 }
