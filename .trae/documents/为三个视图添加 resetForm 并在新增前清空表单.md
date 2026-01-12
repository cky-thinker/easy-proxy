目标：在以下三个视图中新增 resetForm 方法，并在“新增”流程中先清空表单与校验状态后再打开新增弹窗。

涉及文件：
- easy-proxy-web/src/views/UserManageView.vue
- easy-proxy-web/src/views/ClientManageView.vue
- easy-proxy-web/src/views/ClientRulesView.vue

实现要点：
1) UserManageView.vue
- 新增方法 resetForm：重置 currentUser 为默认空对象；调用 userFormRef.clearValidate() 清空校验。
- 在 openAddModal 中首先调用 resetForm，然后再设置 showAddModal/showEditModal/showClientModal。
- 去掉 openAddModal 内对 currentUser 的重复赋值。

2) ClientManageView.vue
- 新增方法 resetForm：重置 currentClient 为默认空对象；调用 clientFormRef.clearValidate()。
- 在 openAddModal 中首先调用 resetForm，然后再设置 showAddModal/showEditModal/showClientModal。
- 去掉 openAddModal 内对 currentClient 的重复赋值。

3) ClientRulesView.vue
- 新增方法 resetForm：重置 newRule 为默认空对象（proxyClientId 取当前 queryForm.clientFilter）；调用 addRuleFormRef.clearValidate()。
- 在 openAddRuleModal 中首先调用 resetForm，然后再设置 showAddRuleModal。
- 去掉 openAddRuleModal 内对 newRule 的重复赋值。

验证：
- 运行前端类型检查确保通过。
- 手动点击三处“新增”入口，表单应为空且无历史校验红框。