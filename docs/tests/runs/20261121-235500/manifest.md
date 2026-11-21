# 测试运行清单

## 基本信息

| 字段 | 值 |
| --- | --- |
| 运行编号 | `20261121-235500` |
| 测试范围 | SSH 公钥、NAS Docker 权限、WSL 最新源码构建、ACR 发布、远端清单、云服务器拉取、JAR 一致性 |
| 执行方式 | 当前 Windows 管理机自动化执行，构建在 WSL 中完成 |
| 开始时间 | `2026-11-21 23:35:00 +08:00` |
| 结束时间 | `2026-11-22 00:04:00 +08:00` |
| Git 分支/提交 | `develop` / `4e68667e40e1adb1eba96c48f9d567705525d3c2` |
| 运行状态 | 部分完成：构建与发布链路已验证；单元测试失败，正式部署阻断 |
| 结论 | 有条件通过：SSH、ACR 发布、远端拉取、JAR 一致性和 NAS Docker 权限有效；当前镜像仅用于探索，不构成系统验收通过 |

## 环境

| 主机 | 角色 | 规格 | 系统/运行时 | 地址 |
| --- | --- | --- | --- | --- |
| 当前管理机 | SSH 客户端与压测源 | Windows 11 `10.0.26200` | OpenSSH、PowerShell | 当前主机 |
| WSL Ubuntu-22.04 | 镜像构建与推送 | WSL `2` | Ubuntu `22.04.5`、Docker `29.10.0`、Buildx `v0.39.1`、Compose `v5.8.0` | 本机 |
| 云服务器 | `proxy-server` | 2 vCPU / 1964 MiB | Ubuntu `22.04.5` | `68.64.181.242:1514` |
| 群晖 NAS | `proxy-client` | 6 CPU / 15972 MiB / x86_64 | DSM Container Manager Docker `24.0.2` | `192.168.1.9:10080` |

## 镜像

