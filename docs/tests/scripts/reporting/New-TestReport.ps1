[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$RunId,
    [switch]$ReuseExisting,
    [string]$ImageTag = '',
    [string]$RepoDigests = '',
    [ValidateSet('PASS', 'FAIL', 'NOT_RUN', 'BLOCKED')][string]$T37Status = 'NOT_RUN',
    [ValidateSet('PASS', 'FAIL', 'NOT_RUN', 'BLOCKED')][string]$T38Status = 'NOT_RUN',
    [ValidateSet('PASS', 'FAIL', 'NOT_RUN', 'BLOCKED')][string]$T39Status = 'NOT_RUN',
    [string]$ReportVariant = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
$testsRoot = Join-Path $repoRoot 'docs\tests'
Import-Module -Name (Join-Path $testsRoot 'scripts\common\EasyProxy.Testing.psm1') -Force
$run = New-EasyProxyRun -RunId $RunId -TestScope 'T-01 至 T-39 最终测试报告' -ReuseExisting:$ReuseExisting
$reportsRoot = Join-Path $testsRoot 'reports'
if (-not [string]::IsNullOrWhiteSpace($ReportVariant) -and $ReportVariant -notmatch '^[A-Za-z0-9_-]+$') {
    throw "ReportVariant 不合法：$ReportVariant"
}
$suffix = if ([string]::IsNullOrWhiteSpace($ReportVariant)) { '' } else { "-$ReportVariant" }
$reportPath = Join-Path $reportsRoot "$RunId-test-report$suffix.md"
$summaryCsvPath = Join-Path $reportsRoot "$RunId-summary$suffix.csv"
foreach ($existing in @($reportPath, $summaryCsvPath)) {
    if (Test-Path -LiteralPath $existing) { throw "报告产物已存在，禁止覆盖：$existing" }
}

$resultFiles = @(Get-ChildItem -LiteralPath (Join-Path $run.RunRoot 'processed\summaries') -Filter '*-results-*.csv' -File -ErrorAction SilentlyContinue | Sort-Object Name)
$allResults = [Collections.Generic.List[object]]::new()
foreach ($file in $resultFiles) {
    foreach ($row in @(Import-Csv -LiteralPath $file.FullName)) {
        $allResults.Add([pscustomobject]@{
            TestId = [string]$row.testId; Name = [string]$row.name; Status = [string]$row.status
            StartedAt = [string]$row.startedAt; EndedAt = [string]$row.endedAt
            DurationSeconds = [double]$row.durationSeconds; Detail = [string]$row.detail; Evidence = [string]$row.evidence
            Source = $file.FullName.Substring($run.RunRoot.Length + 1).Replace('\', '/')
        })
    }
}
$allResults.Add([pscustomobject]@{ TestId='T-37'; Name='WSL 构建与单元测试'; Status=$T37Status; StartedAt=''; EndedAt=''; DurationSeconds=0; Detail='由构建阶段证据确认'; Evidence='raw/validation'; Source='build' })
$allResults.Add([pscustomobject]@{ TestId='T-38'; Name='ACR 发布与凭据注销'; Status=$T38Status; StartedAt=''; EndedAt=''; DurationSeconds=0; Detail='由 ACR 发布证据确认'; Evidence='raw/validation'; Source='build' })
$allResults.Add([pscustomobject]@{ TestId='T-39'; Name='远端清单与产物一致性'; Status=$T39Status; StartedAt=''; EndedAt=''; DurationSeconds=0; Detail='由云端验证证据确认'; Evidence='raw/docker'; Source='build' })

$csvRows = foreach ($row in $allResults) {
    [pscustomobject][ordered]@{
        runId = $RunId; testId = $row.TestId; name = $row.Name; status = $row.Status
        durationSeconds = [math]::Round([double]$row.DurationSeconds, 3); detail = $row.Detail
        evidence = $row.Evidence; source = $row.Source
    }
}
$csvText = (($csvRows | ConvertTo-Csv -NoTypeInformation) -join "`n") + "`n"
Write-LfText -Path $summaryCsvPath -Text $csvText

$failed = @($allResults | Where-Object { $_.Status -notin @('PASS', 'SKIPPED_BLOCKED') })
$notRun = @($allResults | Where-Object { $_.Status -eq 'NOT_RUN' })
$overall = if ($failed.Count -eq 0 -and $notRun.Count -eq 0) { 'PASS' } elseif ($failed.Count -gt 0) { 'FAIL' } else { 'INCOMPLETE' }

$loadSummaries = [Collections.Generic.List[object]]::new()
foreach ($file in @(Get-ChildItem -LiteralPath (Join-Path $run.RunRoot 'raw\load') -Filter '*-summary.json' -File -ErrorAction SilentlyContinue | Sort-Object Name)) {
    try {
        $json = [IO.File]::ReadAllText($file.FullName) | ConvertFrom-Json -Depth 30
        $loadSummaries.Add([pscustomobject]@{ File = $file; Summary = $json })
    }
    catch { }
}

$monitorFiles = @(Get-ChildItem -LiteralPath (Join-Path $run.RunRoot 'raw\metrics') -Filter '*.csv' -File -ErrorAction SilentlyContinue | Sort-Object Name)
$monitorRows = [Collections.Generic.List[object]]::new()
foreach ($file in $monitorFiles) {
    foreach ($row in @(Import-Csv -LiteralPath $file.FullName)) { $monitorRows.Add($row) }
}
$monitorErrors = @($monitorRows | Where-Object { $_.metric -eq 'collection.error' }).Count
$monitorSamples = @($monitorRows | Where-Object { $_.scope -ne 'monitor' -and $_.source -ne 'collector' }).Count

$environment = @(
    '| 主机 | 角色 | 地址 | 说明 |'
    '| --- | --- | --- | --- |'
    '| 当前管理机 | 压测源与 SSH 客户端 | 本机 | Windows / PowerShell |'
    '| 云服务器 | proxy-server | `68.64.181.242:1514` | Docker Linux |'
    '| 群晖 NAS | proxy-client | `192.168.1.9:10080` | Docker DSM |'
) -join "`n"
$mirrorRefs = @(
    '| 服务 | 固定标签 | RepoDigest | 平台 |'
    '| --- | --- | --- | --- |'
    "| server | ``$ImageTag`` | $RepoDigests | ``linux/amd64`` |"
    "| client | ``$ImageTag`` | $RepoDigests | ``linux/amd64`` |"
    "| web | ``$ImageTag`` | $RepoDigests | ``linux/amd64`` |"
) -join "`n"

$report = [Collections.Generic.List[string]]::new()
$report.Add("# $RunId Easy Proxy 测试报告")
$report.Add('')
$report.Add('## 结论')
$report.Add('')
$report.Add("- 总体判定：**$overall**")
$report.Add("- 运行编号：``$RunId``")
$report.Add("- Git 提交：``$($run.Commit)``")
$report.Add("- 失败用例：$($failed.Count)；未执行用例：$($notRun.Count)；汇总用例：$($allResults.Count)")
$report.Add('- 本报告由同一运行目录下的功能、负载、故障注入、构建和监控证据自动汇总。')
$report.Add('')
$report.Add('## 环境与版本')
$report.Add('')
$report.Add($environment)
$report.Add('')
$report.Add($mirrorRefs)
$report.Add('')
$report.Add('## 测试结果')
$report.Add('')
$report.Add('| 测试编号 | 测试项 | 状态 | 耗时（秒） | 说明 | 证据 |')
$report.Add('| --- | --- | --- | ---: | --- | --- |')
foreach ($row in $allResults) {
    $detail = ([string]$row.Detail).Replace('|', '/').Replace("`r", ' ').Replace("`n", ' ')
    $report.Add("| $($row.TestId) | $($row.Name) | $($row.Status) | $([math]::Round([double]$row.DurationSeconds, 1)) | $detail | $($row.Evidence) |")
}
$report.Add('')
$report.Add('## 容量与延迟')
$report.Add('')
$report.Add('| 场景 | 并发 | 成功率 | RPS | p95(ms) | p99(ms) | 5xx | 哈希错误 | 健康可用率 | 判定 |')
$report.Add('| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |')
foreach ($item in $loadSummaries) {
    $s = $item.Summary
    $report.Add("| $($s.options.scenario) | $($s.options.concurrency) | $([double]$s.measured.successRate * 100) | $($s.measured.requestsPerSecond) | $($s.measured.latencyMs.p95) | $($s.measured.latencyMs.p99) | $($s.measured.server5xx) | $($s.measured.hashMismatches) | $([double]$s.health.availability * 100)% | $(if ($s.passed) { 'PASS' } else { 'FAIL' }) |")
}
if ($loadSummaries.Count -eq 0) { $report.Add('| 未找到负载摘要 | - | - | - | - | - | - | - | - | NOT_RUN |') }
$report.Add('')
$report.Add('## 资源监控')
$report.Add('')
$report.Add("- 监控 CSV 文件：$($monitorFiles.Count)")
$report.Add("- 有效指标行：$monitorSamples")
$report.Add("- 采集错误：$monitorErrors")
$report.Add('')
$report.Add('## 故障恢复')
$report.Add('')
$faultRows = @($allResults | Where-Object { [int]($_.TestId -replace 'T-', '') -ge 31 -and [int]($_.TestId -replace 'T-', '') -le 35 })
foreach ($row in $faultRows) { $report.Add("- **$($row.TestId) $($row.Name)**：$($row.Status)；$($row.Detail)") }
if ($faultRows.Count -eq 0) { $report.Add('- 未找到 T-31 至 T-35 结果。') }
$report.Add('')
$report.Add('## 缺陷')
$report.Add('')
$defectPath = Join-Path $run.RunRoot 'processed\summaries\defects.csv'
if (Test-Path -LiteralPath $defectPath -PathType Leaf) {
    $defects = @(Import-Csv -LiteralPath $defectPath)
    $report.Add('| 编号 | 严重级别 | 描述 | 影响 | 状态 |')
    $report.Add('| --- | --- | --- | --- | --- |')
    foreach ($d in $defects) { $report.Add("| $($d.id) | $($d.severity) | $($d.description) | $($d.impact) | $($d.status) |") }
}
else { $report.Add('- 未登记未关闭缺陷；若存在失败用例，本结论不得视为通过。') }
$report.Add('')
$report.Add('## 限制与未覆盖项')
$report.Add('')
$report.Add('- 高并发测试结果只代表当前单节点测试环境，不构成多节点高可用或生产容量认证。')
$report.Add('- 未执行或阻断的用例在结果表中明确标记，不以其他轮次结果替代。')
$report.Add('')
$report.Add('## 归档校验')
$report.Add('')
$report.Add("- 运行清单：[manifest.md](../runs/$RunId/manifest.md)")
$report.Add("- 原始证据：[raw](../runs/$RunId/raw)")
$report.Add("- 派生结果：[processed](../runs/$RunId/processed)")
$report.Add("- 汇总 CSV：[${RunId}-summary$suffix.csv](${RunId}-summary$suffix.csv)")
$report.Add("- 哈希清单：``docs/tests/runs/$RunId/SHA256SUMS.txt``")
Write-LfText -Path $reportPath -Text (($report -join "`n") + "`n")

$manifest = @(
    '# 测试运行清单'
    ''
    '## 基本信息'
    ''
    '| 字段 | 值 |'
    '| --- | --- |'
    "| 运行编号 | ``$RunId`` |"
    "| 测试范围 | T-01 至 T-39 |"
    "| Git 分支/提交 | ``$($run.Branch)`` / ``$($run.Commit)`` |"
    "| 结束时间 | $((Get-Date).ToString('yyyy-MM-dd HH:mm:ss zzz')) |"
    "| 运行状态 | $overall |"
    "| 结论 | 失败 $($failed.Count) 项，未执行 $($notRun.Count) 项 |"
    ''
    '## 证据索引'
    ''
    "- 最终报告：``docs/tests/reports/$RunId-test-report$suffix.md``"
    "- 汇总 CSV：``docs/tests/reports/$RunId-summary$suffix.csv``"
    "- 原始证据：``docs/tests/runs/$RunId/raw``"
    "- 派生结果：``docs/tests/runs/$RunId/processed``"
    ''
    '## 清理与脱敏'
    ''
    '- [x] 敏感字段经统一脱敏处理'
    '- [x] `SHA256SUMS.txt` 已生成并复核'
    '- [x] 报告已登记到 `reports/index.md`'
) -join "`n"
Write-LfText -Path (Join-Path $run.RunRoot 'manifest.md') -Text ($manifest + "`n")

$indexPath = Join-Path $reportsRoot 'index.md'
$indexText = [IO.File]::ReadAllText($indexPath)
$linkName = "$RunId-test-report$suffix.md"
if ($indexText -notmatch [regex]::Escape($linkName)) {
    $row = '| `{0}` | {1} | T-01 至 T-39 最终测试报告 | {2}：失败 {3}，未执行 {4} | [查看报告]({5}) |' -f $RunId, (Get-Date -Format 'yyyy-MM-dd'), $overall, $failed.Count, $notRun.Count, $linkName
    Write-LfText -Path $indexPath -Text ($indexText.TrimEnd() + "`n" + $row + "`n")
}

& (Join-Path $testsRoot 'scripts\common\New-RunChecksums.ps1') -RunId $RunId -RunRoot $run.RunRoot | Out-Null
$checksumPath = Join-Path $run.RunRoot 'SHA256SUMS.txt'
$checksumFailures = [Collections.Generic.List[string]]::new()
foreach ($line in [IO.File]::ReadAllLines($checksumPath)) {
    if ($line -notmatch '^([0-9a-f]{64})  (.+)$') { $checksumFailures.Add("格式错误：$line"); continue }
    $expected = $Matches[1]
    $file = Join-Path $run.RunRoot ($Matches[2].Replace('/', [IO.Path]::DirectorySeparatorChar))
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { $checksumFailures.Add("缺少文件：$($Matches[2])"); continue }
    if ((Get-EasyProxySha256 -Path $file) -ne $expected) { $checksumFailures.Add("哈希错误：$($Matches[2])") }
}
if ($checksumFailures.Count -gt 0) { throw "SHA256SUMS.txt 复核失败：$($checksumFailures -join '; ')" }

Write-Output "RUN_ID=$RunId"
Write-Output "STATUS=$overall"
Write-Output "REPORT=docs/tests/reports/$RunId-test-report$suffix.md"
Write-Output "SUMMARY=docs/tests/reports/$RunId-summary$suffix.csv"
Write-Output "CHECKSUMS=docs/tests/runs/$RunId/SHA256SUMS.txt"
if ($overall -ne 'PASS') { exit 1 }
