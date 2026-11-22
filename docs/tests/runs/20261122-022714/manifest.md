# 测试运行清单

## 基本信息

| 字段 | 值 |
| --- | --- |
| 运行编号 | `20261122-022714` |
| 测试范围 | T-38 ACR 现有镜像推送交互式尝试 |
| 执行人 | Codex 自动化执行 |
| 开始时间 | `2026-11-22 02:27:14 +08:00` |
| 结束时间 | `2026-11-22 02:27:15 +08:00`（最后证据时间；此后终端中止） |
| Git 分支/提交 | `develop` / `f17b50e134a0550a0cd8bedf06f84f8c443934a8` |
| 运行状态 | `SUPERSEDED`：交互终端中止，未进入 ACR 认证 |
| 结论 | 三套本地镜像预检通过；终端无法接收凭据输入，未登录、未推送、未修改远端仓库；由 `20261122-114240` 取代 |

## 环境

| 主机 | 角色 | 规格 | 系统/运行时 | 公网出口或地址 |
| --- | --- | --- | --- | --- |
| 当前管理机 | T-38 发布协调 | 未在本轮单独采集 | Windows / PowerShell | 当前主机 |
| WSL Ubuntu-22.04 | Docker 镜像预检与发布 | WSL 2 / x86_64 | Ubuntu `22.04.5`、Docker `29.10.0` | 本机 |
| 云服务器 | T-38 未参与 | 2 vCPU / 1964 MiB | Ubuntu `22.04.5` | `68.64.181.242:1514` |
| 群晖 NAS | T-38 未参与 | 6 CPU / 15972 MiB | DSM Container Manager Docker `24.0.2` | `192.168.1.9:10080` |

## 镜像

| 服务 | 完整镜像引用 | RepoDigest | 平台 | 验证状态 |
| --- | --- | --- | --- | --- |
| server | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:test-20261122-022005-689e009` | 未推送 | `linux/amd64` | 本地预检通过 |
| client | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:test-20261122-022005-689e009` | 未推送 | `linux/amd64` | 本地预检通过 |
| web | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:test-20261122-022005-689e009` | 未推送 | `linux/amd64` | 本地预检通过 |

## 测试参数

| 参数 | 值 |
| --- | --- |
| 固定标签 | `test-20261122-022005-689e009` |
| 操作方式 | 仅推送 WSL 中已归档的现有镜像，不重新构建 |
| 本地镜像预检 | 三套镜像 ID、`linux`、`amd64` 必须与 T-37 基线一致 |
| 安全停止条件 | 未完成三套推送及 `ACR_AUTH_ENTRY=no` 注销复核即不得判定通过 |

## 结果摘要

| 指标 | 结果 | 阈值 | 判定 |
| --- | ---: | ---: | --- |
| 本地镜像预检 | 3/3 通过 | 3/3 | 通过 |
| ACR 登录 | 未尝试 | 登录成功 | 未执行 |
| ACR 推送 | 0/3 | 3/3 | 未执行 |
| 远端 RepoDigest 与平台 | 未验证 | 3/3 | 未执行 |
| ACR 注销复核 | 未执行 | `ACR_AUTH_ENTRY=no` | 未执行 |

## 证据索引

| 测试编号 | 证据 | 相对路径 | 完整性 |
| --- | --- | --- | --- |
| T-38 | 推送元数据 | `raw/validation/push-metadata.txt` | 完整 |
| T-38 | server 本地镜像预检 | `raw/validation/preflight-image-server.log` | 完整 |
| T-38 | client 本地镜像预检 | `raw/validation/preflight-image-client.log` | 完整 |
| T-38 | web 本地镜像预检 | `raw/validation/preflight-image-web.log` | 完整 |
| T-38 | 交互终端中止说明 | `raw/validation/t38-interactive-terminal-abort.txt` | 完整，人工观察记录 |

## 偏差与限制

- 交互终端无法接收 ACR 凭据输入，本轮在认证前中止。
- 无 ACR 登录、推送或注销证据，因此不得用本轮判定 T-38 通过。
- 原始预检文件保留不覆盖；成功重测见 `docs/tests/runs/20261122-114240/`。

## 清理与脱敏

- [x] 未读取 ACR 凭据
- [x] 未登录、未推送、未修改远端仓库
- [x] 未记录任何凭据或令牌
- [x] `SHA256SUMS.txt` 已生成并复核
- [x] 报告已登记到 `reports/index.md`