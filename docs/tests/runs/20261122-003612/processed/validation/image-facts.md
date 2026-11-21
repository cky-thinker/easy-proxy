# 本地镜像事实

## 来源

- 运行编号：`20261122-003612`
- Git 提交：`ec2342d14a4ad71407013e4c913118f420b9223f`
- 固定标签：`test-20261122-003613-ec2342d`
- 平台：`linux/amd64`
- 原始证据：`raw/validation/image-inspect-server.json`、`image-inspect-client.json`、`image-inspect-web.json`

## 镜像

| 服务 | 本地镜像 ID | 操作系统/架构 | 大小（字节） | 暴露端口 |
| --- | --- | --- | ---: | --- |
| server | `sha256:ef7c15eb46646f298c725e472d1000f0a4abae40002b6f86ac336e85a5fdb189` | `linux/amd64` | `653109728` | `21090/tcp,21091/tcp` |
| client | `sha256:04afaabd71a1c7e3dd9eb8b38b07c20197d0d1fcfe8ae22b0dfb94ce569c4c43` | `linux/amd64` | `621197563` | 无 |
| web | `sha256:21afbd85c2dd069a8975f46ab5685e65a3677dbcb99c6cc24786b9358538053e` | `linux/amd64` | `96707337` | `21092/tcp,80/tcp` |

## 说明

本地镜像 ID 不是 ACR RepoDigest。T-38 推送后必须重新读取远端 `RepoDigest`，不得用本地镜像 ID 替代。
