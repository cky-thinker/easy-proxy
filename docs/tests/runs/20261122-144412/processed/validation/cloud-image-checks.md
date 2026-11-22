# T-39 云服务器镜像核验检查

| 检查项 | 结果 | 详情 |
| --- | --- | --- |
| 云服务器连接与 Docker 可用 | PASS | SSH 公钥登录和 Docker 命令成功 |
| 拉取前 ACR 登录态 | FAIL | 云端没有可用的 ACR 登录条目 |
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
