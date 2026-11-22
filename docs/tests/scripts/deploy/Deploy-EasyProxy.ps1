[CmdletBinding()]
param(
    [string]$RunId = '20261122-121200',
    [string]$Tag = 'test-20261122-022005-689e009',
    [string]$CloudHost = '68.64.181.242',
    [int]$CloudPort = 1514,
    [string]$CloudUser = 'yudejijie',
    [string]$NasHost = '192.168.1.9',
    [int]$NasPort = 10080,
    [string]$NasUser = 'chenkeyudev',
    [string]$PublicHost = '68.64.181.242',
    [int]$ProxyPort = 21090,
    [int]$ApiPort = 21091,
    [int]$WebPort = 21092,
    [int]$RulePort = 28080,
    [int]$NasTargetPort = 18080,
    [string]$ClientName = 'nas-test',
    [string]$RuleName = 'nas-http-18080'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($RunId -notmatch '^\d{8}-\d{6}(?:-retry\d{2})?$') { throw "运行编号格式错误：$RunId" }
if ($Tag -eq 'latest' -or $Tag -notmatch '^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$') { throw "镜像标签不合法：$Tag" }

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
$testsRoot = Join-Path $repoRoot 'docs\tests'
$runRoot = Join-Path $testsRoot "runs\$RunId"
if (-not (Test-Path -LiteralPath $runRoot -PathType Container)) { throw "运行目录不存在：$runRoot" }
$rawDocker = Join-Path $runRoot 'raw\docker'
$rawFunctional = Join-Path $runRoot 'raw\functional'
$processed = Join-Path $runRoot 'processed\summaries'
New-Item -ItemType Directory -Path $rawDocker, $rawFunctional, $processed -Force | Out-Null

Import-Module -Name (Join-Path $testsRoot 'scripts\common\EasyProxy.Testing.psm1') -Force
Import-Module -Name (Join-Path $testsRoot 'scripts\common\EasyProxy.Secrets.psm1') -Force
$secret = Read-EasyProxySecretMaterial -RunId $RunId
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$cloudRollbackSuffix = "-rollback-$timestamp"
$nasRollbackSuffix = "-rollback-$timestamp"
$cloudComposeProject = "easy-proxy-cloud-$timestamp"
$nasComposeProject = "easy-proxy-nas-$timestamp"
$apiBase = "http://$PublicHost`:$WebPort"
$serverImage = "registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:$Tag"
$clientImage = "registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:$Tag"
$webImage = "registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:$Tag"
$targetImage = 'nginx:1.27.4-alpine'
$checks = [Collections.Generic.List[string]]::new()
$failures = [Collections.Generic.List[string]]::new()

function Add-DeployCheck {
    param([string]$Name, [bool]$Passed, [string]$Detail)
    $status = if ($Passed) { 'PASS' } else { 'FAIL' }
    $checks.Add("| $Name | $status | $Detail |")
    if (-not $Passed) { $failures.Add("$Name：$Detail") }
}

function Write-RemoteEvidence {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$HostName,
        [Parameter(Mandatory)][int]$Port,
        [Parameter(Mandatory)][string]$User,
        [Parameter(Mandatory)][string]$Script,
        [int]$TimeoutSeconds = 300
    )
    $result = Invoke-SshBash -HostName $HostName -Port $Port -User $User -Script $Script -TimeoutSeconds $TimeoutSeconds
    $body = @(
        "command_started_utc=$($result.StartedAtUtc.ToString('o'))"
        "command_finished_utc=$($result.FinishedAtUtc.ToString('o'))"
        "exit_code=$($result.ExitCode)"
        '--- output ---'
        $result.Combined.TrimEnd()
    ) -join "`n"
    Write-EasyProxyEvidence -Path $Path -Text ($body + "`n")
    return $result
}

function Invoke-Cloud {
    param([string]$Script, [int]$TimeoutSeconds = 300)
    return Invoke-SshBash -HostName $CloudHost -Port $CloudPort -User $CloudUser -Script $Script -TimeoutSeconds $TimeoutSeconds
}

