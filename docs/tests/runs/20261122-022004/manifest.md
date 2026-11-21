# 测试运行清单

## 基本信息

| 字段 | 值 |
| --- | --- |
| 运行编号 | `20261122-022004` |
| 测试范围 | T-37 正式候选构建：Maven 单元测试、fat JAR 打包、三套本地镜像构建与镜像事实采集 |
| 执行人 | Codex 自动化执行 |
| 开始时间 | `2026-11-22 02:20:04 +08:00` |
| 结束时间 | `2026-11-22 02:21:06 +08:00` |
| Git 分支/提交 | `develop` / `689e00944533076c65f6e49688cc682a26e189bb` |
| 运行状态 | `通过（T-37）` |
| 结论 | 13 个单元测试全部通过，Maven 打包成功，server/client/web 三套 `linux/amd64` 本地镜像构建成功，本地镜像事实为 UTF-8 无 BOM、LF |

## 环境

| 主机 | 角色 | 规格 | 系统/运行时 | 公网出口或地址 |
| --- | --- | --- | --- | --- |
| 当前管理机 | Maven 测试、打包、Git 追溯与构建协调 | 未在本轮单独采集 | Windows / PowerShell、Maven `3.6.3`、Java `25.0.3` | 当前主机 |
| WSL Ubuntu-22.04 | Docker 镜像构建 | WSL 2 / x86_64 | Ubuntu `22.04.5 LTS`、Docker `29.10.0`、Buildx `v0.39.1`、Compose `v5.8.0` | 本机 |
| 云服务器 | T-37 未参与，T-39 待使用 | 2 vCPU / 1964 MiB | Ubuntu `22.04.5` | `68.64.181.242:1514` |
| 群晖 NAS | T-37 未参与 | 6 CPU / 15972 MiB | DSM Container Manager Docker `24.0.2` | `192.168.1.9:10080` |

## 镜像

本轮执行 `-SkipPush`，因此暂无远端 RepoDigest。固定标签和本地镜像将作为 T-38 推送基线。

| 服务 | 完整镜像引用 | RepoDigest | 平台 | 本地镜像 ID / 验证状态 |
| --- | --- | --- | --- | --- |
| server | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:test-20261122-022005-689e009` | 待 T-38 推送 | `linux/amd64` | `sha256:37755831105cdb87e6044e2e1f016cf2faa6a811b8c97eb1f68b008fa8cbfa2a`；构建成功 |
| client | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:test-20261122-022005-689e009` | 待 T-38 推送 | `linux/amd64` | `sha256:11f252500e74781489dc82b6b558639718e11c2170e5a206c5137a365c5e7009`；构建成功 |
| web | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:test-20261122-022005-689e009` | 待 T-38 推送 | `linux/amd64` | `sha256:471123ace2c06375f83122a9cea7df29ba2222baa956a10a91ed4b579d5eb1ae`；构建成功 |

## 测试参数

| 参数 | 值 |
| --- | --- |
| 单元测试命令 | `mvn -B -ntp clean test` |
| 打包命令 | `mvn -B -ntp package -DskipTests` |
| 镜像构建 | WSL Buildx，`linux/amd64`，`-SkipPush` |
| 固定标签 | `test-20261122-022005-689e009` |
| 允许测试失败 | `false` |
| 安全停止条件 | 任一单测、打包、镜像构建或证据采集失败即停止 |

## 结果摘要

| 指标 | 结果 | 阈值 | 判定 |
| --- | ---: | ---: | --- |
| Maven 单元测试 | 13 个测试，0 失败，0 错误，0 跳过 | 全部通过 | 通过 |
| Maven 打包 | `BUILD SUCCESS` | 成功 | 通过 |
| server/client/web 镜像构建 | 三套成功 | 三套成功 | 通过 |
| 本地镜像平台 | 三套均为 `linux/amd64` | 三套均为 `linux/amd64` | 通过 |
| 镜像事实编码与换行 | UTF-8 无 BOM、LF | UTF-8 无 BOM、LF | 通过 |
| ACR 推送与注销 | 未执行 | 推送成功且 `ACR_AUTH_ENTRY=no` | 待 T-38 |
| RepoDigest、云端拉取与 JAR 一致性 | 未执行 | 全部通过 | 待 T-39 |

## fat JAR 基线

| 服务 | 大小（字节） | SHA-256 |
| --- | ---: | --- |
| server | 22612993 | `63f727f770acc57fe7c5251810b81baacc82d9c8da623cd191f478fdb53b9161` |
| client | 5668935 | `69f41783c9445fc8fd133149ada7e526755d05428862a425833177f9fd16f5be` |

## 证据索引

| 测试编号 | 证据 | 相对路径 | 完整性 |
| --- | --- | --- | --- |
| 前置 | 构建元数据 | `raw/build/build-metadata.txt` | 完整 |
| T-37 | Maven 单元测试 | `raw/build/maven-clean-test.log` | 完整 |
| T-37 | Maven 打包 | `raw/build/maven-package-skip-tests.log` | 完整 |
| T-37 | fat JAR 大小与 SHA-256 | `raw/build/fat-jars.txt` | 完整 |
| T-37 | server/client/web 构建日志 | `raw/build/build-load-server.log`、`raw/build/build-load-client.log`、`raw/build/build-load-web.log` | 完整 |
| T-37 | 本地镜像事实汇总 | `raw/build/local-images.txt` | 完整 |
| T-37 | 本地镜像原始 JSON | `raw/build/local-image-server.json`、`raw/build/local-image-client.json`、`raw/build/local-image-web.json` | 完整，UTF-8 无 BOM、LF |
| 缺陷回归 | 静态校验全通过 | `docs/tests/runs/20261122-022144/raw/validation/test-assets-validation.txt` | 完整 |

## 偏差与限制

- 本轮只完成 T-37，尚未推送 ACR，因此暂无 RepoDigest、云服务器拉取和容器内 JAR 一致性证据。
- 本地镜像 ID 不能替代 ACR RepoDigest；T-38/T-39 必须保持相同 Git 提交、固定标签和本地镜像 ID。
- WSL 内没有可用的 Java/Maven，Maven 测试与打包在 Windows 管理机完成，镜像构建在 WSL 内完成。
- 本运行已通过 `20261122-022144` 的静态回归校验，关闭 `DEF-T37-EVID-001`。

## 清理与脱敏

- [x] 未记录 ACR 用户名或密码
- [x] 未记录私钥、客户端 Token、Cookie 或 Authorization 头
- [x] `SHA256SUMS.txt` 已生成并复核
- [x] 报告已登记到 `reports/index.md`