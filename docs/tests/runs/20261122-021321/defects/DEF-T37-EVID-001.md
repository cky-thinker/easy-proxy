# DEF-T37-EVID-001 本地镜像事实采集产生 CRLF 并导致静态校验失败

## 基本信息

| 字段 | 值 |
| --- | --- |
| 缺陷编号 | `DEF-T37-EVID-001` |
| 严重级别 | P2 |
| 首次发现运行 | `20261122-021321` |
| 触发用例 | T-37、测试资产静态校验 |
| 修复提交 | 待回填 |
| 验证运行 | 待新 T-37 运行验证 |
| 状态 | 已修复，待回归验证 |

## 现象

`20261122-021050` 的本轮构建、单元测试和三套镜像均成功，但手工采集的 3 个 `local-image-*.json` 使用 CRLF 换行。后续静态校验在“统一 LF 换行”检查中失败，涉及：

- `docs/tests/runs/20261122-021050/raw/build/local-image-server.json`
- `docs/tests/runs/20261122-021050/raw/build/local-image-client.json`
- `docs/tests/runs/20261122-021050/raw/build/local-image-web.json`

## 影响范围

- T-37 构建结果仍可人工核验，但该轮证据不符合统一文本格式要求，不能作为最终通过链的一部分。
- 静态校验无法整体通过，T-38/T-39 必须等待新轮次闭环。
- 旧轮次必须标记为 `SUPERSEDED`，不得覆盖或删除。

## 根因

`build-and-push-images.ps1` 只负责构建和日志归档，本地镜像事实依靠构建后手工命令采集。Windows PowerShell 重定向默认产生 CRLF；同时早期采集使用 bash 格式管道，WSL 传输警告混入标准输出，导致证据格式与解析都不稳定。

## 修复说明

- 在 `build-and-push-images.ps1` 内新增 WSL 原生命令捕获函数，使用 `ProcessStartInfo.ArgumentList` 传参，避免 shell 引号与管道解析。
- 新增 `docker image inspect` 结构化采集，直接保存完整 JSON。
- JSON、标准错误和汇总文件统一通过 `Write-LfText` 写入 UTF-8 无 BOM、LF 换行。
- 自动校验镜像 ID、固定标签、`linux/amd64` 平台和创建时间。
- 标准错误单独归档为 `local-image-<service>.stderr.txt`，不再污染 JSON。
- 为失败轮次的 3 个原始 CRLF JSON 保留字节级副本，扩展名为 `.original-crlf.bin`；原 JSON 在保留原件后仅做换行规范化，字段内容不变。

## 原始字节存档

| 服务 | 原始 SHA-256 |
| --- | --- |
| server | `c0a90e4100dc8e2249b644fc2b7c37f78446291f412ca44563eb591abc198921` |
| client | `6af61f3f6267c242539cee923adf84f63e407bacc3fe059174859c786a742244` |
| web | `b5ce8ced8258030c009125b50826284bf36f01261e095e3ff0d4d0487b118e77` |

## 验证证据

- 首次失败：`docs/tests/runs/20261122-021321/raw/validation/test-assets-validation.txt`
- 原始字节：`docs/tests/runs/20261122-021050/raw/build/local-image-*.json.original-crlf.bin`
- 修复脚本：`docs/tests/scripts/build/build-and-push-images.ps1`
- 验证方式：使用全新 RunId 重新执行 T-37，再使用另一个全新 RunId 执行静态校验。

## 回归范围

- T-37 本地镜像构建与事实采集。
- 文本资产编码、BOM 和换行检查。
- T-38/T-39 的固定标签、RepoDigest、远端平台和拉取一致性验证。
