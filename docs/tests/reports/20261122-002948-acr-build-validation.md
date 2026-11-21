# 20261122-002948 Easy Proxy 修复前预检报告

## 结论

- 总体判定：**SUPERSEDED（修复前预检）**。本轮仅闭环测试资产与构建前置检查，不是系统验收。
- Web 依赖安装与生产构建通过。
- 测试资产静态校验最初因缺少运行清单、预检报告和 ACR 注销证据失败；补齐后复跑通过。
- 正式镜像构建、ACR 发布、云端/NAS 部署、T-01～T-39 尚未在本轮执行。

## 结果

| 检查项 | 结果 | 证据 |
| --- | --- | --- |
| Web `npm ci` | 通过 | [日志](../runs/20261122-002948/raw/build/web-npm-ci.log) |
| Web `npm run build` | 通过 | [日志](../runs/20261122-002948/raw/build/web-build.log) |
| 首次静态校验 | 失败：缺少本轮文档和 ACR 注销证据 | [记录](../runs/20261122-002948/raw/validation/test-assets-validation.txt) |
| ACR 注销复核 | 通过：`ACR_AUTH_ENTRY=no` | [记录](../runs/20261122-002948/raw/validation/acr-logout.txt) |
| 静态校验复跑 | 通过 | [记录](../runs/20261122-002948/raw/validation/test-assets-validation-final.txt) |

## 缺陷与处置

| 编号 | 级别 | 描述 | 处置 |
| --- | --- | --- | --- |
| DEF-PRE-01 | 测试证据 | 预检轮次缺少运行清单、报告和 ACR 注销证据 | 已补齐并复跑静态校验；正式缺陷编号从最终运行链重新登记 |

## 归档

- 运行清单：[manifest.md](../runs/20261122-002948/manifest.md)
- 原始证据：`docs/tests/runs/20261122-002948/raw/`
- 哈希清单：`docs/tests/runs/20261122-002948/SHA256SUMS.txt`

## 后续

本报告由正式运行轮次取代。正式报告必须对应单一 Git 提交、单一固定镜像标签和同一 RepoDigest。