function Invoke-Nas {
    param([string]$Script, [int]$TimeoutSeconds = 300)
    return Invoke-SshBash -HostName $NasHost -Port $NasPort -User $NasUser -Script $Script -TimeoutSeconds $TimeoutSeconds
}

function Upload-TextViaSsh {
    param(
        [ValidateSet('Cloud', 'Nas')][string]$Target,
        [string]$Path,
        [string]$Text,
        [string]$Mode = '600'
    )
    $bytes = [Text.Encoding]::UTF8.GetBytes((ConvertTo-LfText -Text $Text))
    $encoded = [Convert]::ToBase64String($bytes)
    $script = @(
        'set -eu'
        "printf '%s' '$encoded' | base64 -d > '$Path'"
        "chmod '$Mode' '$Path'"
        "printf 'UPLOADED=%s\n' '$Path'"
    ) -join "`n"
    if ($Target -eq 'Cloud') { $result = Invoke-Cloud -Script $script -TimeoutSeconds 60 }
    else { $result = Invoke-Nas -Script $script -TimeoutSeconds 60 }
    if ($result.ExitCode -ne 0) { throw "远程写入失败：$Target $Path" }
    return $result
}

function Wait-CloudHealth {
    param([int]$TimeoutSeconds = 120)
    return Wait-EasyProxyCondition -Name '云服务器健康检查' -TimeoutSeconds $TimeoutSeconds -IntervalMilliseconds 2000 -Condition {
        $response = Invoke-EasyProxyHttp -Uri "$apiBase/api/open/health" -TimeoutSeconds 5
        $response.StatusCode -eq 200 -and $response.Content -match '"status"\s*:\s*"UP"'
    }
}

function Wait-RuleListening {
    param([int]$TimeoutSeconds = 60)
    return Wait-EasyProxyCondition -Name '规则监听端口' -TimeoutSeconds $TimeoutSeconds -IntervalMilliseconds 1000 -Condition {
        $script = "/usr/bin/ss -lnt | grep -q ':$RulePort\b'"
        (Invoke-Cloud -Script $script -TimeoutSeconds 10).ExitCode -eq 0
    }
}

function Wait-RuleReleased {
    param([int]$TimeoutSeconds = 60)
    return Wait-EasyProxyCondition -Name '规则端口释放' -TimeoutSeconds $TimeoutSeconds -IntervalMilliseconds 1000 -Condition {
        $script = "if /usr/bin/ss -lnt | grep -q ':$RulePort\b'; then exit 1; fi"
        (Invoke-Cloud -Script $script -TimeoutSeconds 10).ExitCode -eq 0
    }
}

function Wait-ClientOnline {
    param([Parameter(Mandatory)][string]$AdminToken, [int]$TimeoutSeconds = 60)
    return Wait-EasyProxyCondition -Name 'NAS 客户端在线' -TimeoutSeconds $TimeoutSeconds -IntervalMilliseconds 2000 -Condition {
        $clients = @(Get-EasyProxyClients -BaseUri "$apiBase" -Token $AdminToken)
        $item = $clients | Where-Object { $_.name -eq $ClientName } | Select-Object -First 1
        $null -ne $item -and [string]$item.status -eq 'online' -and [bool]$item.enableFlag
    }
}

function Restore-CloudRollback {
    param(
        [Parameter(Mandatory)][string]$ComposeProject,
        [Parameter(Mandatory)][string]$RollbackSuffix
    )
    $script = @(
        'set +e'
        'cd /opt/easy-proxy'
        "/usr/bin/docker compose -p '$ComposeProject' down --remove-orphans"
        'for name in easy-proxy-server easy-proxy-web; do'
        "  rollback=`"`$name$RollbackSuffix`""
        '  if /usr/bin/docker inspect "$rollback" >/dev/null 2>&1; then'
        '    /usr/bin/docker start "$rollback"'
        '    printf "RESTORED_CONTAINER=%s\n" "$rollback"'
        '  else'
        '    printf "MISSING_ROLLBACK_CONTAINER=%s\n" "$rollback"'
        '  fi'
        'done'
        'sleep 3'
        '/usr/bin/docker ps -a --format "{{.Names}}\t{{.Status}}\t{{.Image}}" | grep -E "^easy-proxy-(server|web)" || true'
    ) -join "`n"
    return Write-RemoteEvidence -Path (Join-Path $rawDocker 'deploy-cloud-rollback.txt') -HostName $CloudHost -Port $CloudPort -User $CloudUser -Script $script -TimeoutSeconds 120
}

