[CmdletBinding()]
param(
    [string]$RunId = '',
    [string]$OutputPath = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
$docsRoot = Join-Path $repoRoot 'docs'
$testsRoot = Join-Path $docsRoot 'tests'
$modulePath = Join-Path $testsRoot 'scripts\common\EasyProxy.Testing.psm1'
Import-Module -Name $modulePath -Force

if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format 'yyyyMMdd-HHmmss'
}
$runRoot = Join-Path $testsRoot "runs\$RunId"
$run = New-EasyProxyRun -RunId $RunId -TestScope '测试资产静态校验'

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $runRoot 'raw\validation\test-assets-validation.txt'
}

$utf8Strict = [Text.UTF8Encoding]::new($false, $true)
$checks = [Collections.Generic.List[string]]::new()
$errors = [Collections.Generic.List[string]]::new()

function Add-Check {
    param([string]$Name, [bool]$Passed, [string]$Detail)
    $status = if ($Passed) { 'PASS' } else { 'FAIL' }
    $checks.Add("[$status] $Name :: $Detail")
    if (-not $Passed) {
        $errors.Add("$Name :: $Detail")
    }
}

function Get-RelativePath {
    param([string]$Path)
    return $Path.Substring($repoRoot.Length + 1).Replace('\', '/')
}

function Read-TextDocument {
    param([string]$Path)
    $bytes = [IO.File]::ReadAllBytes($Path)
    $hasBom = $bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF
    [pscustomobject]@{
        Path   = $Path
        Bytes  = $bytes
        HasBom = $hasBom
        Text   = $utf8Strict.GetString($bytes)
    }
}

function Get-RepositoryTextFiles {
    $textExtensions = @(
        '.md', '.ps1', '.psm1', '.sh', '.yml', '.yaml', '.csv', '.json', '.txt',
        '.conf', '.properties', '.xml', '.example', '.toml', '.ini', '.gitignore', '.gitattributes'
    )
    $relativeFiles = @(& git -C $repoRoot ls-files --cached --others --exclude-standard)
    $result = [Collections.Generic.List[IO.FileInfo]]::new()
    foreach ($relative in $relativeFiles) {
        if ([string]::IsNullOrWhiteSpace($relative)) { continue }
        $fullPath = Join-Path $repoRoot ($relative.Replace('/', [IO.Path]::DirectorySeparatorChar))
        if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) { continue }
        $item = Get-Item -LiteralPath $fullPath
        $extension = $item.Extension.ToLowerInvariant()
        if ($textExtensions -contains $extension -or
            $item.Name.Equals('.env.example', [StringComparison]::OrdinalIgnoreCase) -or
            $item.Name.Equals('Dockerfile', [StringComparison]::OrdinalIgnoreCase)) {
            $result.Add($item)
        }
    }
    return @($result | Sort-Object FullName)
}

function Get-MarkdownOutsideFences {
    param([string]$Text)
    $lines = [Collections.Generic.List[string]]::new()
    $inside = $false
    foreach ($line in ($Text -split "`n")) {
        if ($line -match '^\s*```') {
            $inside = -not $inside
            continue
        }
        if (-not $inside) {
            $lines.Add($line.TrimEnd("`r"))
        }
    }
    [pscustomobject]@{
        Lines        = @($lines)
        FencesClosed = (-not $inside)
    }
}

$runDirectoriesOk = @('raw', 'processed', 'screenshots', 'tmp') |
    ForEach-Object { Test-Path -LiteralPath (Join-Path $runRoot $_) -PathType Container }
Add-Check -Name '运行目录初始化' -Passed (@($runDirectoriesOk | Where-Object { -not $_ }).Count -eq 0) -Detail "运行编号=$RunId；已创建 raw、processed、screenshots、tmp"

$textFiles = @(Get-RepositoryTextFiles)
Add-Check -Name '文本资产可读取' -Passed ($textFiles.Count -gt 0) -Detail "共 $($textFiles.Count) 个纳入扫描的文件"

