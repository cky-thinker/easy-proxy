# 20261121-235500 Easy Proxy 构建、发布与 NAS 权限验证报告

## 结论

- 总体判定：**有条件通过**。该结论仅覆盖 SSH、NAS Docker 权限、最新源码探索性构建、ACR 发布、远端清单、云服务器拉取和产物一致性，不是系统验收通过。
- 构建与发布链路：有条件通过。三套镜像已推送并可拉取，RepoDigest、平台和容器内 JAR 已核对。
- 单元测试：**失败**，4 个测试中 3 个错误，正式验收镜像被阻断。
- NAS Docker 权限：**已修复并通过普通用户会话复验**，Socket 为 `root:docker 660`。
- 实际部署、端到端、故障恢复、高并发、高负载和 SOAK：**未执行**。

## 测试范围

- 验证云服务器与群晖 NAS 的本地公钥登录。
- 在 WSL `Ubuntu-22.04` 中使用当前项目源码构建 `server`、`client`、`web` 三套 `linux/amd64` 镜像。
- 推送至 `registry.cn-hangzhou.aliyuncs.com/ckyspace`，验证固定标签、RepoDigest、平台与凭据清理。
- 从云服务器重新拉取三套镜像，核对远端摘要。
- 比较本地 fat JAR 与容器内 `/app/app.jar` 的 SHA-256。
- 修复 NAS Docker Socket 权限，并验证普通 SSH 用户可通过绝对路径执行 Docker。

## 环境与版本

- Git：`develop`，提交 `4e68667e40e1adb1eba96c48f9d567705525d3c2`。
- 镜像标签：`exploratory-20261121-233608-4e68667-clean`。
- 构建环境：WSL Ubuntu `22.04.5`、Docker `29.10.0`、Buildx `v0.39.1`、Compose `v5.8.0`。
- 云服务器：Ubuntu `22.04.5`，2 vCPU / 1964 MiB，`68.64.181.242:1514`。
- 群晖 NAS：x86_64，6 CPU / 15972 MiB，DSM Container Manager Docker `24.0.2`，`192.168.1.9:10080`。
- 压测源：当前 Windows 管理机；本轮尚未执行压测。

## 镜像与产物

| 服务 | 固定标签 | RepoDigest | 平台 | 结果 |
| --- | --- | --- | --- | --- |
| server | `exploratory-20261121-233608-4e68667-clean` | `sha256:9c18e11d395f3c757e1622320515f3105bf206777612d3946b73410adc492302` | `linux/amd64` | 推送、远端清单、云端拉取通过 |
| client | `exploratory-20261121-233608-4e68667-clean` | `sha256:d36112d730c53d708a61d83c9a3b3dd4af16eb7d81193177468c631335bc8cd3` | `linux/amd64` | 推送、远端清单、云端拉取通过 |
| web | `exploratory-20261121-233608-4e68667-clean` | `sha256:16e7b4502d69f4d38f731ca8f698720370b3c97744aa99b0b2ca5bca83bf7c81` | `linux/amd64` | 推送、远端清单、云端拉取通过 |

| 产物 | SHA-256 | 结果 |
| --- | --- | --- |
| server fat JAR | `81e96ca3fad3178c321b265a1c7d7111fa19fb27856e0e970a1d14ad6c0f0abc` | 与容器内 `/app/app.jar` 一致 |
| client fat JAR | `702b6fd00ee5f1187127ca88dac7a21363b5fbb68ac62d64897e7d1e6d203768` | 与容器内 `/app/app.jar` 一致 |

## 结果

