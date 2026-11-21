# 测试运行清单（修复前预检）

## 基本信息

| 字段 | 值 |
| --- | --- |
| 运行编号 | `20261122-002948` |
| 测试范围 | Web 构建、测试资产静态校验与 ACR 注销状态预检 |
| 执行方式 | 当前 Windows 管理机自动化执行 |
| 开始时间 | `2026-11-22 00:29:48 +08:00` |
| 结束时间 | `2026-11-22 00:36:00 +08:00` |
| Git 分支/提交 | `develop` / `bdc18bad09c0ab61c888a49587c176a48686d6a9` |
| 运行状态 | `SUPERSEDED`：修复前预检，不作为验收依据 |
| 结论 | 预检证据已补齐；正式构建、ACR 发布、部署和 T-01～T-39 将在新的通过轮次执行 |

## 环境

| 主机 | 角色 | 系统/运行时 |
| --- | --- | --- |
| 当前管理机 | 构建与压测源 | Windows；Node/npm；Maven；OpenSSH |
| WSL Ubuntu-22.04 | Docker/Buildx/Compose 与 ACR 凭据检查 | Ubuntu 22.04；Docker/Buildx/Compose 可用 |
| 云服务器 | 服务端/Web 部署 | Ubuntu 22.04 |
| 群晖 NAS | 客户端/目标服务部署 | DSM；Docker 24.0.2 |

## 本轮执行内容

| 项目 | 结果 | 证据 |
| --- | --- | --- |
| Web 依赖安装 | 通过 | `raw/build/web-npm-ci.log` |
| Web 生产构建 | 通过；存在依赖告警但不阻断 | `raw/build/web-build.log` |
| 测试资产静态校验 | 修复前失败，缺运行清单、报告和注销证据 | `raw/validation/test-assets-validation.txt` |
| ACR 凭据清理复核 | 通过，`ACR_AUTH_ENTRY=no` | `raw/validation/acr-logout.txt` |
| 静态校验复跑 | 通过 | `raw/validation/test-assets-validation-final.txt` |

## 限制

- 本轮为预检轮次，不部署镜像、不执行 T-01～T-39。
- 正式验收必须使用修复后同一 Git 提交、固定 `test-` 镜像标签和同一 RepoDigest 的全新运行。
- 本报告已标记为 `SUPERSEDED`，最终通过链接以正式轮次报告为准。

## 清理与脱敏

- [x] 未记录 ACR 用户名、密码或其他凭据
- [x] 未记录客户端 Token、Cookie 或私钥
- [x] 原始证据保留且未覆盖失败结果