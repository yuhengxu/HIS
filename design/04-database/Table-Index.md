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

## 5. 第一阶段新增表与索引

| 表 | 主键 | 唯一索引 | 普通索引 |
|---|---|---|---|
| `oa_process_definition` | `pk_oa_process_definition` | `uk_oa_process_definition_code_version` | `idx_oa_process_definition_enabled` |
| `oa_process_node` | `pk_oa_process_node` | `uk_oa_process_node_definition_code` | `idx_oa_process_node_definition_sort` |
| `oa_process_node_assignee` | `pk_oa_process_node_assignee` | - | `idx_oa_process_node_assignee_node` |
| `oa_process_instance` | `pk_oa_process_instance` | - | `idx_oa_process_instance_initiator_status`、`idx_oa_process_instance_business` |
| `oa_task` | `pk_oa_task` | - | `idx_oa_task_status_user`、`idx_oa_task_status_role`、`idx_oa_task_instance` |
| `oa_task_action_log` | `pk_oa_task_action_log` | - | `idx_oa_task_action_log_task` |
| `oa_form_data` | `pk_oa_form_data` | `uk_oa_form_data_instance` | - |
| `oa_reminder_policy` | `pk_oa_reminder_policy` | - | `idx_oa_reminder_policy_definition_node` |
| `oa_reminder_log` | `pk_oa_reminder_log` | - | `idx_oa_reminder_log_task` |
| `oa_urge_log` | `pk_oa_urge_log` | - | `idx_oa_urge_log_instance_time` |
| `oa_wecom_robot_config` | `pk_oa_wecom_robot_config` | - | `idx_oa_wecom_robot_config_enabled` |
| `oa_wecom_message_log` | `pk_oa_wecom_message_log` | - | `idx_oa_wecom_message_log_task` |
| `inv_price_record` | `pk_inv_price_record` | - | `idx_inv_price_record_item_time` |
| `inv_inbound_order_line` | `pk_inv_inbound_order_line` | - | `idx_inv_inbound_order_line_order` |
| `inv_outbound_order_line` | `pk_inv_outbound_order_line` | - | `idx_inv_outbound_order_line_order` |
| `inv_stocktake_order_line` | `pk_inv_stocktake_order_line` | - | `idx_inv_stocktake_order_line_order` |
| `inv_reimbursement_record` | `pk_inv_reimbursement_record` | - | `idx_inv_reimbursement_record_oa`、`idx_inv_reimbursement_record_inbound` |

## 6. 增强版索引

| 表 | 主键 | 唯一索引 | 普通索引 |
|---|---|---|---|
| `file_attachment` | `pk_file_attachment` | - | `idx_file_attachment_biz` |
| `inv_item_image` | `pk_inv_item_image` | - | `idx_inv_item_image_item` |
| `oa_instance_attachment` | `pk_oa_instance_attachment` | - | `idx_oa_instance_attachment_instance` |
| `inv_reimbursement_voucher` | `pk_inv_reimbursement_voucher` | - | `idx_reimbursement_voucher_record` |
| `oa_form_material_draft` | `pk_oa_form_material_draft` | - | `idx_oa_form_material_draft_instance` |