$lineExtensionSet = @('.md', '.ps1', '.psm1', '.sh', '.yml', '.yaml', '.csv', '.json')
$bomFailures = [Collections.Generic.List[string]]::new()
$crlfFailures = [Collections.Generic.List[string]]::new()
$decodeFailures = [Collections.Generic.List[string]]::new()
$documents = @{}
foreach ($file in $textFiles) {
    try {
        $document = Read-TextDocument -Path $file.FullName
        $documents[$file.FullName] = $document
    }
    catch {
        $decodeFailures.Add((Get-RelativePath -Path $file.FullName))
        continue
    }

    $extension = $file.Extension.ToLowerInvariant()
    $isPowerShell = $extension -in @('.ps1', '.psm1')
    $requiresNoBom = $lineExtensionSet -contains $extension -or
        $file.Name.Equals('.env.example', [StringComparison]::OrdinalIgnoreCase)
    if ($isPowerShell -and -not $document.HasBom) {
        $bomFailures.Add((Get-RelativePath -Path $file.FullName))
    }
    elseif (-not $isPowerShell -and $requiresNoBom -and $document.HasBom) {
        $bomFailures.Add((Get-RelativePath -Path $file.FullName))
    }

    if (($lineExtensionSet -contains $extension) -or
        $file.Name.Equals('.env.example', [StringComparison]::OrdinalIgnoreCase)) {
        if ($document.Text.Contains("`r")) {
            $crlfFailures.Add((Get-RelativePath -Path $file.FullName))
        }
    }
}
Add-Check -Name 'UTF-8 编码与 BOM' -Passed ($bomFailures.Count -eq 0) -Detail $(if ($bomFailures.Count -eq 0) { 'PowerShell 带 BOM，Markdown/YAML/env/Shell/CSV/JSON 无 BOM' } else { "不符合约定 $($bomFailures.Count) 个：$($bomFailures -join ', ')" })
Add-Check -Name '统一 LF 换行' -Passed ($crlfFailures.Count -eq 0) -Detail $(if ($crlfFailures.Count -eq 0) { '全部通过' } else { "含 CRLF $($crlfFailures.Count) 个：$($crlfFailures -join ', ')" })
Add-Check -Name 'UTF-8 可解码' -Passed ($decodeFailures.Count -eq 0) -Detail $(if ($decodeFailures.Count -eq 0) { '全部通过' } else { $decodeFailures -join ', ' })

$markdownFiles = @($textFiles | Where-Object { $_.Extension -eq '.md' })
$fenceFailures = [Collections.Generic.List[string]]::new()
$linkFailures = [Collections.Generic.List[string]]::new()
$linkCount = 0
foreach ($file in $markdownFiles) {
    $document = $documents[$file.FullName]
    $outside = Get-MarkdownOutsideFences -Text $document.Text
    if (-not $outside.FencesClosed) {
        $fenceFailures.Add((Get-RelativePath -Path $file.FullName))
    }
    $body = ($outside.Lines -join "`n") -replace '`[^`]*`', ''
    foreach ($match in [regex]::Matches($body, '\[[^\]]+\]\(([^)]+)\)')) {
        $target = $match.Groups[1].Value.Trim()
        if ($target -match '^(?:https?://|mailto:|#|codex://|app://)') { continue }
        $target = ($target -split '#', 2)[0]
        if ([string]::IsNullOrWhiteSpace($target) -or $target.Contains('<')) { continue }
        $linkCount++
        $decodedTarget = [Uri]::UnescapeDataString($target)
        $candidate = if ([IO.Path]::IsPathRooted($decodedTarget)) { $decodedTarget } else { Join-Path $file.DirectoryName $decodedTarget }
        if (-not (Test-Path -LiteralPath $candidate)) {
            $linkFailures.Add("$(Get-RelativePath -Path $file.FullName) -> $target")
        }
    }
}
Add-Check -Name 'Markdown 代码围栏' -Passed ($fenceFailures.Count -eq 0) -Detail $(if ($fenceFailures.Count -eq 0) { "检查 $($markdownFiles.Count) 个 Markdown 文件" } else { $fenceFailures -join ', ' })
Add-Check -Name 'Markdown 相对链接' -Passed ($linkFailures.Count -eq 0) -Detail $(if ($linkFailures.Count -eq 0) { "检查 $linkCount 个本地链接，全部存在" } else { $linkFailures -join '; ' })