| 维度 | 结果 | 证据 |
| --- | --- | --- |
| SSH 公钥登录 | 通过，云端和 NAS 均可仅用公钥登录 | `../runs/20261121-235500/raw/validation/local-ssh-key.txt` |
| 源码构建 | 有条件通过，使用最新源码但跳过失败测试并生成探索性标签 | `../runs/20261121-235500/raw/build/` |
| ACR 发布 | 通过，三套镜像推送成功 | `../runs/20261121-235500/raw/build/` |
| 远端 manifest | 通过，三套镜像均为 `linux/amd64` 且 RepoDigest 可读取 | `../runs/20261121-235500/raw/registry/` |
| 云服务器拉取 | 通过，摘要与 ACR 一致 | `../runs/20261121-235500/raw/validation/cloud-pull-verify-recheck.txt` |
| JAR 一致性 | 通过，本地与容器内 SHA-256 一致 | `../runs/20261121-235500/raw/validation/local-fat-jar-sha256-recheck.txt` |
| ACR 凭据清理 | 通过，`ACR_AUTH_ENTRY=no` | `../runs/20261121-235500/raw/validation/acr-logout.txt` |
| NAS Docker 权限 | 通过，普通用户通过 `/usr/local/bin/docker` 返回 `DOCKER_ACCESS=yes` | `../runs/20261121-235500/raw/validation/nas-docker-access-fixed.txt`、`nas-docker-permission-live-recheck.txt` |
| 单元测试 | 失败，4 个测试中 3 个错误 | `../runs/20261121-235500/raw/build/maven-clean-test.log` |
| Docker 实际部署 | 未执行，正式镜像被单元测试阻断 | 运行清单 |
| 功能正确性 | 未执行 | 无 |
| 可用性与可靠性 | 未执行 | 无 |
| 高并发与高负载 | 未执行 | 无 |

## NAS Docker 权限修复

修复前 `/var/run/docker.sock` 为 `root:root 660`，`chenkeyudev` 不在 Docker 管理组，`docker info` 因权限不足失败。修复采用专用 `docker` 组，不使用 `666`：

1. 创建 `docker` 组并加入 `chenkeyudev`。
2. 备份 `/var/packages/ContainerManager/etc/dockerd.json`。
3. 在守护进程配置增加 `"group":"docker"`，重启 `pkg-ContainerManager-dockerd.service`。
4. 将 Socket 修正为 `root:docker 660`。
5. 使用新的普通 SSH 会话，通过 `/usr/local/bin/docker` 验证 Docker Server/Client `24.0.2` 和 `DOCKER_ACCESS=yes`。当前会话 PATH 不含 `/usr/local/bin`，裸命令 `docker` 不可用，不属于权限故障。

`docker` 组具有主机高权限，后续必须限制成员并审计账号。临时修复脚本已从 NAS 删除，可复用脚本已归档在 `docs/tests/scripts/common/fix-nas-docker-access.sh`。

## 缺陷与阻断项

| 编号 | 严重级别 | 描述 | 影响 | 状态 |
| --- | --- | --- | --- | --- |
| B-01 | 阻断 | `mvn -B -ntp clean test` 失败，`UserServiceSecurityTest` 3 个错误，根因 `BadPaddingException: Padding error in decryption` | 不能生成或部署正式验收镜像 | 未修复 |
| B-02 | 高 | Web 实际监听 `21092`，但镜像 Dockerfile 与根目录 Compose 仍出现 `21093` | 直接使用根目录 Compose 可能导致 Web 端口不匹配 | 未修复 |
| B-03 | 中 | 本轮只完成构建、发布、权限与产物核验 | 无法评价系统功能、容量、可用性和可靠性 | 待部署后执行 |

## 关键数据

高并发、高负载、数据完整性和长时间稳定性均未执行，因此本轮没有成功率、RPS、分位延迟、哈希错误数或恢复时间数据，不能给出容量结论。计划压测请求从当前 Windows 管理机发出，脚本、数据和报告统一归档到 `docs/tests/`。

## 限制与未覆盖项

- 探索性镜像使用 `-DskipTests` 构建，不得作为正式验收对象。
- 未在云服务器实际启动 server/web，未在 NAS 实际启动 client/target。
- 未验证客户端 Token、TLS 在线状态、端口转发、断线重连、规则持久化和流量统计。
- 未执行 `50/100/250/500` 并发梯度、大文件、混合负载和 SOAK 测试。
- 未确认云服务安全组、管理机公网出口和 NAS 出口地址的最终策略。

## 归档校验

- 运行清单：[manifest.md](../runs/20261121-235500/manifest.md)
- 原始证据：`docs/tests/runs/20261121-235500/raw/`
- 派生结果：`docs/tests/runs/20261121-235500/processed/`
- 哈希清单：`docs/tests/runs/20261121-235500/SHA256SUMS.txt`
- 本轮报告已登记到 [报告索引](index.md)。

## 最终判定说明

当前可以确认构建、发布、远端拉取、产物一致性和 NAS Docker 权限修复有效；但单元测试失败且未进行实际部署和端到端负载验证，因此只能给出“有条件通过”，不能判定正式验收通过。
