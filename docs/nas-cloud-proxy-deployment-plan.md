# Easy Proxy 群晖 NAS ↔ 云服务器 Docker 测试方案

> 状态：**SSH 公钥已验证；WSL 最新代码构建、阿里云 ACR 推送和云服务器拉取已验证；NAS Docker socket 权限已修复并由普通用户会话复验；单元测试失败，当前镜像仅为探索性构建；Docker 实际部署、端到端和高并发测试待执行。**  
> 编制日期：2026-11-21  
> 代码基线：`develop` 分支，提交 `4e68667`  
> 部署角色：云服务器 `proxy-server`，群晖 NAS `proxy-client`

## 1. 目标与范围

本方案用于在真实网络中验证 Easy Proxy 的内网穿透能力，使用 Docker 分别部署服务端和客户端。

### 1.1 验证目标

1. 验证群晖 NAS 与云服务器的 SSH 公钥登录。
2. 在云服务器上以 Docker 部署 Easy Proxy 服务端和 Web 管理界面。
3. 在群晖 NAS 上以 Docker 部署 Easy Proxy 客户端。
4. 验证客户端 Token 鉴权、TLS 通信、在线状态与证书下载。
5. 创建 TCP 转发规则，验证公网端口 → 云服务器 → NAS 本地服务的完整链路。
6. 验证断线重连、服务重启、规则持久化、端口释放和基础流量统计。
7. 验证高并发、高负载和长时间运行时的数据正确性、服务可用性、资源稳定性与故障恢复能力。
8. 形成后续正式部署所需的端口、配置、安全、容量与运维基线。

### 1.2 本阶段不做

- 不配置正式域名、公网 HTTPS 证书和反向代理。
- 不做多节点、高可用、自动扩缩容。
- 不做 UDP 转发、HTTP 七层路由或复杂鉴权。
- 本阶段高并发与高负载测试用于建立单节点容量和稳定性基线，不等同于生产容量认证、压测报告认证或多节点高可用验证。
- 不把当前测试部署直接视为生产环境。

## 2. 已知环境与端口规划

### 2.1 主机信息

| 主机 | 角色 | 地址 | SSH 端口 | SSH 用户 | Docker 部署内容 |
| --- | --- | --- | ---: | --- | --- |
| 本地管理机 | 运维入口 | 当前 Windows 主机 | - | - | SSH 客户端 |
| 云服务器 | `proxy-server` | `68.64.181.242` | `1514` | `yudejijie` | server + web |
| 群晖 NAS | `proxy-client` | `192.168.1.9` | `10080` | `chenkeyudev` | client + 测试目标服务 |

> 云服务器 SSH 用户名已确认为 `yudejijie`；本地公钥已配置到云服务器和群晖 NAS。
>
> 后续直接使用公钥登录，公钥登录已验证通过。云服务器密码曾在聊天中出现，不得写入本仓库、Docker Compose 或项目配置；建议尽快更换。

### 2.2 端口规划

| 端口 | 所在主机 | 用途 | 暴露范围 |
| ---: | --- | --- | --- |
| `1514/tcp` | 云服务器 | SSH | 仅本地管理机公网 IP |
| `21090/tcp` | 云服务器 | 客户端与服务器之间的 TLS 控制/数据通道 | 仅 NAS 出口 IP；无法固定时临时对所有 IPv4 开放 |
| `21091/tcp` | 云服务器 | 服务端 HTTP API | 仅云服务器本机，不对公网开放 |
| `21092/tcp` | 云服务器 | Web 管理界面和证书公开下载入口 | 管理机公网 IP；测试期可按需放大 |
| `28080/tcp` | 云服务器 | 本次测试的代理规则监听端口 | 测试来源 IP |
| `18080/tcp` | 群晖 NAS | NAS 内测试 HTTP 服务 | 仅局域网/NAS 本地 |
| `10080/tcp` | 群晖 NAS | SSH | 仅局域网管理机 |

### 2.3 流量路径

```text
当前 Windows 管理机（公网访问与压测源）
  └─ HTTP :28080
       └─ 云服务器 Easy Proxy Server
            └─ TLS :21090
                 └─ 群晖 NAS Easy Proxy Client
                      └─ TCP 192.168.1.9:18080
                           └─ NAS 测试 HTTP 容器
```

Web 管理路径：

```text
浏览器
  └─ HTTP :21092
       └─ easy-proxy-web (Nginx)
            └─ http://127.0.0.1:21091
                 └─ Easy Proxy Server API
```

### 2.4 当前代码端口核对结论

当前代码中的实际端口关系为：

- 服务端客户端通道：`server.proxyPort = 21090`
- 服务端 API：`server.webPort = 21091`
- Web 容器 Nginx：监听 `21092`
- 客户端连接配置：`server.port = 21090`
- 客户端证书下载端口：`server.webPort`，建议配置为 `21092`
- 根目录 `docker-compose.yml` 当前把 Web 映射为 `21093:21093`，与 `nginx.conf` 的 `listen 21092` 不一致。本方案不使用该根目录 Compose 文件，而是在云服务器使用单独的生产测试 Compose。

## 3. 总体实施顺序

1. 验证本地 SSH 配置及 NAS、云服务器公钥登录。
2. 在 WSL 环境从当前最新源码运行单元测试、打包三套镜像并推送固定标签到阿里云 ACR。
3. 核验 ACR 远端清单、RepoDigest、`linux/amd64` 架构、容器内 JAR 哈希和云服务器拉取结果；任一质量门槛失败时停止正式部署。
4. 检查两台主机的 Docker、磁盘、端口和网络；NAS Docker socket 权限已修复，并已由新的普通 SSH 会话复验。
5. 云服务器从 ACR 拉取固定镜像并部署 `server` 和 `web`。
6. 初始化 Web 管理员，创建 NAS 客户端并获取 Token。
7. NAS 从 ACR 拉取同一固定标签并部署测试 HTTP 服务及 `client`。
8. 在 Web 管理界面创建转发规则。
9. 按 9.5 节创建本轮 `docs/tests/runs/YYYYMMDD-HHmmss/` 归档目录，准备运行清单、脚本和测试数据。
10. 从公网执行端到端访问、异常恢复和长时间稳定性测试。
11. 从当前 Windows 管理机执行高并发、高负载与容量边界测试。
12. 归档脚本、数据、日志、证据和报告，核对产物完整性并完成敏感信息脱敏。
13. 根据功能、稳定性、容量和恢复结果决定是否进入正式部署。
14. 按 9.7 节执行强制闭环：任何失败均进入缺陷修复与重新验证；发现 P0/P1 严重缺陷时，立即终止当前测试组并跳转到缺陷修复环节，不得继续剩余测试或直接进入验收。
## 4. SSH 公钥登录

> 当前状态：本地 SSH 公钥已配置到群晖 NAS 和云服务器，并已通过仅公钥登录验证。本节用于复核本地 SSH 配置；如需重新验证，可直接执行下文命令。

### 4.1 确认本地密钥

方案使用专用 Ed25519 密钥。先确认私钥和公钥文件存在，并记录公钥指纹：

```powershell
Get-ChildItem "$env:USERPROFILE\.ssh\id_ed25519*"
ssh-keygen -lf "$env:USERPROFILE\.ssh\id_ed25519.pub"
```

如果实际配置到两台主机的是其他密钥，后续 `IdentityFile` 必须改为对应私钥路径；不要重新生成并覆盖当前可用密钥。

### 4.2 本地 SSH 配置

在 `C:\Users\Administrator\.ssh\config` 中增加：

```sshconfig
Host easy-proxy-cloud
    HostName 68.64.181.242
    User yudejijie
    Port 1514
    IdentityFile C:/Users/Administrator/.ssh/id_ed25519
    IdentitiesOnly yes
    ServerAliveInterval 30
    ServerAliveCountMax 3

Host easy-proxy-nas
    HostName 192.168.1.9
    User chenkeyudev
    Port 10080
    IdentityFile C:/Users/Administrator/.ssh/id_ed25519
    IdentitiesOnly yes
    ServerAliveInterval 30
    ServerAliveCountMax 3
```

如果实际使用的私钥文件名不同，只调整两个 `IdentityFile` 路径，不要改动主机地址、端口和用户名。

### 4.3 验证公钥登录

使用主机别名分别登录：

```powershell
ssh easy-proxy-cloud "hostname; id; uname -a"
ssh easy-proxy-nas "hostname; id; uname -a"
```

预期结果：不再要求输入账号密码。若私钥设置了口令，只需要输入私钥口令。也可以强制仅使用公钥认证进行验证：

```powershell
ssh -o PreferredAuthentications=publickey -o PasswordAuthentication=no easy-proxy-cloud "true"
ssh -o PreferredAuthentications=publickey -o PasswordAuthentication=no easy-proxy-nas "true"
```

> 验证结果：2026-11-21 使用本地 `id_ed25519` 私钥完成仅公钥登录，NAS 与云服务器均登录成功。

若登录失败，依次检查：

- `IdentityFile` 指向的私钥是否存在，且与已配置到两台服务器的公钥配对
- 两端用户的 `~/.ssh/authorized_keys` 是否包含当前公钥
- `~/.ssh` 权限是否为 `700`
- `~/.ssh/authorized_keys` 权限是否为 `600`
- 云服务器是否允许 `yudejijie` 登录，NAS 是否允许 `chenkeyudev` 远程登录
- NAS 的 SSH 服务是否启用，端口是否为 `10080`
- 私钥带口令时，是否已通过 `ssh-agent` 加载或输入了正确的私钥口令