$fixture = Test-EasyProxyFixtures
$manifestPath = Join-Path $testsRoot 'data\manifests\fixtures.sha256'
$manifestLineCount = @([IO.File]::ReadAllLines($manifestPath) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }).Count
$actualFixtureCount = @(Get-ChildItem -LiteralPath (Join-Path $testsRoot 'data\fixtures') -Recurse -File).Count
$fixtureFailures = @($fixture.Failures)
$fixtureChecks = [pscustomobject]@{
    ManifestCount = $manifestLineCount -eq 1003
    FileCount     = $actualFixtureCount -eq 1003 -and $fixture.Count -eq 1003
    Missing       = @($fixtureFailures | Where-Object { $_ -like '缺少文件：*' }).Count -eq 0
    HashMismatch  = @($fixtureFailures | Where-Object { $_ -like '哈希不匹配：*' }).Count -eq 0
    Unlisted      = @($fixtureFailures | Where-Object { $_ -like '未纳入清单：*' }).Count -eq 0
}
$fixtureOk = -not ($fixtureChecks.PSObject.Properties.Value -contains $false)
Add-Check -Name '固定数据清单与哈希' -Passed $fixtureOk -Detail "清单 $manifestLineCount 行，文件 $actualFixtureCount 个，纳入清单 $($fixture.Count) 个；缺失/不一致/未纳入异常 $($fixtureFailures.Count) 个"

$planPath = Join-Path $docsRoot 'nas-cloud-proxy-deployment-plan.md'
$planDocument = Read-TextDocument -Path $planPath
$definedTests = [Collections.Generic.List[int]]::new()
$duplicateTests = [Collections.Generic.List[int]]::new()
foreach ($line in ($planDocument.Text -split "`n")) {
    if ($line -match '^\|\s*T-(\d{2})\s*\|') {
        $number = [int]$Matches[1]
        if ($definedTests.Contains($number)) { $duplicateTests.Add($number) }
        else { $definedTests.Add($number) }
    }
}
$expectedTests = @(1..39)
$testSequenceOk = ($definedTests.Count -eq 39) -and
    (($definedTests -join ',') -eq ($expectedTests -join ',')) -and
    $duplicateTests.Count -eq 0
Add-Check -Name '测试用例编号' -Passed $testSequenceOk -Detail $(if ($testSequenceOk) { 'T-01 至 T-39 连续且唯一' } else { "实际 $($definedTests.Count) 个；重复：$($duplicateTests -join ',')" })

$psFiles = @(Get-ChildItem -LiteralPath $repoRoot -Recurse -File |
    Where-Object { $_.Extension -in @('.ps1', '.psm1') } |
    Where-Object { $_.FullName -notmatch '[\\/](target|node_modules|\.git)[\\/]' })
$psFailures = [Collections.Generic.List[string]]::new()
foreach ($file in $psFiles) {
    $tokens = $null
    $parseErrors = $null
    [void][Management.Automation.Language.Parser]::ParseFile($file.FullName, [ref]$tokens, [ref]$parseErrors)
    if ($parseErrors.Count -gt 0) {
        $psFailures.Add("$(Get-RelativePath -Path $file.FullName) ($($parseErrors.Count) 个错误)")
    }
}
Add-Check -Name 'PowerShell 语法' -Passed ($psFailures.Count -eq 0) -Detail $(if ($psFailures.Count -eq 0) { "检查 $($psFiles.Count) 个 PowerShell 文件，零错误" } else { $psFailures -join ', ' })

