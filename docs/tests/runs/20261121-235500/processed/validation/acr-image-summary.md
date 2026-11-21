# ACR 镜像清单验证

运行编号：`20261121-235500`

| 服务 | 镜像引用 | RepoDigest | 平台 | 判定 |
| --- | --- | --- | --- | --- |
| server | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:exploratory-20261121-233608-4e68667-clean` | `sha256:9c18e11d395f3c757e1622320515f3105bf206777612d3946b73410adc492302` | `linux/amd64` | 通过 |
| client | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:exploratory-20261121-233608-4e68667-clean` | `sha256:d36112d730c53d708a61d83c9a3b3dd4af16eb7d81193177468c631335bc8cd3` | `linux/amd64` | 通过 |
| web | `registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:exploratory-20261121-233608-4e68667-clean` | `sha256:16e7b4502d69f4d38f731ca8f698720370b3c97744aa99b0b2ca5bca83bf7c81` | `linux/amd64` | 通过 |

验证命令：`docs/tests/scripts/build/verify-acr-images.ps1`

判定：三套镜像均存在，标签固定，平台为 `linux/amd64`，且可读取 RepoDigest。
