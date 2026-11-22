# T-39 首次尝试摘要

- server、client 拉取成功且 RepoDigest 正确。
- web 在同一批量命令中返回 `invalid reference format`。
- 立即使用完整的单条镜像引用复验 web，拉取成功且 RepoDigest 正确。
- 因此该次失败定位为批量命令传输/执行方式不稳定，不是镜像仓库或产品镜像缺陷。
- 本运行保留为 `SUPERSEDED`，不得计入最终通过证据。