群晖主目录权限异常时，通常需要管理员在 NAS 本机或 SSH 会话中修正主目录权限。不要在未确认影响前递归修改整个 `/var/services/homes`。

### 4.4 关闭密码登录

公钥登录验证通过后，可进一步关闭 SSH 账号密码登录。操作前必须满足：

1. 已使用当前本地公钥成功登录云服务器和 NAS。
2. 保留另一个已登录的 SSH 会话。
3. 已确认当前用户可以执行后续 Docker 管理命令。
4. 已确认云服务商控制台或 NAS 本地终端可作为应急入口。

云服务器建议最终设置：

```text
PasswordAuthentication no
PermitRootLogin prohibit-password
PubkeyAuthentication yes
```

NAS 优先在 DSM 的安全设置中关闭密码登录；如直接修改 `sshd_config`，必须先备份并在保留会话的情况下验证配置。

## 5. 镜像构建、测试校验与阿里云发布

> 本节把“从项目最新代码构建测试镜像”固化为可复现、可追溯、可审计的发布流程。构建、推送和拉取验证均已完成一次探索性执行，但由于单元测试失败，当前产物只允许用于问题定位，不得作为最终验收镜像。

### 5.1 构建环境与代码基线

本轮构建基线：

| 项目 | 实测值 |
| --- | --- |
| WSL 发行版 | `Ubuntu-22.04`（Ubuntu 22.04.5 LTS） |
| WSL 内核 | `Linux 6.18.33.2-microsoft-standard-WSL2 x86_64` |
| Docker Client/Server | `29.10.0` |
| Buildx | `v0.39.1` |
| Docker Compose | `v5.8.0` |
| 项目路径 | `/mnt/f/workspace/my/easy-proxy` |
| Git 分支与提交 | `develop` / `4e68667e40e1adb1eba96c48f9d567705525d3c2` |
| 镜像平台 | `linux/amd64` |
| Windows 构建辅助环境 | Maven `3.6.3`、Java `25.0.3` |

WSL 内没有可用的 Java/Maven，因此当前流程在 Windows 管理机完成 Maven 测试与 fat JAR 打包，再由 WSL 内的 Docker/Buildx 构建并推送镜像。该事实必须记录在本轮 `manifest.md` 中，不能把构建环境描述为完全位于 WSL。

正式构建前执行：

```powershell
git status --short
git rev-parse --show-toplevel
git branch --show-current
git rev-parse HEAD
wsl.exe -d Ubuntu-22.04 -- bash -lc "docker version && docker buildx version && docker compose version"
mvn -version
```

要求：

- 构建必须来自当前检出的项目最新源码，并记录完整 Git 提交。
- 已跟踪源码存在未提交修改时，构建脚本必须拒绝继续，避免产生无法追溯的镜像。
- `docs/` 测试资产可以保持未跟踪，但不能混入业务镜像构建上下文。
- 最终验收构建必须先通过单元测试；仅有 `-AllowFailedTests` 的探索性构建不得用于部署验收。

### 5.2 镜像命名与固定标签

阿里云 ACR 地址和命名空间固定为：

```text
registry.cn-hangzhou.aliyuncs.com/ckyspace
```

三套镜像名称：

```text
registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:<TAG>
registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:<TAG>
registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:<TAG>
```

标签规则：

- 探索性构建：`exploratory-<yyyyMMdd-HHmmss>-<commit7>`
- 测试通过后的验收构建：`test-<yyyyMMdd-HHmmss>-<commit7>`
- 禁止使用 `latest`，也不得在 Compose、运行清单或报告中把浮动标签作为验收依据。
- 标签、Git 提交、RepoDigest、平台、构造时间和 GitHub 工作区状态必须同时记录。
- 部署时优先使用固定标签；需要防止标签被覆盖时可改用 `@sha256:<digest>`。

本轮探索性标签为：

```text
exploratory-20261121-233608-4e68667-clean
```

### 5.3 WSL 构建和 ACR 交互式发布

脚本入口：

```text
docs/tests/scripts/build/build-and-push-images.ps1
docs/tests/scripts/build/build-push-image.sh
docs/tests/scripts/build/verify-acr-images.ps1
docs/tests/scripts/common/verify-acr-logged-out.sh
```

探索性构建命令：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File `
  docs\tests\scripts\build\build-and-push-images.ps1 `
  -Tag exploratory-20261121-233608-4e68667-clean `
  -RunId 20261121-235500 `
  -AllowFailedTests
```

最终验收构建必须移除 `-AllowFailedTests`。脚本默认生成以 `test-` 开头的固定标签，并在单元测试失败时立即终止。

安全与构建要求：

- ACR 用户名和密码在运行时交互输入，密码使用 `Read-Host -AsSecureString` 读取，不写入脚本、命令历史、日志或报告。
- 密码只通过 `docker login --password-stdin` 的标准输入传递。
- 无论推送成功或失败，`finally` 块都必须执行 `docker logout`。
- 注销后运行 `verify-acr-logged-out.sh`，证据必须包含 `ACR_AUTH_ENTRY=no`。
- 构建使用 `BUILDX_NO_DEFAULT_ATTESTATIONS=1`、`--platform linux/amd64`、`--provenance=false` 和 `--sbom=false`，确保远端 manifest 结构可重复核验。
- 三套镜像均使用独立 build context：`easy-proxy-server/`、`easy-proxy-client/`、`easy-proxy-web/`。
- 每个服务的构建日志、推送日志、远端 manifest 和验证摘要均写入本轮运行目录。

### 5.4 已发布镜像与云端拉取验证

本轮三套镜像均为 `linux/amd64`，已推送至 ACR，并在云服务器成功拉取：

| 服务 | 固定标签 | RepoDigest | 云端拉取压缩大小 |
| --- | --- | --- | ---: |
| server | `exploratory-20261121-233608-4e68667-clean` | `sha256:9c18e11d395f3c757e1622320515f3105bf206777612d3946b73410adc492302` | `177363105` bytes |
| client | `exploratory-20261121-233608-4e68667-clean` | `sha256:d36112d730c53d708a61d83c9a3b3dd4af16eb7d81193177468c631335bc8cd3` | `162388011` bytes |
| web | `exploratory-20261121-233608-4e68667-clean` | `sha256:16e7b4502d69f4d38f731ca8f698720370b3c97744aa99b0b2ca5bca83bf7c81` | `27009800` bytes |

容器内 `/app/app.jar` 与 Windows 本地 fat JAR 的 SHA-256 对照：

| 服务 | 本地 fat JAR | 容器内 `/app/app.jar` | 结果 |
| --- | --- | --- | --- |
| server | `81e96ca3fad3178c321b265a1c7d7111fa19fb27856e0e970a1d14ad6c0f0abc` | `81e96ca3fad3178c321b265a1c7d7111fa19fb27856e0e970a1d14ad6c0f0abc` | 一致 |
| client | `702b6fd00ee5f1187127ca88dac7a21363b5fbb68ac62d64897e7d1e6d203768` | `702b6fd00ee5f1187127ca88dac7a21363b5fbb68ac62d64897e7d1e6d203768` | 一致 |

比较基准必须是：

```text
easy-proxy-server\target\easy-proxy-server-0.0.1-SNAPSHOT-fat.jar
easy-proxy-client\target\easy-proxy-client-0.0.1-SNAPSHOT-fat.jar
```

证据入口：

- 构建与发布报告：[20261121-235500-acr-build-validation.md](tests/reports/20261121-235500-acr-build-validation.md)
- 运行清单：`docs/tests/runs/20261121-235500/manifest.md`
- ACR manifest：`docs/tests/runs/20261121-235500/raw/registry/`
- 云端拉取记录：`docs/tests/runs/20261121-235500/raw/validation/cloud-pull-verify.txt`
- JAR 哈希记录：`docs/tests/runs/20261121-235500/raw/validation/jar-sha256-server.txt`、`jar-sha256-client.txt`
- 凭据清理：`docs/tests/runs/20261121-235500/raw/validation/acr-logout.txt`

### 5.5 质量门槛、阻断项与准入判定

当前验证结论为“构建与发布链路有条件通过”，不能判定最终验收通过：

1. 单元测试执行 `mvn -B -ntp clean test`，共 4 个测试，其中 3 个错误，根因日志为 `javax.crypto.BadPaddingException: Padding error in decryption`。在该问题修复且全量测试通过前，不得生成或部署正式验收镜像。
2. NAS Docker socket 权限已修复：创建专用 `docker` 组、加入 `chenkeyudev`、配置 Docker 守护进程使用该组并重启。Socket 现为 `root:docker 660`；新的普通 SSH 会话可通过 `/usr/local/bin/docker info` 正常访问 Docker，Server/Client 均为 `24.0.2`。当前非交互 SSH 的 `PATH` 不含 `/usr/local/bin`，直接输入 `docker` 会提示找不到命令，部署脚本必须使用绝对路径或显式补充 PATH。证据见 `docs/tests/runs/20261121-235500/raw/validation/nas-docker-access-fixed.txt`、`nas-docker-permission-live-recheck.txt`。`docker` 组具有主机高权限，必须仅授予受控运维账号，严禁将 socket 改为 `666`。
3. Web Nginx 实际监听 `21092`，但镜像 Dockerfile 与根目录 Compose 仍出现 `21093`。本方案以 `21092` 为运行端口；最终构建前应修复源码中的端口不一致，并加入配置校验。
4. Docker 实际部署、端到端转发、异常恢复和高并发测试尚未执行，不能在当前阶段给出可用性、可靠性或容量结论。

正式部署准入必须同时满足：

- 单元测试全部通过。
- 三套镜像由通过测试的同一 Git 提交构建，使用固定 `test-` 标签。
- 三套镜像均已推送到 ACR，并完成远端 RepoDigest、平台、容器内 JAR 和云服务器拉取核验。
- ACR 登录凭据已注销并验证为 `ACR_AUTH_ENTRY=no`。
- NAS Docker socket 权限已解决并复验：普通 SSH 会话可通过 `/usr/local/bin/docker` 执行受控 Docker 管理命令。
- 本报告和运行清单已归档，关键产物已生成 SHA-256。

## 6. 云服务器部署方案

### 6.1 目录规划

建议使用：

```text
/opt/easy-proxy/
├── docker-compose.yml
├── .env
├── server/
│   ├── server.yaml
│   ├── config/          # 证书等运行时文件
│   ├── data/
│   └── logs/
└── web/
```

目录和配置权限：

```bash
sudo install -d -m 700 /opt/easy-proxy
sudo install -d -m 700 /opt/easy-proxy/server/config
sudo install -d -m 700 /opt/easy-proxy/server/data
sudo install -d -m 700 /opt/easy-proxy/server/logs
```

### 6.2 Docker Compose

云服务器使用 host 网络模式。原因是：

- 服务端需要动态监听用户创建的代理端口。
- Web Nginx 通过 `127.0.0.1:21091` 访问服务端 API。
- 如果使用 bridge 网络，每新增一个代理端口都要修改 Compose 或手工发布端口，不适合内网穿透场景。

建议的 `/opt/easy-proxy/docker-compose.yml`：

```yaml
services:
  server:
    image: "registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:${EASY_PROXY_VERSION:?EASY_PROXY_VERSION must be set}"
    container_name: easy-proxy-server
    network_mode: host
    restart: unless-stopped
    environment:
      TZ: Asia/Shanghai
      SERVER_PUBLIC_HOST: "68.64.181.242"
      SERVER_PROXY_PORT: "21090"
      SERVER_WEB_PORT: "21091"
      SERVER_CAPTCHA_IMAGE_ENABLE: "true"
    volumes:
      - ./server/server.yaml:/app/server.yaml:ro
      - ./server/config:/app/config
      - ./server/data:/app/data
      - ./server/logs:/app/logs

  web:
    image: "registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:${EASY_PROXY_VERSION:?EASY_PROXY_VERSION must be set}"
    container_name: easy-proxy-web
    network_mode: host
    restart: unless-stopped
    depends_on:
      - server
