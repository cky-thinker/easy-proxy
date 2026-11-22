# ACR 镜像清单验证

运行编号：`20261122-114240`

| 服务 | 镜像引用 | RepoDigest | 平台 | 判定 |
| --- | --- | --- | --- | --- |
| server | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:test-20261122-022005-689e009` | `sha256:37755831105cdb87e6044e2e1f016cf2faa6a811b8c97eb1f68b008fa8cbfa2a` | `linux/amd64` | 通过 |
| client | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:test-20261122-022005-689e009` | `sha256:11f252500e74781489dc82b6b558639718e11c2170e5a206c5137a365c5e7009` | `linux/amd64` | 通过 |
| web | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:test-20261122-022005-689e009` | `sha256:471123ace2c06375f83122a9cea7df29ba2222baa956a10a91ed4b579d5eb1ae` | `linux/amd64` | 通过 |

验证命令：`docs/tests/scripts/build/verify-acr-images.ps1`

判定：三套镜像均存在，标签固定，平台为 `linux/amd64`，且可读取 RepoDigest。
