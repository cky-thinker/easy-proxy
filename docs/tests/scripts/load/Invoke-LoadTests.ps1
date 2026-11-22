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
    [string]$RuleName = 'nas-http-18080',
    [switch]$Quick,
    [switch]$StopOnFailure
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
$testsRoot = Join-Path $repoRoot 'docs\tests'
Import-Module -Name (Join-Path $testsRoot 'scripts\common\EasyProxy.Testing.psm1') -Force
Import-Module -Name (Join-Path $testsRoot 'scripts\common\EasyProxy.Secrets.psm1') -Force
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
$caseWarmup = if ($Quick) { 2 } else { 120 }
$caseCooldown = if ($Quick) { 2 } else { 120 }
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
$largeRequestsPerWorker = if ($Quick) { 1 } else { 3 }
$largeDuration = if ($Quick) { 300 } else { 1800 }
# 20% of the mixed workload is a 10 MiB file. At 5 req/s that asks for roughly
# 10 MB/s before small/slow traffic, above the measured tunnel capacity. Keep
# the full run within capacity so the test measures starvation, not an
# intentionally unbounded backlog. The reduced smoke run still uses 5 req/s.
$mixedTargetRps = if ($Quick) { 5 } else { 1 }
$t27Duration = if ($Quick) { 10 } else { 30 }
$t27RecoverySeconds = if ($Quick) { 10 } else { 60 }
$t27Concurrency = 50

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
        [int]$RequestsPerWorker = 0,
        [int]$TargetRps = 0,
        [Nullable[double]]$MinSuccessRate = $null,
        [Nullable[double]]$MaxP95Ms = $null,
        [Nullable[double]]$MaxP99Ms = $null,
        [Nullable[double]]$MinHealthRate = $null,
        [int]$Expected5xx = 0,
        [int]$RequestTimeoutSeconds = 30,
        [int]$HealthIntervalSeconds = 1,
        [int]$ExpectedRequests = 0,
        [switch]$Allow5xx,
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
        '--health-url', "$ruleBase/fixtures/small-1k.bin", '--health-interval-seconds', [string]$HealthIntervalSeconds,
        '--health-stop-seconds', '30', '--output-dir', $rawLoad, '--file-prefix', $prefix,
        '--expected-5xx', [string]$Expected5xx
    )
    if ($TargetRps -gt 0) { $arguments += @('--target-rps', [string]$TargetRps) }
    if ($TotalRequests -gt 0) { $arguments += @('--total-requests', [string]$TotalRequests) }
    if ($RequestsPerWorker -gt 0) { $arguments += @('--requests-per-worker', [string]$RequestsPerWorker) }
    if ($null -ne $MinSuccessRate) { $arguments += @('--min-success-rate', ([double]$MinSuccessRate).ToString([Globalization.CultureInfo]::InvariantCulture)) }
    if ($null -ne $MaxP95Ms) { $arguments += @('--max-p95-ms', ([double]$MaxP95Ms).ToString([Globalization.CultureInfo]::InvariantCulture)) }
    if ($null -ne $MaxP99Ms) { $arguments += @('--max-p99-ms', ([double]$MaxP99Ms).ToString([Globalization.CultureInfo]::InvariantCulture)) }
    if ($null -ne $MinHealthRate) { $arguments += @('--min-health-rate', ([double]$MinHealthRate).ToString([Globalization.CultureInfo]::InvariantCulture)) }
    if ($Allow5xx) { $arguments += '--no-fail-on-5xx' }

    $startedAt = [DateTimeOffset]::UtcNow
    $timeoutSeconds = [Math]::Max(300, $Warmup + $Duration + $Cooldown + 180)
    $process = Invoke-EasyProxyProcess -FilePath 'dotnet' -ArgumentList $arguments -TimeoutSeconds $timeoutSeconds
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
    $requestsPath = Join-Path $rawLoad "$prefix-requests.csv"
    $healthPath = Join-Path $rawLoad "$prefix-health.csv"
    $passed = $process.ExitCode -eq 0
    $detail = "exit=$($process.ExitCode)"
    $summary = $null
    if (Test-Path -LiteralPath $summaryPath -PathType Leaf) {
        $summary = [IO.File]::ReadAllText($summaryPath) | ConvertFrom-Json -Depth 30
        $passed = $passed -and [bool]$summary.passed
        $detail = "success=$([math]::Round([double]$summary.measured.successRate * 100, 3))% requests=$($summary.measured.totalRequests) rps=$($summary.measured.requestsPerSecond) p95=$($summary.measured.latencyMs.p95)ms p99=$($summary.measured.latencyMs.p99)ms 5xx=$($summary.measured.server5xx) hashErrors=$($summary.measured.hashMismatches) sizeErrors=$($summary.measured.sizeMismatches) health=$([math]::Round([double]$summary.health.availability * 100, 3))% stop=$($summary.stopReason)"
        if (@($summary.thresholdFailures).Count -gt 0) { $detail += " failures=$(@($summary.thresholdFailures) -join '; ')" }
        if ($ExpectedRequests -gt 0 -and [long]$summary.measured.totalRequests -ne $ExpectedRequests) {
            $passed = $false
            $detail += " expectedRequests=$ExpectedRequests"
        }
    }
    elseif ($ExpectedRequests -gt 0) {
        $passed = $false
        $detail += " expectedRequests=$ExpectedRequests summaryMissing=true"
    }
    if ($SkipResult) {
        $result = [pscustomobject]@{ TestId = $CaseId; Name = $Label; Status = $(if ($passed) { 'PASS' } else { 'FAIL' }); Detail = $detail; Evidence = $evidence }
    }
    else {
        $result = Add-LoadResult -CaseId $CaseId -Name "$Label" -StartedAt $startedAt -EndedAt $endedAt -Passed $passed -Detail $detail -Evidence $evidence
    }
    $result | Add-Member -NotePropertyName StartedAt -NotePropertyValue $startedAt -Force
    $result | Add-Member -NotePropertyName EndedAt -NotePropertyValue $endedAt -Force
    $result | Add-Member -NotePropertyName Summary -NotePropertyValue $summary -Force
    $result | Add-Member -NotePropertyName SummaryPath -NotePropertyValue $summaryPath -Force
    $result | Add-Member -NotePropertyName RequestsPath -NotePropertyValue $requestsPath -Force
    $result | Add-Member -NotePropertyName HealthPath -NotePropertyValue $healthPath -Force
    return $result
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