```

> 当前服务端代码只对 `server` 配置提供了环境变量覆盖，数据库配置仍需写入 `server.yaml`。H2 Console 开关必须在 `server.yaml` 中修改，当前没有对应的环境变量覆盖项。

`.env` 必须固定本轮镜像标签；正式验收时替换为测试通过后重新构建的 `test-` 标签：

```dotenv
EASY_PROXY_VERSION=exploratory-20261121-233608-4e68667-clean
```

不得使用浮动 `latest`。拉取后应通过 `docker image inspect` 核对 RepoDigest，并在运行清单中同时记录标签和摘要。

### 6.3 服务端配置

首次启动前准备 `/opt/easy-proxy/server/server.yaml`：

> 配置路径必须准确：当前服务端优先读取 jar 同级的 `/app/server.yaml`。`ConfigBootstrap` 虽然会把内置默认文件复制到 `/app/config/server.yaml`，但 `ConfigProperty` 实际不会从该位置读取外部配置。若挂载到错误路径，服务端可能继续使用内置默认值，包括示例数据库密码和开启的 H2 Console。因此必须把外部文件直接绑定到 `/app/server.yaml`，并确保容器启动前该文件已存在。

```yaml
server:
  proxyPort: 21090
  webPort: 21091
  captchaImageEnable: true
  publicHost: 68.64.181.242
  certValidityDays: 3650
  certPassword: "<CHANGE_ME_STRONG_RANDOM>"
  webhook:
  loginFailureThreshold: 5
  loginFailureWindowMinutes: 15
  loginLockMinutes: 15
  loginIpFailureThreshold: 20
  loginIpWindowMinutes: 10
  loginIpBlockMinutes: 10

db:
  url: jdbc:h2:./data/database
  username: "<CHANGE_ME_DB_USER>"
  password: "<CHANGE_ME_STRONG_RANDOM>"
  h2ConsoleEnable: false
```

要求：

- `publicHost` 必须为客户端实际访问的公网地址。
- `certPassword` 不得沿用示例值，首次启动前设置。
- 数据库密码使用独立随机值。
- `h2ConsoleEnable` 测试和正式环境均关闭。
- 配置文件权限设置为 `600`。
- `cert.jks`、`cert.pem`、数据库文件和日志不得提交到 Git。

### 6.4 启动与检查

```bash
cd /opt/easy-proxy
chmod 600 .env server/server.yaml
docker compose pull
docker compose up -d
docker compose ps
docker compose logs --tail=200 server web
```

健康检查：

```bash
curl -fsS http://127.0.0.1:21091/api/open/health
curl -fsS http://127.0.0.1:21092/api/open/health
ss -lntp | grep -E ':(21090|21091|21092)\b'
docker inspect --format '{{.Image}}' easy-proxy-server easy-proxy-web
```

预期：

- `server` 和 `web` 均为运行状态。
- 两个健康检查均返回 `{"status":"UP"}` 或等价成功响应。
- `21090`、`21091`、`21092` 均处于监听状态。
- `21091` 不对公网开放。

### 6.5 防火墙与安全组

云服务商安全组和主机防火墙需同时配置。

测试期建议：

| 端口 | 来源 | 说明 |
| ---: | --- | --- |
| `1514/tcp` | 本地管理机公网 IP | SSH |
| `21090/tcp` | NAS 公网出口 IP | 客户端 TLS 通道 |
| `21092/tcp` | 本地管理机公网 IP | Web 管理和证书下载 |
| `28080/tcp` | 测试访问来源 IP | 本次代理规则 |
| `21091/tcp` | 禁止公网访问 | 仅本机回环访问 |

如果 NAS 公网出口 IP 不固定，可临时允许 `21090` 对所有 IPv4 开放。由于该端口仍需要 Token 和 TLS，风险可控，但正式环境应尽量按来源限制。

## 7. 群晖 NAS 部署方案

### 7.1 目录规划

假定 DSM 的 Docker 共享目录为 `/volume1/docker`：

```text
/volume1/docker/easy-proxy-client/
├── docker-compose.yml
├── config/
└── logs/
```

实际卷路径以 NAS 存储空间编号为准，不要直接假定目录一定存在。

### 7.2 Docker Socket 权限修复与基线

初始状态为 `/var/run/docker.sock` 属主 `root:root`、权限 `660`，`chenkeyudev` 不属于 Docker 管理组，直接执行 `docker info` 返回权限错误；非交互 `sudo -n` 也不可用，导致 NAS 无法部署容器。

采用专用 `docker` 组修复，不使用 `666`：

1. 创建 NAS 本地 `docker` 组，并将 `chenkeyudev` 加入该组。
2. 备份 `/var/packages/ContainerManager/etc/dockerd.json`，本轮备份文件为 `dockerd.json.bak-easy-proxy-20261122-000313`。
3. 在守护进程配置中增加 `"group":"docker"`，通过 JSON 校验后重启 `pkg-ContainerManager-dockerd.service`。
4. 将 Socket 纠正为 `root:docker 660`。
5. 使用全新的普通 SSH 会话，通过 `/usr/local/bin/docker info` 验证 `DOCKER_ACCESS=yes`，Docker Server/Client 均为 `24.0.2`；该非交互会话的 PATH 不含 `/usr/local/bin`，因此不以裸命令 `docker` 作为判定依据。

修复脚本：`docs/tests/scripts/common/fix-nas-docker-access.sh`。复验证据：`docs/tests/runs/20261121-235500/raw/validation/nas-docker-permission-recheck.txt`、`nas-docker-access-fixed.txt`、`nas-docker-permission-live-recheck.txt`。

安全说明：加入 `docker` 组等同于获得主机高权限，应只授予受控运维账号，并纳入账号审计。回滚时恢复上述备份配置、重启 Docker 服务，并重新核对 Socket 属主与权限。

### 7.3 部署测试目标服务
先在 NAS 上启动一个简单的 HTTP 目标：

```bash
docker run -d \
  --name easy-proxy-target \
  --restart unless-stopped \
  -p 18080:80 \
  nginx:stable-alpine
```

在 NAS 本地验证：

```bash
curl -I http://127.0.0.1:18080
curl -I http://192.168.1.9:18080
```

再从局域网内另一台机器验证 `http://192.168.1.9:18080`，确保目标服务确实可从容器网络访问。

### 7.4 客户端 Docker Compose

客户端只需要主动访问公网，不需要开放入站端口。默认使用 bridge 网络即可：

