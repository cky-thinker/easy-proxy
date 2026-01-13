# EventBus Refactoring Plan

Refactor the EventBus usage to be centrally managed by `EventBusUtil`, replacing hardcoded event strings with constants and using the utility class for publishing and subscribing.

## 1. Enhance `EventBusUtil`
Add missing event constants to `f:\workspace\easy-proxy\easy-proxy-server\src\main\java\com\cky\proxy\server\util\EventBusUtil.java`:
-   `DB_CLIENT_UPDATE = "db.client.update"`
-   `DB_RULE_UPDATE = "db.rule.update"`

## 2. Refactor `ProxyClientController`
Modify `f:\workspace\easy-proxy\easy-proxy-server\src\main\java\com\cky\proxy\server\controller\ProxyClientController.java`:
-   **Subscription**: In `initEventBus()`, replace `vertx.eventBus().consumer("proxy.client.status", ...)` with `EventBusUtil.subscribe` listening to `EventBusUtil.SOCKET_CLIENT_ONLINE` and `EventBusUtil.SOCKET_CLIENT_OFFLINE`.
-   **Publication**:
    -   In `updateProxyClient()`, replace `vertx.eventBus().publish("proxy.client.updated", ...)` with `EventBusUtil.publish(EventBusUtil.DB_CLIENT_UPDATE, ...)`.
    -   In `deleteProxyClient()`, replace `vertx.eventBus().publish("proxy.client.deleted", ...)` with `EventBusUtil.publish(EventBusUtil.DB_CLIENT_DELETE, ...)`.

## 3. Refactor `ProxyClientRuleController`
Modify `f:\workspace\easy-proxy\easy-proxy-server\src\main\java\com\cky\proxy\server\controller\ProxyClientRuleController.java`:
-   **Publication**:
    -   In `addProxyClientRule()` and `updateProxyClientRule()`, replace `vertx.eventBus().publish("proxy.rule.updated", ...)` with `EventBusUtil.publish(EventBusUtil.DB_RULE_UPDATE, ...)`.
    -   In `deleteProxyClientRule()`, replace `vertx.eventBus().publish("proxy.rule.deleted", ...)` with `EventBusUtil.publish(EventBusUtil.DB_RULE_DELETE, ...)`.

## 4. Refactor `ProxyServerVerticle`
Modify `f:\workspace\easy-proxy\easy-proxy-server\src\main\java\com\cky\proxy\server\verticle\ProxyServerVerticle.java`:
-   **Subscription**: In `registerEventConsumers()`, replace all `vertx.eventBus().consumer(...)` calls with `EventBusUtil.subscribe(...)` using the corresponding constants:
    -   `"proxy.rule.updated"` -> `EventBusUtil.DB_RULE_UPDATE`
    -   `"proxy.rule.deleted"` -> `EventBusUtil.DB_RULE_DELETE`
    -   `"proxy.client.updated"` -> `EventBusUtil.DB_CLIENT_UPDATE`
    -   `"proxy.client.deleted"` -> `EventBusUtil.DB_CLIENT_DELETE`

## 5. Refactor `ServerMngSocketHandler`
Modify `f:\workspace\easy-proxy\easy-proxy-server\src\main\java\com\cky\proxy\server\socket\ServerMngSocketHandler.java`:
-   **Publication**:
    -   In `handleClose()`, replace `vertx.eventBus().publish("proxy.client.status", ...)` with `EventBusUtil.publish(EventBusUtil.SOCKET_CLIENT_OFFLINE, ...)`.
    -   In `processAuth()`, replace `vertx.eventBus().publish("proxy.client.status", ...)` with `EventBusUtil.publish(EventBusUtil.SOCKET_CLIENT_ONLINE, ...)`.
