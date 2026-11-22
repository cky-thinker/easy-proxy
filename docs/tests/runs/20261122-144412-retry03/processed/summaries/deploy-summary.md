# 正式候选部署摘要

运行编号：20261122-144412-retry03
固定标签：test-20261122-144412-aba070f
云服务器回滚保留后缀：-rollback-20261122-145841
NAS 回滚保留后缀：-rollback-20261122-145841
云服务器 Compose 项目：easy-proxy-cloud-20261122-145841
NAS Compose 项目：easy-proxy-nas-20261122-145841

| 检查项 | 结果 | 说明 |
| --- | --- | --- |
| 云服务器 SSH 与预检 | PASS | exit=0 |
| 云服务器部署目录 | PASS | exit=0 |
| 云服务器敏感配置写入 | PASS | 经 SSH stdin 写入，未在证据中记录内容 |
| 云服务器容器启动 | PASS | exit=0 |
| 服务端/API 健康 | PASS | 等待 2.5 秒 |
| 管理员初始化与登录 | PASS | JWT 仅保存在进程内存 |
| NAS 客户端记录 | PASS | id=1 |
| NAS 部署目录 | PASS | exit=0 |
| NAS 敏感配置写入 | PASS | 经 SSH stdin 写入，未在证据中记录内容 |
| 测试固定数据上传 | PASS | exit=0，经 SSH 归档上传 |
| 测试固定数据校验 | PASS | 文件数=1003，关键样本 SHA256 已留证 |
| NAS 固定镜像可用 | PASS | exit=0 |
| NAS 目标与客户端启动 | PASS | exit=0 |
| NAS 客户端上线 | PASS | 等待 5.3 秒 |
| NAS 目标服务可用 | PASS | 等待 0 秒 |
| 转发规则生效 | PASS | id=1，28080 -> 192.168.1.9:18080 |
| 公网规则监听 | PASS | 等待 1.4 秒 |
| 公网端到端链路 | PASS | 等待 3.5 秒 |
| 客户端/服务端证书指纹一致 | PASS | SHA256=96:7E:EA:75:56:FF:12:77:7B:D2:45:78:04:21:35:99:46:79:80:75:FF:63:F8:8F:4C:B6:7D:E5:C4:4B:2F:14 |
| 运行状态归档 | PASS | exit=0 |
| NAS 运行状态归档 | PASS | exit=0 |

敏感信息仅从 DPAPI 存储读取并经 SSH 标准输入写入；本摘要不记录密码、Token、JWT 或证书密码。