function Get-RequestStats {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return [pscustomobject]@{
            Measured = 0; Successful = 0; Small = 0; Large = 0; Slow = 0
            SmallP99Ms = 0.0; SuccessfulBytes = 0L; HashErrors = 0; SizeErrors = 0
            Timeouts = 0; ConnectionFailures = 0
        }
    }
    $measured = @(Import-Csv -LiteralPath $Path | Where-Object { [string]$_.phase -eq 'measured' })
    $successful = @($measured | Where-Object { [string]$_.success -eq 'true' })
    $small = @($successful | Where-Object { [string]$_.path -eq 'small-1k.bin' })
    $large = @($successful | Where-Object { [string]$_.path -eq 'large-10m.bin' })
    $slow = @($successful | Where-Object { [string]$_.path -eq 'slow-2s.bin' })
    $smallLatencies = [double[]]@($small | ForEach-Object { [double]$_.latency_ms })
    if ($smallLatencies.Count -gt 0) { [Array]::Sort($smallLatencies) }
    $smallP99 = if ($smallLatencies.Count -gt 0) {
        $index = [Math]::Min($smallLatencies.Count - 1, [Math]::Ceiling($smallLatencies.Count * 0.99) - 1)
        $smallLatencies[$index]
    } else { 0.0 }
    $successfulBytes = 0L
    foreach ($row in $successful) { $successfulBytes += [long]$row.actual_bytes }
    return [pscustomobject]@{
        Measured = $measured.Count
        Successful = $successful.Count
        Small = $small.Count
        Large = $large.Count
        Slow = $slow.Count
        SmallP99Ms = [math]::Round([double]$smallP99, 3)
        SuccessfulBytes = $successfulBytes
        HashErrors = @($measured | Where-Object { [string]$_.error_type -eq 'hash_mismatch' }).Count
        SizeErrors = @($measured | Where-Object { [string]$_.error_type -eq 'size_mismatch' }).Count
        Timeouts = @($measured | Where-Object { [string]$_.error_type -eq 'timeout' }).Count
        ConnectionFailures = @($measured | Where-Object {
            [string]$_.error_type -in @('http', 'io') -and
            [string]$_.error -match '(?i)refused|reset|connect(?:ion)? (?:timed out|failed)|broken pipe'
        }).Count
    }
}

