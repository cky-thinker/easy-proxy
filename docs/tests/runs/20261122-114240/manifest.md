# 测试运行清单

## 基本信息

| 字段 | 值 |
| --- | --- |
| 运行编号 | `20261122-114240` |
| 测试范围 | T-38 ACR 现有镜像推送、远端清单核验与凭据注销 |
| 执行人 | Codex 自动化执行 |
| 开始时间 | `2026-11-22 11:42:42 +08:00` |
| 结束时间 | `2026-11-22 11:50:14 +08:00` |
| Git 分支/提交 | 发布工具 `develop` / `f17b50e134a0550a0cd8bedf06f84f8c443934a8`；镜像产品基线 `689e00944533076c65f6e49688cc682a26e189bb` |
| 运行状态 | `通过（T-38）` |
| 结论 | 三套固定标签镜像推送成功；远端 RepoDigest 与本地镜像 ID 一致；三套平台均为 `linux/amd64`；ACR 凭据已注销且复核为 `ACR_AUTH_ENTRY=no` |

## 环境

| 主机 | 角色 | 规格 | 系统/运行时 | 公网出口或地址 |
| --- | --- | --- | --- | --- |
| 当前管理机 | ACR 发布协调与登录交互 | 未在本轮单独采集 | Windows / PowerShell | 当前主机 |
| WSL Ubuntu-22.04 | Docker 镜像预检、推送与清单查询 | WSL 2 / x86_64 | Ubuntu `22.04.5`、Docker `29.10.0`、Buildx `v0.39.1` | 本机 |
| 云服务器 | T-39 待执行 | 2 vCPU / 1964 MiB | Ubuntu `22.04.5` | `68.64.181.242:1514` |
| 群晖 NAS | T-39 待执行 | 6 CPU / 15972 MiB | DSM Container Manager Docker `24.0.2` | `192.168.1.9:10080` |

## 镜像

| 服务 | 完整镜像引用 | RepoDigest | 平台 | 验证状态 |
| --- | --- | --- | --- | --- |
| server | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:test-20261122-022005-689e009` | `sha256:37755831105cdb87e6044e2e1f016cf2faa6a811b8c97eb1f68b008fa8cbfa2a` | `linux/amd64` | 本地预检、推送、远端清单通过 |
| client | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:test-20261122-022005-689e009` | `sha256:11f252500e74781489dc82b6b558639718e11c2170e5a206c5137a365c5e7009` | `linux/amd64` | 本地预检、推送、远端清单通过 |
| web | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:test-20261122-022005-689e009` | `sha256:471123ace2c06375f83122a9cea7df29ba2222baa956a10a91ed4b579d5eb1ae` | `linux/amd64` | 本地预检、推送、远端清单通过 |

## 测试参数

| 参数 | 值 |
| --- | --- |
| 固定标签 | `test-20261122-022005-689e009` |
| 操作方式 | 仅推送 WSL 中已归档的现有镜像，不重新构建 |
| 本地镜像预检 | 三套镜像 ID、`linux`、`amd64` 必须与 T-37 基线一致 |
| 凭据传输 | 密码仅经 `docker login --password-stdin` 传入，不写入日志或文件 |
| 安全停止条件 | 任一预检、登录、推送、远端清单或注销复核失败即停止 |

## 结果摘要

| 指标 | 结果 | 阈值 | 判定 |
| --- | ---: | ---: | --- |
| 本地镜像预检 | 3/3 通过 | 3/3 | 通过 |
| ACR 登录 | 成功，凭据未落盘 | 成功 | 通过 |
| ACR 推送 | 3/3 成功 | 3/3 | 通过 |
| 远端 RepoDigest | 3/3 可读取且与本地镜像 ID 一致 | 3/3 一致 | 通过 |
| 远端平台 | 3/3 为 `linux/amd64` | 3/3 | 通过 |
| ACR 注销复核 | `ACR_AUTH_ENTRY=no` | `ACR_AUTH_ENTRY=no` | 通过 |
| 云服务器拉取与 JAR 一致性 | 未执行 | T-39 通过 | 待 T-39 |

## 证据索引

| 测试编号 | 证据 | 相对路径 | 完整性 |
| --- | --- | --- | --- |
| T-38 | 推送元数据 | `raw/validation/push-metadata.txt` | 完整 |
| T-38 | server/client/web 本地镜像预检 | `raw/validation/preflight-image-server.log`、`raw/validation/preflight-image-client.log`、`raw/validation/preflight-image-web.log` | 完整 |
| T-38 | ACR 登录结果 | `raw/registry/acr-login.txt` | 完整，无凭据 |
| T-38 | server/client/web 推送日志 | `raw/registry/push-existing-server.log`、`raw/registry/push-existing-client.log`、`raw/registry/push-existing-web.log` | 完整 |
| T-38 | 远端镜像清单原始 JSON | `raw/registry/imagetools-server.json`、`raw/registry/imagetools-client.json`、`raw/registry/imagetools-web.json` | 完整 |
| T-38 | 远端镜像清单摘要 | `processed/validation/acr-image-summary.md` | 完整 |
| T-38 | ACR 注销 | `raw/registry/acr-logout.txt`、`raw/registry/acr-logout-verify.log`、`raw/validation/acr-logout.txt` | 完整 |

## 偏差与限制

- 推送脚本未保留其预期的验证控制台日志；后续独立执行 `verify-acr-images.ps1`，完整生成远端清单与摘要，满足 T-38 的远端事实核验要求。
- 本运行只验证 ACR 发布与注销；云服务器拉取、容器内 JAR 一致性和部署仍由 T-39 验证。
- `DEF-T38-RUNINIT-001` 已由提交 `f17b50e` 修复，并由本运行回归关闭。

## 清理与脱敏

- [x] 未记录 ACR 用户名、密码或 Docker 凭据
- [x] 已注销 ACR 凭据并确认 `ACR_AUTH_ENTRY=no`
- [x] 临时凭据变量已清除
- [x] `SHA256SUMS.txt` 已生成并复核
- [x] 报告已登记到 `reports/index.md`