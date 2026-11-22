[CmdletBinding()]
param(
    [string]$RunId = (Get-Date -Format 'yyyyMMdd-HHmmss'),
    [switch]$ReuseExisting,
    [int]$DurationSeconds = 0,
    [int]$IntervalSeconds = 5,
    [string]$StopFile = '',
    [string]$CloudHost = '68.64.181.242',
    [int]$CloudPort = 1514,
    [string]$CloudUser = 'yudejijie',
    [string]$NasHost = '192.168.1.9',
    [int]$NasPort = 10080,
    [string]$NasUser = 'chenkeyudev',
    [int]$RulePort = 28080
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ($IntervalSeconds -lt 1) { throw 'IntervalSeconds 必须大于等于 1' }
if ($DurationSeconds -lt 0) { throw 'DurationSeconds 不能为负数' }

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
$testsRoot = Join-Path $repoRoot 'docs\tests'
Import-Module -Name (Join-Path $testsRoot 'scripts\common\EasyProxy.Testing.psm1') -Force
$run = New-EasyProxyRun -RunId $RunId -TestScope 'T-21 至 T-36 运行中资源监控' -ReuseExisting:$ReuseExisting
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$csvPath = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/metrics/monitor-$stamp.csv"
$logPath = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "raw/metrics/monitor-$stamp.log"
$utf8 = [Text.UTF8Encoding]::new($false)
$writer = [IO.StreamWriter]::new($csvPath, $false, $utf8)
$writer.NewLine = "`n"
$writer.WriteLine('timestampUtc,scope,source,metric,value,details')
$writer.Flush()
$startedAt = [DateTimeOffset]::UtcNow
$sampleCount = 0
$errors = [Collections.Generic.List[string]]::new()

function Escape-Csv {
    param([AllowNull()][string]$Value)
    if ($null -eq $Value) { return '' }
    if ($Value -match '[",\r\n]') { return '"' + $Value.Replace('"', '""') + '"' }
    return $Value
}

function Add-Metric {
    param([string]$Scope, [string]$Source, [string]$Metric, [string]$Value, [string]$Details = '')
    $line = @(
        [DateTimeOffset]::UtcNow.ToString('o'), $Scope, $Source, $Metric, $Value, $Details
    ) | ForEach-Object { Escape-Csv ([string]$_) }
    $writer.WriteLine(($line -join ','))
}

function Write-MonitorLog {
    param([string]$Text)
    [IO.File]::AppendAllText($logPath, (ConvertTo-LfText -Text ($Text.TrimEnd() + "`n")), $utf8)
}

function Get-RemoteMetrics {
    param([string]$HostName, [int]$Port, [string]$User, [string]$Scope, [string[]]$Containers)
    $containerList = $Containers -join ' '
    $remoteScript = @(
        'set -u'
        'if command -v docker >/dev/null 2>&1; then DOCKER=$(command -v docker); else DOCKER=/usr/local/bin/docker; fi'
        'echo "host.hostname=$(hostname)"'
        'echo "host.uptime_seconds=$(cut -d. -f1 /proc/uptime 2>/dev/null || echo unknown)"'
        'df -Pk / | awk ''NR==2 {printf "host.disk_root.percent=%s\n", $5}'''
        'free -k | awk ''/^Mem:/ {printf "host.memory.total_kb=%s\nhost.memory.used_kb=%s\nhost.memory.available_kb=%s\n", $2, $3, $7}'''
        "for c in $containerList; do"
        '  if ! "$DOCKER" inspect "$c" >/dev/null 2>&1; then echo "container.$c.present=0"; continue; fi'
        '  echo "container.$c.present=1"'
        '  "$DOCKER" stats --no-stream --format ''stats|{{.CPUPerc}}|{{.MemUsage}}|{{.PIDs}}'' "$c" | sed "s/^/container.$c./"'
        '  "$DOCKER" inspect "$c" --format ''inspect|{{.State.Status}}|{{.RestartCount}}|{{.State.OOMKilled}}|{{.State.ExitCode}}'' | sed "s/^/container.$c./"'
        '  "$DOCKER" exec "$c" sh -c ''grep -E "^(VmRSS|VmSize|Threads):" /proc/1/status; echo fd=$(ls /proc/1/fd 2>/dev/null | wc -l)'' 2>/dev/null | sed "s/:/=/" | sed "s/^/container.$c.proc./" || true'
        'done'
    ) -join "`n"
    $result = Invoke-SshBash -HostName $HostName -Port $Port -User $User -Script $remoteScript -TimeoutSeconds ([Math]::Max(20, $IntervalSeconds * 3))
    if ($result.ExitCode -ne 0) {
        throw "$Scope 指标采集失败，exit=$($result.ExitCode)"
    }
    foreach ($line in ($result.Combined -split "`n")) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        if ($line -match '^([^=]+)=(.*)$') {
            Add-Metric -Scope $Scope -Source 'remote' -Metric $Matches[1] -Value $Matches[2]
        }
        elseif ($line -match '^([^|]+)\|(.*)$') {
            Add-Metric -Scope $Scope -Source 'remote' -Metric $Matches[1] -Value $Matches[2]
        }
    }
    Write-MonitorLog -Text "--- $Scope command ---`n$($result.Combined)"
}

