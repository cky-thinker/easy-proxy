# Easy Proxy 测试资产与证据库

本目录保存 Easy Proxy 群晖 NAS ↔ 云服务器 Docker 测试方案的全部可复用资产：测试脚本、配置、固定数据、运行证据、分析结果与最终报告。

## 当前状态

| 项目 | 状态 | 说明 |
| --- | --- | --- |
| SSH 公钥登录 | 已验证 | 当前 Windows 管理机使用 Ed25519 公钥，仅公钥认证登录云端和 NAS 成功 |
| WSL Docker 构建环境 | 已验证 | `Ubuntu-22.04`、Docker/Client `29.10.0`、Buildx `v0.39.1`、Compose `v5.8.0` |
| 阿里云 ACR 发布 | 候选待发布 | 固定候选镜像已本地构建；等待一次交互认证后推送并读取 RepoDigest |
| 单元测试 | 已通过 | 修复后共 13 个测试，0 失败、0 错误；当前静态校验以脚本输出为准 |
| 云服务器镜像拉取 | 候选待验证 | 旧探索镜像曾验证；当前固定候选需在 ACR 发布后重新拉取并核对容器内 JAR |
| NAS Docker socket 权限 | 已验证 | 已创建专用 `docker` 组并配置守护进程；普通用户可通过 `/usr/local/bin/docker` 访问，Socket 为 `root:docker 660`，非交互 SSH 的 PATH 需显式补充 |
| Docker 实际部署 | 待执行 | 单元测试已通过；等待固定候选镜像发布、远端清单与拉取核验完成 |
| 高并发与稳定性测试 | 未执行 | 依赖可用的端到端 Docker 部署 |

当前构建验证报告：[20261121-235500-acr-build-validation.md](reports/20261121-235500-acr-build-validation.md)

## 目录结构

```text
docs/tests/
├── README.md
├── scripts/
│   ├── build/             # 镜像构建、推送与仓库验证
│   ├── common/            # 环境采集、凭据清理与通用工具
│   ├── functional/        # 基础功能与异常恢复测试
│   ├── load/              # 高并发、高负载与 SOAK 测试
│   ├── fault-injection/   # 重启、断网和目标故障注入
│   ├── monitoring/        # 管理机、云端、NAS 采样
│   └── reporting/         # 汇总、图表与报告生成
├── data/
│   ├── fixtures/          # 只读固定测试数据
│   ├── generated/         # 可复现的生成数据
│   └── manifests/         # 文件清单与基准哈希
├── configs/
│   ├── k6/                # 压测脚本配置
│   ├── hey/               # 轻量吞吐基线配置
│   ├── docker/            # 云端、NAS Compose 与环境变量示例
│   └── environment/       # 主机与环境快照
├── templates/             # 运行清单、报告与证据索引模板
├── runs/                  # 每轮原始证据和派生结果
└── reports/               # 最终报告与索引
```

## 归档规则

1. 每轮测试使用唯一的 `runs/yyyyMMdd-HHmmss/` 目录，不覆盖旧运行；重试使用新编号或 `-retryNN` 后缀。
2. 原始输出放入 `raw/`，派生结果放入 `processed/`，截图放入 `screenshots/`，临时文件放入 `tmp/`。
3. 每轮必须有 `manifest.md`，记录代码提交、环境、镜像标签与摘要、测试参数和完成状态。
4. 最终报告写入 `reports/`，同步更新 `reports/index.md`。
5. 运行目录的关键文件使用 `SHA256SUMS.txt` 固化；报告发布前再次校验。
6. 脚本、配置和数据均使用可复现的固定路径和版本，不使用隐式 `latest` 镜像。

## 缺陷闭环与终止规则

1. 测试按“缺陷修复 → 验证 → 测试 → 报告 → 再次修复”的闭环执行；任一步失败都不得直接进入验收。
2. 发现 P0/P1 严重缺陷时立即终止当前测试组，保存日志、指标、容器状态和配置快照，未执行用例标记为 `SKIPPED_BLOCKED`。
3. 终止后的下一环节是“缺陷修复与重新验证”，不是后续测试组或验收；修复后使用新的运行编号和固定镜像标签，从最早受影响阶段重跑全部下游测试。
4. 最终通过报告必须对应同一 Git 提交、同一镜像标签与 RepoDigest；失败轮次和证据必须保留，不得用修复前结果替代最终回归结果。

## 安全规则

- 不保存服务器密码、ACR 密码、客户端 Token、Cookie、Authorization 头、私钥或 Docker 凭据。
- ACR 登录使用交互输入与 `--password-stdin`，推送完成后立即执行 `docker logout`。
- 原始日志入库前必须脱敏；无法脱敏的证据不得归档，需说明原因和替代证据。
- 镜像使用固定标签和 RepoDigest，不以浮动标签作为验收依据。

## 快速入口

- 构建并发布镜像：`scripts/build/build-and-push-images.ps1`
- 验证 ACR 清单：`scripts/build/verify-acr-images.ps1`
- 采集 WSL 构建主机：`scripts/common/capture-wsl-host.sh`
- 确认 ACR 凭据已清除：`scripts/common/verify-acr-logged-out.sh`
- 修复 NAS Docker Socket 权限：`scripts/common/fix-nas-docker-access.sh`
- 云端 Compose：`configs/docker/cloud-compose.yml`
- NAS Compose：`configs/docker/nas-compose.yml`