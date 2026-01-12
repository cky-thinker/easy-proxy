下面是我对“前端 api 和后端接口保持统一”的对齐方案，默认以后端现有路由实现为标准来调整前端。

## 一、梳理当前前后端接口差异

**后端 UserController 路由（节选）**  `easy-proxy-server/src/main/java/com/cky/proxy/server/controller/UserController.java:22-40`
- `GET   /api/users` → `getUsersPageable`：分页 + 可选 `q`、`enableFlag` 过滤
- `GET   /api/users/detail` → `getUserDetail`：需要 `id` 参数
- `POST  /api/users` → `addUser`：创建账号（请求体 `SysUser`）
- `PUT   /api/users` → `updateUser`：更新账号（请求体 `SysUser`，必须有 `id`）
- `DELETE /api/users` → `deleteUser`：需要 `id` 参数
- `POST  /api/users/reset-password` → `resetPassword`：请求体需要 `id` + `password`
- `PUT   /api/users/enableFlag` → `updateEnableFlag`：请求体需要 `id` + `enableFlag`
- `GET   /api/users/permissions` → `getPermissions`：返回静态权限列表

**前端 user.ts 当前实现**  `easy-proxy-web/src/api/user.ts:1-82`
- `getUsers(params)`：`GET /api/users`，携带 `page/pageSize/q/enableFlag` → 与后端对齐
- `getUser(id)`：`GET /api/users`（没有走 `/detail`，也没带 `id`）→ 与后台不一致
- `createUser`：`POST /api/users` → 对齐
- `updateUser`：`PUT /api/users` → 对齐
- `deleteUser(id)`：`DELETE /api/users`，没有传 `id` → 与后台不一致
- `deleteUsers(ids)`：`POST /api/users/batch-delete` → 后端不存在该路由
- `resetUserPassword(id, newPassword)`：`POST /api/users/reset-password`，只发 `{ password }`，未带 `id` → 与后台不一致
- `toggleUserEnableFlag(id, enableFlag)`：`PUT /api/users/enableFlag`，请求体 `{ id, enableFlag }` → 对齐
- `getPermissions()`：`GET /api/permissions` → 后端路径是 `/api/users/permissions`
- `updateUserPermissions(id, permissions)`：`PATCH /api/users/permissions` → 后端没有对应更新接口
- `searchUsers(...)`：`GET /api/users/search?...` → 后端没有 `/api/users/search`，分页查询已经由 `GET /api/users` + 查询参数实现

**这些 API 在前端的使用情况**  `easy-proxy-web/src/views/UserManageView.vue`
- 实际用到的只有：`getUsersApi`, `createUserApi`, `updateUserApi`, `toggleUserEnableFlagApi`，都已与后端对齐
- `getUser`, `deleteUser`, `deleteUsers`, `resetUserPassword`, `getPermissions`, `updateUserPermissions`, `searchUsers` 目前都未被实际调用（有一块权限管理 UI 现在是用前端本地 `permissions` 常量，而不是调后端）

## 二、对齐策略选择

- 以**后端现有接口**为标准，调整前端 `user.ts` 的 URL 和请求体，避免“虽然没用，但写错”的代码继续留在项目里导致误导。
- 对于前端定义但后端没有的接口：
  - 如果当前没有实际使用（如批量删除、搜索、更新权限），可以暂时删除或标注为“保留将来扩展”。
  - 如果你计划后续要做这些功能，可在后端新增对应路由；当前版本先保证不会有“假接口”。

## 三、具体前端修改方案（对齐到后端）

1. **`getUser(id)` 对齐 `/api/users/detail`**
- 目标：按 `id` 获取单个账号详情。
- 调整方式：
  - URL 改为 `/api/users/detail`
  - 将 `id` 通过查询参数传递，例如：`apiClient.get('/api/users/detail', { params: { id } })`

2. **`deleteUser(id)` 对齐 `DELETE /api/users`**
- 目标：删除指定 id 的账号。
- 调整方式：
  - 调用 `DELETE /api/users` 时，加上 `id` 查询参数，例如：`apiClient.delete('/api/users', { params: { id } })`

3. **`resetUserPassword(id, newPassword)` 对齐 `POST /api/users/reset-password`**
- 目标：符合后端对请求体的校验：需要 `id` 和 `password`。
- 调整方式：
  - 请求体从 `{ password: newPassword }` 改为 `{ id, password: newPassword }`

4. **`getPermissions()` 对齐 `GET /api/users/permissions`**
- 目标：使用后端返回的静态权限列表，而不是无效的 `/api/permissions`。
- 调整方式：
  - URL 由 `'/api/permissions'` 改为 `'/api/users/permissions'`
- 后续如果你希望账号管理页真正使用服务端权限：
  - 在 `UserManageView.vue` 中替换当前本地 `permissions` 常量为调用 `getPermissions()` 的结果。

5. **`updateUserPermissions` / `deleteUsers` / `searchUsers` 的处理**
- 后端目前没有对应路由：
  - `POST /api/users/batch-delete`（批量删除）
  - `PATCH /api/users/permissions`（更新权限）
  - `GET /api/users/search`（专用搜索）
- 前端也没有调用这些 API。

有两种可选方案：
- A. **精简方案（推荐作为当前迭代）**
  - 删除 `deleteUsers`, `updateUserPermissions`, `searchUsers` 这几个未使用、无后端支持的 API 函数。
  - 保持 `UserManageView.vue` 里权限配置继续使用本地常量（功能已可用）。
- B. **扩展方案**（如果你确实需要这些能力）
  - 在后端 `UserController` 和 `UserService` 中新增对应实现，然后前端保留并完善这些调用。

目前看你的需求是先“保持前后端统一”，所以我会优先推荐方案 A，保持代码简洁、无“假接口”。

## 四、与前端页面逻辑的配合

- 核心列表和增删改逻辑（账号管理页）已经与后端匹配：
  - 列表：`getUsersApi` → `GET /api/users`，带 `page/pageSize/q/enableFlag` 参数。
  - 新增：`createUserApi` → `POST /api/users`。
  - 更新：`updateUserApi` → `PUT /api/users`。
  - 状态切换：`toggleUserEnableFlagApi` → `PUT /api/users/enableFlag`。
- 本次对齐主要是：
  - 修正“预留接口”的 URL 和请求体，使其与后端语义完全一致。
  - 清理没有后端支持、也未使用的接口，减少未来踩坑风险。

## 五、后续可选优化方向（按需）

在以上对齐完成之后，如果你希望进一步增强功能，可以按下面的优先级考虑：

1. **真正接入服务端权限管理**
- 使用 `getPermissions()` 渲染权限列表
- 在后端新增 `PATCH /api/users/permissions`，前端 `updateUserPermissions` 调用它

2. **批量删除账号**
- 后端新增 `POST /api/users/batch-delete`（或更 REST 的路径），接受 id 列表
- 前端保留/实现 `deleteUsers(ids)`，并在账号管理页增加多选 + 批量删除按钮

3. **单独的搜索接口（可选）**
- 如果现有分页接口性能够用，可以一直用 `GET /api/users` + 查询参数；否则再考虑专门的 `/api/users/search`。

---
如果你确认以上方向，我下一步会：
- 按上述第“三”部分的具体修改方案更新 `user.ts`
- 根据你选择的方案（A 精简 / B 扩展）决定是否删除未使用接口或扩展后端路由
- 在完成后跑一次前端类型检查和基础页面验证，确保账号管理功能仍然正常。