```yaml
services:
  client:
    image: "registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:${EASY_PROXY_VERSION:?EASY_PROXY_VERSION must be set}"
    container_name: easy-proxy-client
    restart: unless-stopped
    environment:
      TZ: Asia/Shanghai
    volumes:
      - ./config:/app/config
      - ./logs:/app/logs
```

首次启动：

```bash
cd /volume1/docker/easy-proxy-client
docker compose pull
docker compose up -d
docker compose logs --tail=100 client
```

客户端首次启动会在 `config` 目录生成 `client.yaml`。

### 7.5 客户端配置

将 `config/client.yaml` 修改为：

```yaml
server:
  ip: 68.64.181.242
  port: 21090
  webPort: 21092
  token: "<WEB_UI_GENERATED_CLIENT_TOKEN>"
```

说明：

- `ip` 是云服务器公网地址，不是 NAS 地址。
- `port` 是客户端 TLS 通道端口 `21090`。
- `webPort` 使用 Web 容器的 `21092`，用于下载 `cert.pem`。
- `token` 必须在 Web 管理界面创建客户端后获取。
- 不要沿用仓库中的示例 Token。

设置权限并重启：

```bash
chmod 600 config/client.yaml
docker compose restart client
docker compose logs -f --tail=200 client
```

预期日志包含连接成功、鉴权成功和在线状态，不应持续出现 `Connect fail` 或 `Client not found for token`。

### 7.6 客户端网络兼容性

优先使用 bridge 网络，规则中的 NAS 目标地址使用 `192.168.1.9:18080`，不要使用 `127.0.0.1:18080`，否则容器会把回环地址解释为客户端容器自身。

如果 bridge 网络无法访问 NAS 本地服务，再评估使用 `network_mode: host`。切换前检查：

- DSM Container Manager 是否允许 host 网络。
- 是否存在端口冲突。
- 规则目标地址是否应改为 `127.0.0.1:18080`。

## 8. 服务端与客户端联调配置

### 8.1 初始化 Web 管理员

访问：

```text
http://68.64.181.242:21092
```

完成管理员初始化。生产环境不要使用弱口令，也不要在公共网络直接暴露未设置强口令的管理界面。

### 8.2 创建客户端

在 Web 管理界面中：

1. 新建客户端，名称建议 `nas-test`。
2. 启用客户端。
3. 生成并保存 Token。
4. 将 Token 写入 NAS 的 `client.yaml`。
5. 重启客户端并确认在线。

### 8.3 创建转发规则

在 Web 管理界面新建规则：

| 配置项 | 测试值 |
| --- | --- |
| 规则名称 | `nas-http-18080` |
| 服务端监听端口 | `28080` |
| 客户端转发地址 | `192.168.1.9:18080` |
| 连接数限制 | 测试期留空或设置合理上限 |
| 带宽限制 | 测试期留空 |
| 启用 | 是 |

规则创建后，服务端应监听 `28080`：

```bash
ss -lntp | grep ':28080\b'
```

### 8.4 证书一致性校验

当前客户端通过 HTTP 下载自签名证书。首次下载链路没有独立认证，因此测试时必须核对证书指纹。

云服务器：

```bash
openssl x509 -in /opt/easy-proxy/server/config/cert.pem -noout -fingerprint -sha256
```

NAS：

```bash
openssl x509 -in /volume1/docker/easy-proxy-client/config/cert.pem -noout -fingerprint -sha256
```

两边指纹必须一致。若不一致，停止客户端并重新下载证书，排查 Web 容器映射、旧证书残留或中间人问题。

## 9. 测试用例

### 9.1 基础连通性

| 编号 | 测试项 | 操作 | 预期结果 |
| --- | --- | --- | --- |
| T-01 | NAS 公钥登录 | `ssh easy-proxy-nas` | 不输入密码即可登录 |
| T-02 | 云服务器公钥登录 | `ssh easy-proxy-cloud` | 不输入密码即可登录 |
| T-03 | Docker 环境 | 两机执行 `docker version`、`docker compose version` | 命令成功，版本满足部署要求 |
| T-04 | NAS 目标服务 | 请求 `http://192.168.1.9:18080` | 返回 Nginx 页面或 `200` |
| T-05 | 服务端健康检查 | 请求 `http://127.0.0.1:21091/api/open/health` | 返回健康状态 |
| T-06 | Web 健康检查 | 请求 `http://68.64.181.242:21092/api/open/health` | 返回健康状态 |
| T-07 | 客户端在线 | 查看 Web 和客户端日志 | 客户端显示在线，无鉴权错误 |

### 9.2 端到端链路

| 编号 | 测试项 | 操作 | 预期结果 |
| --- | --- | --- | --- |
| T-08 | 规则监听 | 云服务器检查 `28080` | 端口处于监听状态 |
| T-09 | 公网访问 | 请求 `http://68.64.181.242:28080` | 返回 NAS 测试服务页面 |
| T-10 | 响应头 | `curl -I http://68.64.181.242:28080` | 返回有效 HTTP 状态和响应头 |
| T-11 | 数据完整性 | 下载固定大小测试文件并对比哈希 | 哈希一致，无截断或乱码 |
| T-12 | 流量统计 | 访问后查看 Web 仪表盘 | 连接数、上下行流量有变化 |

基础端到端访问默认从当前 Windows 管理机执行，并先确认本机网络路径、代理/VPN 状态和实际公网出口 IP。只有在本机出口异常、需要排除本地代理影响或当前管理机无法访问目标时，才使用手机蜂窝网络等备用网络做对照验证；备用网络结果不得替代以当前管理机为压测源的 T-21 至 T-36 结果。

示例：

```powershell
curl.exe -v --connect-timeout 5 http://68.64.181.242:28080/
```

### 9.3 稳定性与异常测试

| 编号 | 测试项 | 操作 | 预期结果 |
| --- | --- | --- | --- |
| T-13 | 客户端重连 | `docker compose restart client` | 30 秒内恢复在线 |
| T-14 | 服务端重启 | `docker compose restart server web` | 客户端自动重连，规则恢复监听 |
| T-15 | 规则持久化 | 重启后查看规则和端口 | 原规则仍存在且生效 |
| T-16 | 错误 Token | 临时使用错误 Token 启动客户端 | 服务端拒绝，日志明确，不能上线 |
| T-17 | 端口冲突 | 占用 `28080` 后重建规则或启动服务 | 有明确冲突错误，不影响其他端口 |
| T-18 | 禁用规则 | 在 Web 中禁用 `28080` 规则 | 公网连接失败，端口释放 |
| T-19 | 删除规则 | 删除测试规则 | 服务端停止监听 `28080` |
| T-20 | 客户端禁用 | 在 Web 中禁用客户端 | 客户端连接关闭，规则不再转发 |

关键观察命令：

```bash
# 云服务器
docker compose logs -f --tail=300 server web
ss -lntp | grep -E ':(21090|21091|21092|28080)\b'

# NAS
docker compose logs -f --tail=300 client
docker logs --tail=100 easy-proxy-target
```

### 9.4 高并发、高负载与长时间稳定性测试

本节验证程序在持续并发和资源接近上限时的正确性、可用性与可靠性。高负载测试用于建立单节点容量基线，不等同于生产容量认证，也不替代多节点高可用测试。

#### 9.4.1 判定原则

| 维度 | 验证重点 | 主要证据 |
| --- | --- | --- |
| 正确性 | 状态码、响应体、文件哈希和请求归属正确，不出现截断、乱码、跨请求串包或串到其他客户端 | 成功请求逐项校验、固定字典文件哈希、大文件下载哈希 |
| 可用性 | 在目标负载下仍可访问，成功率、延迟和健康检查处于可接受范围 | 当前管理机压测结果、持续健康探测、Web 在线状态 |
| 可靠性 | 容器不崩溃、不 OOM、不异常重启，资源有界，故障后自动恢复 | CPU/内存/文件描述符曲线、重启次数、异常日志、恢复时间 |

强制规则：

1. 所有压力请求统一由当前 Windows 管理机发起，不从云服务器本机或 NAS 本机向 `28080` 发起，避免本机回环、安全组和带宽统计失真。
2. 当前管理机与 NAS 位于同一局域网时，公网请求可能共享同一出口上/下行带宽。必须单独记录当前管理机的 CPU、内存和网络占用，不能把管理机资源耗尽或带宽饱和误判为 Easy Proxy 的容量上限。
3. 压测期间当前管理机不运行其他高带宽、高 CPU 任务；关闭或明确绕过 HTTP 代理、VPN 和流量整形工具，并记录实际使用的公网出口。
4. `10`、`100`、`250` 并发为核心测试档位；`500` 并发为高负载探索档位。若首次基线表明当前管理机或本地宽带先于程序达到上限，可以调整档位，但必须记录调整原因。
5. 每个档位先预热 2 分钟，再持续施压至少 5 分钟，档位之间冷却 2 分钟。只有在健康检查恢复后才能进入下一档。
6. 成功请求的数据校验必须为 100% 正确。任何哈希不一致、响应体错位或跨请求污染均直接判定失败，不能以更高吞吐量抵消。
7. 故障注入窗口单独统计。故障前的稳定负载和故障后的恢复负载分别满足成功率与延迟门槛。

#### 9.4.2 测试数据与前置条件

在 NAS 测试目标服务中准备固定测试资源，并在压测前记录基准哈希：

