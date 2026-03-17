[English](./README_en.md) | 中文

# Easy Proxy

<p align="center">
  <img src="wiki/asserts/images/logo.png" alt="Easy Proxy Logo" width="200">
</p>

一个简单、高效的内网穿透工具。

## 项目简介

Easy Proxy 是一个基于 Vert.x 开发的高性能内网穿透工具，支持 TCP 流量转发。它包含服务端、客户端和 Web 管理界面，旨在提供简单易用的内网穿透服务。

## 软件截图

<p align="center">
  <img src="wiki/asserts/images/page_login.jpeg" alt="登录页" width="48%">
  <img src="wiki/asserts/images/page_dashboard.jpeg" alt="仪表盘" width="48%">
</p>

## 核心功能一览

| 功能名称        | 功能描述                                              | 支持情况  |
| :---------- | :------------------------------------------------ | :---- |
| **TCP内网穿透** | 基于Vert.x事件驱动模型，实现高性能TCP流量转发，支持双向数据透传，适用于各种TCP应用场景 | ✅ 已支持 |
| **TLS加密传输** | 服务端与客户端通信支持TLS加密，自动生成/下载证书，防止数据在传输过程中被窃听或篡改       | ✅ 已支持 |
| **流控管理**    | 支持基于规则的带宽限制（KB/s）和连接数限制，采用令牌桶算法，精准控制流量速率，防止滥用     | ✅ 已支持 |
| **流量分析**    | 提供实时/小时/天维度的流量统计，支持连接数监控与历史报表查询，帮助用户了解业务流量趋势      | ✅ 已支持 |
| **端口动态管理**  | 支持Web端动态申请/释放端口，实时生效，无需重启服务，灵活应对业务变化              | ✅ 已支持 |
| **多账号管理**   | 支持多用户管理，提供可视化用户管理界面，保障系统安全性                       | ✅ 已支持 |

## Docker快速部署

### 服务端部署

```bash
# api服务
docker rm -f easy-proxy-server
docker run -d \
  --name easy-proxy-server \
  --network host \
  --restart always \
  -v $(pwd)/easy-proxy-server/config:/app/config \
  -v $(pwd)/easy-proxy-server/data:/app/data \
  -v $(pwd)/easy-proxy-server/logs:/app/logs \
  yudejijie/easy-proxy-server:latest

# web管理界面
docker rm -f easy-proxy-web
docker run -d \
  --name easy-proxy-web \
  --network host \
  --restart always \
  yudejijie/easy-proxy-web:latest
```

#### 配置流程

1. 访问 `http://server-ip:10093` 即可打开 Web 管理界面。(**server-ip为服务端ip**)
2. 初始化管理员账号。
3. 新建客户端，生成token。(**客户端需要使用该token与服务端建立连接**)
4. 新建转发规则，指定公网端口和客户端服务地址。

### 客户端部署

```bash
# 客户端服务
docker run -d \
  --name easy-proxy-client \
  --network host \
  --restart always \
  -v $(pwd)/easy-proxy-client/config:/app/config \
  -v $(pwd)/easy-proxy-client/logs:/app/logs \
  yudejijie/easy-proxy-client:latest
```

#### 配置流程

1. 服务启动成功，会在 `config` 目录下生成 `config.yaml` 文件。
2. 修改 `config.yaml` 文件，配置服务端地址和token。

```yaml
server:
  address: "server-ip" # 服务端ip
  port: 10092
  token: "client-token" # 客户端token
```

1. 重启客户端服务，使配置生效。
```bash
docker restart easy-proxy-client
```
2. 查看管理配置页面，客户端应该已在线。(如果未在线，查看常见问题排查)

## 常见问题排查

### 服务端无法访问、代理服务无法访问

1. 确保防火墙或安全策略开放对应的端口。
- 端口10090: socket代理端口
- 端口10092: web管理端口
- 代理端口：根据配置的转发规则，开放对应公网端口。

### 客户端无法连接服务端

1. 确保客户端已配置正确的服务端地址和token。


## 技术栈

- **后端**: Java 17, Vert.x 4
- **前端**: Vue 3, TypeScript, Element Plus, Vite
- **构建工具**: Maven, npm