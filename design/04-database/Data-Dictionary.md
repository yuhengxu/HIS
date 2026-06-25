# 数据字典

## 1. 设计依据

- `README.MD` §3.1、§3.3、§3.5
- `SAD.md` §2、§3、§4
- `Permission-Design.md` §1、§2、§3

## 2. IAM 账户与权限

### `iam_user`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 用户主键 |
| `username` | varchar(64) | 登录名，全局唯一 |
| `display_name` | varchar(80) | 显示名称 |
| `phone` | varchar(32) | 手机号 |
| `status` | varchar(20) | `enabled`、`disabled` |
| `created_at` | timestamptz | 创建时间 |
| `updated_at` | timestamptz | 更新时间 |
| `deleted_at` | timestamptz | 软删除时间 |

用途：统一账户体系用户表。生命周期：用户离职或停用后保留审计数据，使用软删除。

### `iam_role`

用途：角色定义。核心字段：`id`、`code`、`name`、`description`、`created_at`、`updated_at`、`deleted_at`。

### `iam_permission`

用途：权限点定义。核心字段：`id`、`code`、`name`、`domain`、`resource_type`、`description`、`created_at`、`updated_at`。

### `iam_user_role`

用途：用户与角色关系。核心字段：`id`、`user_id`、`role_id`、`created_at`。

### `iam_role_permission`

用途：角色权限关系。核心字段：`id`、`role_id`、`permission_id`、`created_at`。

### `iam_user_permission_override`

用途：用户单独权限加授/撤销。核心字段：`id`、`user_id`、`permission_id`、`effect`、`reason`、`created_at`。

## 3. OA 基座

### `oa_permission_group`

用途：OA 权限分组，支撑菜单与权限配置。核心字段：`id`、`code`、`name`、`sort_order`、`created_at`、`updated_at`。

## 4. 物资管理

### `inv_category`

用途：物资分类。核心字段：`id`、`parent_id`、`code`、`name`、`created_at`、`updated_at`、`deleted_at`。

### `inv_item`

用途：物资档案。核心字段：`id`、`category_id`、`code`、`name`、`specification`、`unit`、`safety_stock`、`status`、`created_at`、`updated_at`、`deleted_at`。主键 `id` 使用数据库自增 `bigserial`。

新增字段：`material_tag`，取值 `NORMAL` / `SPECIAL`，分别表示普通物资与特殊物资。仅 `SYSTEM_ADMIN` 可查看和修改该标签；`INVENTORY_ADMIN` 只能查看普通物资记录。

### `inv_supplier`

用途：供应商档案。核心字段：`id`、`code`、`name`、`contact_name`、`contact_phone`、`status`、`created_at`、`updated_at`、`deleted_at`。

### `inv_warehouse`

用途：仓库档案。核心字段：`id`、`code`、`name`、`location`、`status`、`created_at`、`updated_at`、`deleted_at`。

### `inv_stock`

用途：当前库存余额。核心字段：`id`、`warehouse_id`、`item_id`、`quantity`、`owner_user_id`、`updated_at`。主键 `id` 使用数据库自增 `bigserial`。`owner_user_id` 表示库存录入人/归属人；首期库存查询按 `owner_user_id` 过滤，仅返回本人入库或本人初始化的库存，`SYSTEM_ADMIN` 例外可查看全部。

物资库总库存不单独建表，首期由 `inv_stock` 按 `warehouse_id` + `item_id` 汇总得到；物资明细库对应 `inv_stock` 明细行。物品领用审批通过时按总库存校验，并按明细行入库顺序扣减。

### `inv_stock_txn`

用途：库存流水，记录入库、出库、盘点差异。核心字段：`id`、`txn_type`、`biz_no`、`warehouse_id`、`item_id`、`quantity_delta`、`operator_user_id`、`occurred_at`。`operator_user_id` 表示本次库存变动录入/触发人；流水查询按 `operator_user_id` 隔离，`SYSTEM_ADMIN` 例外可查看全部。

`txn_type=ADJUST` 表示系统管理员不经过 OA 的直接库存调整，必须保留流水和操作审计。

### `inv_inbound_order`

用途：入库单主表。核心字段：`id`、`order_no`、`supplier_id`、`warehouse_id`、`status`、`created_by`、`created_at`、`approved_at`。

### `inv_outbound_order`

用途：出库单主表。核心字段：`id`、`order_no`、`warehouse_id`、`status`、`created_by`、`created_at`、`approved_at`。

### `inv_stocktake_order`

用途：盘点单主表。核心字段：`id`、`order_no`、`warehouse_id`、`status`、`created_by`、`created_at`、`completed_at`。

## 5. 审计与 AI

### `audit_operation_log`

用途：操作审计。核心字段：`id`、`actor_user_id`、`action`、`resource_type`、`resource_id`、`request_id`、`created_at`。

### `audit_permission_change_log`

用途：权限变更审计。核心字段：`id`、`actor_user_id`、`target_user_id`、`target_role_id`、`permission_id`、`change_type`、`reason`、`created_at`。

