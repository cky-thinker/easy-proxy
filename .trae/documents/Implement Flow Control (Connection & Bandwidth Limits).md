I will implement flow control (connection limits and bandwidth limits) for proxy rules.

### Backend Implementation
1.  **Entity Update**:
    -   Modify `ProxyClientRule.java` to add `limitConn` (connection limit) and `limitRate` (bandwidth limit in KB/s) fields.
    -   Add corresponding database annotations.

2.  **Database Migration**:
    -   Update `BeanContext.java` to include a migration step that executes SQL to add `limit_conn` and `limit_rate` columns to the `proxy_client_rule` table if they don't exist.

3.  **Traffic Statistics Manager**:
    -   Update `TrafficStatisticManager.java` to track active connections per rule.
    -   Add methods `addConnection(userId, ruleId)`, `removeConnection(userId)`, and `getActiveConnections(ruleId)`.
    -   Add method `isRateExceeded(ruleId, limitRate, bytes)` to check if bandwidth limit is exceeded for the current second.

4.  **Flow Control Logic**:
    -   Update `UserProxySocketHandler.java`:
        -   **Connection Limit**: In `handle()`, check if active connections exceed `limitConn`. If so, close the connection.
        -   **Bandwidth Limit**: In `processRead()`, check `isRateExceeded`. If exceeded, pause the socket and resume it after a delay (1 second) to throttle the speed.

### Frontend Implementation
1.  **Type Definitions**:
    -   Update `src/api/types.ts` to include `limitConn` and `limitRate` in `ProxyRule` interface.

2.  **UI Update**:
    -   Update `src/views/ClientRulesView.vue`:
        -   Add input fields for "Connection Limit" and "Bandwidth Limit" in both "Add Rule" and "Edit Rule" modals.
        -   Update table columns to display these limits (optional but good for visibility).

### Verification
-   I will verify the changes by creating a new rule with limits and checking if the fields are saved correctly.
-   I will check if the database columns are created.