| 资源 | 建议大小 | 用途 |
| --- | ---: | --- |
| `/fixtures/small-1k.bin` | `1 KiB` | 小响应、高并发状态码和延迟测试 |
| `/fixtures/distinct/0001.bin` 至 `1000.bin` | 每个 `1 KiB`，内容各不相同 | 检测响应串包、缓存污染和请求归属错误 |
| `/fixtures/large-10m.bin` | `10 MiB` | 并发大文件完整性与吞吐测试 |
| `/fixtures/slow-2s.bin` | 固定内容，目标端延迟约 `2 s` | 混合负载、慢请求隔离和队列测试 |

测试文件应关闭动态压缩，或验证解压后的内容哈希。压测请求优先显式携带 `Accept-Encoding: identity`，避免把压缩差异误判为数据损坏。

压测前还需确认：

- 当前 Windows 管理机的公网出口 IP 已加入 `28080` 的安全组或防火墙允许列表。
- 记录当前管理机的 CPU、内存、磁盘、公网上下行带宽和网络出口，避免管理机自身先成为瓶颈。
- 确认当前管理机未通过未知 VPN 或 HTTP 代理访问目标，避免请求路径与预期不一致。
- 压测期间单独采集当前管理机的 CPU、内存和网络占用，并保留 `Get-NetTCPConnection` 或等效连接数采样。
- 记录云服务器与 NAS 的 CPU、内存、上行带宽和容器镜像摘要。
- 记录空闲状态下的 CPU、内存、文件描述符和网络基线。
- 运行前按 9.5 节创建 `docs/tests/runs/YYYYMMDD-HHmmss/`，将压测输出、监控采样、日志、响应样本和截图分别写入对应子目录。
- 已约定安全停止条件：错误率连续 1 分钟超过 `5%`、健康检查连续失败 30 秒、容器退出或出现 OOM 时立即停止加压。

建议优先使用 `k6` 编写带状态码、响应体和场景标签的测试；仅测吞吐和延迟时也可使用 `hey` 或 `wrk`。小响应可用以下命令快速建立基线：

```powershell
hey -z 5m -c 100 http://68.64.181.242:28080/fixtures/small-1k.bin
```

并发大文件下载必须逐个校验哈希，示例：

```powershell
$url = 'http://68.64.181.242:28080/fixtures/large-10m.bin'
$expected = (Get-FileHash -Algorithm SHA256 -LiteralPath '.\fixtures\large-10m.bin').Hash
$jobs = 1..20 | ForEach-Object {
    Start-Job -ArgumentList $_, $url, $expected -ScriptBlock {
        param($id, $requestUrl, $expectedHash)
        $outFile = Join-Path $env:TEMP "easy-proxy-large-$id.bin"
        curl.exe -fsS --max-time 120 -o $outFile $requestUrl
        if ($LASTEXITCODE -ne 0) { return "FAIL download $id" }
        $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $outFile).Hash
        Remove-Item -LiteralPath $outFile -Force
        if ($actual -ne $expectedHash) { "FAIL hash $id" } else { "OK $id" }
    }
}
$jobs | Wait-Job | Receive-Job
$jobs | Remove-Job
```

#### 9.4.3 压测用例

| 编号 | 维度 | 测试项 | 负载或操作 | 通过标准 |
| --- | --- | --- | --- | --- |
| T-21 | 基线 | 容量梯度基线 | 依次执行空闲、`10`、`100`、`250`、`500` 并发小请求，每档预热 2 分钟后运行 5 分钟，记录 RPS、成功率、p50/p95/p99、最大延迟和错误类型 | 形成完整阶梯结果；每档都记录停止原因和资源曲线；档位间健康检查恢复；无数据错误或进程崩溃 |
| T-22 | 正确性 | 高并发响应归属 | 在 `10`、`100`、`250` 并发下轮询 1000 个唯一小文件，逐次校验状态码、Content-Length 和 SHA-256 | 成功请求响应体 100% 匹配；无截断、乱码、缓存串包或串到其他客户端；非预期 5xx 为 0 |
| T-23 | 可用性 | 短连接与长连接并发 | `100` 并发运行 5 分钟，混合 `Connection: keep-alive` 与 `Connection: close`，同时每秒执行外部健康探测 | 成功率不低于 `99.5%`；无持续 connection refused/reset；停止后 60 秒内活动连接和文件描述符回落 |
| T-24 | 可用性 | 高负载容量与饱和保护 | 以 `250` 并发运行 10 分钟，再以 `500` 并发运行 3 至 5 分钟；逐步升至错误率或延迟超过阈值 | `250` 并发成功率不低于 `99%`、p95 不高于 `1 s`、p99 不高于 `2 s`；`500` 档允许暴露容量上限，但不得崩溃、OOM、挂死或损坏数据；撤压后 2 分钟内恢复正常 |
| T-25 | 正确性 | 并发大文件传输 | 20 路并发下载 `10 MiB` 文件，执行 3 轮，每轮全部文件与基准 SHA-256 对比 | 所有成功下载哈希一致且大小正确；无零字节、截断或不同请求内容混合；server/client 不重启 |
| T-26 | 可用性 | 小请求、大请求与慢请求混合 | 持续 10 分钟，约 70% 小请求、20% 大文件、10% 慢请求，固定并发和到达速率 | 小请求不被大文件或慢请求长期饿死；成功请求哈希正确；小请求 p99 不超过无混合负载基线的 2 倍；无无限排队 |
| T-27 | 正确性 | 流量与连接统计一致性 | 记录压测前后 Web 仪表盘、连接数、上下行流量和实际成功传输字节数 | 统计方向正确、变化量级合理，无负数、重置或异常跳变；停止压测后活动连接在 60 秒内回落到基线附近 |
| T-28 | 可靠性 | 资源占用与泄漏 | `100` 并发持续 15 分钟，每 5 秒采集 cloud `server/web` 与 NAS `client/target` 的 CPU、内存、文件描述符、容器状态和重启次数 | CPU 和内存 1 分钟均值持续低于 `80%`；预热后内存增长不超过 `10%`；文件描述符有界且停止后回落；无 OOM、panic 或容器重启 |
| T-29 | 可靠性 | 长时间稳定性 SOAK | 使用稳定容量的 50% 或 `100` 并发，持续 60 分钟；测试窗口不足时至少运行 30 分钟并记录原因 | 无崩溃、OOM 或异常重启；健康检查可用性不低于 `99.9%`；成功请求数据正确；预热后内存无持续单调增长，p95 漂移不超过 20% |
| T-30 | 可靠性 | 撤压后的资源释放 | `500` 并发运行 3 分钟后立即停止压测，再以低速率探测 2 分钟 | 5xx、超时和连接失败在 60 秒内消失；活动连接、文件描述符和内存回落；低速率请求成功率恢复至 `100%` |
| T-31 | 可靠性 | 压测中重启 NAS 客户端 | `100` 并发运行至第 2 分钟时执行 `docker compose restart client` | 故障窗口内返回明确超时或 502/504，不出现错误成功；客户端 30 秒内恢复在线，转发在 60 秒内恢复；server 不崩溃且无重复监听 |
| T-32 | 可靠性 | 压测中重启云服务器端 | `100` 并发运行至第 2 分钟时执行 `docker compose restart server` | 原有连接中断可观测；客户端自动重连，规则在 60 秒内恢复；恢复后的请求状态码和数据 100% 正确；无端口残留或重复监听 |
| T-33 | 可用性 | 压测中重启 Web | `100` 并发持续 5 分钟，在第 2 分钟重启 `web` 容器 | Web 短暂不可用属于预期；`28080` 数据面不中断或仅在数秒内波动；Web 30 秒内恢复健康；数据面成功率不低于 `99.5%` |
| T-34 | 可靠性 | 压测中重启 NAS 目标服务 | `100` 并发运行至第 2 分钟时重启 `easy-proxy-target` | 目标不可用期间返回明确 502/504 或连接错误；client/server 不崩溃不重启；目标恢复后 30 秒内转发恢复，后续哈希一致 |
| T-35 | 可靠性 | 网络中断与自动重连 | 压测期间临时阻断 `21090` 或在可控网络设备上断网 30 秒，然后恢复 | 故障期间请求有明确失败结果且不挂死；恢复后 60 秒内自动重连；不出现重复会话、规则重复监听或持续鉴权/证书错误 |
| T-36 | 可靠性 | 日志、队列与容器健康 | 汇总 T-21 至 T-35 的容器日志、退出码、`RestartCount`、`OOMKilled`、磁盘和内存采样 | 无 panic、OOM、反复重启、无界队列积压、磁盘快速耗尽或持续错误刷屏；异常日志能在故障恢复后停止 |

#### 9.4.4 运行中监控

云服务器采样：

```bash
docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.PIDs}}'
docker inspect --format '{{.Name}} status={{.State.Status}} restart={{.RestartCount}} oom={{.State.OOMKilled}}' easy-proxy-server easy-proxy-web
docker exec easy-proxy-server sh -c 'grep -E "VmRSS|Threads" /proc/1/status; echo fd=$(ls /proc/1/fd | wc -l)'
docker compose logs --tail=300 server web
```

NAS 采样：

```bash
docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.PIDs}}'
docker inspect --format '{{.Name}} status={{.State.Status}} restart={{.RestartCount}} oom={{.State.OOMKilled}}' easy-proxy-client easy-proxy-target
docker exec easy-proxy-client sh -c 'grep -E "VmRSS|Threads" /proc/1/status; echo fd=$(ls /proc/1/fd | wc -l)'
docker compose logs --tail=300 client
```

