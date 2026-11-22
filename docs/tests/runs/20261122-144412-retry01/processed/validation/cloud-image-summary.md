# T-39 云服务器镜像拉取与产物一致性

运行编号：20261122-144412-retry01
固定标签：test-20261122-144412-aba070f

## 镜像事实

| 服务 | 完整镜像引用 | RepoDigest | 平台 | 验证状态 |
| --- | --- | --- | --- | --- |
| server | registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:test-20261122-144412-aba070f | sha256:6c5ae6e1d3ef49dc5def2c7c54568e5af7c70e4eb12b6ba5da4a58e327019d9e | linux/amd64 | 已拉取并核验 |
| client | registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:test-20261122-144412-aba070f | sha256:7e509b6de3021788f2b66abf6e6f05b6af12b2811f451cce9280099da8a89354 | linux/amd64 | 已拉取并核验 |
| web | registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:test-20261122-144412-aba070f | sha256:471123ace2c06375f83122a9cea7df29ba2222baa956a10a91ed4b579d5eb1ae | linux/amd64 | 已拉取并核验 |

## fat JAR 一致性

| 服务 | 本地字节数 | 本地 SHA-256 | 容器内 SHA-256 | 判定 |
| --- | ---: | --- | --- | --- |
| server | 22613008 | 97e09ddc19863256e223c353e489557c6d872ef475112244bf3ec9b4cb227c50 | 97e09ddc19863256e223c353e489557c6d872ef475112244bf3ec9b4cb227c50 | 一致 |
| client | 5668936 | d4ac28dbec77b4481bc9f0e664c203946a3105dbeb79c5223c652ec8d5c25708 | d4ac28dbec77b4481bc9f0e664c203946a3105dbeb79c5223c652ec8d5c25708 | 一致 |

## 判定

- 状态：通过（T-39）
- 结论：三套镜像云端拉取、RepoDigest、linux/amd64 平台和容器内 JAR 一致性均通过；ACR 凭据已注销。
