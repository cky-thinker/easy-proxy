# 20261122-121702 测试资产静态校验报告

## 结论

- 总体判定：**通过**。
- 全部 16 项静态校验通过。
- 本报告只证明静态资产满足要求，不代表构建、发布、部署、端到端或性能测试已通过。

## 测试范围

- 检查纳入 Git 的文本编码、BOM 与 LF 换行。
- 检查 Markdown 围栏和本地相对链接。
- 校验固定数据文件清单与 SHA-256。
- 检查 T-01 至 T-39 编号完整性、PowerShell 语法、固定镜像版本和安全默认值。
- 扫描私钥、Bearer 令牌、固定客户端 Token、JWT 和非占位符密码。

## 环境与版本

- Git 提交：`ed2a0fb17ab496385489ee5cbb8c5e3793c8002c`。
- 执行环境：Windows PowerShell，仓库工作区。
- 运行编号：`20261122-121702`。

## 结果

| 检查项 | 结果 |
| --- | --- |
| [PASS] 运行目录初始化 | 运行编号=20261122-121702；已创建 raw、processed、screenshots、tmp |
| [PASS] 文本资产可读取 | 共 288 个纳入扫描的文件 |
| [PASS] UTF-8 编码与 BOM | PowerShell 带 BOM，Markdown/YAML/env/Shell/CSV/JSON 无 BOM |
| [PASS] 统一 LF 换行 | 全部通过 |
| [PASS] UTF-8 可解码 | 全部通过 |
| [PASS] Markdown 代码围栏 | 检查 84 个 Markdown 文件 |
| [PASS] Markdown 相对链接 | 检查 125 个本地链接，全部存在 |
| [PASS] 固定数据清单与哈希 | 清单 1003 行，文件 1003 个，纳入清单 1003 个；缺失/不一致/未纳入异常 0 个 |
| [PASS] 测试用例编号 | T-01 至 T-39 连续且唯一 |
| [PASS] PowerShell 语法 | 检查 9 个 PowerShell 文件，零错误 |
| [PASS] H2 Console 默认关闭 | server.yaml 中 h2ConsoleEnable=false |
| [PASS] NAS 测试目标固定版本 | TARGET_IMAGE=nginx:1.27.4-alpine |
| [PASS] 测试目标 Nginx 配置 | 根路径返回 200，监听 80，慢请求限速与 no-store 均存在 |
| [PASS] README 单元测试数量 | 记录为 13 个测试 |
| [PASS] 固定镜像引用 | 旧仓库引用 0 个，latest 镜像引用 0 个 |
| [PASS] 敏感信息静态扫描 | 未发现私钥、Bearer 令牌、固定客户端 Token、JWT 或非占位符密码 |

## 归档校验

- 运行清单：[manifest.md](../runs/20261122-121702/manifest.md)
- 原始输出：[test-assets-validation.txt](../runs/20261122-121702/raw/validation/test-assets-validation.txt)
- 哈希清单：`docs/tests/runs/20261122-121702/SHA256SUMS.txt`
