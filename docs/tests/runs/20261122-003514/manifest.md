# 测试运行清单

## 基本信息

| 字段 | 值 |
| --- | --- |
| 运行编号 | `20261122-003514` |
| 测试范围 | T-37 正式构建前置：Maven 单元测试与三镜像本地构建 |
| 执行人 | 当前 Windows 管理机自动化执行 |
| 开始时间 | `2026-11-22 00:35:14 +08:00` |
| 结束时间 | `2026-11-22 00:36:09 +08:00` |
| Git 分支/提交 | `develop` / `ed30a35ac7492c17a28e395a5f415e26a70318bf` |
| 运行状态 | `TERMINATED_BY_BLOCKER`：构建脚本工具缺陷，未进入 Maven 日志与 Docker 构建 |
| 结论 | `SUPERSEDED`：由 `DEF-001` 修复后的运行 `20261122-003612` 取代 |

## 环境

| 主机 | 角色 | 规格 | 系统/运行时 | 地址 |
| --- | --- | --- | --- | --- |
| 当前管理机 | 构建协调与测试执行 | Windows 11 `10.0.26200` | PowerShell、Maven `3.6.3`、Java `25.0.3` | 当前主机 |
| WSL Ubuntu-22.04 | Docker 镜像构建 | WSL `2` | Docker/Buildx/Compose | 本机 |
| 云服务器 | proxy-server | 2 vCPU / 1964 MiB | Ubuntu `22.04.5` | `68.64.181.242:1514` |
| 群晖 NAS | proxy-client | 6 CPU / 15972 MiB | DSM Container Manager Docker `24.0.2` | `192.168.1.9:10080` |

## 镜像

| 服务 | 完整镜像引用 | RepoDigest | 平台 | 验证状态 |
| --- | --- | --- | --- | --- |
| server | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:test-20261122-003515-ed30a35` | 未生成 | `linux/amd64` | 未构建 |
| client | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:test-20261122-003515-ed30a35` | 未生成 | `linux/amd64` | 未构建 |
| web | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:test-20261122-003515-ed30a35` | 未生成 | `linux/amd64` | 未构建 |

## 测试参数

| 参数 | 值 |
| --- | --- |
| 单元测试命令 | `mvn -B -ntp clean test` |
| 镜像构建 | WSL Buildx，`linux/amd64`，本次参数为 `-SkipPush` |
| 允许测试失败 | `false` |
| 安全停止条件 | 单元测试失败、脚本异常或镜像构建失败时立即停止 |

## 结果摘要

| 指标 | 结果 | 阈值 | 判定 |
| --- | ---: | ---: | --- |
| Maven 单元测试采集 | 未形成日志 | 必须完整归档 | 失败：脚本将警告误判为终止错误 |
| 三镜像本地构建 | 未执行 | 三套均成功 | 未执行 |
| 固定标签生成 | 已确定但未使用 | `test-<时间>-<commit7>` | 不构成有效产物 |
| 凭据清理 | `ACR_AUTH_ENTRY=no` | 必须为 `no` | 通过 |

## 证据索引

| 测试编号 | 证据 | 相对路径 | 完整性 |
| --- | --- | --- | --- |
| T-37 | 构建元数据 | `raw/build/build-metadata.txt` | 完整 |
| T-37 | 缺陷记录 | `defects/DEF-001.md` | 完整 |
| T-38 | ACR 注销复核 | `raw/validation/acr-logout.txt` | 完整 |

## 偏差与限制

- PowerShell 7 将 Maven 写入标准错误流的警告包装为 `NativeCommandError`，构建脚本按终止错误退出；Docker 构建和推送均未开始。
- 因脚本在 Maven 日志管道建立前终止，本轮只保留构建元数据；终端原始错误未重新伪造，按缺失证据处理。
- 本轮没有可部署镜像，不得用于云端或 NAS 部署。

## 清理与脱敏

- [x] 未启动临时容器或修改远端服务
- [x] 未登录或写入 ACR 凭据
- [x] 敏感字段已复核
- [x] `SHA256SUMS.txt` 已生成
- [x] 报告已登记到 `docs/tests/reports/index.md`
