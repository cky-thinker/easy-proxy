# ProxyClientService Update Plan

I will modify `ProxyClientService.java` to implement cascading deletion, system logging, and event publishing as requested.

## Technical Implementation

### 1. Dependency Injection
Add the following DAOs to `ProxyClientService` using `BeanContext`:
- `ProxyClientRuleDao`: For deleting associated rules.
- `SysLogDao`: For recording system logs.

### 2. Implement `deleteProxyClient`
- **Cascade Delete**: Before deleting the client, delete all associated rules using `proxyClientRuleDao`.
- **Event Publishing**: Publish `EventBusUtil.DB_CLIENT_DELETE` event with the client ID to notify `ProxyServerVerticle` to stop running services.
- **System Logging**: Record a "CLIENT_DELETE" log entry with the client name.

### 3. Implement `updateProxyClient`
- **Event Publishing**: Detect changes to critical fields (token, enableFlag) and publish `EventBusUtil.DB_CLIENT_UPDATE` event with the client ID. This ensures the server restarts listeners with the new configuration.

### 4. Implement `updateClientStatus`
- **Event Publishing**: Publish `EventBusUtil.DB_CLIENT_UPDATE` event after updating the status to keep the system synchronized.

## Verification
- Verify that deleting a client removes its rules from the database.
- Verify that system logs are created upon deletion.
- Verify that events are published correctly (though full runtime verification of Vert.x event bus requires a running environment, code logic will be checked).
