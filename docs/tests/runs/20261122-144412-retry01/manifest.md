# 测试运行清单

## 基本信息

| 字段 | 值 |
| --- | --- |
| 运行编号 | `20261122-144412-retry01` |
| 测试范围 | T-39 云服务器镜像拉取、RepoDigest/平台核验、容器内 JAR 一致性与 ACR 注销 |
| 执行人 | Codex 自动化执行 |
| 开始时间 | 2026-11-22 14:49:41 +08:00 |
| 结束时间 | 2026-11-22 14:49:58 +08:00 |
| Git 分支/提交 | `develop` / `1a17c12cc8a6cada0d23279e859b7e920d8182ce` |
| 运行状态 | 通过（T-39） |
| 结论 | 三套镜像云端拉取、RepoDigest、linux/amd64 平台和容器内 JAR 一致性均通过；ACR 凭据已注销。 |

## 环境

| 主机 | 角色 | 规格 | 系统/运行时 | 公网出口或地址 |
| --- | --- | --- | --- | --- |
| 当前管理机 | SSH 客户端与证据归档 | 未在本轮单独采集 | Windows / PowerShell | 当前主机 |
| 云服务器 | proxy-server 镜像拉取与核验 | 2 vCPU / 1964 MiB（方案基线） | Ubuntu / Docker | `68.64.181.242:1514` |

## 镜像

| 服务 | 完整镜像引用 | RepoDigest | 平台 | 验证状态 |
| --- | --- | --- | --- | --- |
| server | registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:test-20261122-144412-aba070f | sha256:6c5ae6e1d3ef49dc5def2c7c54568e5af7c70e4eb12b6ba5da4a58e327019d9e | linux/amd64 | 已拉取并核验 |
| client | registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:test-20261122-144412-aba070f | sha256:7e509b6de3021788f2b66abf6e6f05b6af12b2811f451cce9280099da8a89354 | linux/amd64 | 已拉取并核验 |
| web | registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:test-20261122-144412-aba070f | sha256:471123ace2c06375f83122a9cea7df29ba2222baa956a10a91ed4b579d5eb1ae | linux/amd64 | 已拉取并核验 |

## 测试参数

| 参数 | 值 |
| --- | --- |
| 固定标签 | test-20261122-144412-aba070f |
| 镜像仓库 | registry.cn-hangzhou.aliyuncs.com/ckyspace |
| 拉取方式 | server/client/web 分别执行独立 SSH 与 docker pull |
| JAR 比较 | 本地 fat JAR 与容器内 /app/app.jar SHA-256 |
| 凭据处理 | 使用云端既有 ACR 登录态；结束后注销，不读取或归档凭据内容 |

## 结果摘要

| 指标 | 结果 | 判定 |
| --- | --- | --- |
| 镜像拉取 | 3/3 | 通过 |
| RepoDigest | 3/3 | 通过 |
| 平台 | 3/3 | 通过 |
| JAR 一致性 | 2/2 | 通过 |
| ACR 注销 | ACR_AUTH_ENTRY=no | 通过 |

## 证据索引

| 测试编号 | 证据 | 相对路径 | 完整性 |
| --- | --- | --- | --- |
| T-39 | 云端主机与 Docker 环境 | raw/validation/cloud-host.txt | 完整 |
| T-39 | 拉取前 ACR 登录态 | raw/validation/acr-auth-before.txt | 完整，无凭据 |
| T-39 | 三套镜像拉取日志 | raw/validation/pull-server.txt、pull-client.txt、pull-web.txt | 完整 |
| T-39 | 三套镜像 inspect | raw/validation/inspect-server.txt、inspect-client.txt、inspect-web.txt | 完整 |
| T-39 | 容器内 JAR 哈希 | raw/validation/jar-in-container-server.txt、jar-in-container-client.txt | 完整 |
| T-39 | 检查明细与摘要 | processed/validation/cloud-image-checks.md、cloud-image-summary.md | 完整 |
| T-39 | ACR 注销复核 | raw/validation/acr-logout.txt | 完整，无凭据 |

## 偏差与限制

- 本运行只验证镜像发布产物和云端拉取一致性，不代表已完成部署、端到端或容量测试。

## 清理与脱敏

- [x] 未记录 ACR 用户名、密码或 Docker 凭据
- [x] 已注销 ACR 凭据并确认 ACR_AUTH_ENTRY=no
- [x] 临时容器由 docker run --rm 自动清理
- [x] SHA256SUMS.txt 已生成并复核
- [x] 报告已登记到 reports/index.md
