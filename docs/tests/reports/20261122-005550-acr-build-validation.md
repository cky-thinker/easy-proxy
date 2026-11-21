# 20261122-005550 Easy Proxy 构建与发布验证报告

## 结论

- 总体判定：`SUPERSEDED`
- 可发布范围：不可发布
- 主要阻断项：web 构建上下文过大导致取消；由 `DEF-003` 修复后的 `20261122-005952` 取代

## 测试范围

- T-37：同一 Git 提交的全量单元测试、打包和三套固定标签镜像构建。
- T-38：ACR 交互式发布与凭据清理。
- T-39：远端 RepoDigest、平台、云端拉取和容器内 JAR 一致性。

## 环境与版本

- Git 提交：`ddd8a6d1e994645362ac82048b838d81fe148b78`
- 镜像标签：`test-20261122-005550-ddd8a6d`
- 构建环境：Windows 管理机协调，WSL Ubuntu-22.04 执行 Docker/Buildx。

## 结果

| 维度 | 结果 | 证据 |
| --- | --- | --- |
| 单元测试 | 通过：`BUILD SUCCESS` | [测试日志](../runs/20261122-005550/raw/build/maven-clean-test.log) |
| 镜像产物 | server/client 成功；web 取消 | [web 构建日志](../runs/20261122-005550/raw/build/build-load-web.log) |
| ACR 发布 | 未执行 | 无 |
| 产物一致性 | 未执行 | 无 |
| 安全与脱敏 | 预检通过 | `raw/validation/test-assets-validation.txt` |

## 缺陷

| 编号 | 严重级别 | 描述 | 影响 | 状态 |
| --- | --- | --- | --- | --- |
| `DEF-002` | P1 | 服务端 JWT 使用硬编码密钥且未复核用户状态 | 令牌可伪造、禁用用户仍可访问 | 已修复，提交 `ddd8a6d` |

## 限制与未覆盖项

- 本报告只覆盖构建与发布，不代表 Docker 部署、端到端、恢复或容量测试已通过。

## 归档校验

- 运行清单：[manifest.md](../runs/20261122-005550/manifest.md)
- 原始证据：`docs/tests/runs/20261122-005550/raw/`
- 派生结果：`docs/tests/runs/20261122-005550/processed/`
- 哈希清单：`docs/tests/runs/20261122-005550/SHA256SUMS.txt`