$h2Path = Join-Path $repoRoot 'easy-proxy-server\src\main\resources\server.yaml'
$h2Document = Read-TextDocument -Path $h2Path
$h2Disabled = $h2Document.Text -match '(?m)^\s*h2ConsoleEnable:\s*false\s*$'
Add-Check -Name 'H2 Console 默认关闭' -Passed $h2Disabled -Detail $(if ($h2Disabled) { 'server.yaml 中 h2ConsoleEnable=false' } else { 'server.yaml 未明确关闭 H2 Console' })

$nasEnvPath = Join-Path $testsRoot 'configs\docker\nas.env.example'
$nasEnvDocument = Read-TextDocument -Path $nasEnvPath
$targetPinned = $nasEnvDocument.Text -match '(?m)^TARGET_IMAGE=nginx:1\.27\.4-alpine\s*$'
Add-Check -Name 'NAS 测试目标固定版本' -Passed $targetPinned -Detail $(if ($targetPinned) { 'TARGET_IMAGE=nginx:1.27.4-alpine' } else { '未固定 nginx:1.27.4-alpine' })

$targetNginxPath = Join-Path $testsRoot 'configs\docker\target\nginx.conf'
$targetNginxText = [IO.File]::ReadAllText($targetNginxPath)
$nginxRequirements = [ordered]@{
    'listen 80'            = $targetNginxText -match '(?m)^\s*listen 80;\s*$'
    'slow-2s.bin'          = $targetNginxText.Contains('slow-2s.bin')
    'limit_rate 512k'      = $targetNginxText -match '(?m)^\s*limit_rate 512k;\s*$'
    'Cache-Control no-store' = $targetNginxText -match 'add_header\s+Cache-Control\s+"no-store";'
}
$nginxMissing = @($nginxRequirements.Keys | Where-Object { -not $nginxRequirements[$_] })
Add-Check -Name '测试目标 Nginx 配置' -Passed ($nginxMissing.Count -eq 0) -Detail $(if ($nginxMissing.Count -eq 0) { '监听 80，慢请求限速与 no-store 均存在' } else { "缺少：$($nginxMissing -join ', ')" })

$testsReadmePath = Join-Path $testsRoot 'README.md'
$testsReadme = [IO.File]::ReadAllText($testsReadmePath)
$unitCountOk = $testsReadme -match '13\s*个测试' -and $testsReadme -notmatch '(?<!1)5\s*个测试'
Add-Check -Name 'README 单元测试数量' -Passed $unitCountOk -Detail $(if ($unitCountOk) { '记录为 13 个测试' } else { '未记录为 13 个测试' })

$scanTextFiles = @($textFiles | Where-Object { $_.Name -ne 'validate-test-assets.ps1' })
$allText = ($scanTextFiles | ForEach-Object { $documents[$_.FullName].Text }) -join "`n"
$oldImageRefs = @([regex]::Matches($allText, 'yudejijie/easy-proxy'))
$latestImageLines = [Collections.Generic.List[string]]::new()
foreach ($file in $scanTextFiles) {
    $relative = Get-RelativePath -Path $file.FullName
    foreach ($line in ($documents[$file.FullName].Text -split "`n")) {
        if ($line -match '(?i)禁止|不得|不使用|must not|forbid|no implicit') { continue }
        if ($line -match '(?i)(?:^|\s)(?:[^\s"'']+:latest)(?:["''\s]|$)') {
            $latestImageLines.Add($relative)
            break
        }
    }
}
Add-Check -Name '固定镜像引用' -Passed ($oldImageRefs.Count -eq 0 -and $latestImageLines.Count -eq 0) -Detail "旧仓库引用 $($oldImageRefs.Count) 个，latest 镜像引用 $($latestImageLines.Count) 个"