try {
    $preflight = Write-RemoteEvidence -Path (Join-Path $rawDocker 'deploy-cloud-preflight.txt') -HostName $CloudHost -Port $CloudPort -User $CloudUser -Script (@(
        'set -eu'
        'printf ''cloud_host=%s\n'' "$(hostname)"'
        'printf ''cloud_uid_gid=%s:%s\n'' "$(id -u)" "$(id -g)"'
        '/usr/bin/docker version --format ''client={{.Client.Version}} server={{.Server.Version}}'''
        '/usr/bin/docker compose version'
        '/usr/bin/docker ps -a --format ''{{.Names}}\t{{.Status}}\t{{.Image}}'''
        '/usr/bin/ss -lnt | grep -E '':(21090|21091|21092|28080)\b'' || true'
    ) -join "`n")
    Add-DeployCheck -Name '云服务器 SSH 与预检' -Passed ($preflight.ExitCode -eq 0) -Detail "exit=$($preflight.ExitCode)"

    $cloudPull = Write-RemoteEvidence -Path (Join-Path $rawDocker 'deploy-cloud-pull.txt') -HostName $CloudHost -Port $CloudPort -User $CloudUser -Script (@(
        'set -eu'
        "/usr/bin/docker pull '$serverImage'"
        "/usr/bin/docker pull '$webImage'"
        "/usr/bin/docker image inspect '$serverImage' '$webImage' --format '{{.RepoTags}} {{.Id}} {{.Os}}/{{.Architecture}}'"
    ) -join "`n") -TimeoutSeconds 300
    Add-DeployCheck -Name '云服务器固定镜像可用' -Passed ($cloudPull.ExitCode -eq 0) -Detail "exit=$($cloudPull.ExitCode)"
    if ($cloudPull.ExitCode -ne 0) { throw '云服务器固定镜像拉取失败，未停止现有容器' }

    $prepareCloud = @(
        'set -eu'
        "root_image='$webImage'"
        'owner_id="$(id -u)"'
        'group_id="$(id -g)"'
        'if /usr/bin/docker image inspect "$root_image" >/dev/null 2>&1; then'
        '  /usr/bin/docker run --rm --user 0:0 -v /opt:/hostopt "$root_image" sh -c "install -d -m 700 -o $owner_id -g $group_id /hostopt/easy-proxy /hostopt/easy-proxy/server /hostopt/easy-proxy/server/config /hostopt/easy-proxy/server/data /hostopt/easy-proxy/server/logs"'
        'else'
        '  printf ''缺少可用于创建目录的容器镜像：%s\n'' "$root_image" >&2'
        '  exit 2'
        'fi'
        'ls -ld /opt/easy-proxy /opt/easy-proxy/server /opt/easy-proxy/server/config /opt/easy-proxy/server/data /opt/easy-proxy/server/logs'
    ) -join "`n"
    $prepared = Write-RemoteEvidence -Path (Join-Path $rawDocker 'deploy-cloud-directories.txt') -HostName $CloudHost -Port $CloudPort -User $CloudUser -Script $prepareCloud
    Add-DeployCheck -Name '云服务器部署目录' -Passed ($prepared.ExitCode -eq 0) -Detail "exit=$($prepared.ExitCode)"
    if ($prepared.ExitCode -ne 0) { throw '云服务器部署目录准备失败，未停止现有容器' }

    $serverTemplate = [IO.File]::ReadAllText((Join-Path $testsRoot 'configs\docker\server\server.yaml'))
    $serverYaml = $serverTemplate.Replace('<CHANGE_ME_STRONG_RANDOM_CERT_PASSWORD>', [string]$secret.CertPassword).
        Replace('<CHANGE_ME_STRONG_RANDOM_JWT_SECRET>', [string]$secret.JwtSecret).
        Replace('<CHANGE_ME_DB_USER>', [string]$secret.DbUsername).
        Replace('<CHANGE_ME_STRONG_RANDOM_DB_PASSWORD>', [string]$secret.DbPassword).
        Replace('jdbc:h2:./data/database', "jdbc:h2:./data/database-$($RunId.Replace('-', ''))")
    $cloudEnv = @(
        "EASY_PROXY_VERSION=$Tag"
        "SERVER_JWT_SECRET=$($secret.JwtSecret)"
        "SERVER_CERT_PASSWORD=$($secret.CertPassword)"
        'SERVER_JWT_EXPIRE_MINUTES=60'
    ) -join "`n"
    $cloudCompose = [IO.File]::ReadAllText((Join-Path $testsRoot 'configs\docker\cloud-compose.yml'))
    $clientYaml = @(
        'server:'
        "  ip: $PublicHost"
        "  port: $ProxyPort"
        "  webPort: $WebPort"
        "  token: `"$($secret.ClientToken)`""
    ) -join "`n"
    $nasEnv = @(
        "EASY_PROXY_VERSION=$Tag"
        "TARGET_IMAGE=$targetImage"
    ) -join "`n"
    $nasCompose = [IO.File]::ReadAllText((Join-Path $testsRoot 'configs\docker\nas-compose.yml'))
    $targetConfig = [IO.File]::ReadAllText((Join-Path $testsRoot 'configs\docker\target\nginx.conf'))

    Upload-TextViaSsh -Target Cloud -Path '/opt/easy-proxy/.env' -Text $cloudEnv -Mode '600' | Out-Null
    Upload-TextViaSsh -Target Cloud -Path '/opt/easy-proxy/docker-compose.yml' -Text $cloudCompose -Mode '600' | Out-Null
    Upload-TextViaSsh -Target Cloud -Path '/opt/easy-proxy/server/server.yaml' -Text $serverYaml -Mode '600' | Out-Null
    Add-DeployCheck -Name '云服务器敏感配置写入' -Passed $true -Detail '经 SSH stdin 写入，未在证据中记录内容'

    $rollback = @(
        'set -eu'
        'cd /opt/easy-proxy'
        "for name in easy-proxy-server easy-proxy-web; do"
        '  if /usr/bin/docker inspect "$name" >/dev/null 2>&1; then'
        '    /usr/bin/docker stop "$name" >/dev/null || true'
        "    /usr/bin/docker rename `"`$name`" `"`$name$cloudRollbackSuffix`""
        "    printf 'ROLLBACK_CONTAINER=%s\n' `"`$name$cloudRollbackSuffix`""
        '  fi'
        'done'
        'for leftover in $(/usr/bin/docker ps --format "{{.Names}}" | grep -E "^easy-proxy-(server|web)-rollback-[0-9]{8}-[0-9]{6}$" || true); do'
        '  /usr/bin/docker stop "$leftover" >/dev/null || true'
        '  printf "STOPPED_OLD_ROLLBACK=%s\n" "$leftover"'
        'done'
        "/usr/bin/docker compose -p '$cloudComposeProject' up -d --pull never"
        "/usr/bin/docker compose -p '$cloudComposeProject' ps"
    ) -join "`n"
    $cloudStarted = Write-RemoteEvidence -Path (Join-Path $rawDocker 'deploy-cloud-start.txt') -HostName $CloudHost -Port $CloudPort -User $CloudUser -Script $rollback -TimeoutSeconds 180
    Add-DeployCheck -Name '云服务器容器启动' -Passed ($cloudStarted.ExitCode -eq 0) -Detail "exit=$($cloudStarted.ExitCode)"

    $health = Wait-CloudHealth -TimeoutSeconds 120
    Add-DeployCheck -Name '服务端/API 健康' -Passed ([bool]$health.Satisfied) -Detail "等待 $([math]::Round($health.ElapsedMs / 1000, 1)) 秒"
    if (-not $health.Satisfied) {
        Write-RemoteEvidence -Path (Join-Path $rawDocker 'deploy-cloud-health-failed.txt') -HostName $CloudHost -Port $CloudPort -User $CloudUser -Script (@(
            "cd /opt/easy-proxy && /usr/bin/docker compose -p '$cloudComposeProject' ps"
            "server_id=`$(/usr/bin/docker compose -p '$cloudComposeProject' ps -q server)"
            "web_id=`$(/usr/bin/docker compose -p '$cloudComposeProject' ps -q web)"
            'test -n "$server_id" && /usr/bin/docker logs --tail=300 "$server_id" 2>&1 || true'
            'test -n "$web_id" && /usr/bin/docker logs --tail=200 "$web_id" 2>&1 || true'
        ) -join "`n") | Out-Null
        $rollbackResult = Restore-CloudRollback -ComposeProject $cloudComposeProject -RollbackSuffix $cloudRollbackSuffix
        Add-DeployCheck -Name '云服务器失败自动回滚' -Passed ($rollbackResult.ExitCode -eq 0) -Detail "exit=$($rollbackResult.ExitCode)，详见 deploy-cloud-rollback.txt"
        throw '云服务器健康检查未通过'
    }

    $admin = Initialize-EasyProxyAdmin -BaseUri "$apiBase" -Username ([string]$secret.AdminUsername) -Password ([string]$secret.AdminPassword)
    Add-DeployCheck -Name '管理员初始化与登录' -Passed (-not [string]::IsNullOrWhiteSpace($admin.Token)) -Detail 'JWT 仅保存在进程内存'

    $clients = @(Get-EasyProxyClients -BaseUri "$apiBase" -Token $admin.Token)
    $client = $clients | Where-Object { $_.name -eq $ClientName } | Select-Object -First 1
    if ($null -eq $client) {
        $client = New-EasyProxyClient -BaseUri "$apiBase" -Token $admin.Token -ClientName $ClientName -ClientToken ([string]$secret.ClientToken)
    }
    Add-DeployCheck -Name 'NAS 客户端记录' -Passed ($null -ne $client -and -not [string]::IsNullOrWhiteSpace([string]$client.id)) -Detail "id=$([string]$client.id)"

    $prepareNas = @(
        'set -eu'
        'DOCKER=/usr/local/bin/docker'
        'install -d -m 700 /volume1/docker/easy-proxy-client /volume1/docker/easy-proxy-client/config /volume1/docker/easy-proxy-client/logs /volume1/docker/easy-proxy-client/target /volume1/docker/easy-proxy-client/fixtures'
        'ls -ld /volume1/docker/easy-proxy-client /volume1/docker/easy-proxy-client/config /volume1/docker/easy-proxy-client/logs /volume1/docker/easy-proxy-client/target /volume1/docker/easy-proxy-client/fixtures'
    ) -join "`n"
    $nasPrepared = Write-RemoteEvidence -Path (Join-Path $rawDocker 'deploy-nas-directories.txt') -HostName $NasHost -Port $NasPort -User $NasUser -Script $prepareNas
    Add-DeployCheck -Name 'NAS 部署目录' -Passed ($nasPrepared.ExitCode -eq 0) -Detail "exit=$($nasPrepared.ExitCode)"

    Upload-TextViaSsh -Target Nas -Path '/volume1/docker/easy-proxy-client/.env' -Text $nasEnv -Mode '600' | Out-Null
    Upload-TextViaSsh -Target Nas -Path '/volume1/docker/easy-proxy-client/docker-compose.yml' -Text $nasCompose -Mode '600' | Out-Null
    Upload-TextViaSsh -Target Nas -Path '/volume1/docker/easy-proxy-client/config/client.yaml' -Text $clientYaml -Mode '600' | Out-Null
    Upload-TextViaSsh -Target Nas -Path '/volume1/docker/easy-proxy-client/target/nginx.conf' -Text $targetConfig -Mode '600' | Out-Null
    Add-DeployCheck -Name 'NAS 敏感配置写入' -Passed $true -Detail '经 SSH stdin 写入，未在证据中记录内容'

    $fixturesRoot = Join-Path $testsRoot 'data\fixtures'
    $fixtureUpload = Send-EasyProxyDirectory `
        -HostName $NasHost -Port $NasPort -User $NasUser `
        -LocalDirectory $fixturesRoot `
        -RemoteDirectory '/volume1/docker/easy-proxy-client/fixtures' `
        -TimeoutSeconds 300
    Add-DeployCheck -Name '测试固定数据上传' -Passed ($fixtureUpload.ExitCode -eq 0) -Detail "exit=$($fixtureUpload.ExitCode)，经 SSH 归档上传"

    $expectedFixtureCount = @(Get-ChildItem -LiteralPath $fixturesRoot -Recurse -File -Force).Count
    $fixtureVerify = Write-RemoteEvidence -Path (Join-Path $rawDocker 'deploy-nas-fixtures.txt') -HostName $NasHost -Port $NasPort -User $NasUser -Script (@(
        'set -eu'
        "expected='$expectedFixtureCount'"
        "actual=`$(find /volume1/docker/easy-proxy-client/fixtures -type f | wc -l)"
        'test "$actual" -eq "$expected"'
        'printf "fixture_count=%s expected=%s\n" "$actual" "$expected"'
        'sha256sum /volume1/docker/easy-proxy-client/fixtures/small-1k.bin /volume1/docker/easy-proxy-client/fixtures/large-10m.bin'
    ) -join "`n")
    Add-DeployCheck -Name '测试固定数据校验' -Passed ($fixtureVerify.ExitCode -eq 0) -Detail "文件数=$expectedFixtureCount，关键样本 SHA256 已留证"

    $nasPull = Write-RemoteEvidence -Path (Join-Path $rawDocker 'deploy-nas-pull.txt') -HostName $NasHost -Port $NasPort -User $NasUser -Script (@(
        'set -eu'
        'DOCKER=/usr/local/bin/docker'
        "`$DOCKER pull registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:$Tag"
        '$DOCKER pull nginx:1.27.4-alpine'
        "`$DOCKER image inspect registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:$Tag nginx:1.27.4-alpine --format '{{.RepoTags}} {{.Id}} {{.Os}}/{{.Architecture}}'"
    ) -join "`n") -TimeoutSeconds 300
    Add-DeployCheck -Name 'NAS 固定镜像可用' -Passed ($nasPull.ExitCode -eq 0) -Detail "exit=$($nasPull.ExitCode)"

    $nasStart = Write-RemoteEvidence -Path (Join-Path $rawDocker 'deploy-nas-start.txt') -HostName $NasHost -Port $NasPort -User $NasUser -Script (@(
        'set -eu'
        'cd /volume1/docker/easy-proxy-client'
        "project='$nasComposeProject'"
        'for name in easy-proxy-client easy-proxy-target; do'
        '  if /usr/local/bin/docker inspect "$name" >/dev/null 2>&1; then'
        '    /usr/local/bin/docker stop "$name" >/dev/null || true'
        "    /usr/local/bin/docker rename `"`$name`" `"`$name$nasRollbackSuffix`""
        '  fi'
        'done'
        '/usr/local/bin/docker compose -p "$project" up -d --pull never'
        '/usr/local/bin/docker compose -p "$project" ps'
        "netstat -ltn 2>/dev/null | grep -E ':18080\b' || true"
    ) -join "`n") -TimeoutSeconds 180
    Add-DeployCheck -Name 'NAS 目标与客户端启动' -Passed ($nasStart.ExitCode -eq 0) -Detail "exit=$($nasStart.ExitCode)"

    $online = Wait-ClientOnline -AdminToken $admin.Token -TimeoutSeconds 90
    Add-DeployCheck -Name 'NAS 客户端上线' -Passed ([bool]$online.Satisfied) -Detail "等待 $([math]::Round($online.ElapsedMs / 1000, 1)) 秒"

    $targetLocal = Wait-EasyProxyCondition -Name 'NAS 目标端口' -TimeoutSeconds 60 -IntervalMilliseconds 1000 -Condition {
        $response = Invoke-EasyProxyHttp -Uri "http://$NasHost`:$NasTargetPort/" -TimeoutSeconds 5
        $response.StatusCode -eq 200 -and $response.Content -match 'Easy Proxy NAS target OK'
    }
    Add-DeployCheck -Name 'NAS 目标服务可用' -Passed ([bool]$targetLocal.Satisfied) -Detail "等待 $([math]::Round($targetLocal.ElapsedMs / 1000, 1)) 秒"

    $rules = @(Get-EasyProxyRules -BaseUri "$apiBase" -Token $admin.Token)
    $rule = $rules | Where-Object { $_.name -eq $RuleName } | Select-Object -First 1
    if ($null -eq $rule) {
        $rule = New-EasyProxyRule -BaseUri "$apiBase" -Token $admin.Token -ProxyClientId ([int]$client.id) -RuleName $RuleName -ServerPort $RulePort -ClientAddress "${NasHost}:$NasTargetPort" -EnableFlag $true
    }
    else {
        $rule = Set-EasyProxyRuleEnabled -BaseUri "$apiBase" -Token $admin.Token -Rule $rule -Enabled $true
    }
    Add-DeployCheck -Name '转发规则生效' -Passed ($null -ne $rule -and [bool]$rule.enableFlag) -Detail "id=$([string]$rule.id)，$RulePort -> ${NasHost}:$NasTargetPort"

    $listening = Wait-RuleListening -TimeoutSeconds 60
    Add-DeployCheck -Name '公网规则监听' -Passed ([bool]$listening.Satisfied) -Detail "等待 $([math]::Round($listening.ElapsedMs / 1000, 1)) 秒"

    $route = Wait-EasyProxyCondition -Name '公网端到端链路' -TimeoutSeconds 60 -IntervalMilliseconds 1000 -Condition {
        $response = Invoke-EasyProxyHttp -Uri "http://$PublicHost`:$RulePort/" -TimeoutSeconds 10
        $response.StatusCode -eq 200 -and $response.Content -match 'Easy Proxy NAS target OK'
    }
    Add-DeployCheck -Name '公网端到端链路' -Passed ([bool]$route.Satisfied) -Detail "等待 $([math]::Round($route.ElapsedMs / 1000, 1)) 秒"

    $certScript = @(
        'set -eu'
        'printf ''SERVER_CERT_SHA256=%s\n'' "$(openssl x509 -in /opt/easy-proxy/server/config/cert.pem -noout -fingerprint -sha256 | sed ''s/^.*=//'')"'
    ) -join "`n"
    $serverCert = Write-RemoteEvidence -Path (Join-Path $rawDocker 'deploy-server-cert.txt') -HostName $CloudHost -Port $CloudPort -User $CloudUser -Script $certScript
    $nasCert = Write-RemoteEvidence -Path (Join-Path $rawDocker 'deploy-client-cert.txt') -HostName $NasHost -Port $NasPort -User $NasUser -Script (@(
        'set -eu'
        'printf ''CLIENT_CERT_SHA256=%s\n'' "$(openssl x509 -in /volume1/docker/easy-proxy-client/config/cert.pem -noout -fingerprint -sha256 | sed ''s/^.*=//'')"'
    ) -join "`n")
    $serverFp = if ($serverCert.StdOut -match 'SERVER_CERT_SHA256=([0-9A-Fa-f:]+)') { $Matches[1] } else { '' }
    $clientFp = if ($nasCert.StdOut -match 'CLIENT_CERT_SHA256=([0-9A-Fa-f:]+)') { $Matches[1] } else { '' }
    Add-DeployCheck -Name '客户端/服务端证书指纹一致' -Passed (-not [string]::IsNullOrWhiteSpace($serverFp) -and $serverFp -eq $clientFp) -Detail $(if ($serverFp -eq $clientFp) { "SHA256=$serverFp" } else { '指纹不一致或无法读取' })

    $runtime = Write-RemoteEvidence -Path (Join-Path $rawDocker 'deploy-runtime-state.txt') -HostName $CloudHost -Port $CloudPort -User $CloudUser -Script (@(
        'set -eu'
        'cd /opt/easy-proxy'
        "project='$cloudComposeProject'"
        '/usr/bin/docker compose -p "$project" ps'
        'server_id="$(/usr/bin/docker compose -p "$project" ps -q server)"'
        'web_id="$(/usr/bin/docker compose -p "$project" ps -q web)"'
        'test -n "$server_id" -a -n "$web_id"'
        '/usr/bin/docker inspect "$server_id" "$web_id" --format ''{{.Name}} image={{.Config.Image}} image_id={{.Image}} restart={{.RestartCount}} status={{.State.Status}}'''
        '/usr/bin/ss -lnt | grep -E '':(21090|21091|21092|28080)\b'''
        '/usr/bin/docker logs --tail=120 "$server_id" 2>&1'
        '/usr/bin/docker logs --tail=80 "$web_id" 2>&1'
    ) -join "`n") -TimeoutSeconds 120
    Add-DeployCheck -Name '运行状态归档' -Passed ($runtime.ExitCode -eq 0) -Detail "exit=$($runtime.ExitCode)"

    $nasRuntime = Write-RemoteEvidence -Path (Join-Path $rawDocker 'deploy-nas-runtime-state.txt') -HostName $NasHost -Port $NasPort -User $NasUser -Script (@(
        'set -eu'
        'cd /volume1/docker/easy-proxy-client'
        "project='$nasComposeProject'"
        '/usr/local/bin/docker compose -p "$project" ps'
        'client_id="$(/usr/local/bin/docker compose -p "$project" ps -q client)"'
        'target_id="$(/usr/local/bin/docker compose -p "$project" ps -q target)"'
        'test -n "$client_id" -a -n "$target_id"'
        '/usr/local/bin/docker inspect "$client_id" "$target_id" --format ''{{.Name}} image={{.Config.Image}} image_id={{.Image}} restart={{.RestartCount}} status={{.State.Status}}'''
        'netstat -ltn 2>/dev/null | grep -E '':18080\b'' || true'
        '/usr/local/bin/docker logs --tail=160 "$client_id" 2>&1'
        '/usr/local/bin/docker logs --tail=80 "$target_id" 2>&1'
    ) -join "`n") -TimeoutSeconds 120
    Add-DeployCheck -Name 'NAS 运行状态归档' -Passed ($nasRuntime.ExitCode -eq 0) -Detail "exit=$($nasRuntime.ExitCode)"

    $summary = @(
        '# 正式候选部署摘要'
        ''
        "运行编号：$RunId"
        "固定标签：$Tag"
        "云服务器回滚保留后缀：$cloudRollbackSuffix"
        "NAS 回滚保留后缀：$nasRollbackSuffix"
        "云服务器 Compose 项目：$cloudComposeProject"
        "NAS Compose 项目：$nasComposeProject"
        ''
        '| 检查项 | 结果 | 说明 |'
        '| --- | --- | --- |'
        $checks
        ''
        '敏感信息仅从 DPAPI 存储读取并经 SSH 标准输入写入；本摘要不记录密码、Token、JWT 或证书密码。'
    ) -join "`n"
    Write-LfText -Path (Join-Path $processed 'deploy-summary.md') -Text ($summary + "`n")

    if ($failures.Count -gt 0) { throw "部署检查未通过：$($failures -join '；')" }
    Write-Output "DEPLOY_STATUS=PASS"
    Write-Output "SUMMARY=$((Join-Path $processed 'deploy-summary.md').Substring($repoRoot.Length + 1).Replace('\', '/'))"
}
catch {
    $message = ConvertTo-SafeText -Text $_.Exception.Message
    Write-EasyProxyEvidence -Path (Join-Path $rawDocker 'deploy-error.txt') -Text "$message`n"
    $summary = @(
        '# 正式候选部署摘要'
        ''
        '- 总体判定：**FAIL**'
        "- 运行编号：$RunId"
        "- 固定标签：$Tag"
        ''
        '| 检查项 | 结果 | 说明 |'
        '| --- | --- | --- |'
        $checks
        ''
        "失败：$message"
    ) -join "`n"
    Write-LfText -Path (Join-Path $processed 'deploy-summary.md') -Text ($summary + "`n")
    throw
}
