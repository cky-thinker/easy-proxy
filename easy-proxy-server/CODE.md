## 项目规范
### 核心入口
ProxyServerVerticle: 负责通过socket进行内网穿透的相关操作，包括建立客户端连接、处理请求、转发请求到客户端、返回响应等。
WebManageVerticle: 负责通过web进行数据管理的相关操作，包括对客户端、转发规则等数据进行管理。

### 项目模块
项目分controller、service、dao、util等模块，每个模块都有其特定的职责和功能。
com.cky.proxy.server.controller: 负责处理http请求，调用service层进行业务逻辑处理，返回http响应。
com.cky.proxy.server.service: 负责实现业务逻辑，调用dao层进行数据操作。
com.cky.proxy.server.dao: 负责封装数据库操作，包括查询、更新、删除等；不同条件的拼接查询应该统一在dao中封装，service层只负责调用。
com.cky.proxy.server.util: 负责封装一些通用的工具类，如json序列化、事件总线封装等。

### 事件总线
com.cky.proxy.server.util.EventBusUtil: 对Vertx事件总线的封装，管理所有事件类型与事件的分发订阅。所有子定义事件的分发订阅都应该通过该类中进行，方便统一管理。
事件类型分为数据管理事件和socket通信事件；
数据管理事件：事件类型应以db.xx开头
socket通信事件：事件类型应以socket.xx开头
