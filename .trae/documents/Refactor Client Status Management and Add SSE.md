I will refactor the code to encapsulate database operations and implement real-time status updates via SSE.

### 1. Refactor DAO
*   **File**: `src/main/java/com/cky/proxy/server/dao/ProxyClientDao.java`
*   **Action**: Add a `selectByToken(String token)` method to encapsulate the query logic using `selectList`.

### 2. Refactor Service
*   **File**: `src/main/java/com/cky/proxy/server/service/ProxyClientService.java`
*   **Action**: Add a `updateClientStatus(String token, String status)` method.
    *   This method will find the client by token, update its status and `updateTime`, and save it to the database.
    *   It will return the updated `ProxyClient` object.

### 3. Implement SSE in Controller
*   **File**: `src/main/java/com/cky/proxy/server/controller/ProxyClientController.java`
*   **Action**:
    *   Add a new route `GET /api/proxyClient/subscribe` for Server-Sent Events (SSE).
    *   Maintain a set of active SSE connections (`Set<HttpServerResponse>`).
    *   Subscribe to the Vert.x EventBus address `proxy.client.status`.
    *   When a message is received on this address, broadcast it to all connected SSE clients.

### 4. Update Socket Handler
*   **File**: `src/main/java/com/cky/proxy/server/socket/ServerMngSocketHandler.java`
*   **Action**:
    *   Instantiate `ProxyClientService`.
    *   Replace the direct DAO query code with `proxyClientDao.selectByToken(token)`.
    *   Replace the status update code with `proxyClientService.updateClientStatus(token, "online"/"offline")`.
    *   After updating the status, use `sMngSocket.owner().eventBus().publish("proxy.client.status", client)` to notify the controller.

### 5. Dependency Update (Optional but recommended)
*   Ensure `JsonUtil` is available for serializing objects for SSE. (Already exists in imports).

This plan addresses all requirements: DAO encapsulation, Service encapsulation, and SSE real-time notifications.
