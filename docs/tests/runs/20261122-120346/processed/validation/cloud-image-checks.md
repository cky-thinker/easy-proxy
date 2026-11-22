# T-39 云服务器镜像核验检查

| 检查项 | 结果 | 详情 |
| --- | --- | --- |
| 云服务器连接与 Docker 可用 | PASS | SSH 公钥登录和 Docker 命令成功 |
| 拉取前 ACR 登录态 | PASS | 云端已有 ACR 登录条目；凭据内容未读取或归档 |
| server 本地 fat JAR | PASS | exists=True；size=22612993；sha256=63f727f770acc57fe7c5251810b81baacc82d9c8da623cd191f478fdb53b9161 |
| client 本地 fat JAR | PASS | exists=True；size=5668935；sha256=69f41783c9445fc8fd133149ada7e526755d05428862a425833177f9fd16f5be |
| server 镜像拉取 | PASS | 独立 SSH 拉取成功 |
| server RepoDigest | PASS | actual=sha256:37755831105cdb87e6044e2e1f016cf2faa6a811b8c97eb1f68b008fa8cbfa2a；expected=sha256:37755831105cdb87e6044e2e1f016cf2faa6a811b8c97eb1f68b008fa8cbfa2a |
| server 平台 | PASS | actual=linux/amd64 |
| server 容器内 JAR 一致性 | PASS | local=63f727f770acc57fe7c5251810b81baacc82d9c8da623cd191f478fdb53b9161；container=63f727f770acc57fe7c5251810b81baacc82d9c8da623cd191f478fdb53b9161 |
| client 镜像拉取 | PASS | 独立 SSH 拉取成功 |
| client RepoDigest | PASS | actual=sha256:11f252500e74781489dc82b6b558639718e11c2170e5a206c5137a365c5e7009；expected=sha256:11f252500e74781489dc82b6b558639718e11c2170e5a206c5137a365c5e7009 |
| client 平台 | PASS | actual=linux/amd64 |
| client 容器内 JAR 一致性 | PASS | local=69f41783c9445fc8fd133149ada7e526755d05428862a425833177f9fd16f5be；container=69f41783c9445fc8fd133149ada7e526755d05428862a425833177f9fd16f5be |
| web 镜像拉取 | PASS | 独立 SSH 拉取成功 |
| web RepoDigest | PASS | actual=sha256:471123ace2c06375f83122a9cea7df29ba2222baa956a10a91ed4b579d5eb1ae；expected=sha256:471123ace2c06375f83122a9cea7df29ba2222baa956a10a91ed4b579d5eb1ae |
| web 平台 | PASS | actual=linux/amd64 |
| ACR 注销复核 | PASS | ACR_AUTH_ENTRY=no |