当前管理机采样：

```powershell
(Get-CimInstance Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average
Get-CimInstance Win32_OperatingSystem | Select-Object FreePhysicalMemory, TotalVisibleMemorySize
(Get-NetTCPConnection -RemotePort 28080 -ErrorAction SilentlyContinue).Count
Get-NetAdapterStatistics | Select-Object Name, ReceivedBytes, SentBytes
```

当前管理机上的健康探测应独立于压测进程运行：

```powershell
curl.exe -o NUL -sS -w "%{http_code} %{time_total}\n" --max-time 2 http://68.64.181.242:21092/api/open/health
curl.exe -o NUL -sS -w "%{http_code} %{time_total}\n" --max-time 2 http://68.64.181.242:28080/fixtures/small-1k.bin
```

#### 9.4.5 结果记录与初始阈值

每次压测至少记录：运行编号、开始与结束时间、当前管理机规格与公网出口、管理机 CPU/内存/网络采样、镜像摘要、负载工具和版本、并发数、持续时间、总请求数、成功数、失败数、状态码分布、RPS、p50/p95/p99/max、哈希校验结果、服务端与 NAS 资源峰值、容器重启/OOM 次数、异常日志和截图在对应 `docs/tests/runs/<run-id>/` 归档目录中的相对路径。若管理机网络先饱和，必须在报告中标注“压测源瓶颈”，不能据此给出程序容量上限。

初始验收阈值如下。首次基线后可调整，但必须在本目录记录调整前后的数值和原因。CPU 按容器限额归一化；未设置限额时按单核折算并记录口径，内存按容器限额或主机可用内存记录口径：

- `100` 并发稳定负载：成功率不低于 `99.5%`，小响应 p95 不高于 `500 ms`，p99 不高于 `1 s`。
- `250` 并发高负载：成功率不低于 `99%`，小响应 p95 不高于 `1 s`，p99 不高于 `2 s`。
- 稳定负载期间非预期 5xx 为 0；连接拒绝、鉴权错误和证书错误不得持续出现。
- 成功请求的数据哈希不一致数为 0。
- CPU、内存持续占用低于 `80%`，预热后内存增长不超过 `10%`，文件描述符无持续增长。
- SOAK 期间健康检查可用性不低于 `99.9%`，无异常重启，p95 漂移不超过 20%。
- 计划内的 client/server 重启后 30 至 60 秒内恢复；网络恢复后 60 秒内自动重连。
- 撤压后 2 分钟内数据面、资源占用和日志恢复到稳定状态。

### 9.5 测试资产、数据与报告归档

所有测试过程中的脚本、配置、测试数据、生成文件、命令输出、原始日志、监控采样、截图、分析结果和最终报告，必须归档到当前项目的 `docs/tests/` 目录；不得只保留在桌面、用户临时目录、容器内部或远端服务器。标准目录结构如下：

```text
docs/
├── nas-cloud-proxy-deployment-plan.md
└── tests/
    ├── README.md
    ├── scripts/
    │   ├── common/
    │   ├── functional/
    │   ├── load/
    │   ├── fault-injection/
    │   ├── monitoring/
    │   └── reporting/
    ├── data/
    │   ├── fixtures/
    │   │   ├── small/
    │   │   ├── distinct/
    │   │   ├── large/
    │   │   └── slow/
    │   ├── generated/
    │   └── manifests/
    ├── configs/
    │   ├── k6/
    │   ├── hey/
    │   ├── docker/
    │   └── environment/
    ├── templates/
    │   ├── run-manifest.md
    │   ├── test-report.md
    │   └── evidence-index.md
    ├── runs/
    │   └── YYYYMMDD-HHmmss/
    │       ├── manifest.md
    │       ├── raw/
    │       │   ├── functional/
    │       │   ├── load/
    │       │   ├── health/
    │       │   ├── metrics/
    │       │   ├── logs/
    │       │   └── responses/
    │       ├── processed/
    │       │   ├── summaries/
    │       │   ├── charts/
    │       │   └── validation/
    │       ├── screenshots/
    │       ├── tmp/
    │       └── SHA256SUMS.txt
    └── reports/
        ├── index.md
        ├── YYYYMMDD-HHmmss-test-report.md
        ├── YYYYMMDD-HHmmss-summary.csv
        └── YYYYMMDD-HHmmss-capacity.png
```

目录职责：

| 目录 | 保存内容 | 归档要求 |
| --- | --- | --- |
| `scripts/` | 当前管理机 PowerShell 脚本、k6 脚本、监控和报告生成脚本 | 按用途分类；临时命令应先固化为脚本，或在运行清单中记录完整命令 |
| `data/fixtures/` | 固定小文件、1000 个唯一文件、大文件和慢请求文件 | 输入数据保持只读；不得在运行中覆盖 |
| `data/generated/` | 测试脚本生成的数据、请求样本和可复用回放数据 | 同时记录生成参数、生成脚本和 SHA-256 |
| `data/manifests/` | 数据文件清单、大小、用途和基准 SHA-256 | 每轮测试开始前复核，发现变化必须停止并说明原因 |
| `configs/` | k6、hey、Docker、主机与环境快照等测试配置 | 与运行清单关联；不得只保留截图版本 |
| `runs/<run-id>/raw/` | 原始输出、原始日志、监控采样、健康探测和错误响应 | 只追加不覆盖，不做手工美化；失败和重试证据必须保留 |
| `runs/<run-id>/processed/` | 汇总 CSV、图表、校验结果和派生数据 | 必须能由 `raw/` 和脚本复现，并注明生成命令 |
| `runs/<run-id>/screenshots/` | Web 仪表盘、容器状态、告警和异常截图 | 文件名包含测试编号和时间，敏感内容需遮盖 |
| `runs/<run-id>/tmp/` | 运行期间临时下载、解压和中间文件 | 仅作临时位置；清理前将需保留证据移入 `raw/` 或 `processed/` |
| `reports/` | 最终测试报告、汇总 CSV、容量图表和报告索引 | 每轮一份独立报告，并从 `reports/index.md` 建立索引 |

执行规则：

1. 每轮测试开始前创建唯一运行目录，运行编号使用本地时间 `yyyyMMdd-HHmmss`，格式示例为 `20261121-143000`。重试不得覆盖原运行，应使用新的运行编号或在文件名末尾增加 `-retry01`。
2. 每个运行目录必须包含 `manifest.md`，至少记录运行编号、测试范围、执行人、开始和结束时间、Git 提交、当前管理机规格与公网出口、云服务器和 NAS 环境、镜像摘要、测试参数、预期结果和实际完成情况。
3. 文件命名统一为 `<测试编号>_<用途>_<时间戳>.<扩展名>`，例如 `T-24_load-c250_20261121-143500.csv`；文件名使用英文、数字、短横线和下划线，避免空格和中文路径问题。
4. 所有命令的标准输出和标准错误通过 `Tee-Object` 或等效方式同时写入 `raw/logs/`，不得只显示在终端。测试失败、超时、连接中断和取消执行同样需要归档。
5. `raw/` 中的数据视为原始证据，完成后不再修改。分析结果放入 `processed/`，最终报告必须使用相对路径链接对应原始证据。
6. 测试脚本、固定数据、运行清单和最终报告生成 SHA-256；运行目录使用 `SHA256SUMS.txt` 记录关键产物摘要，报告生成后重新校验。
7. 最终报告统一写入 `docs/tests/reports/`，至少包含环境、测试范围、用例结果、容量曲线、成功率与延迟、数据完整性、资源使用、故障恢复、缺陷、压测源瓶颈说明、清理结果和结论。
8. 密钥、密码、Token、Cookie、Authorization 头和其他敏感信息不得明文归档；原始数据入库前必须脱敏，并在报告中记录脱敏范围和脱敏后的可验证性限制。
9. 测试结束前核对脚本、数据、日志、指标、截图、哈希和报告是否闭环；无法归档的证据必须说明原因、影响和替代证据。

运行前可在项目根目录执行以下 PowerShell 片段创建标准目录：

```powershell
$runId = Get-Date -Format 'yyyyMMdd-HHmmss'
$runRoot = Join-Path (Get-Location) "docs\tests\runs\$runId"
$relativeDirs = @(
    'raw\functional',
    'raw\load',
    'raw\health',
    'raw\metrics',
    'raw\logs',
    'raw\responses',
    'processed\summaries',
    'processed\charts',
    'processed\validation',
    'screenshots',
    'tmp'
)
New-Item -ItemType Directory -Path $runRoot -Force | Out-Null
foreach ($relativeDir in $relativeDirs) {
    New-Item -ItemType Directory -Path (Join-Path $runRoot $relativeDir) -Force | Out-Null
}
Write-Output $runRoot
```
### 9.6 镜像构建与发布验证

