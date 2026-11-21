[CmdletBinding()]
param(
    [string]$RunId = '20261121-235500',
    [string]$OutputPath = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
$docsRoot = Join-Path $repoRoot 'docs'
$planPath = Join-Path $docsRoot 'nas-cloud-proxy-deployment-plan.md'
$testsRoot = Join-Path $docsRoot 'tests'
$runRoot = Join-Path $testsRoot "runs\$RunId"
$reportPath = Join-Path $testsRoot "reports\$RunId-acr-build-validation.md"
$manifestPath = Join-Path $runRoot 'manifest.md'

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $runRoot 'raw\validation\test-assets-validation.txt'
}
New-Item -ItemType Directory -Path (Split-Path $OutputPath -Parent) -Force | Out-Null

$utf8Strict = New-Object System.Text.UTF8Encoding($false, $true)
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$errors = New-Object System.Collections.Generic.List[string]
$checks = New-Object System.Collections.Generic.List[string]

function Add-Check {
    param([string]$Name, [bool]$Passed, [string]$Detail)
    $status = if ($Passed) { 'PASS' } else { 'FAIL' }
    $line = "[$status] $Name :: $Detail"
    $checks.Add($line)
    if (-not $Passed) {
        $errors.Add("$Name :: $Detail")
    }
}

