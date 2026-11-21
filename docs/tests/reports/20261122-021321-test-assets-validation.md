# 20261122-021321 测试资产静态校验报告

## 结论

- 总体判定：**失败（SUPERSEDED）**。
- Git 提交：`7c4f79a97fea78de24530d40cbd2f6ff591d7669`。
- 唯一失败项：统一 LF 换行。
- 已登记缺陷：`DEF-T37-EVID-001`，P2。`n- 修复提交：`a53804f0e4047c1ac7b045544626bd395cda7df1`。

## 检查结果

| 检查项 | 结果 | 说明 |
| --- | --- | --- |
| 文本资产可读取 | 通过 | 共 194 个纳入扫描的文件 |
| UTF-8 编码与 BOM | 通过 | PowerShell 带 BOM；Markdown、YAML、env、Shell、CSV、JSON 无 BOM |
| 统一 LF 换行 | 失败 | 3 个本地镜像 JSON 含 CRLF |
| Markdown 代码围栏 | 通过 | 检查 48 个 Markdown 文件 |
| Markdown 相对链接 | 通过 | 检查 61 个本地链接 |
| 固定数据清单与哈希 | 通过 | 1003 个文件全部一致 |
| 测试用例编号 | 通过 | T-01 至 T-39 连续且唯一 |
| PowerShell 语法 | 通过 | 8 个文件零错误 |
| 固定镜像引用 | 通过 | 旧仓库引用 0 个，latest 引用 0 个 |
| 敏感信息静态扫描 | 通过 | 未发现私钥、令牌、认证头或非占位符密码 |

## 缺陷

| 编号 | 级别 | 现象 | 处置 |
| --- | --- | --- | --- |
| `DEF-T37-EVID-001` | P2 | 手工采集镜像事实产生 3 个 CRLF JSON | 已改为脚本内置结构化采集，保留原始字节，使用新轮次回归 |

缺陷记录：[DEF-T37-EVID-001.md](../runs/20261122-021321/defects/DEF-T37-EVID-001.md)

## 证据

- 原始校验输出：[test-assets-validation.txt](../runs/20261122-021321/raw/validation/test-assets-validation.txt)
- 运行清单：[manifest.md](../runs/20261122-021321/manifest.md)
- 被取代构建轮次：[20261122-021050-t37-build.md](20261122-021050-t37-build.md)

## 后续动作

1. 提交工具修复和缺陷记录。
2. 使用全新 RunId 重跑 T-37，确认脚本自动生成 UTF-8 无 BOM、LF 的镜像事实。
3. 使用另一个全新 RunId 重跑静态校验；全部通过后继续 T-38/T-39。
