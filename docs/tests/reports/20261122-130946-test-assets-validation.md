# 20261122-130946 测试资产静态校验报告

## 结论

- 总体判定：**失败**。
- 静态校验失败：2 项未通过。
- 本报告只证明静态资产满足要求，不代表构建、发布、部署、端到端或性能测试已通过。

## 测试范围

- 检查纳入 Git 的文本编码、BOM 与 LF 换行。
- 检查 Markdown 围栏和本地相对链接。
- 校验固定数据文件清单与 SHA-256。
- 检查 T-01 至 T-39 编号完整性、PowerShell 语法、固定镜像版本和安全默认值。
- 扫描私钥、Bearer 令牌、固定客户端 Token、JWT 和非占位符密码。

## 环境与版本

- Git 提交：`7729a6ab0c0e06789f0b058e717892ceaf85ddcb`。
- 执行环境：Windows PowerShell，仓库工作区。
- 运行编号：`20261122-130946`。

## 结果

| 检查项 | 结果 |
| --- | --- |
| [PASS] 运行目录初始化 | 运行编号=20261122-130946；已创建 raw、processed、screenshots、tmp |
| [PASS] 文本资产可读取 | 共 310 个纳入扫描的文件 |
| [PASS] UTF-8 编码与 BOM | PowerShell 带 BOM，Markdown/YAML/env/Shell/CSV/JSON 无 BOM |
| [PASS] 统一 LF 换行 | 全部通过 |
| [PASS] UTF-8 可解码 | 全部通过 |
| [PASS] Markdown 代码围栏 | 检查 87 个 Markdown 文件 |
| [PASS] Markdown 相对链接 | 检查 128 个本地链接，全部存在 |
| [PASS] 固定数据清单与哈希 | 清单 1003 行，文件 1003 个，纳入清单 1003 个；缺失/不一致/未纳入异常 0 个 |
| [PASS] 测试用例编号 | T-01 至 T-39 连续且唯一 |
| [PASS] PowerShell 语法 | 检查 12 个 PowerShell 文件，零错误 |
| [PASS] H2 Console 默认关闭 | server.yaml 中 h2ConsoleEnable=false |
| [PASS] NAS 测试目标固定版本 | TARGET_IMAGE=nginx:1.27.4-alpine |
| [PASS] 测试目标 Nginx 配置 | 根路径返回 200，监听 80，慢请求限速与 no-store 均存在 |
| [PASS] README 单元测试数量 | 记录为 13 个测试 |
| [FAIL] 固定镜像引用 | 旧仓库引用 2 个，latest 镜像引用 0 个 |
| [FAIL] 敏感信息静态扫描 | 命中 2 项，涉及文件：docs/tests/scripts/common/EasyProxy.Secrets.psm1（不输出明文） |
| [PASS] 生产日志级别 | 服务端与客户端 com.cky.proxy 均为 INFO |
| [PASS] Token 日志脱敏 | 客户端 Token 校验失败日志不包含 Token 原文 |
| [PASS] 证据脱敏回归 | SQL 参数、Token、密码、URL 凭据、JSON 密钥与 bcrypt 哈希均已脱敏 |

## 归档校验

- 运行清单：[manifest.md](../runs/20261122-130946/manifest.md)
- 原始输出：[test-assets-validation.txt](../runs/20261122-130946/raw/validation/test-assets-validation.txt)
- 哈希清单：`docs/tests/runs/20261122-130946/SHA256SUMS.txt`