| 编号 | 测试项 | 操作 | 预期结果 |
| --- | --- | --- | --- |
| T-37 | WSL 从当前源码构建 | 确认 Git 提交和已跟踪文件状态；在 `Ubuntu-22.04` 内用 Docker/Buildx 构建 `server`、`client`、`web` 三套 `linux/amd64` 镜像；最终验收构建必须先通过 `mvn clean test` | 三套镜像构建成功且标签固定；构建元数据记录 Git 提交、平台、构建时间和测试结果；单元测试失败时仅允许生成 `exploratory-` 镜像，不得进入验收部署 |
| T-38 | ACR 交互式发布与凭据清理 | 运行 `build-and-push-images.ps1`，交互输入凭据；检查构建、推送和注销日志 | 密码仅经 stdin 传递，日志、报告和命令记录中无凭据；三套镜像推送成功；完成后必须出现 `ACR_AUTH_ENTRY=no` |
| T-39 | 远端清单与产物一致性 | 对三套镜像执行远端 manifest、RepoDigest、平台和云服务器拉取核验；比较本地 fat JAR 与容器内 `/app/app.jar` 的 SHA-256 | 远端标签和 RepoDigest 可读取；三套均为 `linux/amd64`；云服务器可拉取；容器内 JAR 与本地产物一致；所有证据归档到 `docs/tests/runs/<run-id>/` |

### 9.7 缺陷闭环与严重缺陷终止规则

本节定义本轮测试的强制闭环。测试结束不等于闭环完成；只有在同一代码和镜像基线下，所有适用门槛均满足、缺陷均已关闭且证据链可追溯，才能形成最终通过结论。

#### 9.7.1 闭环阶段与状态

闭环按以下阶段运行：

```text
基线确认
→ 缺陷复现与定级
→ 根因修复
→ 单元/集成验证
→ 正式镜像构建与 ACR 发布
→ 云端/NAS 部署
→ 端到端测试
→ 稳定性与故障恢复
→ 高并发/高负载/SOAK
→ 报告、哈希与归档
→ 最终复核
```

每个阶段都必须有明确的进入条件、退出条件和证据。任一步骤失败时，必须保存现场并跳转到“缺陷修复与重新验证”，不得把失败用例记为通过，也不得跳过失败项直接进入验收。

用例和阶段状态至少使用以下取值：

| 状态 | 含义 | 处理要求 |
| --- | --- | --- |
| `PASS` | 当前运行实际执行并满足预期 | 关联原始证据、镜像摘要和环境记录 |
| `FAILED` | 当前运行实际执行但结果不符合预期 | 创建缺陷记录，进入修复与重新验证 |
| `BLOCKED` | 环境、依赖或权限导致无法执行 | 记录解除条件和责任人，不得记为通过 |
| `TERMINATED_BY_BLOCKER` | 因 P0/P1 严重缺陷主动终止 | 保存终止现场，立即进入缺陷修复与重新验证 |
| `SKIPPED_BLOCKED` | 因前置用例终止而未执行 | 关联阻断缺陷和终止记录，不得计入通过率 |
| `NOT_RUN` | 尚未开始执行 | 最终报告中必须说明原因，不得作为验收依据 |

每个缺陷使用唯一编号（如 `DEF-001`）跟踪，至少记录严重级别、触发用例、现象、影响范围、根因、修复提交、修复说明、验证证据、回归范围和关闭状态。缺陷修复提交后，必须重新构建正式镜像并执行验证；仅通过局部单元测试不能关闭端到端或容量缺陷。

修复后应从最早受影响的阶段开始重跑，并重跑其全部下游阶段。代码发生变化时必须使用新的固定镜像标签和新的运行编号，不得复用修复前的通过结果。最终通过报告必须同时对应同一 Git 提交、同一镜像标签、同一 RepoDigest、同一套测试资产哈希和一条完整的最终通过链；否则结论只能是“有条件通过”或“未通过”。

#### 9.7.2 严重缺陷与提前终止

P0 和 P1 均属于严重缺陷，必须触发提前终止。典型判定如下：

| 级别 | 典型情形 |
| --- | --- |
| P0 | 数据丢失、截断、损坏、乱码或哈希不一致；跨请求或跨客户端串包；鉴权绕过、错误 Token 仍可上线、证书校验失效或敏感信息泄露；核心转发链路完全不可用或转发到错误目标；进程崩溃、持续重启、OOM 或无法恢复；测试证据被污染，导致后续结果不可采信 |
| P1 | 客户端持续无法重连或在线状态长期错误；规则删除后端口未释放；状态持久化失效；严重资源泄漏；容量门槛下持续 5xx、无界队列积压或服务不可用；核心测试场景重复失败且无法在计划窗口内恢复 |

发现 P0/P1 时执行以下规则：

1. 立即终止当前测试组，不再启动新的高并发、高负载或破坏性用例；正在进行的压力请求应安全降载并停止。
2. 保存终止时的日志、指标、容器状态、配置快照、错误响应、截图和当前运行清单，记录触发用例、缺陷编号、严重级别、影响范围和终止时间。
3. 将当前阶段标记为 `TERMINATED_BY_BLOCKER`，未执行用例标记为 `SKIPPED_BLOCKED`，不得继续补记通过。
4. 直接跳转到闭环中的“缺陷修复与重新验证”环节。这里的下一环节不是后续测试组，也不是验收；不得带严重缺陷继续测试或给出通过结论。
5. 修复完成并通过针对性验证后，使用新运行编号和新固定镜像标签，从最早受影响阶段重跑并覆盖全部下游链路。
6. 涉及凭据、证书或数据安全的 P0 事件，应隔离受影响环境并按规定轮换凭据；文档、日志和报告中不得记录任何明文凭据。

P2/P3 普通缺陷可在不影响后续结果可信度时记录后继续当前测试，但不得通过重试掩盖确定性失败。若无法判断是否影响后续结果，按更高级别处理并终止当前测试组。

#### 9.7.3 轮次、重测与报告追溯

每轮修复和重测都必须生成新的运行目录或新的轮次记录，失败运行不得覆盖或删除。报告中至少包含轮次表：

| 字段 | 内容 |
| --- | --- |
| 运行编号 | `docs/tests/runs/<run-id>/` |
| Git 提交 | 本轮实际部署的提交 |
| 镜像标签与 RepoDigest | server/client/web 三套固定镜像 |
| 失败或终止用例 | 用例编号、状态、现象和证据路径 |
| 缺陷编号 | 对应 `DEF-xxx` 和严重级别 |
| 修复提交 | 代码或配置修复提交，必须可追溯 |
| 重测范围 | 最早受影响阶段及其全部下游用例 |
| 最终状态 | `PASS`、`FAILED` 或 `BLOCKED` |

修复前的报告保留并标记为 `SUPERSEDED`，通过链接指向最新运行；最终报告必须列出闭环轮次、已关闭缺陷、重测结果和最终通过链。报告发布后若发现新的 P0/P1、证据缺陷或复现出已关闭问题，必须重新打开闭环，更新报告状态并再次执行修复和回归。
## 10. 验收标准

满足以下条件可判定测试通过：

1. NAS 和云服务器均可仅使用公钥登录。
2. 云服务器上的 `server`、`web` 容器稳定运行。
3. NAS 上的 `client` 容器能自动连接并保持在线。
4. 客户端证书与云服务器证书 SHA-256 指纹一致。
5. 公网 `28080` 能稳定访问 NAS `18080` 的测试服务。
6. 10 MB 级测试数据传输后哈希一致。
7. 客户端或服务端重启后能在 30 秒内自动恢复。
8. 禁用或删除规则后，对应公网端口被释放。
9. Web 管理界面能看到在线状态、连接和流量变化。
10. 日志中不存在持续的鉴权失败、证书错误或连接异常。
11. 完成 T-21 容量梯度测试，并记录各档位的 RPS、成功率、延迟、资源使用和容量拐点。
12. `100` 并发稳定负载成功率不低于 `99.5%`，小响应 p95 不高于 `500 ms`、p99 不高于 `1 s`，非预期 5xx 为 0。
13. `250` 并发高负载成功率不低于 `99%`，小响应 p95 不高于 `1 s`、p99 不高于 `2 s`。
14. `500` 并发可作为探索档位暴露容量上限，但程序不得崩溃、OOM、挂死、异常重启或返回错误数据；撤压后 2 分钟内恢复。
15. 高并发小文件、并发大文件、混合负载和大文件下载均通过 SHA-256 或响应体校验，无截断、乱码、串包或客户端错配。
16. server/client 在压测期间 CPU、内存持续占用低于 `80%`，预热后内存增长不超过 `10%`，文件描述符无持续增长。
17. SOAK 测试至少持续 30 分钟（目标 60 分钟），健康检查可用性不低于 `99.9%`，无异常重启，延迟漂移不超过 20%。
18. 压测中重启 client/server 后，在线状态和转发在 30 至 60 秒内恢复；重启 Web 不影响数据面稳定；重启目标服务后转发自动恢复。
19. 网络中断恢复后 60 秒内自动重连，无重复会话、重复监听或持续错误日志。
20. 压测撤除后 2 分钟内连接数、文件描述符、内存和错误日志恢复到稳定状态。
21. 日志中无 panic、OOM、反复重启、无界队列积压或磁盘快速耗尽。
22. Web 仪表盘的连接数和流量统计与实际压测结果方向一致、量级合理，无负数或异常重置。
23. 所有测试脚本、配置、固定数据、生成数据、原始输出、日志、监控采样和截图均归档在 `docs/tests/` 标准目录中，不以系统临时目录或远端主机作为唯一保存位置。
24. 每轮测试均有 `manifest.md`、原始证据校验和最终报告，报告可从 `docs/tests/reports/index.md` 追溯；敏感信息已完成脱敏。
25. 单元测试全部通过；当前 `4` 个测试中 `3` 个错误的解密测试失败已修复，且 `mvn -B -ntp clean test` 成功。
26. 三套正式验收镜像使用同一通过测试的 Git 提交构建，标签不以 `latest` 命名，RepoDigest、`linux/amd64` 平台、容器内 JAR 哈希和云服务器拉取结果均已记录。
27. NAS Docker socket 权限已解决，server/client/web 均从 ACR 拉取固定镜像完成实际部署；端到端、高并发和高负载测试全部执行并满足本方案门槛。
28. 按 9.7 节完成缺陷闭环：不存在未关闭的 P0/P1 缺陷；所有失败与终止轮次均保留并关联缺陷、修复提交和重测证据；最终通过链对应同一 Git 提交、同一固定镜像标签与 RepoDigest，且修复后已从最早受影响阶段重跑全部下游测试。

