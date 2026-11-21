# 测试运行清单

## 基本信息

| 字段 | 值 |
| --- | --- |
| 运行编号 | `20261122-005952` |
| 测试范围 | T-37 的 DEF-003 修复后本地候选构建；测试资产静态校验 |
| 执行人 | Codex 自动化执行 |
| 开始时间 | `2026-11-22 00:59:52 +08:00` |
| 结束时间 | `2026-11-22 01:10:07 +08:00` |
| Git 分支/提交 | `develop` / `2fe78609d5e9d0405bf9ca27d3d425f56afc9118` |
| 运行状态 | `SUPERSEDED` |
| 结论 | 三套本地镜像构建成功；静态校验因缺少本轮清单/报告及 ACR 注销证据失败；未执行 T-38/T-39，由后续正式轮次取代 |

## 环境

| 主机 | 角色 | 规格 | 系统/运行时 | 公网出口或地址 |
| --- | --- | --- | --- | --- |
| 当前管理机 | 构建协调、测试源与 SSH 客户端 | Windows 11 `10.0.26200` | PowerShell、Maven `3.6.3`、Java `25.0.3` | 当前主机 |
| WSL Ubuntu-22.04 | Docker 镜像构建 | WSL `2` | Docker `29.10.0`、Buildx `v0.39.1`、Compose `v5.8.0` | 本机 |
| 云服务器 | proxy-server | 2 vCPU / 1964 MiB | Ubuntu `22.04.5` | `68.64.181.242:1514` |
| 群晖 NAS | proxy-client | 6 CPU / 15972 MiB | DSM Container Manager Docker `24.0.2` | `192.168.1.9:10080` |

## 镜像

本轮未向 ACR 推送，因此没有 RepoDigest；本地 Buildx 产物仅作为修复后构建链路证据。

| 服务 | 完整镜像引用 | RepoDigest | 平台 | 验证状态 |
| --- | --- | --- | --- | --- |
| server | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:test-20261122-005952-2fe7860` | 未推送 | `linux/amd64` | 本地构建成功 |
| client | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:test-20261122-005952-2fe7860` | 未推送 | `linux/amd64` | 本地构建成功 |
| web | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:test-20261122-005952-2fe7860` | 未推送 | `linux/amd64` | 本地构建成功 |

## 测试参数

| 参数 | 值 |
| --- | --- |
| 单元测试命令 | `mvn -B -ntp clean test` |
| 镜像构建 | WSL Buildx，`linux/amd64`，`-SkipPush` |
| 固定标签 | `test-20261122-005952-2fe7860` |
| 允许测试失败 | `false` |
| 安全停止条件 | 任一单测、打包、镜像构建、静态校验或凭据清理失败即停止 |

## 结果摘要

| 指标 | 结果 | 阈值 | 判定 |
| --- | ---: | ---: | --- |
| Maven 单元测试 | `BUILD SUCCESS` | 全部通过 | 通过 |
| Maven 打包 | `BUILD SUCCESS` | 成功 | 通过 |
| server/client/web 镜像构建 | 三套成功 | 三套成功 | 通过 |
| 测试资产静态校验 | 失败 2 项 | 全部通过 | 失败 |
| ACR 推送与注销 | 未执行 | 推送成功且 `ACR_AUTH_ENTRY=no` | 未执行 |
| RepoDigest、云端拉取与 JAR 一致性 | 未执行 | 全部通过 | 未执行 |

## 证据索引

| 测试编号 | 证据 | 相对路径 | 完整性 |
| --- | --- | --- | --- |
| 前置 | 构建元数据 | `raw/build/build-metadata.txt` | 完整 |
| 前置 | 本地镜像事实 | `raw/build/local-images.txt` | 完整 |
| 前置 | fat JAR 哈希 | `raw/build/fat-jars.txt` | 完整 |
| T-37 | Maven 单元测试 | `raw/build/maven-clean-test.log` | 完整 |
| T-37 | Maven 打包 | `raw/build/maven-package-skip-tests.log` | 完整 |
| T-37 | server/client/web 构建日志 | `raw/build/build-load-*.log` | 完整 |
| 校验 | 静态校验失败输出 | `raw/validation/test-assets-validation.txt` | 完整 |
| 校验 | 本地镜像预检 | `raw/validation/preflight-image-*.log` | 完整 |

## 偏差与限制

- 原静态校验器判定失败：缺少本轮 `manifest.md`、报告和有效的 ACR 注销证据。该失败证据保持原样，本清单与对应报告于后续归档闭环时补齐，不改变原判定。
- 本轮只完成本地构建，没有 ACR RepoDigest，也没有云服务器拉取和部署证据。
- 本运行由后续同一基线的正式构建、发布和下游测试轮次取代。

## 清理与脱敏

- [x] 未记录 ACR 用户名或密码
- [x] 未记录私钥、客户端 Token、Cookie 或 Authorization 头
- [x] `SHA256SUMS.txt` 已生成并复核
- [x] 报告已登记到 `reports/index.md`