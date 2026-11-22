# 20261122-144412-retry01 T-39 云服务器镜像拉取与产物一致性报告

## 结论

- 总体判定：**通过（T-39）**。
- 固定标签：test-20261122-144412-aba070f。
- 产品基线：689e00944533076c65f6e49688cc682a26e189bb。
- 三套镜像云端拉取、RepoDigest、linux/amd64 平台和容器内 JAR 一致性均通过；ACR 凭据已注销。

## 检查结果

| 检查项 | 结果 | 详情 |
| --- | --- | --- |
| 云服务器连接与 Docker 可用 | PASS | SSH 公钥登录和 Docker 命令成功 |
| 拉取前 ACR 登录态 | PASS | 云端已有 ACR 登录条目；凭据内容未读取或归档 |
| server 本地 fat JAR | PASS | exists=True；size=22613008；sha256=97e09ddc19863256e223c353e489557c6d872ef475112244bf3ec9b4cb227c50 |
| client 本地 fat JAR | PASS | exists=True；size=5668936；sha256=d4ac28dbec77b4481bc9f0e664c203946a3105dbeb79c5223c652ec8d5c25708 |
| server 镜像拉取 | PASS | 独立 SSH 拉取成功 |
| server RepoDigest | PASS | actual=sha256:6c5ae6e1d3ef49dc5def2c7c54568e5af7c70e4eb12b6ba5da4a58e327019d9e；expected=sha256:6c5ae6e1d3ef49dc5def2c7c54568e5af7c70e4eb12b6ba5da4a58e327019d9e |
| server 平台 | PASS | actual=linux/amd64 |
| server 容器内 JAR 一致性 | PASS | local=97e09ddc19863256e223c353e489557c6d872ef475112244bf3ec9b4cb227c50；container=97e09ddc19863256e223c353e489557c6d872ef475112244bf3ec9b4cb227c50 |
| client 镜像拉取 | PASS | 独立 SSH 拉取成功 |
| client RepoDigest | PASS | actual=sha256:7e509b6de3021788f2b66abf6e6f05b6af12b2811f451cce9280099da8a89354；expected=sha256:7e509b6de3021788f2b66abf6e6f05b6af12b2811f451cce9280099da8a89354 |
| client 平台 | PASS | actual=linux/amd64 |
| client 容器内 JAR 一致性 | PASS | local=d4ac28dbec77b4481bc9f0e664c203946a3105dbeb79c5223c652ec8d5c25708；container=d4ac28dbec77b4481bc9f0e664c203946a3105dbeb79c5223c652ec8d5c25708 |
| web 镜像拉取 | PASS | 独立 SSH 拉取成功 |
| web RepoDigest | PASS | actual=sha256:471123ace2c06375f83122a9cea7df29ba2222baa956a10a91ed4b579d5eb1ae；expected=sha256:471123ace2c06375f83122a9cea7df29ba2222baa956a10a91ed4b579d5eb1ae |
| web 平台 | PASS | actual=linux/amd64 |
| ACR 注销复核 | PASS | ACR_AUTH_ENTRY=no |

## 镜像与摘要

| 服务 | 完整镜像引用 | RepoDigest | 平台 | 验证状态 |
| --- | --- | --- | --- | --- |
| server | registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:test-20261122-144412-aba070f | sha256:6c5ae6e1d3ef49dc5def2c7c54568e5af7c70e4eb12b6ba5da4a58e327019d9e | linux/amd64 | 已拉取并核验 |
| client | registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:test-20261122-144412-aba070f | sha256:7e509b6de3021788f2b66abf6e6f05b6af12b2811f451cce9280099da8a89354 | linux/amd64 | 已拉取并核验 |
| web | registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:test-20261122-144412-aba070f | sha256:471123ace2c06375f83122a9cea7df29ba2222baa956a10a91ed4b579d5eb1ae | linux/amd64 | 已拉取并核验 |

## 归档

- 运行清单：[manifest.md](../runs/20261122-144412-retry01/manifest.md)
- 检查明细：[cloud-image-checks.md](../runs/20261122-144412-retry01/processed/validation/cloud-image-checks.md)
- 派生摘要：[cloud-image-summary.md](../runs/20261122-144412-retry01/processed/validation/cloud-image-summary.md)

## 限制

- 本运行只验证镜像发布产物和云端拉取一致性，不代表已完成部署、端到端或容量测试。
