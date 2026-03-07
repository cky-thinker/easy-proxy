# 依赖
node v22.17.0
vue3 https://vuejs.org/
element-plus https://element-plus.org/zh-CN/

1. 总览
- 客户端在线数
- 客户端离线数
- 流量排行
    - 按天
    - 按周
    - 按月
- 实时流量趋势图
    - 按天
    - 按周
    - 按月

2. 客户端管理
- 新增、编辑、删除
- 端口管理

3. 账号管理
- 新增、编辑、删除
- 权限管理

## 构建与部署

### Docker 构建

本项目支持使用 Docker 进行构建和部署。

1. **构建镜像**

```bash
docker build -t easy-proxy-web .
```

2. **运行容器**

```bash
docker run -d -p 10093:10093 --name easy-proxy-web easy-proxy-web
```

如果您需要连接到后端的 `easy-proxy-server`，请确保网络配置正确，或者使用 `docker-compose`。
默认情况下，Nginx 配置会将 `/api` 请求代理到 `http://easy-proxy-server:10091`。

### 本地构建

```bash
npm install
npm run build
```

构建产物位于 `dist` 目录下。
