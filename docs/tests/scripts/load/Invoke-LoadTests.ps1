[CmdletBinding()]
param(
    [string]$RunId = (Get-Date -Format 'yyyyMMdd-HHmmss'),
    [switch]$ReuseExisting,
    [string]$PublicHost = '68.64.181.242',
    [int]$RulePort = 28080,
    [int]$WebPort = 21092,
    [int]$WarmupSeconds = 120,
    [int]$MeasuredSeconds = 300,
    [int]$CooldownSeconds = 120,
    [int]$SoakSeconds = 3600,
    [int]$HighLoadSeconds = 600,
    [int]$SaturationSeconds = 240,
    [switch]$Quick,
    [switch]$StopOnFailure
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
$testsRoot = Join-Path $repoRoot 'docs\tests'
Import-Module -Name (Join-Path $testsRoot 'scripts\common\EasyProxy.Testing.psm1') -Force
$run = New-EasyProxyRun -RunId $RunId -TestScope 'T-21 至 T-30 高并发、高负载与稳定性' -ReuseExisting:$ReuseExisting
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$ruleBase = "http://$PublicHost`:$RulePort"
$webBase = "http://$PublicHost`:$WebPort"
$fixtureManifest = Join-Path $testsRoot 'data\manifests\fixtures.csv'
$project = Join-Path $testsRoot 'tools\EasyProxy.LoadTester\EasyProxy.LoadTester.csproj'
$dll = Join-Path $testsRoot 'tools\EasyProxy.LoadTester\bin\Release\net8.0\EasyProxy.LoadTester.dll'
$rawLoad = Join-Path $run.RunRoot 'raw\load'
$results = [Collections.Generic.List[object]]::new()

if ($Quick) {
    $WarmupSeconds = 2
    $MeasuredSeconds = 5
    $CooldownSeconds = 2
    $SoakSeconds = 10
    $HighLoadSeconds = 10
    $SaturationSeconds = 5
}
if ($WarmupSeconds -lt 0 -or $MeasuredSeconds -lt 1 -or $CooldownSeconds -lt 0 -or $SoakSeconds -lt 1) {
    throw '负载测试时长参数不合法'
}
$caseWarmup = if ($Quick) { 2 } else { 60 }
$caseCooldown = if ($Quick) { 2 } else { 60 }
$distinctDuration = if ($Quick) { 20 } else { 300 }
$distinctTotal = if ($Quick) { 100 } else { 1000 }
$longDuration = if ($Quick) { 10 } else { 300 }
$mixedDuration = if ($Quick) { 10 } else { 600 }
$resourceWarmup = if ($Quick) { 2 } else { 120 }
$resourceDuration = if ($Quick) { 15 } else { 900 }
$resourceCooldown = if ($Quick) { 2 } else { 120 }
$soakWarmup = if ($Quick) { 2 } else { 120 }
$soakCooldown = if ($Quick) { 2 } else { 120 }
$t30Warmup = if ($Quick) { 2 } else { 30 }
$t30Duration = if ($Quick) { 10 } else { 180 }

function Add-LoadResult {
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

function Invoke-LoadCase {
    param(
        [string]$CaseId,
        [string]$Label,
        [string]$Scenario,
        [int]$Concurrency,
        [int]$Warmup = $WarmupSeconds,
        [int]$Duration = $MeasuredSeconds,
        [int]$Cooldown = $CooldownSeconds,
        [string]$Connections = 'keep-alive',
        [long]$TotalRequests = 0,
        [int]$TargetRps = 0,
        [Nullable[double]]$MinSuccessRate = $null,
        [Nullable[double]]$MaxP95Ms = $null,
        [Nullable[double]]$MaxP99Ms = $null,
        [Nullable[double]]$MinHealthRate = $null,
        [int]$Expected5xx = 0,
        [int]$RequestTimeoutSeconds = 30,
        [switch]$SkipResult
    )
    $prefix = "${CaseId}_${Label}_$stamp"
    foreach ($suffix in @('-summary.json', '-requests.csv', '-health.csv', '-console.txt')) {
        $candidate = Join-Path $rawLoad "$prefix$suffix"
        if (Test-Path -LiteralPath $candidate) { throw "负载证据已存在，禁止覆盖：$candidate" }
    }
    $arguments = @(
        $dll, '--base-url', $ruleBase, '--manifest', $fixtureManifest, '--scenario', $Scenario,
        '--concurrency', [string]$Concurrency, '--warmup-seconds', [string]$Warmup,
        '--duration-seconds', [string]$Duration, '--cooldown-seconds', [string]$Cooldown,
        '--connections', $Connections, '--request-timeout-seconds', [string]$RequestTimeoutSeconds,
        '--health-url', "$ruleBase/fixtures/small-1k.bin", '--health-interval-seconds', '1',
        '--health-stop-seconds', '30', '--output-dir', $rawLoad, '--file-prefix', $prefix,
        '--expected-5xx', [string]$Expected5xx
    )
    if ($TargetRps -gt 0) { $arguments += @('--target-rps', [string]$TargetRps) }
    if ($TotalRequests -gt 0) { $arguments += @('--total-requests', [string]$TotalRequests) }
    if ($null -ne $MinSuccessRate) { $arguments += @('--min-success-rate', ([double]$MinSuccessRate).ToString([Globalization.CultureInfo]::InvariantCulture)) }
    if ($null -ne $MaxP95Ms) { $arguments += @('--max-p95-ms', ([double]$MaxP95Ms).ToString([Globalization.CultureInfo]::InvariantCulture)) }
    if ($null -ne $MaxP99Ms) { $arguments += @('--max-p99-ms', ([double]$MaxP99Ms).ToString([Globalization.CultureInfo]::InvariantCulture)) }
    if ($null -ne $MinHealthRate) { $arguments += @('--min-health-rate', ([double]$MinHealthRate).ToString([Globalization.CultureInfo]::InvariantCulture)) }

    $startedAt = [DateTimeOffset]::UtcNow
    $process = Invoke-EasyProxyProcess -FilePath 'dotnet' -ArgumentList $arguments -TimeoutSeconds ($Warmup + $Duration + $Cooldown + 180)
    $endedAt = [DateTimeOffset]::UtcNow
    $consolePath = Join-Path $rawLoad "$prefix-console.txt"
    $consoleText = @(
        "command_started_utc=$($process.StartedAtUtc.ToString('o'))"
        "command_finished_utc=$($process.FinishedAtUtc.ToString('o'))"
        "exit_code=$($process.ExitCode)"
        '--- output ---'
        $process.Combined.TrimEnd()
    ) -join "`n"
    Write-EasyProxyEvidence -Path $consolePath -Text ($consoleText + "`n")
    $evidence = $consolePath.Substring($run.RunRoot.Length + 1).Replace('\', '/')
    $summaryPath = Join-Path $rawLoad "$prefix-summary.json"
    $passed = $process.ExitCode -eq 0
    $detail = "exit=$($process.ExitCode)"
    if (Test-Path -LiteralPath $summaryPath -PathType Leaf) {
        $summary = [IO.File]::ReadAllText($summaryPath) | ConvertFrom-Json -Depth 30
        $passed = $passed -and [bool]$summary.passed
        $detail = "success=$([math]::Round([double]$summary.measured.successRate * 100, 3))% rps=$($summary.measured.requestsPerSecond) p95=$($summary.measured.latencyMs.p95)ms p99=$($summary.measured.latencyMs.p99)ms 5xx=$($summary.measured.server5xx) hashErrors=$($summary.measured.hashMismatches) health=$([math]::Round([double]$summary.health.availability * 100, 3))%"
        if (@($summary.thresholdFailures).Count -gt 0) { $detail += " failures=$(@($summary.thresholdFailures) -join '; ')" }
    }
    if ($SkipResult) {
        return [pscustomobject]@{ TestId = $CaseId; Name = $Label; Status = $(if ($passed) { 'PASS' } else { 'FAIL' }); Detail = $detail; Evidence = $evidence }
    }
    return Add-LoadResult -CaseId $CaseId -Name "$Label" -StartedAt $startedAt -EndedAt $endedAt -Passed $passed -Detail $detail -Evidence $evidence
}

function Test-RecoveryWindow {
    param([int]$Seconds = 120)
    $samples = [Collections.Generic.List[object]]::new()
    $startedAt = [DateTimeOffset]::UtcNow
    for ($index = 0; $index -lt $Seconds; $index++) {
        $response = Invoke-EasyProxyHttp -Uri "$ruleBase/fixtures/small-1k.bin" -TimeoutSeconds 5
        $samples.Add([pscustomobject]@{ Offset = $index; Status = $response.StatusCode; ElapsedMs = $response.ElapsedMs })
        Start-Sleep -Seconds 1
    }
    $secondHalf = @($samples | Where-Object { $_.Offset -ge [math]::Floor($Seconds / 2) })
    $secondHalfPass = @($secondHalf | Where-Object { $_.Status -eq 200 }).Count -eq $secondHalf.Count
    $firstHalf = @($samples | Where-Object { $_.Offset -lt [math]::Floor($Seconds / 2) })
    $firstFailures = @($firstHalf | Where-Object { $_.Status -ne 200 }).Count
    return [pscustomobject]@{
        Passed = $secondHalfPass
        Samples = $samples.Count
        SecondHalfSuccess = $secondHalfPass
        FirstHalfFailures = $firstFailures
        StartedAt = $startedAt
        EndedAt = [DateTimeOffset]::UtcNow
    }
}

function Get-MonitorMetricsInWindow {
    param([DateTimeOffset]$Start, [DateTimeOffset]$End)
    $rows = [Collections.Generic.List[object]]::new()
    foreach ($file in @(Get-ChildItem -LiteralPath (Join-Path $run.RunRoot 'raw\metrics') -Filter '*.csv' -File -ErrorAction SilentlyContinue)) {
        foreach ($row in @(Import-Csv -LiteralPath $file.FullName)) {
            try {
                $at = [DateTimeOffset]::Parse([string]$row.timestampUtc, [Globalization.CultureInfo]::InvariantCulture)
                if ($at -ge $Start -and $at -le $End) { $rows.Add($row) }
            }
            catch { }
        }
    }
    return $rows.ToArray()
}

function Test-ResourceThresholds {
    param([DateTimeOffset]$Start, [DateTimeOffset]$End)
    $rows = @(Get-MonitorMetricsInWindow -Start $Start -End $End)
    $containerRows = @($rows | Where-Object { [string]$_.metric -match '^container\.(easy-proxy-(server|client|web|target))\.' })
    if ($containerRows.Count -eq 0) { return [pscustomobject]@{ Passed = $false; Detail = '未找到容器资源样本' } }
    $cpuMax = 0.0
    foreach ($row in @($containerRows | Where-Object { $_.metric -match '\.stats$' })) {
        if ([string]$row.value -match '^([0-9.]+)%') {
            $value = [double]$Matches[1]
            if ($value -gt $cpuMax) { $cpuMax = $value }
        }
    }
    $rssGrowth = [Collections.Generic.List[string]]::new()
    $growthFailed = $false
    foreach ($group in @($containerRows | Where-Object { $_.metric -match '\.proc\.VmRSS$' } | Group-Object metric)) {
        $values = @($group.Group | ForEach-Object {
            if ([string]$_.value -match '([0-9]+)\s*kB') { [double]$Matches[1] } else { $null }
        } | Where-Object { $null -ne $_ })
        if ($values.Count -ge 10) {
            $first = ($values[0..([math]::Max(0, [math]::Floor($values.Count / 10) - 1))] | Measure-Object -Average).Average
            $last = ($values[([math]::Floor($values.Count * 0.9))..($values.Count - 1)] | Measure-Object -Average).Average
            if ($first -gt 0) {
                $growth = (($last - $first) / $first) * 100
                $rssGrowth.Add("$($group.Name)=$([math]::Round($growth, 2))%")
                if ($growth -gt 10) { $growthFailed = $true }
            }
        }
    }
    $restartIssues = @($containerRows | Where-Object {
        if ($_.metric -match '\.inspect$' -and [string]$_.value -match '\|([0-9]+)\|(true|false)\|') {
            [int]$Matches[1] -gt 0 -or $Matches[2] -eq 'true'
        } else { $false }
    })
    $fdFailed = $false
    foreach ($group in @($containerRows | Where-Object { $_.metric -match '\.proc\.fd$' } | Group-Object metric)) {
        $values = @($group.Group | ForEach-Object { [int]$_.value })
        if ($values.Count -ge 10) {
            $first = ($values[0..([math]::Max(0, [math]::Floor($values.Count / 10) - 1))] | Measure-Object -Average).Average
            $last = ($values[([math]::Floor($values.Count * 0.9))..($values.Count - 1)] | Measure-Object -Average).Average
            if ($last -gt ($first * 1.2 + 5)) { $fdFailed = $true }
        }
    }
    $passed = $cpuMax -lt 80 -and -not $growthFailed -and -not $fdFailed -and $restartIssues.Count -eq 0
    $detail = "containerSamples=$($containerRows.Count) maxCpu=$([math]::Round($cpuMax, 2))% memoryGrowth=[$($rssGrowth -join ', ')] fdGrowth=$fdFailed restartOomIssues=$($restartIssues.Count)"
    return [pscustomobject]@{ Passed = $passed; Detail = $detail }
}

$buildEvidencePath = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/validation/load-tester-build-$stamp.txt"
$build = Invoke-EasyProxyProcess -FilePath 'dotnet' -ArgumentList @('build', $project, '-c', 'Release', '--nologo') -TimeoutSeconds 180
Write-EasyProxyEvidence -Path $buildEvidencePath -Text (@(
    "exit_code=$($build.ExitCode)"
    '--- output ---'
    $build.Combined.TrimEnd()
) -join "`n") -Append
if ($build.ExitCode -ne 0) { throw "LoadTester 编译失败，exit=$($build.ExitCode)" }

$stopFile = Join-Path $run.RunRoot "tmp\monitor-stop-$stamp"
$monitorStdout = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/logs/monitor-stdout-$stamp.log"
$monitorStderr = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/logs/monitor-stderr-$stamp.log"
$monitorScript = Join-Path $testsRoot 'scripts\monitoring\Start-EasyProxyMonitor.ps1'
$pwshPath = (Get-Command pwsh -ErrorAction Stop).Source
$monitorArguments = @('-NoProfile', '-File', $monitorScript, '-RunId', $RunId, '-ReuseExisting', '-IntervalSeconds', '5', '-StopFile', $stopFile)
$monitorProcess = Start-Process -FilePath $pwshPath -ArgumentList $monitorArguments -WindowStyle Hidden -RedirectStandardOutput $monitorStdout -RedirectStandardError $monitorStderr -PassThru
Start-Sleep -Seconds 3

try {
    $idleStart = [DateTimeOffset]::UtcNow
    $idleSamples = 0
    $idleSuccess = 0
    $idleDuration = if ($Quick) { 5 } else { 300 }
    $idleEnd = $idleStart.AddSeconds($idleDuration)
    while ([DateTimeOffset]::UtcNow -lt $idleEnd) {
        $probe = Invoke-EasyProxyHttp -Uri "$webBase/api/open/health" -TimeoutSeconds 5
        $idleSamples++
        if ($probe.StatusCode -eq 200) { $idleSuccess++ }
        Start-Sleep -Seconds 5
    }
    $idleEnded = [DateTimeOffset]::UtcNow
    $idleEvidence = Write-EasyProxyEvidence -Path (New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/health/T-21_idle_$stamp.csv") -Text (@('timestampUtc,status') + @('') -join "`n")
    $idlePass = $idleSamples -gt 0 -and $idleSuccess -eq $idleSamples
    Add-LoadResult -CaseId 'T-21' -Name '容量梯度-空闲' -StartedAt $idleStart -EndedAt $idleEnded -Passed $idlePass -Detail "health=$idleSuccess/$idleSamples" -Evidence "raw/health/T-21_idle_$stamp.csv" | Out-Null

    Invoke-LoadCase -CaseId 'T-21' -Label 'small-c10' -Scenario 'small' -Concurrency 10 -MinSuccessRate 0.995 -MaxP95Ms 500 -MaxP99Ms 1000 | Out-Null
    Invoke-LoadCase -CaseId 'T-21' -Label 'small-c100' -Scenario 'small' -Concurrency 100 -MinSuccessRate 0.995 -MaxP95Ms 500 -MaxP99Ms 1000 | Out-Null
    Invoke-LoadCase -CaseId 'T-21' -Label 'small-c250' -Scenario 'small' -Concurrency 250 -MinSuccessRate 0.99 -MaxP95Ms 1000 -MaxP99Ms 2000 | Out-Null
    Invoke-LoadCase -CaseId 'T-21' -Label 'small-c500' -Scenario 'small' -Concurrency 500 -MinSuccessRate 0.95 -MaxP95Ms 2000 -MaxP99Ms 5000 | Out-Null

    Invoke-LoadCase -CaseId 'T-22' -Label 'distinct-c10' -Scenario 'distinct' -Concurrency 10 -Warmup $caseWarmup -Duration $distinctDuration -Cooldown $caseCooldown -TotalRequests $distinctTotal -MinSuccessRate 1 -MaxP95Ms 500 -MaxP99Ms 1000 | Out-Null
    Invoke-LoadCase -CaseId 'T-22' -Label 'distinct-c100' -Scenario 'distinct' -Concurrency 100 -Warmup $caseWarmup -Duration $distinctDuration -Cooldown $caseCooldown -TotalRequests $distinctTotal -MinSuccessRate 1 -MaxP95Ms 500 -MaxP99Ms 1000 | Out-Null
    Invoke-LoadCase -CaseId 'T-22' -Label 'distinct-c250' -Scenario 'distinct' -Concurrency 250 -Warmup $caseWarmup -Duration $distinctDuration -Cooldown $caseCooldown -TotalRequests $distinctTotal -MinSuccessRate 1 -MaxP95Ms 1000 -MaxP99Ms 2000 | Out-Null

    Invoke-LoadCase -CaseId 'T-23' -Label 'connections-c100' -Scenario 'small' -Concurrency 100 -Connections 'mixed' -Warmup $caseWarmup -Duration $longDuration -Cooldown $caseCooldown -MinSuccessRate 0.995 -MaxP95Ms 500 -MaxP99Ms 1000 | Out-Null
    Invoke-LoadCase -CaseId 'T-24' -Label 'high-c250' -Scenario 'small' -Concurrency 250 -Warmup $caseWarmup -Duration $HighLoadSeconds -Cooldown $caseCooldown -MinSuccessRate 0.99 -MaxP95Ms 1000 -MaxP99Ms 2000 | Out-Null
    Invoke-LoadCase -CaseId 'T-24' -Label 'saturation-c500' -Scenario 'small' -Concurrency 500 -Warmup $caseWarmup -Duration $SaturationSeconds -Cooldown $caseCooldown -MinSuccessRate 0.95 -MaxP95Ms 2000 -MaxP99Ms 5000 | Out-Null
    Invoke-LoadCase -CaseId 'T-25' -Label 'large-c100' -Scenario 'large' -Concurrency 100 -Warmup $caseWarmup -Duration $longDuration -Cooldown $caseCooldown -RequestTimeoutSeconds 60 -MinSuccessRate 0.995 | Out-Null
    Invoke-LoadCase -CaseId 'T-26' -Label 'mixed-c100' -Scenario 'mixed' -Concurrency 100 -Warmup $caseWarmup -Duration $mixedDuration -Cooldown $caseCooldown -RequestTimeoutSeconds 60 -MinSuccessRate 0.995 | Out-Null
    Invoke-LoadCase -CaseId 'T-27' -Label 'slow-c100' -Scenario 'slow' -Concurrency 100 -Warmup $caseWarmup -Duration $longDuration -Cooldown $caseCooldown -RequestTimeoutSeconds 60 -MinSuccessRate 0.995 | Out-Null

    $resourceStart = [DateTimeOffset]::UtcNow
    $t28 = Invoke-LoadCase -CaseId 'T-28' -Label 'resource-c100' -Scenario 'small' -Concurrency 100 -Warmup $resourceWarmup -Duration $resourceDuration -Cooldown $resourceCooldown -MinSuccessRate 0.995 -MaxP95Ms 500 -MaxP99Ms 1000 -SkipResult
    $resourceEnd = [DateTimeOffset]::UtcNow
    Start-Sleep -Seconds 6
    $resource = Test-ResourceThresholds -Start $resourceStart -End $resourceEnd
    Add-LoadResult -CaseId 'T-28' -Name '资源占用与泄漏' -StartedAt $resourceStart -EndedAt $resourceEnd -Passed ($t28.Status -eq 'PASS' -and $resource.Passed) -Detail $resource.Detail -Evidence $t28.Evidence | Out-Null

    $soakLimit = [math]::Min($SoakSeconds, 3600)
    Invoke-LoadCase -CaseId 'T-29' -Label "soak-c100-$($soakLimit)s" -Scenario 'small' -Concurrency 100 -Warmup $soakWarmup -Duration $soakLimit -Cooldown $soakCooldown -MinSuccessRate 0.999 -MaxP95Ms 500 -MaxP99Ms 1000 -MinHealthRate 0.999 | Out-Null

    $t30Start = [DateTimeOffset]::UtcNow
    Invoke-LoadCase -CaseId 'T-30' -Label 'saturation-c500' -Scenario 'small' -Concurrency 500 -Warmup $t30Warmup -Duration $t30Duration -Cooldown 0 -MinSuccessRate 0.90 -RequestTimeoutSeconds 60 | Out-Null
    $recovery = Test-RecoveryWindow -Seconds $(if ($Quick) { 10 } else { 120 })
    $recoveryCsv = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/health/T-30_recovery_$stamp.csv"
    Write-LfText -Path $recoveryCsv -Text ("samples={0}`nsecondHalfSuccess={1}`nfirstHalfFailures={2}`n" -f $recovery.Samples, $recovery.SecondHalfSuccess, $recovery.FirstHalfFailures)
    Add-LoadResult -CaseId 'T-30' -Name '撤压恢复' -StartedAt $recovery.StartedAt -EndedAt $recovery.EndedAt -Passed $recovery.Passed -Detail "samples=$($recovery.Samples) secondHalfSuccess=$($recovery.SecondHalfSuccess) firstHalfFailures=$($recovery.FirstHalfFailures)" -Evidence "raw/health/T-30_recovery_$stamp.csv" | Out-Null
}
finally {
    Write-LfText -Path $stopFile -Text "stop`n"
    if (-not $monitorProcess.WaitForExit(60000)) {
        try { $monitorProcess.Kill($true) } catch { }
        throw '监控进程未能在 60 秒内结束'
    }
    if ($monitorProcess.ExitCode -ne 0) {
        Write-Host "MONITOR_EXIT=$($monitorProcess.ExitCode)"
    }
}

$resultCsv = Write-EasyProxyTestResults -Run $run -Phase 'load' -Results $results.ToArray()
$failed = @($results | Where-Object { $_.Status -ne 'PASS' })
$status = if ($failed.Count -eq 0) { 'PASS' } else { 'FAIL' }
$summary = [Collections.Generic.List[string]]::new()
$summary.Add('# T-21 至 T-30 负载与稳定性摘要'); $summary.Add('')
$summary.Add("- 运行编号：``$RunId``"); $summary.Add("- 总体判定：**$status**")
$summary.Add("- 通过：$($results.Count - $failed.Count)；失败：$($failed.Count)"); $summary.Add('')
$summary.Add('| 测试编号 | 场景 | 状态 | 耗时（秒） | 说明 |'); $summary.Add('| --- | --- | --- | ---: | --- |')
foreach ($item in $results) { $summary.Add("| $($item.TestId) | $($item.Name) | $($item.Status) | $([math]::Round($item.DurationSeconds, 1)) | $($item.Detail.Replace('|', '/')) |") }
$summary.Add(''); $summary.Add("- 结果 CSV：``$($resultCsv.Substring($run.RunRoot.Length + 1).Replace('\', '/'))``")
$summaryPath = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "processed/summaries/load-summary-$stamp.md"
Write-LfText -Path $summaryPath -Text (($summary -join "`n") + "`n")
Write-Output (($summary -join "`n") + "`n")
Write-Output "RUN_ID=$RunId"; Write-Output "STATUS=$status"; Write-Output "RESULTS=$($resultCsv.Substring($run.RunRoot.Length + 1).Replace('\', '/'))"
if ($failed.Count -gt 0) { exit 1 }
