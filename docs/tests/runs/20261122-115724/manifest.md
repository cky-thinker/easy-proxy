# 测试运行清单

## 基本信息

| 字段 | 值 |
| --- | --- |
| 运行编号 | `20261122-115724` |
| 测试范围 | T-39 云服务器镜像拉取首次尝试 |
| 执行人 | Codex 自动化执行 |
| 开始时间 | 2026-11-22 11:58:20 +08:00 |
| 结束时间 | 2026-11-22 11:58:20 +08:00 |
| Git 分支/提交 | `develop` / `2900f3554f089e8adfee524e0f14d5ce14381f10` |
| 运行状态 | 失败（SUPERSEDED） |
| 结论 | 首次批量拉取尝试失败；web 单条复验成功，定位为执行方式问题。本运行标记 SUPERSEDED。 |

## 环境

| 主机 | 角色 | 规格 | 系统/运行时 | 公网出口或地址 |
| --- | --- | --- | --- | --- |
| 当前管理机 | 压测源与 SSH 客户端 | 待采集 | Windows / PowerShell | 待采集 |
| WSL Ubuntu-22.04 | 镜像构建 | 待采集 | Docker/Buildx/Compose | 本地 |
| 云服务器 | proxy-server | 待采集 | Linux / Docker | `68.64.181.242` |
| 群晖 NAS | proxy-client | 待采集 | DSM / Docker | `192.168.1.9` |

## 镜像

| 服务 | 完整镜像引用 | RepoDigest | 平台 | 验证状态 |
| --- | --- | --- | --- | --- |
| server | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:test-20261122-022005-689e009` | `sha256:37755831105cdb87e6044e2e1f016cf2faa6a811b8c97eb1f68b008fa8cbfa2a` | 待本轮 inspect | 已拉取 |
| client | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:test-20261122-022005-689e009` | `sha256:11f252500e74781489dc82b6b558639718e11c2170e5a206c5137a365c5e7009` | 待本轮 inspect | 已拉取 |
| web | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:test-20261122-022005-689e009` | `sha256:471123ace2c06375f83122a9cea7df29ba2222baa956a10a91ed4b579d5eb1ae` | 待本轮 inspect | 批量失败，单条复验成功 |

## 测试参数

- 固定标签：`test-20261122-022005-689e009`

## 结果摘要

| 指标 | 结果 | 判定 |
| --- | --- | --- |
| server 拉取 | 成功 | 通过 |
| client 拉取 | 成功 | 通过 |
| web 批量拉取 | `invalid reference format` | 失败 |
| JAR 一致性 | 未完成 | 中止 |

## 证据索引

| 测试编号 | 证据 | 相对路径 | 完整性 |
| --- | --- | --- | --- |
| T-39 | 首次拉取输出 | `raw/validation/cloud-pull-first-attempt.txt` | 已保留 |
| T-39 | 尝试摘要 | `processed/validation/first-attempt-summary.md` | 完整 |

## 偏差与限制

本运行不用于 T-39 最终判定；完整 RepoDigest、平台与 JAR 一致性由后续运行验证。

## 清理与脱敏

- [x] 未记录任何凭据
- [x] 未删除失败运行
- [x] `SHA256SUMS.txt` 已生成
- [x] 报告已登记到 `reports/index.md`
