# 测试运行清单

## 基本信息

| 字段 | 值 |
| --- | --- |
| 运行编号 | `20261122-003612` |
| 测试范围 | T-37 正式候选构建；T-38/T-39 待 ACR 交互凭据 |
| 执行人 | 当前 Windows 管理机自动化执行 |
| 开始时间 | `2026-11-22 00:36:12 +08:00` |
| 结束时间 | T-37 结束于 `2026-11-22 00:38:32 +08:00`；发布验证待续 |
| Git 分支/提交 | `develop` / `ec2342d14a4ad71407013e4c913118f420b9223f` |
| 运行状态 | 进行中：T-37 通过，T-38/T-39 未执行 |
| 结论 | 候选构建通过；不得在 ACR 发布、云端/NAS 部署和下游测试完成前判定验收通过 |

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
| server | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:test-20261122-003613-ec2342d` | 待 T-38 推送后读取 | `linux/amd64` | 本地构建通过 |
| client | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:test-20261122-003613-ec2342d` | 待 T-38 推送后读取 | `linux/amd64` | 本地构建通过 |
| web | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:test-20261122-003613-ec2342d` | 待 T-38 推送后读取 | `linux/amd64` | 本地构建通过 |

本地镜像 ID、大小与端口见 `processed/validation/image-facts.md`。本地镜像 ID 不作为 RepoDigest。

## 产物一致性基线

| 产物 | SHA-256 | 大小（字节） | 状态 |
| --- | --- | ---: | --- |
| server fat JAR | `2283dddfa570524edff8ba5b93b38d0bbe8b51968601de75e9a78a49191a8aa0` | `22609517` | 待与容器内 `/app/app.jar` 核对 |
| client fat JAR | `d708f8e79e11cd2fe845e1a68c2c308764309eb1a40c182694fdd282af0c456e` | `5668967` | 待与容器内 `/app/app.jar` 核对 |

## 测试参数

| 参数 | 值 |
| --- | --- |
| 单元测试命令 | `mvn -B -ntp clean test` |
| 单元测试结果 | server 4 个、client 1 个，共 5 个；0 失败、0 错误 |
| 镜像构建 | WSL Buildx，`linux/amd64`，`-SkipPush` |
| 固定标签 | `test-20261122-003613-ec2342d` |
| 平台 | `linux/amd64` |
| 允许测试失败 | `false` |
| 安全停止条件 | 任一单测、打包、镜像构建、推送、清单或凭据清理失败即停止 |

## 结果摘要

| 指标 | 结果 | 阈值 | 判定 |
| --- | ---: | ---: | --- |
| Maven 单元测试 | 5 个测试，0 失败/错误 | 全部通过 | 通过 |
| Maven 打包 | `BUILD SUCCESS` | 成功 | 通过 |
| server 镜像本地构建 | 成功 | 成功 | 通过 |
| client 镜像本地构建 | 成功 | 成功 | 通过 |
| web 镜像本地构建 | 成功 | 成功 | 通过 |
| ACR 推送 | 未执行 | 三套成功 | 阻断：等待交互凭据 |
| ACR 注销状态 | `ACR_AUTH_ENTRY=no` | 必须为 `no` | 通过 |
| 云端拉取与 JAR 一致性 | 未执行 | 通过 | 待 T-39 |

## 证据索引

| 测试编号 | 证据 | 相对路径 | 完整性 |
| --- | --- | --- | --- |
| T-37 | 构建元数据 | `raw/build/build-metadata.txt` | 完整 |
| T-37 | Maven 单元测试 | `raw/build/maven-clean-test.log` | 完整 |
| T-37 | Maven 打包 | `raw/build/maven-package-skip-tests.log` | 完整 |
| T-37 | server/client/web 本地构建 | `raw/build/build-load-server.log` 等 | 完整 |
| T-37 | 本地镜像事实 | `processed/validation/image-facts.md` | 完整 |
| T-37 | 本地 fat JAR 哈希 | `raw/validation/local-fat-jar-sha256.txt` | 完整 |
| T-38 | ACR 注销复核 | `raw/validation/acr-logout.txt` | 完整 |
| T-39 | 本地镜像 inspect | `raw/validation/image-inspect-server.json` 等 | 完整 |

## 偏差与限制

- 运行编号与镜像标签时间戳相差 1 秒，原因是脚本先生成 RunId、再生成 Tag；原始元数据保持不变，不重命名证据。
- 三镜像目前只在 WSL 本地存在，尚未推送 ACR，因此无 RepoDigest。
- 当前环境没有 ACR 凭据环境变量，必须通过交互式安全输入完成登录；不得把凭据写入文件或命令历史。

## 清理与脱敏

- [x] 本地构建不修改远端环境
- [x] 未登录 ACR
- [x] 敏感字段已复核
- [x] `SHA256SUMS.txt` 已生成
- [x] 报告已登记到 `docs/tests/reports/index.md`
