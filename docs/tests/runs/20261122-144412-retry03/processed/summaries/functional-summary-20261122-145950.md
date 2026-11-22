# T-01 至 T-20 功能测试摘要

- 运行编号：`20261122-144412-retry03`
- 总体判定：**FAIL**
- 通过：17；失败：3

| 测试编号 | 测试项 | 状态 | 耗时（秒） | 说明 |
| --- | --- | --- | ---: | --- |
| T-01 | NAS 公钥登录 | PASS | 0.3 | BatchMode 公钥登录成功 |
| T-02 | 云服务器公钥登录 | PASS | 1.5 | BatchMode 公钥登录成功 |
| T-03 | Docker 环境 | PASS | 1.2 | 云端与 NAS Docker/Compose 命令成功 |
| T-04 | NAS 目标服务 | PASS | 0 | HTTP 200，目标页面正确 |
| T-05 | 服务端本机健康检查 | PASS | 1.5 | 云端本机 API 健康 |
| T-06 | Web 健康检查 | PASS | 0.1 | 公网 Web 健康 |
| T-07 | 客户端在线 | PASS | 1 | 在线，等待 0.8 秒；无持续鉴权/证书错误 |
| T-08 | 规则监听 | PASS | 2.1 | 云端 28080 正在监听 |
| T-09 | 公网访问 | PASS | 0.9 | 公网规则端口返回 NAS 目标页面 |
| T-10 | 响应头 | PASS | 0.9 | HTTP 200 且包含 Content-Type |
| T-11 | 数据完整性 | PASS | 18.8 | 大小文件 SHA-256 均与固定数据清单一致 |
| T-12 | 流量统计 | PASS | 0.9 | 实时流量与规则报表 API 均可读取 |
| T-13 | 客户端重连 | PASS | 9.6 | 30 秒内在线，1 秒内转发恢复 |
| T-14 | 服务端重启 | PASS | 9.1 | Web、客户端在线、规则监听与转发均恢复 |
| T-15 | 规则持久化 | PASS | 1.5 | 重启后原规则仍存在并监听 |
| T-16 | 错误 Token | FAIL | 16.3 | 错误 Token 失败：exit=0
Using JAR: app.jar
Starting in foreground mode...
[31m2026-11-22 15:00:50[0;39m [32m[virtual-39][0;39m [1;31mERROR[0;39m [1;35mc.c.p.c.h.ClientMngSocketManager
[0;39m - EP>>ClientMng>> Mng socket error: null
[31m2026-11-22 15:00:50[0;39m [32m[virtual-39][0;39m [34mINFO [0;39m [1;35mc.c.p.c.h.ClientMngSocketManager
[0;39m - EP>>ClientMng>> Mng socket closed
[31m2026-11-22 15:00:50[0;39m [32m[virtual-39][0;39m [1;31mERROR[0;39m [1;35mcom.cky.proxy.client.ClientRunner
[0;39m - EP>>ClientMng>> Connect closed
[31m2026-11-22 15:00:50[0;39m [32m[virtual-39][0;39m [34mINFO [0;39m [1;35mcom.cky.proxy.client.ClientRunner
[0;39m - next connect wait time 2000ms
[31m2026-11-22 15:00:52[0;39m [32m[virtual-40][0;39m [34mINFO [0;39m [1;35mcom.cky.proxy.client.ClientRunner
[0;39m - try connect 68.64.181.242:21090...

primary_online=True |
| T-17 | 端口冲突 | PASS | 22.5 | 冲突被服务端记录，未影响云服务器其他服务 |
| T-18 | 禁用规则 | PASS | 13.8 | 禁用后端口释放，公网请求失败 |
| T-19 | 删除规则 | FAIL | 15.4 | 删除规则 失败：rule_id=2 listening=True probe=0 released=True exists_after=False |
| T-20 | 客户端禁用 | FAIL | 72.5 | 客户端禁用 失败：offline=False elapsed=61.7
probe_status=0
probe_error=The request was canceled due to the configured HttpClient.Timeout of 5 seconds elapsing. |

- 结果 CSV：`processed/summaries/functional-results-20261122-150308.csv`