function Get-TextAssetFiles {
    $allowedExtensions = @('.md', '.ps1', '.sh', '.yml', '.yaml', '.example', '.txt')
    Get-ChildItem -LiteralPath $docsRoot -Recurse -File |
        Where-Object {
            $relative = $_.FullName.Substring($docsRoot.Length + 1).Replace('\', '/')
            $relative -notmatch '^tests/runs/[^/]+/raw/' -and
            ($allowedExtensions -contains $_.Extension.ToLowerInvariant() -or $_.Name.EndsWith('.env.example', [StringComparison]::OrdinalIgnoreCase))
        } |
        Sort-Object FullName
}

function Read-TextFile {
    param([string]$Path)
    $bytes = [IO.File]::ReadAllBytes($Path)
    $hasBom = $bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF
    $text = $utf8Strict.GetString($bytes)
    [pscustomobject]@{
        Path = $Path
        Bytes = $bytes
        HasBom = $hasBom
        Text = $text
        Lines = @($text -split "`n")
    }
}

function Get-MarkdownOutsideFences {
    param([string]$Text)
    $result = New-Object System.Collections.Generic.List[string]
    $inside = $false
    foreach ($line in ($Text -split "`n")) {
        if ($line -match '^\s*```') {
            $inside = -not $inside
            continue
        }
        if (-not $inside) {
            $result.Add($line.TrimEnd("`r"))
        }
    }
    [pscustomobject]@{
        Lines = @($result)
        FencesClosed = (-not $inside)
    }
}

$textAssets = @(Get-TextAssetFiles)
Add-Check -Name '文本资产可读取' -Passed ($textAssets.Count -gt 0) -Detail "共 $($textAssets.Count) 个文件"

$unexpectedBomFailures = New-Object System.Collections.Generic.List[string]
$missingScriptBomFailures = New-Object System.Collections.Generic.List[string]
$crlfFailures = New-Object System.Collections.Generic.List[string]
$decodeFailures = New-Object System.Collections.Generic.List[string]
foreach ($file in $textAssets) {
    try {
        $doc = Read-TextFile -Path $file.FullName
    }
    catch {
        $decodeFailures.Add($file.FullName.Substring($repoRoot.Length + 1))
        continue
    }
    if ($file.Extension.ToLowerInvariant() -eq '.ps1') {
        if (-not $doc.HasBom) {
            $missingScriptBomFailures.Add($doc.Path.Substring($repoRoot.Length + 1))
        }
    }
    elseif ($doc.HasBom) {
        $unexpectedBomFailures.Add($doc.Path.Substring($repoRoot.Length + 1))
    }
    if ($doc.Text.Contains("`r")) {
        $crlfFailures.Add($doc.Path.Substring($repoRoot.Length + 1))
    }
}
$encodingOk = $unexpectedBomFailures.Count -eq 0 -and $missingScriptBomFailures.Count -eq 0
$encodingDetail = if ($encodingOk) { 'Markdown/配置/Shell 为 UTF-8 无 BOM；PowerShell 脚本带 BOM 以兼容 Windows PowerShell 5.1' } else { "非脚本多出 BOM：$($unexpectedBomFailures -join ', ')；脚本缺少 BOM：$($missingScriptBomFailures -join ', ')" }
Add-Check -Name 'UTF-8 编码与 BOM' -Passed $encodingOk -Detail $encodingDetail
Add-Check -Name '统一 LF 换行' -Passed ($crlfFailures.Count -eq 0) -Detail $(if ($crlfFailures.Count -eq 0) { '全部通过' } else { $crlfFailures -join ', ' })
Add-Check -Name 'UTF-8 可解码' -Passed ($decodeFailures.Count -eq 0) -Detail $(if ($decodeFailures.Count -eq 0) { '全部通过' } else { $decodeFailures -join ', ' })

$markdownFiles = @($textAssets | Where-Object { $_.Extension -eq '.md' })
$fenceFailures = New-Object System.Collections.Generic.List[string]
foreach ($file in $markdownFiles) {
    $doc = Read-TextFile -Path $file.FullName
    $outside = Get-MarkdownOutsideFences -Text $doc.Text
    if (-not $outside.FencesClosed) {
        $fenceFailures.Add($file.FullName.Substring($repoRoot.Length + 1))
    }
}
Add-Check -Name 'Markdown 代码围栏配对' -Passed ($fenceFailures.Count -eq 0) -Detail $(if ($fenceFailures.Count -eq 0) { "检查 $($markdownFiles.Count) 个 Markdown 文件" } else { $fenceFailures -join ', ' })

$missingRequired = New-Object System.Collections.Generic.List[string]
foreach ($required in @($planPath, (Join-Path $testsRoot 'README.md'), (Join-Path $testsRoot 'reports\index.md'), $manifestPath, $reportPath)) {
    if (-not (Test-Path -LiteralPath $required -PathType Leaf)) {
        $missingRequired.Add($required.Substring($repoRoot.Length + 1))
    }
}
Add-Check -Name '必备文档存在' -Passed ($missingRequired.Count -eq 0) -Detail $(if ($missingRequired.Count -eq 0) { '方案、索引、清单和报告齐全' } else { $missingRequired -join ', ' })

$planDoc = Read-TextFile -Path $planPath
$planOutside = Get-MarkdownOutsideFences -Text $planDoc.Text
$h2 = New-Object System.Collections.Generic.List[int]
$h3ByH2 = @{}
$h4ByH3 = @{}
$currentH2 = $null
$currentH3 = $null
$headingErrors = New-Object System.Collections.Generic.List[string]
foreach ($line in $planOutside.Lines) {
    if ($line -match '^##\s+(\d+)\.\s+') {
        $number = [int]$Matches[1]
        $h2.Add($number)
        if (-not $h3ByH2.ContainsKey($number)) { $h3ByH2[$number] = New-Object System.Collections.Generic.List[int] }
        $currentH2 = $number
        $currentH3 = $null
        continue
    }
    if ($line -match '^###\s+(\d+)\.(\d+)\s+') {
        $major = [int]$Matches[1]
        $minor = [int]$Matches[2]
        if ($null -eq $currentH2 -or $major -ne $currentH2) {
            $headingErrors.Add("H3 编号不属于当前 H2：$line")
        }
        else {
            $h3ByH2[$major].Add($minor)
        }
        $currentH3 = "$major.$minor"
        if (-not $h4ByH3.ContainsKey($currentH3)) { $h4ByH3[$currentH3] = New-Object System.Collections.Generic.List[int] }
        continue
    }
    if ($line -match '^####\s+(\d+)\.(\d+)\.(\d+)\s+') {
        $major = [int]$Matches[1]
        $minor = [int]$Matches[2]
        $patch = [int]$Matches[3]
        $key = "$major.$minor"
        if ($null -eq $currentH3 -or $key -ne $currentH3) {
            $headingErrors.Add("H4 编号不属于当前 H3：$line")
        }
        else {
            $h4ByH3[$key].Add($patch)
        }
    }
}
$expectedH2 = @(1..14)
if (($h2 -join ',') -ne ($expectedH2 -join ',')) {
    $headingErrors.Add("H2 应为 1..14，实际：$($h2 -join ',')")
}
foreach ($key in $h3ByH2.Keys) {
    $values = @($h3ByH2[$key])
    if ($values.Count -gt 0) {
        $expected = @(1..$values.Count)
        if (($values -join ',') -ne ($expected -join ',')) {
            $headingErrors.Add("H3 编号不连续：$key -> $($values -join ',')")
        }
    }
}
foreach ($key in $h4ByH3.Keys) {
    $values = @($h4ByH3[$key])
    if ($values.Count -gt 0) {
        $expected = @(1..$values.Count)
        if (($values -join ',') -ne ($expected -join ',')) {
            $headingErrors.Add("H4 编号不连续：$key -> $($values -join ',')")
        }
    }
}
Add-Check -Name '方案标题编号' -Passed ($headingErrors.Count -eq 0) -Detail $(if ($headingErrors.Count -eq 0) { 'H2/H3/H4 编号连续且归属正确' } else { $headingErrors -join '; ' })

$definedTests = New-Object System.Collections.Generic.List[int]
$duplicateTests = New-Object System.Collections.Generic.List[int]
foreach ($line in $planOutside.Lines) {
    if ($line -match '^\|\s*T-(\d{2})\s*\|') {
        $number = [int]$Matches[1]
        if ($definedTests.Contains($number)) {
            $duplicateTests.Add($number)
        }
        else {
            $definedTests.Add($number)
        }
    }
}
$expectedTests = @(1..39)
$testSequenceOk = (($definedTests -join ',') -eq ($expectedTests -join ',')) -and $duplicateTests.Count -eq 0
Add-Check -Name '测试用例编号' -Passed $testSequenceOk -Detail $(if ($testSequenceOk) { 'T-01 至 T-39 连续且唯一' } else { "实际：$($definedTests -join ','); 重复：$($duplicateTests -join ',')" })

$allText = ($textAssets | Where-Object { $_.Name -ne 'validate-test-assets.ps1' } | ForEach-Object { (Read-TextFile -Path $_.FullName).Text }) -join "`n"
$oldImageRefs = @([regex]::Matches($allText, 'yudejijie/easy-proxy') | ForEach-Object { $_.Value })
$latestEnvRefs = @([regex]::Matches($allText, '(?m)^\s*EASY_PROXY_VERSION\s*=\s*latest\s*$') | ForEach-Object { $_.Value })
$latestImageRefs = @([regex]::Matches($allText, '(?m)^\s*image:\s*.*:latest(?:\s|$|["''])') | ForEach-Object { $_.Value })
Add-Check -Name '固定镜像引用' -Passed (($oldImageRefs.Count + $latestEnvRefs.Count + $latestImageRefs.Count) -eq 0) -Detail "旧仓库引用=$($oldImageRefs.Count)，latest 环境变量=$($latestEnvRefs.Count)，latest 镜像=$($latestImageRefs.Count)"

$secretPatterns = @(
    '-----BEGIN [A-Z ]*PRIVATE KEY-----',
    '(?im)^\s*(?:ACR_PASSWORD|DOCKER_PASSWORD|REGISTRY_PASSWORD)\s*=\s*(?!<CHANGE_ME)[^\s#]+',
    '(?i)https?://[^/\s:@]+:[^/\s@]+@',
    '(?i)("auth"\s*:\s*"[A-Za-z0-9+/=]{12,}")',
    '(?i)\b(ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})\b'
)
$secretHits = New-Object System.Collections.Generic.List[string]
foreach ($pattern in $secretPatterns) {
    foreach ($match in [regex]::Matches($allText, $pattern)) {
        $secretHits.Add($match.Value.Substring(0, [Math]::Min(24, $match.Value.Length)))
    }
}
Add-Check -Name '敏感信息静态扫描' -Passed ($secretHits.Count -eq 0) -Detail $(if ($secretHits.Count -eq 0) { '未发现私钥、仓库凭据或嵌入式 URL 密码' } else { "命中 $($secretHits.Count) 项（不在报告中回显明文）" })

$linkFailures = New-Object System.Collections.Generic.List[string]
$linkCount = 0
foreach ($file in $markdownFiles) {
    $doc = Read-TextFile -Path $file.FullName
    $outside = Get-MarkdownOutsideFences -Text $doc.Text
    $body = ($outside.Lines -join "`n") -replace '`[^`]*`', ''
    foreach ($match in [regex]::Matches($body, '\[[^\]]+\]\(([^)]+)\)')) {
        $target = $match.Groups[1].Value.Trim()
        if ($target -match '^(?:https?://|mailto:|#|codex://|app://)') { continue }
        $target = ($target -split '#', 2)[0]
        if ([string]::IsNullOrWhiteSpace($target) -or $target.Contains('<')) { continue }
        $linkCount++
        $target = [Uri]::UnescapeDataString($target)
        $candidate = if ([IO.Path]::IsPathRooted($target)) { $target } else { Join-Path $file.DirectoryName $target }
        if (-not (Test-Path -LiteralPath $candidate)) {
            $linkFailures.Add("$($file.FullName.Substring($repoRoot.Length + 1)) -> $target")
        }
    }
}
Add-Check -Name 'Markdown 相对链接' -Passed ($linkFailures.Count -eq 0) -Detail $(if ($linkFailures.Count -eq 0) { "检查 $linkCount 个本地链接，全部存在" } else { $linkFailures -join '; ' })

$psFiles = @(Get-ChildItem -LiteralPath (Join-Path $testsRoot 'scripts') -Recurse -File -Filter '*.ps1')
$psFailures = New-Object System.Collections.Generic.List[string]
foreach ($file in $psFiles) {
    $tokens = $null
    $parseErrors = $null
    [void][System.Management.Automation.Language.Parser]::ParseFile($file.FullName, [ref]$tokens, [ref]$parseErrors)
    if ($parseErrors.Count -gt 0) {
        $psFailures.Add($file.FullName.Substring($repoRoot.Length + 1))
    }
}
$webDockerfilePath = Join-Path $repoRoot 'easy-proxy-web\Dockerfile'
$webNginxPath = Join-Path $repoRoot 'easy-proxy-web\nginx.conf'
$rootComposePath = Join-Path $repoRoot 'docker-compose.yml'
$webDockerfileText = [IO.File]::ReadAllText($webDockerfilePath)
$webNginxText = [IO.File]::ReadAllText($webNginxPath)
$rootComposeText = [IO.File]::ReadAllText($rootComposePath)
$webPortOk = $webDockerfileText -match '(?m)^EXPOSE 21092\s*$' -and
    $webNginxText -match '(?m)^\s*listen 21092;\s*$' -and
    $rootComposeText -match '(?m)^\s*-\s*"21092:21092"' -and
    $rootComposeText -match 'easy-proxy-web/nginx\.conf' -and
    $rootComposeText -notmatch '21093|nginx-docker\.conf'
Add-Check -Name 'Web 端口与配置一致性' -Passed $webPortOk -Detail $(if ($webPortOk) { 'Dockerfile、Nginx 与根 Compose 均为 21092，且挂载文件存在' } else { 'Dockerfile、Nginx、Compose 端口或挂载路径不一致' })
Add-Check -Name 'PowerShell 语法' -Passed ($psFailures.Count -eq 0) -Detail $(if ($psFailures.Count -eq 0) { "检查 $($psFiles.Count) 个脚本" } else { $psFailures -join ', ' })

$authEntry = Get-Content -LiteralPath (Join-Path $runRoot 'raw\validation\acr-logout.txt') -Raw -ErrorAction SilentlyContinue
$authOk = $null -ne $authEntry -and $authEntry -match '(?m)^ACR_AUTH_ENTRY=no\s*$'
Add-Check -Name 'ACR 凭据清理' -Passed $authOk -Detail $(if ($authOk) { 'ACR_AUTH_ENTRY=no' } else { '未找到有效注销证据' })

$summary = New-Object System.Collections.Generic.List[string]
$summary.Add("# 测试资产静态校验")
$summary.Add('')
$summary.Add("运行编号：``$RunId``")
$summary.Add("校验时间：``$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')``")
$summary.Add('')
$summary.Add('| 检查项 | 结果 |')
$summary.Add('| --- | --- |')
foreach ($check in $checks) {
    $parts = $check -split ' :: ', 2
    $summary.Add("| $($parts[0]) | $($parts[1]) |")
}
$summary.Add('')
if ($errors.Count -eq 0) {
    $summary.Add('判定：通过。所有静态校验项均成功。')
}
else {
    $summary.Add("判定：失败。共 $($errors.Count) 项未通过：")
    foreach ($errorItem in $errors) {
        $summary.Add("- $errorItem")
    }
}
$summary.Add('')
$summary.Add("脚本：``docs/tests/scripts/validation/validate-test-assets.ps1``")
$summaryText = ($summary -join "`n") + "`n"
[IO.File]::WriteAllText($OutputPath, $summaryText, $utf8NoBom)
Write-Output $summaryText

if ($errors.Count -gt 0) {
    exit 1
}