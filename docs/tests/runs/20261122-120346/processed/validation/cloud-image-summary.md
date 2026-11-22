# T-39 云服务器镜像拉取与产物一致性

运行编号：20261122-120346
固定标签：test-20261122-022005-689e009

## 镜像事实

| 服务 | 完整镜像引用 | RepoDigest | 平台 | 验证状态 |
| --- | --- | --- | --- | --- |
| server | registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-server:test-20261122-022005-689e009 | sha256:37755831105cdb87e6044e2e1f016cf2faa6a811b8c97eb1f68b008fa8cbfa2a | linux/amd64 | 已拉取并核验 |
| client | registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-client:test-20261122-022005-689e009 | sha256:11f252500e74781489dc82b6b558639718e11c2170e5a206c5137a365c5e7009 | linux/amd64 | 已拉取并核验 |
| web | registry.cn-hangzhou.aliyuncs.com/ckyspace/easy-proxy-web:test-20261122-022005-689e009 | sha256:471123ace2c06375f83122a9cea7df29ba2222baa956a10a91ed4b579d5eb1ae | linux/amd64 | 已拉取并核验 |

## fat JAR 一致性

| 服务 | 本地字节数 | 本地 SHA-256 | 容器内 SHA-256 | 判定 |
| --- | ---: | --- | --- | --- |
| server | 22612993 | 63f727f770acc57fe7c5251810b81baacc82d9c8da623cd191f478fdb53b9161 | 63f727f770acc57fe7c5251810b81baacc82d9c8da623cd191f478fdb53b9161 | 一致 |
| client | 5668935 | 69f41783c9445fc8fd133149ada7e526755d05428862a425833177f9fd16f5be | 69f41783c9445fc8fd133149ada7e526755d05428862a425833177f9fd16f5be | 一致 |

## 判定

- 状态：通过（T-39）
- 结论：三套镜像云端拉取、RepoDigest、linux/amd64 平台和容器内 JAR 一致性均通过；ACR 凭据已注销。
