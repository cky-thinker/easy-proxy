# 20261122-005952 Easy Proxy 本地构建与静态校验报告

## 结论

- 总体判定：`SUPERSEDED`。
- 可发布范围：不可发布。本轮仅证明 `DEF-003` 修复后的三套本地 `linux/amd64` 镜像可以构建。
- 主要阻断项：没有 ACR 推送、RepoDigest、云端拉取和 JAR 一致性证据；静态校验按当时规则失败。

## 测试范围

- 验证提交 `2fe7860` 的 Maven 单元测试、打包和 server/client/web 三套本地镜像构建。
- 验证缺失清单和报告会被静态校验正确拦截。

## 环境与版本

- Git 提交：`2fe78609d5e9d0405bf9ca27d3d425f56afc9118`。
- 镜像标签：`test-20261122-005952-2fe7860`。
- 构建环境：Windows Maven，WSL Ubuntu-22.04 Docker/Buildx。
- 平台：`linux/amd64`。

## 结果

| 维度 | 结果 | 证据 |
| --- | --- | --- |
| 单元测试 | `BUILD SUCCESS` | [测试日志](../runs/20261122-005952/raw/build/maven-clean-test.log) |
| 打包 | `BUILD SUCCESS` | [打包日志](../runs/20261122-005952/raw/build/maven-package-skip-tests.log) |
| 三套镜像 | 本地构建成功 | [镜像事实](../runs/20261122-005952/raw/build/local-images.txt) |
| 静态校验 | 失败：缺少清单/报告和 ACR 注销证据 | [原始输出](../runs/20261122-005952/raw/validation/test-assets-validation.txt) |
| T-38/T-39 | 未执行 | 无远端 RepoDigest 或云端拉取证据 |

## 缺陷

| 编号 | 严重级别 | 描述 | 影响 | 状态 |
| --- | --- | --- | --- | --- |
| `DEF-003` | P1 | Web 构建上下文包含无关大目录，Buildx 长时间传输后被取消 | 正式镜像无法稳定构建 | 已修复；本轮三套本地构建验证通过 |
| `DEF-EVID-001` | P2 | 测试运行日志被 `*.log` 规则忽略，且早期轮次存在清单/报告缺口 | 运行证据无法随 Git 追溯和回滚 | 已修复；日志归档规则和运行清单已补齐 |

## 限制与未覆盖项

- 未推送 ACR，无 RepoDigest。
- 未执行云服务器拉取、Docker 部署、端到端、恢复、容量或 SOAK 测试。
- 本地镜像 ID 不能替代远端 RepoDigest。

## 归档校验

- 运行清单：[manifest.md](../runs/20261122-005952/manifest.md)
- 原始证据：`docs/tests/runs/20261122-005952/raw/`
- 派生结果：`docs/tests/runs/20261122-005952/processed/`
- 哈希清单：`docs/tests/runs/20261122-005952/SHA256SUMS.txt`