## 11. 回滚与清理

测试完成后，按以下顺序清理：

1. 在 Web 管理界面禁用或删除测试规则。
2. 停止 NAS 客户端。
3. 停止云服务器 Web 和服务端。
4. 停止 NAS 测试目标服务。
5. 撤销临时防火墙规则。
6. 如需彻底清理，再删除测试容器、网络和持久化目录。

示意命令：

```bash
# NAS
cd /volume1/docker/easy-proxy-client
docker compose down
docker rm -f easy-proxy-target

# 云服务器
cd /opt/easy-proxy
docker compose down
```

删除数据目录前先备份 `server/server.yaml`、`server/data`、`server/config` 和必要日志。不要在未确认路径的情况下执行递归删除。

## 12. 风险与注意事项

### 12.1 已确认与待确认项

- 云服务器 SSH 用户名已确认：`yudejijie`。
- 本地 SSH 公钥已配置到两台主机并验证登录成功；云服务器密码曾在聊天中出现，建议尽快更换。
- WSL 构建环境、ACR 推送、远端 manifest 和云服务器拉取已验证。
- 单元测试当前失败：4 个测试中 3 个错误，必须修复后才能形成正式验收镜像。
- NAS Docker socket 权限已修复并复验：`root:docker 660`，普通用户可通过 `/usr/local/bin/docker` 执行 Docker；非交互 SSH 的 PATH 需显式补充。详细证据和回滚方式见第 7.2 节。
- NAS 公网出口 IP 是否固定尚未确认，影响 `21090` 的来源限制。
- 云服务商安全组是否允许自定义高位端口尚未确认。

### 12.2 实施风险

| 风险 | 影响 | 控制措施 |
| --- | --- | --- |
| 云服务器密码泄露 | 主机被未授权访问 | 立即更换密码，并在验证后关闭密码登录 |
| `21091` 暴露公网 | API 被扫描或攻击 | 安全组阻断，主机防火墙仅允许本机 |
| 证书下载被篡改 | 客户端信任错误证书 | 比对 SHA-256 指纹 |
| Web 管理界面使用 HTTP | 登录凭据可能被窃听 | 限制来源 IP，后续增加 HTTPS |
| 镜像标签漂移或误用 `latest` | 无法复现、测试对象改变 | 仅使用固定 `test-`/`exploratory-` 标签，并记录 RepoDigest；Compose 缺少版本变量时直接失败 |
| NAS 桥接网络访问本地服务失败 | 代理无法连接目标 | 改用 NAS 局域网 IP，必要时切换 host 网络 |
| 动态端口未放行 | 规则监听但公网不可达 | 安全组和主机防火墙同步放行 |
| 默认配置值未修改 | 弱密码和示例密钥风险 | 首次启动前修改证书密码、数据库密码和 Token |
| NAS docker 组成员权限过大 | 组内账号可控制主机容器，近似 root 权限 | 仅授予受控运维账号，定期审计组成员；禁止 socket 666；保留 daemon 配置备份 |
| `server.yaml` 挂载路径错误 | 服务端静默使用内置默认配置，可能暴露 H2 Console | 绑定到 `/app/server.yaml`，启动日志核对端口、数据库和 H2 Console 状态 |
| 单元测试失败仍被部署 | 未验证代码进入验收环境 | 默认构建在测试失败时终止；只有显式 `-AllowFailedTests` 且标签以 `exploratory-` 开头时允许构建定位镜像 |
| ACR 凭据泄露 | 仓库或镜像被未授权推送 | 密码仅经 stdin 传入，禁止写入日志；完成后注销并验证 `ACR_AUTH_ENTRY=no` |

### 12.3 代码与文档不一致

当前 README 和根目录 Compose 中部分端口描述与代码不一致：

- README 中提到 Web 管理端口 `21093`。
- `nginx.conf` 实际监听 `21092`。
- 根目录 Compose 暴露 `21093:21093`，无法与当前 Nginx 配置直接匹配。
- 客户端示例配置字段仍使用旧格式 `address`，而实际代码读取 `ip`。

本方案以当前代码行为为准，不使用根目录 Compose 直接部署。测试完成后再单独提交文档和配置修正。

## 13. 执行前检查清单

- [x] 本地 SSH 公钥已配置到 NAS 和云服务器并验证通过
- [x] 云服务器 SSH 用户名已确认：`yudejijie`
- [x] 使用本地 `id_ed25519` 公钥验证 NAS 和云服务器登录
- [x] 确认 WSL `Ubuntu-22.04`、Docker、Buildx 和 Compose 可用
- [x] 完成探索性三镜像 ACR 推送、远端清单核验和云服务器拉取
- [x] 验证 ACR 凭据已注销：`ACR_AUTH_ENTRY=no`
- [ ] 修复单元测试失败，全部测试通过后才能构建正式验收镜像
- [x] 确认 NAS DSM 版本、Container Manager 权限并解决 Docker socket 权限（`root:docker 660`，普通用户通过 `/usr/local/bin/docker` 返回 `DOCKER_ACCESS=yes`）
- [ ] 确认云服务器安全组可配置 `21090`、`21092`、`28080`
- [ ] 确认本地管理机公网 IP
- [ ] 确认 NAS 公网出口 IP 或接受临时开放 `21090`
- [ ] 确认 `server.yaml` 已绑定到容器 `/app/server.yaml`
- [ ] 确认 `28080` 在云服务器未被占用
- [ ] 确认 `18080` 在 NAS 未被占用
- [ ] 准备新的强密码、数据库密码和客户端 Token
- [x] 记录本轮探索性镜像固定标签、RepoDigest、平台和 JAR 哈希
- [ ] 通过测试后构建 `test-` 固定标签镜像并替换本轮探索性镜像
- [ ] 确认当前 Windows 管理机公网出口 IP，并将其加入 `28080` 允许列表
- [ ] 记录当前管理机 CPU、内存、磁盘和公网上下行带宽，关闭无关高负载任务
- [ ] 确认当前管理机绕过未知代理/VPN，并记录实际公网出口
- [ ] 准备固定小文件、1000 个唯一文件和 10 MiB 大文件，记录 SHA-256 基准
- [ ] 记录空闲状态 CPU、内存、文件描述符、带宽和容器镜像摘要
- [ ] 确认压测档位、预热/冷却时间、安全停止条件和回滚方式
- [ ] 确认 server/client/target 的资源采样命令和日志保存目录可用
- [ ] 约定测试窗口、带宽影响范围和数据清理责任
- [ ] 已建立 `docs/tests/` 标准目录、运行清单模板和测试报告模板
- [ ] 已确认所有测试脚本、临时命令和生成数据均有明确归档路径
- [ ] 已确认原始输出、日志、截图和报告不会只保存在系统临时目录或远端主机
- [ ] 已明确缺陷分级、缺陷编号、闭环责任人、运行状态和严重缺陷终止流程
- [ ] 已确认 P0/P1 修复后使用新运行编号与固定镜像标签，从最早受影响阶段重跑全部下游测试

## 14. 下一步

NAS Docker 权限已解除阻断，但正式验收仍必须按以下顺序推进：

1. 修复密码解密相关单元测试，重新执行 `mvn -B -ntp clean test`，确认全量测试通过。
2. 使用同一通过测试的 Git 提交构建 `test-` 固定标签镜像，重新推送 ACR，核验 RepoDigest、`linux/amd64`、容器内 JAR 和云服务器拉取结果。
3. 云服务器创建部署目录、配置和 Compose，启动 server/web 并完成健康检查与管理员初始化。
4. NAS 启动测试目标与 client，创建客户端 Token 和转发规则，验证在线状态、证书与端到端链路。
5. 从当前 Windows 管理机执行 T-01 至 T-20，完成基础功能、恢复能力和数据正确性验证。
6. 从当前 Windows 管理机执行 T-21 至 T-36 的高并发、高负载与 SOAK 测试，实时归档原始证据、资源采样和生成数据。
7. 汇总 T-37 至 T-39 的构建发布证据，生成图表与最终报告，更新 `docs/tests/reports/index.md`。
8. 任一轮测试发现失败时，按 9.7 节记录缺陷并进入“缺陷修复与重新验证”；发现 P0/P1 时立即终止当前测试组，保存证据后直接跳转到缺陷修复环节。
9. 修复后使用新运行编号和新固定镜像标签，从最早受影响阶段重跑全部下游测试；重复“修复 → 验证 → 测试 → 报告”闭环，直至不存在未关闭的 P0/P1 且所有强制门槛通过。
10. 在给出结论前复核哈希、脱敏、临时文件清理和归档完整性；只有同一代码和镜像基线的最终通过链全部满足，才能判定正式验收通过。