### `ai_api_allowlist`

用途：AI 可调用接口白名单。核心字段：`id`、`api_code`、`method`、`path`、`scope`、`enabled`、`created_at`、`updated_at`。

### `ai_call_log`

用途：AI 调用审计。核心字段：`id`、`caller`、`api_code`、`scope`、`status`、`request_summary`、`created_at`。

## 6. 第一阶段 IAM 扩展

### `iam_user` 新增字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `password_hash` | varchar(255) | 登录认证密码哈希 |
| `report_to_user_id` | bigint | 汇报上级用户 ID，用于 OA 上级审批 |
| `wecom_user_id` | varchar(120) | 企业微信用户 ID |
| `department_name` | varchar(120) | 首期简化部门字段 |
| `last_login_at` | timestamptz | 最近登录时间 |

初始密码规则：管理员用户初始密码为 `1qaz@WSX`，其他成员初始密码为 `qwer1234`；数据库只保存密码哈希，不保存明文。

## 7. 第一阶段 OA 流程表

| 表 | 用途 | 生命周期 |
|---|---|---|
| `oa_process_definition` | 流程定义，如入库、出库、报销 | 版本化，禁用不删除 |
| `oa_process_node` | 流程节点配置 | 随流程版本保存 |
| `oa_process_node_assignee` | 节点指定用户/角色处理人 | 随节点保存 |
| `oa_process_instance` | 流程实例 | 归档保留审计 |
| `oa_task` | 待办任务 | 完成后保留 |
| `oa_task_action_log` | 任务动作日志 | 永久审计 |
| `oa_form_data` | JSONB 表单数据 | 随流程实例保留 |
| `oa_reminder_policy` | 提醒策略 | 可配置启停 |
| `oa_reminder_log` | 自动提醒日志 | 永久审计 |
| `oa_urge_log` | 手动催办日志 | 永久审计 |
| `oa_wecom_robot_config` | 企业微信机器人配置 | Webhook 加密存储 |
| `oa_wecom_message_log` | 企业微信消息日志 | 永久审计 |

关键字段按 `plans/oa20260623.plan.md` §4.2 定义，节点处理模式为 `USER`、`ROLE`、`SUPERVISOR`。

## 8. 第一阶段物资扩展

| 表/字段 | 用途 |
|---|---|
| `inv_warehouse.warehouse_type` | `medical` / `non_medical` |
| `inv_item.item_type` | 医疗/非医疗用品类型 |
| `inv_item.default_price` | 默认参考价格 |
| `inv_item.latest_price` | 最新采购/入库价格 |
| `inv_price_record` | 价格记录，支持采购价、报销价、参考价 |
| `inv_inbound_order_line` | 入库单明细 |
| `inv_outbound_order_line` | 出库单明细 |
| `inv_stocktake_order_line` | 盘点单明细 |
| `inv_reimbursement_record` | 报销记录，关联报销 OA 与入库 OA |
| `inv_inbound_order.oa_instance_id` | 入库单关联 OA 实例 |
| `inv_outbound_order.oa_instance_id` | 出库单关联 OA 实例 |
| `inv_inbound_order.total_amount` | 入库总金额 |
| `inv_outbound_order.total_amount` | 出库总金额 |
| `inv_inbound_order.reimbursement_linked` | 是否已被报销单关联，报销下拉仅显示未关联入库单 |

## 9. IAM / 附件 / OA 物资草稿

依据：`plans/oa20260624-iam-oa-inventory-enhancement.plan.md` §3.3-§3.4。

| 表/字段 | 说明 |
|---|---|
| `iam_role.status` / `sort_order` / `is_system` / `deleted_at` | 角色启停、排序、系统内置保护、软删除 |
| `iam_permission.name` / `description` / `domain` / `resource_type` / `is_system` / `enabled` | 权限点说明与启停 |
| `iam_role_permission` | 角色权限关系，含唯一约束与审计字段 |
| `file_attachment` | 通用附件：业务类型、用途、存储路径、哈希、上传人 |
| `inv_item_image` | 物资图片关联 |
| `oa_instance_attachment` | OA 实例附件关联 |
| `inv_reimbursement_voucher` | 报销凭证关联 |
| `oa_form_material_draft` | OA 表单内新物资草稿 |

## 10. 流程定义、菜单与库存图片（本轮补齐）

依据：`plans/oa20260624-feature-oa-process-menu-image-fix.plan.md` §4。

| 表/字段 | 说明 |
|---|---|
| `oa_process_definition.status/version/form_schema/is_builtin` | 流程版本化与发布状态 |
| `oa_process_node.approve_policy/reject_policy/assignee_mode` | 节点灵活配置 |
| `sys_menu` | 菜单树：编码、路径、组件、图标、权限点绑定 |
| `iam_role_menu` | 角色可见菜单 |
| `iam_user_menu_override` | 用户菜单加授/撤销 |
| `inv_item_image` | 物资主图/附图关联 |
| `inv_stock_image` | 库存现场图关联 |