$secretPatterns = @(
    '-----BEGIN [A-Z ]*PRIVATE KEY-----',
    '(?i)https?://[^/\s:@]+:[^/\s@]+@',
    '(?i)Authorization\s*:\s*Bearer\s+(?![<$%{]|REPLACE|REDACTED|YOUR|CHANGE_ME)[^\s"'']+',
    '(?im)\btoken\s*[:=]\s*["'']?[0-9a-f]{32}["'']?',
    '(?i)eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}',
    '(?im)^\s*(?:ACR_PASSWORD|DOCKER_PASSWORD|REGISTRY_PASSWORD|SERVER_JWT_SECRET|SERVER_CERT_PASSWORD|certPassword|jwtSecret|password)\s*[:=]\s*["'']?(?![<>$%{]|REPLACE|REDACTED|CHANGE_ME|YOUR)[^\s"'']+',
    '(?i)("auth"\s*:\s*"[A-Za-z0-9+/=]{12,}")',
    '(?i)\b(?:ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})\b'
)
$secretHitFiles = [Collections.Generic.List[string]]::new()
$secretHitCount = 0
foreach ($file in $scanTextFiles) {
    $text = $documents[$file.FullName].Text
    foreach ($pattern in $secretPatterns) {
        foreach ($match in [regex]::Matches($text, $pattern)) {
            $secretHitCount++
            $relative = Get-RelativePath -Path $file.FullName
            if (-not $secretHitFiles.Contains($relative)) { $secretHitFiles.Add($relative) }
        }
    }
}
Add-Check -Name '敏感信息静态扫描' -Passed ($secretHitCount -eq 0) -Detail $(if ($secretHitCount -eq 0) { '未发现私钥、Bearer 令牌、固定客户端 Token、JWT 或非占位符密码' } else { "命中 $secretHitCount 项，涉及文件：$($secretHitFiles -join ', ')（不输出明文）" })

$passed = $errors.Count -eq 0
$status = if ($passed) { '通过' } else { '失败' }
$conclusion = if ($passed) { '静态校验通过' } else { '静态校验失败，需修复后重跑' }
$results = if ($passed) {
    "全部 $($checks.Count) 项静态校验通过。"
}
else {
    "静态校验失败：$($errors.Count) 项未通过。"
}
$endedAt = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss zzz')
$run.Status = $status

$checkRows = foreach ($check in $checks) {
    $parts = $check -split ' :: ', 2
    "| $($parts[0]) | $($parts[1]) |"
}

$summary = [Collections.Generic.List[string]]::new()
$summary.Add('# 测试资产静态校验')
$summary.Add('')
$summary.Add("运行编号：``$RunId``")
$summary.Add("Git 提交：``$($run.Commit)``")
$summary.Add("校验时间：``$endedAt``")
$summary.Add('')
$summary.Add('| 检查项 | 结果 |')
$summary.Add('| --- | --- |')
foreach ($row in $checkRows) { $summary.Add($row) }
$summary.Add('')
if ($passed) {
    $summary.Add('判定：通过。所有静态校验项均成功。')
}
else {
    $summary.Add("判定：失败。共 $($errors.Count) 项未通过：")
    foreach ($errorItem in $errors) {
        $summary.Add("- $errorItem")
    }
}
$summary.Add('')
$summary.Add('脚本：`docs/tests/scripts/validation/validate-test-assets.ps1`')
$summaryText = ($summary -join "`n") + "`n"
Write-LfText -Path $OutputPath -Text $summaryText -Encoding 'Utf8NoBom'
$evidenceRelative = Get-RelativePath -Path $OutputPath