| 服务 | 完整镜像引用 | RepoDigest | 平台 | 压缩大小（字节） | 验证状态 |
| --- | --- | --- | --- | ---: | --- |
| server | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:exploratory-20261121-233608-4e68667-clean` | `sha256:9c18e11d395f3c757e1622320515f3105bf206777612d3946b73410adc492302` | `linux/amd64` | 177363105 | 远端清单与云端拉取通过 |
| client | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:exploratory-20261121-233608-4e68667-clean` | `sha256:d36112d730c53d708a61d83c9a3b3dd4af16eb7d81193177468c631335bc8cd3` | `linux/amd64` | 162388011 | 远端清单与云端拉取通过 |
| web | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:exploratory-20261121-233608-4e68667-clean` | `sha256:16e7b4502d69f4d38f731ca8f698720370b3c97744aa99b0b2ca5bca83bf7c81` | `linux/amd64` | 27009800 | 远端清单与云端拉取通过 |

镜像标签含 `exploratory-`，原因是 Maven 单元测试未通过。该标签不得作为正式验收镜像。

## 产物一致性

| 产物 | 本地 SHA-256 | 验证状态 |
| --- | --- | --- |
| server fat JAR | `81e96ca3fad3178c321b265a1c7d7111fa19fb27856e0e970a1d14ad6c0f0abc` | 与容器内 `/app/app.jar` 一致 |
| client fat JAR | `702b6fd00ee5f1187127ca88dac7a21363b5fbb68ac62d64897e7d1e6d203768` | 与容器内 `/app/app.jar` 一致 |

## 关键验证结果

| 项目 | 结果 | 证据 |
| --- | --- | --- |
| 云服务器公钥登录 | 通过 | `raw/validation/local-ssh-key.txt`、`raw/validation/cloud-host.txt` |
| NAS 公钥登录 | 通过 | `raw/validation/local-ssh-key.txt`、`raw/validation/nas-host.txt` |
| NAS Docker 修复前权限 | 失败，已定位 | `raw/validation/nas-docker-permission-recheck.txt` |
| NAS Docker 修复后权限 | 通过，`/usr/local/bin/docker` 返回 `DOCKER_ACCESS=yes`；PATH 需显式补充 | `raw/validation/nas-docker-access-fixed.txt`、`raw/validation/nas-docker-permission-live-recheck.txt` |
| Maven 单元测试 | 失败：4 个测试，3 个错误 | `raw/build/maven-clean-test.log` |
| WSL 三镜像构建与推送 | 通过，使用跳过测试的探索性构建 | `raw/build/build-push-server.log`、`build-push-client.log`、`build-push-web.log` |
| ACR RepoDigest 与平台 | 通过，三套均为 `linux/amd64` | `raw/registry/`、`processed/validation/acr-image-summary.md` |
| 云服务器重新拉取 | 通过，摘要一致 | `raw/validation/cloud-pull-verify-recheck.txt` |
| 本地 JAR 与容器 JAR | 通过，SHA-256 一致 | `raw/validation/local-fat-jar-sha256-recheck.txt` |
| ACR 凭据清理 | 通过，`ACR_AUTH_ENTRY=no` | `raw/validation/acr-logout.txt` |
| Docker 实际部署 | 未执行 | 正式镜像被单元测试阻断 |
| 端到端与高并发测试 | 未执行 | 依赖实际部署与正式镜像 |

## 测试参数

| 参数 | 值 |
| --- | --- |
| 压测工具与版本 | 未执行，待部署后确定并固定版本 |
| 并发档位 | 计划 `50/100/250/500` |
| 持续时间 | 未执行；SOAK 计划目标 60 分钟，最低 30 分钟 |
| 预热/冷却 | 未执行 |
| 健康探测周期 | 未执行 |
| 安全停止条件 | 成功率跌破门槛、错误响应激增、OOM、持续重启或资源超过安全阈值时立即撤压 |

## 结果摘要

| 指标 | 结果 | 阈值 | 判定 |
| --- | ---: | ---: | --- |
| 成功率 | 未执行 | 分档阈值见方案 | 未判定 |
| RPS | 未执行 | 建立容量基线 | 未判定 |
| p50/p95/p99 | 未执行 | 分档阈值见方案 | 未判定 |
| 数据哈希不一致数 | 未执行 | `0` | 未判定 |
| 非预期 5xx | 未执行 | `0` | 未判定 |
| 容器重启/OOM | 未执行 | `0` | 未判定 |
| 健康检查可用性 | 未执行 | SOAK 不低于 `99.9%` | 未判定 |

## 证据索引

| 类别 | 证据 | 相对路径 | 完整性 |
| --- | --- | --- | --- |
| 环境 | 本地、WSL、云端和 NAS 快照 | `raw/validation/` | 已归档 |
| 构建 | Maven 测试与三镜像构建日志 | `raw/build/` | 已归档 |
| 仓库 | ACR 远端 manifest 原始输出 | `raw/registry/` | 已归档 |
| 验证 | 拉取、JAR、权限与凭据清理证据 | `raw/validation/` | 已归档 |
| 派生结果 | ACR 镜像汇总 | `processed/validation/acr-image-summary.md` | 已归档 |
| 报告 | 本轮构建与发布验证报告 | `../../reports/20261121-235500-acr-build-validation.md` | 已登记 |

## 偏差与限制

- 单元测试因 `javax.crypto.BadPaddingException: Padding error in decryption` 失败，镜像只能标记为探索性。
- 镜像构建使用项目最新代码，但不是通过全量测试的正式候选版本。
- NAS Docker 权限已在测试过程中由管理员通道修复；加入 `docker` 组等同高权限，需限制成员并审计。
- 尚未执行容器实际部署、端到端转发、故障恢复、高并发、高负载和 SOAK 测试。
- 当前结论只覆盖构建、发布、权限准备和产物一致性，不代表系统功能或性能验收通过。

## 清理与脱敏

- [x] NAS 临时修复脚本已删除
- [x] ACR 凭据已注销并验证 `ACR_AUTH_ENTRY=no`
- [x] 文档、脚本和证据未保存密码、Token、私钥或 Docker 凭据
- [x] `SHA256SUMS.txt` 已生成并复核
- [x] 报告已登记到 `reports/index.md`
