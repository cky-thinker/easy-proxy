# 测试运行清单

## 基本信息

| 字段 | 值 |
| --- | --- |
| 运行编号 | `20261122-021050` |
| 测试范围 | T-37 正式候选构建（Maven 单元测试、打包、三套镜像本地构建及本地镜像事实采集） |
| 执行人 | Codex 自动化执行 |
| 开始时间 | `2026-11-22 02:10:50 +08:00` |
| 结束时间 | `2026-11-22 02:12:44 +08:00` |
| Git 分支/提交 | `develop` / `7c4f79a97fea78de24530d40cbd2f6ff591d7669` |
| 运行状态 | `SUPERSEDED` |
| 结论 | 产品级构建、单元测试和三套镜像均成功；因 `DEF-T37-EVID-001` 证据格式缺陷，本运行不进入最终通过链 |

## 环境

| 主机 | 角色 | 规格 | 系统/运行时 | 公网出口或地址 |
| --- | --- | --- | --- | --- |
| 当前管理机 | 构建协调、Git 追溯、SSH 客户端 | 待后续容量测试采集 | Windows / PowerShell、Maven `3.6.3`、Java `25.0.3` | 当前主机 |
| WSL Ubuntu-22.04 | Docker 镜像构建 | WSL 2 / x86_64 | Ubuntu `22.04.5 LTS`、Docker `29.10.0`、Buildx `v0.39.1`、Compose `v5.8.0` | 本机 |
| 云服务器 | proxy-server | 2 vCPU / 1964 MiB | Ubuntu `22.04.5` | `68.64.181.242:1514` |
| 群晖 NAS | proxy-client | 6 CPU / 15972 MiB | DSM Container Manager Docker `24.0.2` | `192.168.1.9:10080` |

## 镜像

本轮未推送 ACR，因此暂无 RepoDigest。镜像将在同一 Git 提交和固定标签基础上完成 T-38/T-39。

| 服务 | 完整镜像引用 | RepoDigest | 平台 | 验证状态 |
| --- | --- | --- | --- | --- |
| server | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:test-20261122-021051-7c4f79a` | 待推送 | `linux/amd64` | 本地构建成功，镜像 ID `sha256:fde8400ca137a8f84bb2bd77448a8a024d439d7b8ac968e2326d626d9483a096` |
| client | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:test-20261122-021051-7c4f79a` | 待推送 | `linux/amd64` | 本地构建成功，镜像 ID `sha256:0ffdd21943dc50bde7f65f808c7127589d0c0328bdd943610178da363e611641` |
| web | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:test-20261122-021051-7c4f79a` | 待推送 | `linux/amd64` | 本地构建成功，镜像 ID `sha256:471123ace2c06375f83122a9cea7df29ba2222baa956a10a91ed4b579d5eb1ae` |

## 测试参数

| 参数 | 值 |
| --- | --- |
| 单元测试命令 | `mvn -B -ntp clean test` |
| 打包命令 | `mvn -B -ntp package -DskipTests` |
| 镜像构建 | WSL Buildx，`linux/amd64`，`-SkipPush` |
| 固定标签 | `test-20261122-021051-7c4f79a` |
| 允许测试失败 | `false` |
| 安全停止条件 | 任一单测、打包、镜像构建或证据采集失败即停止 |

## 结果摘要

| 指标 | 结果 | 阈值 | 判定 |
| --- | ---: | ---: | --- |
| Maven 单元测试 | 13 个测试，0 失败，0 错误，0 跳过 | 全部通过 | 通过 |
| Maven 打包 | `BUILD SUCCESS` | 成功 | 通过 |
| server/client/web 镜像构建 | 三套成功 | 三套成功 | 通过 |
| 本地镜像平台 | 三套均为 `linux/amd64` | 三套均为 `linux/amd64` | 通过 |
| ACR 推送与注销 | 未执行 | 推送成功且 `ACR_AUTH_ENTRY=no` | 待执行 |
| RepoDigest、云端拉取与 JAR 一致性 | 未执行 | 全部通过 | 待执行 |

## 证据索引

| 测试编号 | 证据 | 相对路径 | 完整性 |
| --- | --- | --- | --- |
| 前置 | 构建元数据 | `raw/build/build-metadata.txt` | 完整 |
| 前置 | WSL 构建主机信息 | `raw/validation/local-wsl-build-host.txt` | 完整 |
| T-37 | Maven 单元测试 | `raw/build/maven-clean-test.log` | 完整 |
| T-37 | Maven 打包 | `raw/build/maven-package-skip-tests.log` | 完整 |
| T-37 | fat JAR 大小与 SHA-256 | `raw/build/fat-jars.txt` | 完整 |
| T-37 | server/client/web 构建日志 | `raw/build/build-load-*.log` | 完整 |
| T-37 | 本地镜像事实 | `raw/build/local-images.txt`、`raw/build/local-image-*.json` | 完整 |

## 偏差与限制

- 本轮仅完成 T-37，未推送 ACR，因此没有远端 RepoDigest、云服务器拉取和容器内 JAR 一致性证据。
- `local-images.txt` 首次采集命令受 WSL 传输警告和 bash 格式管道解析影响；保留原始内容后以 `[RETRY]` 分段追加修正事实，并使用独立 JSON 检查结果交叉核对。
- T-38/T-39 必须继续使用同一 Git 提交和同一固定标签，不能以本地镜像结果代替最终验收。
- 本运行已被新轮次取代，替代原因：[DEF-T37-EVID-001](../20261122-021321/defects/DEF-T37-EVID-001.md)。

## 清理与脱敏

- [x] 未记录 ACR 用户名或密码
- [x] 未记录私钥、客户端 Token、Cookie 或 Authorization 头
- [x] `SHA256SUMS.txt` 已生成并复核
- [x] 报告已登记到 `reports/index.md`