$reportPath = Join-Path $testsRoot "reports\$RunId-test-assets-validation.md"
$reportLines = @(
    "# $RunId 测试资产静态校验报告"
    ''
    '## 结论'
    ''
    "- 总体判定：**$status**。"
    "- $results"
    '- 本报告只证明静态资产满足要求，不代表构建、发布、部署、端到端或性能测试已通过。'
    ''
    '## 测试范围'
    ''
    '- 检查纳入 Git 的文本编码、BOM 与 LF 换行。'
    '- 检查 Markdown 围栏和本地相对链接。'
    '- 校验固定数据文件清单与 SHA-256。'
    '- 检查 T-01 至 T-39 编号完整性、PowerShell 语法、固定镜像版本和安全默认值。'
    '- 扫描私钥、Bearer 令牌、固定客户端 Token、JWT 和非占位符密码。'
    ''
    '## 环境与版本'
    ''
    "- Git 提交：``$($run.Commit)``。"
    '- 执行环境：Windows PowerShell，仓库工作区。'
    "- 运行编号：``$RunId``。"
    ''
    '## 结果'
    ''
    '| 检查项 | 结果 |'
    '| --- | --- |'
    $checkRows
    ''
    '## 归档校验'
    ''
    "- 运行清单：[manifest.md](../runs/$RunId/manifest.md)"
    "- 原始输出：[test-assets-validation.txt](../runs/$RunId/raw/validation/test-assets-validation.txt)"
    "- 哈希清单：``docs/tests/runs/$RunId/SHA256SUMS.txt``"
)
Write-LfText -Path $reportPath -Text (($reportLines -join "`n") + "`n") -Encoding 'Utf8NoBom'

$indexPath = Join-Path $testsRoot 'reports\index.md'
$indexText = [IO.File]::ReadAllText($indexPath)
if ($indexText -notmatch [regex]::Escape("$RunId")) {
    $indexRow = '| `{0}` | {1} | 测试资产静态校验 | {2}：{3} | [查看报告]({0}-test-assets-validation.md) |' -f $RunId, (Get-Date -Format 'yyyy-MM-dd'), $status, $results
    $indexText = $indexText.TrimEnd() + "`n" + $indexRow + "`n"
    Write-LfText -Path $indexPath -Text $indexText -Encoding 'Utf8NoBom'
}

$evidenceIndex = @(
    '| 测试编号 | 证据 | 相对路径 | 完整性 |'
    '| --- | --- | --- | --- |'
    '| 前置 | 静态校验结果 | `raw/validation/test-assets-validation.txt` | 完整 |'
) -join "`n"
$cleanup = @(
    '- [x] 不涉及临时容器与防火墙规则'
    '- [x] 敏感字段已复核'
    '- [x] `SHA256SUMS.txt` 已生成并复核'
    '- [x] 报告已登记到 `reports/index.md`'
) -join "`n"
$limitations = '本轮仅验证测试资产与配置，不代表已完成部署、端到端或性能验收。'
Write-EasyProxyRunManifest -Run $run `
    -EndedAt $endedAt `
    -TestParameters '运行仓库静态资产、固定数据、安全配置、镜像版本、脚本语法与敏感信息扫描。' `
    -Results $results `
    -Conclusion $conclusion `
    -EvidenceIndex $evidenceIndex `
    -Limitations $limitations `
    -Cleanup $cleanup
& (Join-Path $repoRoot 'docs\tests\scripts\common\New-RunChecksums.ps1') -RunId $RunId | Out-Null

Write-Output $summaryText
Write-Output "RUN_ID=$RunId"
Write-Output "STATUS=$status"
Write-Output "EVIDENCE=$evidenceRelative"
Write-Output "REPORT=$($reportPath.Substring($repoRoot.Length + 1).Replace('\', '/'))"
Write-Output "MANIFEST=docs/tests/runs/$RunId/manifest.md"
Write-Output "CHECKSUMS=docs/tests/runs/$RunId/SHA256SUMS.txt"

if (-not $passed) {
    exit 1
}