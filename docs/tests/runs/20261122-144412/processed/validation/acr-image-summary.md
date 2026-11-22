# ACR 镜像清单验证

运行编号：`20261122-144412`

| 服务 | 镜像引用 | RepoDigest | 平台 | 判定 |
| --- | --- | --- | --- | --- |
| server | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:test-20261122-144412-aba070f` | `sha256:6c5ae6e1d3ef49dc5def2c7c54568e5af7c70e4eb12b6ba5da4a58e327019d9e` | `linux/amd64` | 通过 |
| client | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:test-20261122-144412-aba070f` | `sha256:7e509b6de3021788f2b66abf6e6f05b6af12b2811f451cce9280099da8a89354` | `linux/amd64` | 通过 |
| web | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:test-20261122-144412-aba070f` | `sha256:471123ace2c06375f83122a9cea7df29ba2222baa956a10a91ed4b579d5eb1ae` | `linux/amd64` | 通过 |

验证命令：`docs/tests/scripts/build/verify-acr-images.ps1`

判定：三套镜像均存在，标签固定，平台为 `linux/amd64`，且可读取 RepoDigest。