function Get-DashboardSnapshot {
    param([string]$Token)
    $statsResponse = Invoke-EasyProxyApi -BaseUri $webBase -Path '/api/dashboard/stats' -Token $Token
    if ($statsResponse.StatusCode -ne 200 -or $statsResponse.Json.code -ne 200) {
        throw "读取仪表盘统计失败：$($statsResponse.Error)"
    }
    $rankingResponse = Invoke-EasyProxyApi -BaseUri $webBase -Path '/api/dashboard/trafficRanking?period=day' -Token $Token
    if ($rankingResponse.StatusCode -ne 200 -or $rankingResponse.Json.code -ne 200) {
        throw "读取流量排行失败：$($rankingResponse.Error)"
    }
    return [pscustomobject]@{
        Stats = $statsResponse.Json.data
        Ranking = @($rankingResponse.Json.data)
        CapturedAt = [DateTimeOffset]::UtcNow
    }
}

function Test-ResourceRelease {
    param([DateTimeOffset]$Start, [DateTimeOffset]$End)
    $rows = @(Get-MonitorMetricsInWindow -Start $Start -End $End | Where-Object {
        [string]$_.metric -match '^container\.(easy-proxy-(server|client|web|target))\.(proc\.(fd|VmRSS)|stats)$'
    })
    if ($rows.Count -eq 0) { return [pscustomobject]@{ Passed = $false; Detail = '未找到资源释放样本' } }
    $failures = [Collections.Generic.List[string]]::new()
    foreach ($group in @($rows | Where-Object { [string]$_.metric -match '\.proc\.fd$' } | Group-Object metric)) {
        $values = @($group.Group | ForEach-Object { [double]$_.value })
        if ($values.Count -ge 2) {
            $peak = ($values | Measure-Object -Maximum).Maximum
            $final = $values[-1]
            if ($final -gt ($peak * 1.1 + 5)) { $failures.Add("$($group.Name) final=$final peak=$peak") }
        }
    }
    foreach ($group in @($rows | Where-Object { [string]$_.metric -match '\.proc\.VmRSS$' } | Group-Object metric)) {
        $values = @($group.Group | ForEach-Object {
            if ([string]$_.value -match '([0-9]+)\s*kB') { [double]$Matches[1] } else { $null }
        } | Where-Object { $null -ne $_ })
        if ($values.Count -ge 2) {
            $peak = ($values | Measure-Object -Maximum).Maximum
            $final = $values[-1]
            if ($final -gt ($peak * 1.05 + 1024)) { $failures.Add("$($group.Name) final=${final}kB peak=${peak}kB") }
        }
    }
    return [pscustomobject]@{
        Passed = $failures.Count -eq 0
        Detail = "samples=$($rows.Count) releaseFailures=$($failures.Count)$(if ($failures.Count -gt 0) { ' [' + ($failures -join '; ') + ']' } else { '' })"
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
    $idleRows = [Collections.Generic.List[string]]::new()
    $idleRows.Add('timestampUtc,status,elapsedMs')
    while ([DateTimeOffset]::UtcNow -lt $idleEnd) {
        $probe = Invoke-EasyProxyHttp -Uri "$webBase/api/open/health" -TimeoutSeconds 5
        $idleSamples++
        if ($probe.StatusCode -eq 200) { $idleSuccess++ }
        $idleRows.Add("$([DateTimeOffset]::UtcNow.ToString('o')),$($probe.StatusCode),$([math]::Round($probe.ElapsedMs, 3))")
        Start-Sleep -Seconds 5
    }
    $idleEnded = [DateTimeOffset]::UtcNow
    $idleEvidencePath = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/health/T-21_idle_$stamp.csv"
    Write-LfText -Path $idleEvidencePath -Text (($idleRows -join "`n") + "`n")
    $idlePass = $idleSamples -gt 0 -and $idleSuccess -eq $idleSamples
    Add-LoadResult -CaseId 'T-21' -Name '容量梯度-空闲' -StartedAt $idleStart -EndedAt $idleEnded -Passed $idlePass -Detail "health=$idleSuccess/$idleSamples" -Evidence "raw/health/T-21_idle_$stamp.csv" | Out-Null

    $t21C10 = Invoke-LoadCase -CaseId 'T-21' -Label 'small-c10' -Scenario 'small' -Concurrency 10 -MinSuccessRate 0.995 -MinHealthRate 0.99
    $t21C100 = Invoke-LoadCase -CaseId 'T-21' -Label 'small-c100' -Scenario 'small' -Concurrency 100 -MinSuccessRate 0.995 -MaxP95Ms 500 -MaxP99Ms 1000 -MinHealthRate 0.99
    $t21C250 = Invoke-LoadCase -CaseId 'T-21' -Label 'small-c250' -Scenario 'small' -Concurrency 250 -MinSuccessRate 0.99 -MinHealthRate 0.99
    $t21C500 = Invoke-LoadCase -CaseId 'T-21' -Label 'small-c500' -Scenario 'small' -Concurrency 500 -MinSuccessRate 0.95 -MinHealthRate 0.99
    $baselineP95 = if ($null -ne $t21C100.Summary) { [double]$t21C100.Summary.measured.latencyMs.p95 } else { 0.0 }
    $baselineP99 = if ($null -ne $t21C100.Summary) { [double]$t21C100.Summary.measured.latencyMs.p99 } else { 0.0 }

    Invoke-LoadCase -CaseId 'T-22' -Label 'distinct-c10' -Scenario 'distinct' -Concurrency 10 -Warmup $caseWarmup -Duration $distinctDuration -Cooldown $caseCooldown -TotalRequests $distinctTotal -MinSuccessRate 0.995 -MinHealthRate 0.99 | Out-Null
    Invoke-LoadCase -CaseId 'T-22' -Label 'distinct-c100' -Scenario 'distinct' -Concurrency 100 -Warmup $caseWarmup -Duration $distinctDuration -Cooldown $caseCooldown -TotalRequests $distinctTotal -MinSuccessRate 0.995 -MinHealthRate 0.99 | Out-Null
    Invoke-LoadCase -CaseId 'T-22' -Label 'distinct-c250' -Scenario 'distinct' -Concurrency 250 -Warmup $caseWarmup -Duration $distinctDuration -Cooldown $caseCooldown -TotalRequests $distinctTotal -MinSuccessRate 0.995 -MinHealthRate 0.99 | Out-Null

    $t23 = Invoke-LoadCase -CaseId 'T-23' -Label 'connections-c100' -Scenario 'small' -Concurrency 100 -Connections 'mixed' -Warmup $caseWarmup -Duration $longDuration -Cooldown $caseCooldown -MinSuccessRate 0.995 -MinHealthRate 0.99 -SkipResult
    $t23Stats = Get-RequestStats -Path $t23.RequestsPath
    $t23Passed = $t23.Status -eq 'PASS' -and $t23Stats.ConnectionFailures -eq 0 -and $t23Stats.HashErrors -eq 0 -and $t23Stats.SizeErrors -eq 0
    Add-LoadResult -CaseId 'T-23' -Name '短连接与长连接' -StartedAt $t23.StartedAt -EndedAt $t23.EndedAt -Passed $t23Passed -Detail "$($t23.Detail) connFailures=$($t23Stats.ConnectionFailures)" -Evidence $t23.Evidence | Out-Null

    Invoke-LoadCase -CaseId 'T-24' -Label 'high-c250' -Scenario 'small' -Concurrency 250 -Warmup $caseWarmup -Duration $HighLoadSeconds -Cooldown $caseCooldown -MinSuccessRate 0.99 -MaxP95Ms 1000 -MaxP99Ms 2000 -MinHealthRate 0.99 | Out-Null
    $t24Saturation = Invoke-LoadCase -CaseId 'T-24' -Label 'saturation-c500' -Scenario 'small' -Concurrency 500 -Warmup $caseWarmup -Duration $SaturationSeconds -Cooldown 0 -MinSuccessRate 0.5 -Allow5xx -SkipResult
    Add-LoadResult -CaseId 'T-24' -Name '饱和探索-500' -StartedAt $t24Saturation.StartedAt -EndedAt $t24Saturation.EndedAt -Passed ($t24Saturation.Status -eq 'PASS') -Detail "$($t24Saturation.Detail)（容量上限探索）" -Evidence $t24Saturation.Evidence | Out-Null
    $t24Recovery = Test-RecoveryWindow -Seconds $(if ($Quick) { 10 } else { 120 })
    Add-LoadResult -CaseId 'T-24' -Name '撤压恢复' -StartedAt $t24Recovery.StartedAt -EndedAt $t24Recovery.EndedAt -Passed $t24Recovery.Passed -Detail "samples=$($t24Recovery.Samples) secondHalfSuccess=$($t24Recovery.SecondHalfSuccess) firstHalfFailures=$($t24Recovery.FirstHalfFailures)" -Evidence '' | Out-Null

    $t25Expected = 20 * $largeRequestsPerWorker
    $t25 = Invoke-LoadCase -CaseId 'T-25' -Label "large-c20-rounds$largeRequestsPerWorker" -Scenario 'large' -Concurrency 20 -Warmup 0 -Duration $largeDuration -Cooldown 0 -RequestsPerWorker $largeRequestsPerWorker -ExpectedRequests $t25Expected -RequestTimeoutSeconds 180 -MinSuccessRate 1 -MinHealthRate 0.99 -SkipResult
    $t25Stats = Get-RequestStats -Path $t25.RequestsPath
    $t25Passed = $t25.Status -eq 'PASS' -and $t25Stats.Successful -eq $t25Expected -and $t25Stats.HashErrors -eq 0 -and $t25Stats.SizeErrors -eq 0
    Add-LoadResult -CaseId 'T-25' -Name '并发大文件传输' -StartedAt $t25.StartedAt -EndedAt $t25.EndedAt -Passed $t25Passed -Detail "$($t25.Detail) completedLarge=$($t25Stats.Successful) expected=$t25Expected" -Evidence $t25.Evidence | Out-Null

    $t26Duration = if ($Quick) { 20 } else { 600 }
    $t26 = Invoke-LoadCase -CaseId 'T-26' -Label 'mixed-c100' -Scenario 'mixed' -Concurrency 100 -Warmup $caseWarmup -Duration $t26Duration -Cooldown $caseCooldown -TargetRps $mixedTargetRps -RequestTimeoutSeconds 60 -MinSuccessRate 0.95 -MinHealthRate 0.99 -SkipResult
    $t26Stats = Get-RequestStats -Path $t26.RequestsPath
    $mixedTotal = [Math]::Max(1, $t26Stats.Measured)
    $smallShare = $t26Stats.Small / $mixedTotal
    $largeShare = $t26Stats.Large / $mixedTotal
    $slowShare = $t26Stats.Slow / $mixedTotal
    $distributionPass = if ($Quick) { $t26Stats.Measured -gt 0 } else {
        $smallShare -ge 0.60 -and $smallShare -le 0.80 -and
        $largeShare -ge 0.15 -and $largeShare -le 0.25 -and
        $slowShare -ge 0.05 -and $slowShare -le 0.15
    }
    $p99Limit = $baselineP99 * 2
    $smallP99Pass = if ($Quick) {
        # The smoke run yields too few small samples for a stable p99. Keep the
        # distribution/correctness checks and defer the strict tail-latency gate
        # to the full-duration acceptance run.
        $t26Stats.Measured -ge 50
    } else {
        $baselineP99 -gt 0 -and $t26Stats.SmallP99Ms -le $p99Limit
    }
    $t26Passed = $t26.Status -eq 'PASS' -and $distributionPass -and $smallP99Pass -and $t26Stats.HashErrors -eq 0 -and $t26Stats.SizeErrors -eq 0
    Add-LoadResult -CaseId 'T-26' -Name '小请求与大请求混合' -StartedAt $t26.StartedAt -EndedAt $t26.EndedAt -Passed $t26Passed -Detail "$($t26.Detail) shares=small:$([math]::Round($smallShare*100,1))%/large:$([math]::Round($largeShare*100,1))%/slow:$([math]::Round($slowShare*100,1))% smallP99=$($t26Stats.SmallP99Ms)ms limit=$([math]::Round($p99Limit,3))ms" -Evidence $t26.Evidence | Out-Null

    $secret = Read-EasyProxySecretMaterial -RunId $RunId
    $admin = Initialize-EasyProxyAdmin -BaseUri $webBase -Username ([string]$secret.AdminUsername) -Password ([string]$secret.AdminPassword)
    $loadRules = @(Get-EasyProxyRules -BaseUri $webBase -Token $admin.Token)
    $t27Rule = $loadRules | Where-Object { [string]$_.name -eq $RuleName } | Select-Object -First 1
    if ($null -eq $t27Rule) { throw "T-27 未找到规则：$RuleName" }
    $t27Before = Get-DashboardSnapshot -Token $admin.Token
    $t27Prefix = "T-27_traffic-consistency_$stamp"
    $t27Args = @(
        $dll, '--base-url', $ruleBase, '--manifest', $fixtureManifest, '--scenario', 'small',
        '--concurrency', [string]$t27Concurrency, '--warmup-seconds', '0', '--duration-seconds', [string]$t27Duration,
        '--cooldown-seconds', '0', '--target-rps', '20', '--connections', 'keep-alive',
        '--request-timeout-seconds', '30', '--health-url', "$ruleBase/fixtures/small-1k.bin",
        '--health-interval-seconds', '3600', '--health-stop-seconds', '30', '--output-dir', $rawLoad,
        '--file-prefix', $t27Prefix, '--no-fail-on-5xx'
    )
    $t27Stdout = Join-Path $rawLoad "$t27Prefix-stdout.txt"
    $t27Stderr = Join-Path $rawLoad "$t27Prefix-stderr.txt"
    $t27Started = [DateTimeOffset]::UtcNow
    $t27Process = Start-Process -FilePath 'dotnet' -ArgumentList $t27Args -WindowStyle Hidden -RedirectStandardOutput $t27Stdout -RedirectStandardError $t27Stderr -PassThru
    Start-Sleep -Seconds 5
    $t27During = Get-DashboardSnapshot -Token $admin.Token
    if (-not $t27Process.WaitForExit(180000)) {
        try { $t27Process.Kill($true) } catch { }
        throw 'T-27 负载进程未在 180 秒内结束'
    }
    $t27Ended = [DateTimeOffset]::UtcNow
    $t27ConsolePath = Join-Path $rawLoad "$t27Prefix-console.txt"
    $t27Console = @(
        "exit_code=$($t27Process.ExitCode)"
        '--- stdout ---'
        (Get-Content -LiteralPath $t27Stdout -Raw -ErrorAction SilentlyContinue)
        '--- stderr ---'
        (Get-Content -LiteralPath $t27Stderr -Raw -ErrorAction SilentlyContinue)
    ) -join "`n"
    Write-EasyProxyEvidence -Path $t27ConsolePath -Text ($t27Console.TrimEnd() + "`n")
    $t27SummaryPath = Join-Path $rawLoad "$t27Prefix-summary.json"
    $t27RequestsPath = Join-Path $rawLoad "$t27Prefix-requests.csv"
    $t27Summary = if (Test-Path -LiteralPath $t27SummaryPath) { [IO.File]::ReadAllText($t27SummaryPath) | ConvertFrom-Json -Depth 30 } else { $null }
    $t27After = Get-DashboardSnapshot -Token $admin.Token
    $t27BaselineConnections = [long]$t27Before.Stats.activeConnections
    $t27Deadline = [DateTimeOffset]::UtcNow.AddSeconds($t27RecoverySeconds)
    $t27Settled = $t27After
    do {
        if ([long]$t27Settled.Stats.activeConnections -le ($t27BaselineConnections + 1)) { break }
        Start-Sleep -Seconds 2
        $t27Settled = Get-DashboardSnapshot -Token $admin.Token
    } while ([DateTimeOffset]::UtcNow -lt $t27Deadline)
    $t27Stats = Get-RequestStats -Path $t27RequestsPath
    $t27UploadDelta = [long]$t27Settled.Stats.uploadBytes - [long]$t27Before.Stats.uploadBytes
    $t27DownloadDelta = [long]$t27Settled.Stats.downloadBytes - [long]$t27Before.Stats.downloadBytes
    $t27ActualBytes = [long]$t27Stats.SuccessfulBytes
    $t27PeakConnections = [long]$t27During.Stats.activeConnections
    $t27NoNegative = $t27UploadDelta -ge 0 -and $t27DownloadDelta -ge 0 -and [long]$t27Before.Stats.uploadBytes -ge 0 -and [long]$t27Before.Stats.downloadBytes -ge 0
    $t27DownloadMatches = $t27DownloadDelta -ge $t27ActualBytes -and $t27DownloadDelta -le ($t27ActualBytes + [Math]::Max(65536, $t27ActualBytes * 0.05))
    # GET requests still carry HTTP/1.1 request headers through the tunnel. The
    # dashboard upload counter measures those wire bytes, not response-body bytes.
    $t27UploadPerRequest = $t27UploadDelta / [Math]::Max(1, $t27Stats.Measured)
    $t27UploadMatches = $t27UploadDelta -gt 0 -and $t27UploadDelta -lt $t27DownloadDelta -and $t27UploadPerRequest -le 8192
    $t27ConnectionsPass = $t27PeakConnections -gt 0 -and [long]$t27Settled.Stats.activeConnections -le ($t27BaselineConnections + 1)
    $t27Passed = $null -ne $t27Summary -and [bool]$t27Summary.passed -and $t27Process.ExitCode -eq 0 -and $t27NoNegative -and $t27DownloadMatches -and $t27UploadMatches -and $t27ConnectionsPass -and $t27Stats.HashErrors -eq 0 -and $t27Stats.SizeErrors -eq 0
    Add-LoadResult -CaseId 'T-27' -Name '流量与连接统计一致性' -StartedAt $t27Started -EndedAt $t27Ended -Passed $t27Passed -Detail "connections=$($t27Before.Stats.activeConnections)->peak=$t27PeakConnections->final=$($t27Settled.Stats.activeConnections) downloadDelta=$t27DownloadDelta actualBytes=$t27ActualBytes uploadDelta=$t27UploadDelta hashErrors=$($t27Stats.HashErrors)" -Evidence $t27ConsolePath.Substring($run.RunRoot.Length + 1).Replace('\', '/') | Out-Null

    $resourceStart = [DateTimeOffset]::UtcNow
    $t28 = Invoke-LoadCase -CaseId 'T-28' -Label 'resource-c100' -Scenario 'small' -Concurrency 100 -Warmup $resourceWarmup -Duration $resourceDuration -Cooldown $resourceCooldown -MinSuccessRate 0.995 -MaxP95Ms 500 -MaxP99Ms 1000 -MinHealthRate 0.99 -SkipResult
    $resourceEnd = [DateTimeOffset]::UtcNow
    Start-Sleep -Seconds 6
    $resource = Test-ResourceThresholds -Start $resourceStart -End $resourceEnd
    Add-LoadResult -CaseId 'T-28' -Name '资源占用与泄漏' -StartedAt $resourceStart -EndedAt $resourceEnd -Passed ($t28.Status -eq 'PASS' -and $resource.Passed) -Detail "$($resource.Detail) load=$($t28.Detail)" -Evidence $t28.Evidence | Out-Null

    $soakLimit = [math]::Min($SoakSeconds, 3600)
    $soakP95Limit = [Math]::Max(1.0, $baselineP95 * 1.2)
    $soakStart = [DateTimeOffset]::UtcNow
    $t29 = Invoke-LoadCase -CaseId 'T-29' -Label "soak-c100-$($soakLimit)s" -Scenario 'small' -Concurrency 100 -Warmup $soakWarmup -Duration $soakLimit -Cooldown $soakCooldown -MinSuccessRate 0.995 -MaxP95Ms $soakP95Limit -MinHealthRate 0.999 -SkipResult
    $soakEnd = [DateTimeOffset]::UtcNow
    Start-Sleep -Seconds 6
    $soakResources = Test-ResourceThresholds -Start $soakStart -End $soakEnd
    $soakNote = if ($soakLimit -lt 3600) { " shortened=$($soakLimit)s" } else { '' }
    Add-LoadResult -CaseId 'T-29' -Name '长时间稳定性 SOAK' -StartedAt $soakStart -EndedAt $soakEnd -Passed ($t29.Status -eq 'PASS' -and $soakResources.Passed) -Detail "$($t29.Detail) p95Limit=$([math]::Round($soakP95Limit,3))ms resources=[$($soakResources.Detail)]$soakNote" -Evidence $t29.Evidence | Out-Null

    $t30Start = [DateTimeOffset]::UtcNow
    $t30 = Invoke-LoadCase -CaseId 'T-30' -Label 'saturation-c500' -Scenario 'small' -Concurrency 500 -Warmup $t30Warmup -Duration $t30Duration -Cooldown 0 -MinSuccessRate 0.5 -RequestTimeoutSeconds 60 -Allow5xx -SkipResult
    Add-LoadResult -CaseId 'T-30' -Name '500 并发饱和运行' -StartedAt $t30.StartedAt -EndedAt $t30.EndedAt -Passed ($t30.Status -eq 'PASS') -Detail $t30.Detail -Evidence $t30.Evidence | Out-Null
    $recovery = Test-RecoveryWindow -Seconds $(if ($Quick) { 10 } else { 120 })
    $recoveryCsv = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/health/T-30_recovery_$stamp.csv"
    Write-LfText -Path $recoveryCsv -Text ("samples={0}`nsecondHalfSuccess={1}`nfirstHalfFailures={2}`n" -f $recovery.Samples, $recovery.SecondHalfSuccess, $recovery.FirstHalfFailures)
    Add-LoadResult -CaseId 'T-30' -Name '撤压恢复' -StartedAt $recovery.StartedAt -EndedAt $recovery.EndedAt -Passed $recovery.Passed -Detail "samples=$($recovery.Samples) secondHalfSuccess=$($recovery.SecondHalfSuccess) firstHalfFailures=$($recovery.FirstHalfFailures)" -Evidence "raw/health/T-30_recovery_$stamp.csv" | Out-Null
    $release = Test-ResourceRelease -Start $t30Start -End $recovery.EndedAt
    Add-LoadResult -CaseId 'T-30' -Name '连接与资源回落' -StartedAt $recovery.EndedAt -EndedAt ([DateTimeOffset]::UtcNow) -Passed $release.Passed -Detail $release.Detail -Evidence "raw/health/T-30_recovery_$stamp.csv" | Out-Null
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
$summary.Add("- 运行模式：$(if ($Quick) { '快速冒烟（缩减时长，不作为最终验收）' } else { '完整验收' })")
$summary.Add("- 通过：$($results.Count - $failed.Count)；失败：$($failed.Count)"); $summary.Add('')
$summary.Add('| 测试编号 | 场景 | 状态 | 耗时（秒） | 说明 |'); $summary.Add('| --- | --- | --- | ---: | --- |')
foreach ($item in $results) { $summary.Add("| $($item.TestId) | $($item.Name) | $($item.Status) | $([math]::Round($item.DurationSeconds, 1)) | $($item.Detail.Replace('|', '/')) |") }
$summary.Add(''); $summary.Add("- 结果 CSV：``$($resultCsv.Substring($run.RunRoot.Length + 1).Replace('\', '/'))``")
$summaryPath = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "processed/summaries/load-summary-$stamp.md"
Write-LfText -Path $summaryPath -Text (($summary -join "`n") + "`n")
Write-Output (($summary -join "`n") + "`n")
Write-Output "RUN_ID=$RunId"; Write-Output "STATUS=$status"; Write-Output "RESULTS=$($resultCsv.Substring($run.RunRoot.Length + 1).Replace('\', '/'))"
if ($failed.Count -gt 0) { exit 1 }
