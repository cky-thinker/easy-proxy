# T-39 云服务器镜像拉取首次尝试

## 判定

`FAILED / SUPERSEDED`

本次尝试在同一 SSH 批量脚本中拉取三套镜像。server 和 client 成功，web 返回 `invalid reference format`。随后使用完整、单条的 web 镜像引用立即复验，拉取成功且 RepoDigest 与 ACR 清单一致。

## 根因与处置

- 现象不能在产品镜像或 ACR 仓库侧复现。
- 根因归类为测试执行方式的命令传输不稳定。
- 后续改为可复用的 PowerShell 核验脚本，分别执行拉取、inspect 和 JAR 哈希检查，并逐项强制校验。
- 本运行保留，不覆盖、不删除，由后续 T-39 通过运行取代。

## 证据

- `docs/tests/runs/20261122-115724/manifest.md`
- `docs/tests/runs/20261122-115724/raw/validation/cloud-pull-first-attempt.txt`
- `docs/tests/runs/20261122-115724/processed/validation/first-attempt-summary.md`

## 限制

本运行未完成 JAR 哈希核验，不计入 T-39 通过证据。