function Get-ManagerMetrics {
    $cpu = $null
    $os = $null
    try { $cpu = @(Get-CimInstance Win32_Processor -ErrorAction Stop | Measure-Object -Property LoadPercentage -Average).Average } catch { }
    try { $os = Get-CimInstance Win32_OperatingSystem -ErrorAction Stop } catch { }
    if ($null -ne $cpu) { Add-Metric -Scope 'manager' -Source 'cim' -Metric 'cpu.percent' -Value ([string][math]::Round([double]$cpu, 2)) }
    if ($null -ne $os) {
        $total = [double]$os.TotalVisibleMemorySize
        $free = [double]$os.FreePhysicalMemory
        $usedPercent = if ($total -gt 0) { (($total - $free) / $total) * 100 } else { 0 }
        Add-Metric -Scope 'manager' -Source 'cim' -Metric 'memory.used_percent' -Value ([string][math]::Round($usedPercent, 2))
        Add-Metric -Scope 'manager' -Source 'cim' -Metric 'memory.available_kb' -Value ([string]$os.FreePhysicalMemory)
    }
    try {
        $connections = @(Get-NetTCPConnection -RemotePort $RulePort -ErrorAction SilentlyContinue)
        Add-Metric -Scope 'manager' -Source 'net' -Metric "tcp.remote_port_$RulePort" -Value ([string]$connections.Count)
    }
    catch { }
    try {
        $received = 0L
        $sent = 0L
        foreach ($adapter in @(Get-NetAdapterStatistics -ErrorAction SilentlyContinue)) {
            $received += [int64]$adapter.ReceivedBytes
            $sent += [int64]$adapter.SentBytes
        }
        Add-Metric -Scope 'manager' -Source 'net' -Metric 'adapter.received_bytes' -Value ([string]$received)
        Add-Metric -Scope 'manager' -Source 'net' -Metric 'adapter.sent_bytes' -Value ([string]$sent)
    }
    catch { }
}

try {
    Write-MonitorLog -Text "monitor started utc=$($startedAt.ToString('o')) interval=$IntervalSeconds duration=$DurationSeconds stopFile=$StopFile"
    while ($true) {
        if (-not [string]::IsNullOrWhiteSpace($StopFile) -and (Test-Path -LiteralPath $StopFile)) { break }
        if ($DurationSeconds -gt 0 -and ([DateTimeOffset]::UtcNow - $startedAt).TotalSeconds -ge $DurationSeconds) { break }
        try {
            Get-ManagerMetrics
            Get-RemoteMetrics -HostName $CloudHost -Port $CloudPort -User $CloudUser -Scope 'cloud' -Containers @('easy-proxy-server', 'easy-proxy-web')
            Get-RemoteMetrics -HostName $NasHost -Port $NasPort -User $NasUser -Scope 'nas' -Containers @('easy-proxy-client', 'easy-proxy-target')
        }
        catch {
            $message = $_.Exception.Message
            $errors.Add($message)
            Write-MonitorLog -Text "ERROR $message"
            Add-Metric -Scope 'monitor' -Source 'collector' -Metric 'collection.error' -Value '1' -Details $message
        }
        $sampleCount++
        $writer.Flush()
        if ($sampleCount % 12 -eq 0) {
            Write-Host "MONITOR samples=$sampleCount elapsed=$([math]::Round(([DateTimeOffset]::UtcNow - $startedAt).TotalSeconds, 1))s"
        }
        Start-Sleep -Seconds $IntervalSeconds
    }
}
finally {
    $writer.Flush()
    $writer.Dispose()
    $endedAt = [DateTimeOffset]::UtcNow
    $summary = @(
        '# Easy Proxy 资源监控摘要'
        ''
        "- 运行编号：``$RunId``"
        "- 开始：$($startedAt.ToString('o'))"
        "- 结束：$($endedAt.ToString('o'))"
        "- 采样轮次：$sampleCount"
        "- 采集错误：$($errors.Count)"
        "- CSV：``raw/metrics/monitor-$stamp.csv``"
        "- 日志：``raw/metrics/monitor-$stamp.log``"
        ''
        $(if ($errors.Count -gt 0) { "- 最后错误：$($errors[-1])" } else { '- 采集过程中无错误。' })
    ) -join "`n"
    $summaryPath = New-EasyProxyEvidencePath -RunRoot $run.RunRoot -RelativePath "processed/summaries/monitor-$stamp.md"
    Write-LfText -Path $summaryPath -Text ($summary + "`n")
    Write-Host "MONITOR_CSV=$($csvPath.Substring($run.RunRoot.Length + 1).Replace('\', '/'))"
    Write-Host "MONITOR_LOG=$($logPath.Substring($run.RunRoot.Length + 1).Replace('\', '/'))"
    Write-Host "MONITOR_SUMMARY=$($summaryPath.Substring($run.RunRoot.Length + 1).Replace('\', '/'))"
}
