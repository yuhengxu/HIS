# 数据表与索引清单

## 1. IAM

| 表 | 主键 | 唯一索引 | 普通索引 |
|---|---|---|---|
| `iam_user` | `pk_iam_user` | `uk_iam_user_username` | `idx_iam_user_status` |
| `iam_role` | `pk_iam_role` | `uk_iam_role_code` | - |
| `iam_permission` | `pk_iam_permission` | `uk_iam_permission_code` | `idx_iam_permission_domain` |
| `iam_user_role` | `pk_iam_user_role` | `uk_iam_user_role_user_role` | `idx_iam_user_role_role_id` |
| `iam_role_permission` | `pk_iam_role_permission` | `uk_iam_role_permission_role_permission` | `idx_iam_role_permission_permission_id` |
| `iam_user_permission_override` | `pk_iam_user_permission_override` | `uk_iam_user_permission_override_user_permission` | `idx_iam_user_permission_override_effect` |

## 2. OA

| 表 | 主键 | 唯一索引 | 普通索引 |
|---|---|---|---|
| `oa_permission_group` | `pk_oa_permission_group` | `uk_oa_permission_group_code` | `idx_oa_permission_group_sort_order` |

## 3. Inventory

| 表 | 主键 | 唯一索引 | 普通索引 |
|---|---|---|---|
| `inv_category` | `pk_inv_category` | `uk_inv_category_code` | `idx_inv_category_parent_id` |
| `inv_item` | `pk_inv_item` | `uk_inv_item_code` | `idx_inv_item_category_id` |
| `inv_supplier` | `pk_inv_supplier` | `uk_inv_supplier_code` | `idx_inv_supplier_status` |
| `inv_warehouse` | `pk_inv_warehouse` | `uk_inv_warehouse_code` | `idx_inv_warehouse_status` |
| `inv_stock` | `pk_inv_stock` | `uk_inv_stock_warehouse_item` | `idx_inv_stock_item_id` |
| `inv_stock_txn` | `pk_inv_stock_txn` | - | `idx_inv_stock_txn_biz_no`、`idx_inv_stock_txn_item_time` |
| `inv_inbound_order` | `pk_inv_inbound_order` | `uk_inv_inbound_order_no` | `idx_inv_inbound_order_status` |
| `inv_outbound_order` | `pk_inv_outbound_order` | `uk_inv_outbound_order_no` | `idx_inv_outbound_order_status` |
| `inv_stocktake_order` | `pk_inv_stocktake_order` | `uk_inv_stocktake_order_no` | `idx_inv_stocktake_order_status` |

## 4. Audit And AI

| 表 | 主键 | 唯一索引 | 普通索引 |
|---|---|---|---|
| `audit_operation_log` | `pk_audit_operation_log` | - | `idx_audit_operation_log_actor_time`、`idx_audit_operation_log_resource` |
| `audit_permission_change_log` | `pk_audit_permission_change_log` | - | `idx_audit_permission_change_log_target_time` |
| `ai_api_allowlist` | `pk_ai_api_allowlist` | `uk_ai_api_allowlist_api_code` | `idx_ai_api_allowlist_scope` |
| `ai_call_log` | `pk_ai_call_log` | - | `idx_ai_call_log_api_time` |
