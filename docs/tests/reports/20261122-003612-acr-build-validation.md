# 20261122-003612 Easy Proxy 候选构建报告

## 结论

- 总体判定：**有条件通过（候选构建）**。T-37 已通过，T-38/T-39 尚未执行。
- 可发布范围：仅固定候选标签 `test-20261122-003613-ec2342d`；在 ACR 推送和云端拉取核验完成前不得部署为验收对象。
- 主要阻断项：等待一次 ACR 交互式登录；无业务代码缺陷。

## 测试范围

- 验证 Git 提交 `ec2342d` 的单元测试、fat JAR 打包、固化标签和 WSL 三镜像本地构建。
- 验证 `DEF-001` 修复后构建脚本能正确识别原生命令退出码。
- 未执行 ACR 推送、RepoDigest 核验、云端拉取和容器内 JAR 哈希核对。

## 环境与版本

- Git 提交：`ec2342d14a4ad71407013e4c913118f420b9223f`。
- 镜像标签：`test-20261122-003613-ec2342d`。
- 构建环境：Windows Maven 打包，WSL Ubuntu-22.04 Docker/Buildx 构建 `linux/amd64`。
- 云端和 NAS：尚未部署。

## 结果

| 维度 | 结果 | 证据 |
| --- | --- | --- |
| 单元测试 | 通过：5 个测试，0 失败、0 错误 | [Maven 测试日志](../runs/20261122-003612/raw/build/maven-clean-test.log) |
| 打包 | `BUILD SUCCESS` | [Maven 打包日志](../runs/20261122-003612/raw/build/maven-package-skip-tests.log) |
| server 构建 | 本地 Buildx 成功 | [构建日志](../runs/20261122-003612/raw/build/build-load-server.log) |
| client 构建 | 本地 Buildx 成功 | [构建日志](../runs/20261122-003612/raw/build/build-load-client.log) |
| web 构建 | 本地 Buildx 成功 | [构建日志](../runs/20261122-003612/raw/build/build-load-web.log) |
| 本地镜像平台 | 三套均为 `linux/amd64` | [镜像事实](../runs/20261122-003612/processed/validation/image-facts.md) |
| 凭据清理 | 通过：`ACR_AUTH_ENTRY=no` | [注销复核](../runs/20261122-003612/raw/validation/acr-logout.txt) |
| 安全与脱敏 | 未发现凭据明文 | 静态扫描 |

## 产物

| 产物 | SHA-256 | 大小（字节） | 当前状态 |
| --- | --- | ---: | --- |
| server fat JAR | `2283dddfa570524edff8ba5b93b38d0bbe8b51968601de75e9a78a49191a8aa0` | `22609517` | 本地基线，待容器核对 |
| client fat JAR | `d708f8e79e11cd2fe845e1a68c2c308764309eb1a40c182694fdd282af0c456e` | `5668967` | 本地基线，待容器核对 |

## 缺陷

| 编号 | 严重级别 | 描述 | 影响 | 状态 |
| --- | --- | --- | --- | --- |
| `DEF-001` | P2 | 构建脚本将原生命令警告误判为失败 | 前一运行 T-37 提前终止 | 已修复；本运行验证通过 |

## 限制与未覆盖项

- 无 RepoDigest；本地镜像 ID 不能替代远端摘要。
- 未执行 ACR 推送、凭据清理后的完整 T-38 和云端拉取 T-39。
- 未执行任何部署、功能、故障恢复、容量、资源或 SOAK 测试。

## 后续准入

1. 仅推送现有三套 `test-20261122-003613-ec2342d` 镜像，避免生成与本地证据不一致的新标签。
2. 读取三套 ACR RepoDigest，验证 `linux/amd64`，随后立即注销并确认 `ACR_AUTH_ENTRY=no`。
3. 在云服务器拉取三套镜像，比较容器内 `/app/app.jar` 与本地 fat JAR。
4. T-38/T-39 通过后更新本报告为完整构建发布结论，再进入部署和下游测试。

## 归档校验

- 运行清单：[manifest.md](../runs/20261122-003612/manifest.md)
- 原始证据：`docs/tests/runs/20261122-003612/raw/`
- 派生结果：`docs/tests/runs/20261122-003612/processed/`
- 哈希清单：`docs/tests/runs/20261122-003612/SHA256SUMS.txt`
