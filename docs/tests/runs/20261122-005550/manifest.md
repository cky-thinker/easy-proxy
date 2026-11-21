# 测试运行清单

## 基本信息

| 字段 | 值 |
| --- | --- |
| 运行编号 | `20261122-005550` |
| 测试范围 | T-37 正式候选构建；T-38 ACR 发布；T-39 远端清单与产物一致性 |
| 执行人 | 当前 Windows 管理机自动化执行 |
| 开始时间 | `2026-11-22 00:55:50 +08:00` |
| 结束时间 | `2026-11-22 00:59:33 +08:00` |
| Git 分支/提交 | `develop` / `ddd8a6d1e994645362ac82048b838d81fe148b78` |
| 运行状态 | `SUPERSEDED` |
| 结论 | server/client 本地构建成功，web 因构建上下文过大被取消；由 `DEF-003` 修复后的 `20261122-005952` 取代 |

## 环境

| 主机 | 角色 | 规格 | 系统/运行时 | 地址 |
| --- | --- | --- | --- | --- |
| 当前管理机 | 构建协调、测试源与 SSH 客户端 | Windows 11 `10.0.26200` | PowerShell、Maven `3.6.3`、Java `25.0.3` | 当前主机 |
| WSL Ubuntu-22.04 | Docker 镜像构建 | WSL `2` | Docker `29.10.0`、Buildx `v0.39.1`、Compose `v5.8.0` | 本机 |
| 云服务器 | proxy-server | 2 vCPU / 1964 MiB | Ubuntu `22.04.5` | `68.64.181.242:1514` |
| 群晖 NAS | proxy-client | 6 CPU / 15972 MiB | DSM Container Manager Docker `24.0.2` | `192.168.1.9:10080` |

## 镜像

| 服务 | 完整镜像引用 | RepoDigest | 平台 | 验证状态 |
| --- | --- | --- | --- | --- |
| server | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:test-20261122-005550-ddd8a6d` | 待推送后读取 | `linux/amd64` | 本地构建待执行 |
| client | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:test-20261122-005550-ddd8a6d` | 待推送后读取 | `linux/amd64` | 本地构建待执行 |
| web | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:test-20261122-005550-ddd8a6d` | 待推送后读取 | `linux/amd64` | 本地构建待执行 |

## 测试参数

| 参数 | 值 |
| --- | --- |
| 单元测试命令 | `mvn -B -ntp clean test` |
| 镜像构建 | WSL Buildx，`linux/amd64`，`--provenance=false`，`--sbom=false` |
| 固定标签 | `test-20261122-005550-ddd8a6d` |
| 允许测试失败 | `false` |
| 安全停止条件 | 任一单测、打包、镜像构建、推送、清单或凭据清理失败即停止 |

## 结果摘要

| 指标 | 结果 | 阈值 | 判定 |
| --- | ---: | ---: | --- |
| Maven 单元测试 | `BUILD SUCCESS` | 全部通过 | 通过 |
| Maven 打包 | `BUILD SUCCESS` | 成功 | 通过 |
| server/client/web 镜像构建 | server/client 成功，web 取消 | 三套成功 | 失败 |
| ACR 推送 | 待执行 | 三套成功 | 待验证 |
| ACR 注销状态 | `ACR_AUTH_ENTRY=no` | 必须为 `no` | 通过 |
| 云端拉取与 JAR 一致性 | 待执行 | 通过 | 待验证 |

## 证据索引

| 测试编号 | 证据 | 相对路径 | 完整性 |
| --- | --- | --- | --- |
| 预检 | ACR 注销复核 | `raw/validation/acr-logout.txt` | 完整 |
| 预检 | 测试资产静态校验 | `raw/validation/test-assets-validation.txt` | 完整 |
| T-37 | Maven 与镜像构建 | `raw/build/` | 待生成 |
| T-38 | ACR 清单 | `raw/registry/` | 待生成 |
| T-39 | 云端拉取与容器内 JAR 校验 | `raw/validation/` | 待生成 |

## 偏差与限制

- 测试资产校验首次执行时，本轮清单和报告骨架尚未创建，属于归档初始化顺序问题；骨架创建后已重新执行校验。

## 清理与脱敏

- [x] 未记录 ACR 用户名或密码
- [x] 未在文档中写入私钥、Token 或 Cookie
- [x] `SHA256SUMS.txt` 已生成并复核
- [x] 报告已登记到 `reports/index.md`
