-- Based on plans/oa20260623.plan.md and design/04-database/*.

ALTER TABLE iam.iam_user ADD COLUMN IF NOT EXISTS password_hash varchar(255);
ALTER TABLE iam.iam_user ADD COLUMN IF NOT EXISTS report_to_user_id bigint REFERENCES iam.iam_user(id);
ALTER TABLE iam.iam_user ADD COLUMN IF NOT EXISTS wecom_user_id varchar(120);
ALTER TABLE iam.iam_user ADD COLUMN IF NOT EXISTS department_name varchar(120);
ALTER TABLE iam.iam_user ADD COLUMN IF NOT EXISTS last_login_at timestamptz;
CREATE INDEX IF NOT EXISTS idx_iam_user_report_to_user_id ON iam.iam_user(report_to_user_id);

ALTER TABLE inventory.inv_warehouse ADD COLUMN IF NOT EXISTS warehouse_type varchar(30) NOT NULL DEFAULT 'non_medical';
CREATE INDEX IF NOT EXISTS idx_inv_warehouse_type ON inventory.inv_warehouse(warehouse_type);

ALTER TABLE inventory.inv_stock ADD COLUMN IF NOT EXISTS owner_user_id bigint REFERENCES iam.iam_user(id);
CREATE INDEX IF NOT EXISTS idx_inv_stock_owner_user_id ON inventory.inv_stock(owner_user_id);

ALTER TABLE inventory.inv_item ADD COLUMN IF NOT EXISTS item_type varchar(30) NOT NULL DEFAULT 'non_medical';
ALTER TABLE inventory.inv_item ADD COLUMN IF NOT EXISTS default_price numeric(18, 2) NOT NULL DEFAULT 0;
ALTER TABLE inventory.inv_item ADD COLUMN IF NOT EXISTS latest_price numeric(18, 2) NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_inv_item_type ON inventory.inv_item(item_type);

ALTER TABLE inventory.inv_inbound_order ADD COLUMN IF NOT EXISTS oa_instance_id bigint;
ALTER TABLE inventory.inv_inbound_order ADD COLUMN IF NOT EXISTS total_amount numeric(18, 2) NOT NULL DEFAULT 0;
ALTER TABLE inventory.inv_inbound_order ADD COLUMN IF NOT EXISTS reimbursement_linked boolean NOT NULL DEFAULT false;
CREATE INDEX IF NOT EXISTS idx_inv_inbound_order_oa_instance_id ON inventory.inv_inbound_order(oa_instance_id);
CREATE INDEX IF NOT EXISTS idx_inv_inbound_order_created_reimbursement ON inventory.inv_inbound_order(created_by, reimbursement_linked);

ALTER TABLE inventory.inv_outbound_order ADD COLUMN IF NOT EXISTS oa_instance_id bigint;
ALTER TABLE inventory.inv_outbound_order ADD COLUMN IF NOT EXISTS total_amount numeric(18, 2) NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_inv_outbound_order_oa_instance_id ON inventory.inv_outbound_order(oa_instance_id);

CREATE TABLE IF NOT EXISTS oa.oa_process_definition (
  id bigserial CONSTRAINT pk_oa_process_definition PRIMARY KEY,
  code varchar(80) NOT NULL,
  name varchar(120) NOT NULL,
  version integer NOT NULL DEFAULT 1,
  enabled boolean NOT NULL DEFAULT true,
  description varchar(255),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uk_oa_process_definition_code_version UNIQUE (code, version)
);
CREATE INDEX IF NOT EXISTS idx_oa_process_definition_enabled ON oa.oa_process_definition(enabled);

CREATE TABLE IF NOT EXISTS oa.oa_process_node (
  id bigserial CONSTRAINT pk_oa_process_node PRIMARY KEY,
  process_definition_id bigint NOT NULL REFERENCES oa.oa_process_definition(id),
  node_code varchar(80) NOT NULL,
  node_name varchar(120) NOT NULL,
  node_type varchar(30) NOT NULL,
  sort_order integer NOT NULL,
  assignee_mode varchar(30) NOT NULL,
  require_self_only boolean NOT NULL DEFAULT true,
  supervisor_level integer NOT NULL DEFAULT 1,
  timeout_hours integer,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uk_oa_process_node_definition_code UNIQUE (process_definition_id, node_code)
);
CREATE INDEX IF NOT EXISTS idx_oa_process_node_definition_sort ON oa.oa_process_node(process_definition_id, sort_order);

CREATE TABLE IF NOT EXISTS oa.oa_process_node_assignee (
  id bigserial CONSTRAINT pk_oa_process_node_assignee PRIMARY KEY,
  node_id bigint NOT NULL REFERENCES oa.oa_process_node(id),
  assignee_user_id bigint REFERENCES iam.iam_user(id),
  assignee_role_id bigint REFERENCES iam.iam_role(id)
);
CREATE INDEX IF NOT EXISTS idx_oa_process_node_assignee_node ON oa.oa_process_node_assignee(node_id);

CREATE TABLE IF NOT EXISTS oa.oa_process_instance (
  id bigserial CONSTRAINT pk_oa_process_instance PRIMARY KEY,
  process_definition_id bigint NOT NULL REFERENCES oa.oa_process_definition(id),
  process_code varchar(80) NOT NULL,
  business_type varchar(80) NOT NULL,
  business_id bigint,
  title varchar(160) NOT NULL,
  initiator_user_id bigint NOT NULL REFERENCES iam.iam_user(id),
  status varchar(40) NOT NULL DEFAULT 'running',
  current_node_id bigint REFERENCES oa.oa_process_node(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  finished_at timestamptz
);
CREATE INDEX IF NOT EXISTS idx_oa_process_instance_initiator_status ON oa.oa_process_instance(initiator_user_id, status);
CREATE INDEX IF NOT EXISTS idx_oa_process_instance_business ON oa.oa_process_instance(business_type, business_id);

CREATE TABLE IF NOT EXISTS oa.oa_task (
  id bigserial CONSTRAINT pk_oa_task PRIMARY KEY,
  process_instance_id bigint NOT NULL REFERENCES oa.oa_process_instance(id),
  node_id bigint NOT NULL REFERENCES oa.oa_process_node(id),
  assignee_mode varchar(30) NOT NULL,
  assignee_user_id bigint REFERENCES iam.iam_user(id),
  assignee_role_id bigint REFERENCES iam.iam_role(id),
  resolved_supervisor_user_id bigint REFERENCES iam.iam_user(id),
  status varchar(30) NOT NULL DEFAULT 'pending',
  claimed_by_user_id bigint REFERENCES iam.iam_user(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  handled_at timestamptz,
  next_remind_at timestamptz,
  remind_count integer NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_oa_task_status_user ON oa.oa_task(status, assignee_user_id, resolved_supervisor_user_id);
CREATE INDEX IF NOT EXISTS idx_oa_task_status_role ON oa.oa_task(status, assignee_role_id);
CREATE INDEX IF NOT EXISTS idx_oa_task_instance ON oa.oa_task(process_instance_id);

CREATE TABLE IF NOT EXISTS oa.oa_task_action_log (
  id bigserial CONSTRAINT pk_oa_task_action_log PRIMARY KEY,
  task_id bigint NOT NULL REFERENCES oa.oa_task(id),
  process_instance_id bigint NOT NULL REFERENCES oa.oa_process_instance(id),
  actor_user_id bigint NOT NULL REFERENCES iam.iam_user(id),
  action varchar(30) NOT NULL,
  comment varchar(500),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_oa_task_action_log_task ON oa.oa_task_action_log(task_id);

CREATE TABLE IF NOT EXISTS oa.oa_form_data (
  id bigserial CONSTRAINT pk_oa_form_data PRIMARY KEY,
  process_instance_id bigint NOT NULL REFERENCES oa.oa_process_instance(id),
  form_json jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uk_oa_form_data_instance UNIQUE (process_instance_id)
);

CREATE TABLE IF NOT EXISTS oa.oa_reminder_policy (
  id bigserial CONSTRAINT pk_oa_reminder_policy PRIMARY KEY,
  process_definition_id bigint NOT NULL REFERENCES oa.oa_process_definition(id),
  node_id bigint REFERENCES oa.oa_process_node(id),
  enabled boolean NOT NULL DEFAULT true,
  interval_minutes integer NOT NULL DEFAULT 120,
  max_remind_count integer NOT NULL DEFAULT 3,
  cooldown_minutes_for_urge integer NOT NULL DEFAULT 30
);
CREATE INDEX IF NOT EXISTS idx_oa_reminder_policy_definition_node ON oa.oa_reminder_policy(process_definition_id, node_id);

CREATE TABLE IF NOT EXISTS oa.oa_reminder_log (
  id bigserial CONSTRAINT pk_oa_reminder_log PRIMARY KEY,
  task_id bigint NOT NULL REFERENCES oa.oa_task(id),
  process_instance_id bigint NOT NULL REFERENCES oa.oa_process_instance(id),
  target_user_id bigint REFERENCES iam.iam_user(id),
  reminder_type varchar(30) NOT NULL,
  status varchar(30) NOT NULL,
  error_message varchar(500),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_oa_reminder_log_task ON oa.oa_reminder_log(task_id);

CREATE TABLE IF NOT EXISTS oa.oa_urge_log (
  id bigserial CONSTRAINT pk_oa_urge_log PRIMARY KEY,
  process_instance_id bigint NOT NULL REFERENCES oa.oa_process_instance(id),
  task_id bigint NOT NULL REFERENCES oa.oa_task(id),
  urge_by_user_id bigint NOT NULL REFERENCES iam.iam_user(id),
  target_user_id bigint REFERENCES iam.iam_user(id),
  status varchar(30) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_oa_urge_log_instance_time ON oa.oa_urge_log(process_instance_id, created_at);

CREATE TABLE IF NOT EXISTS oa.oa_wecom_robot_config (
  id bigserial CONSTRAINT pk_oa_wecom_robot_config PRIMARY KEY,
  name varchar(120) NOT NULL,
  webhook_url_encrypted varchar(500) NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_oa_wecom_robot_config_enabled ON oa.oa_wecom_robot_config(enabled);

CREATE TABLE IF NOT EXISTS oa.oa_wecom_message_log (
  id bigserial CONSTRAINT pk_oa_wecom_message_log PRIMARY KEY,
  task_id bigint REFERENCES oa.oa_task(id),
  target_user_id bigint REFERENCES iam.iam_user(id),
  message_type varchar(40) NOT NULL,
  status varchar(30) NOT NULL,
  error_message varchar(500),
  sent_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_oa_wecom_message_log_task ON oa.oa_wecom_message_log(task_id);

CREATE TABLE IF NOT EXISTS inventory.inv_price_record (
  id bigserial CONSTRAINT pk_inv_price_record PRIMARY KEY,
  item_id bigint NOT NULL REFERENCES inventory.inv_item(id),
  price_type varchar(30) NOT NULL,
  price numeric(18, 2) NOT NULL,
  effective_at timestamptz NOT NULL DEFAULT now(),
  source_biz_type varchar(80),
  source_biz_id bigint,
  created_by bigint REFERENCES iam.iam_user(id),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_inv_price_record_item_time ON inventory.inv_price_record(item_id, effective_at);

CREATE TABLE IF NOT EXISTS inventory.inv_inbound_order_line (
  id bigserial CONSTRAINT pk_inv_inbound_order_line PRIMARY KEY,
  inbound_order_id bigint NOT NULL REFERENCES inventory.inv_inbound_order(id),
  item_id bigint NOT NULL REFERENCES inventory.inv_item(id),
  quantity numeric(18, 2) NOT NULL,
  unit_price numeric(18, 2) NOT NULL DEFAULT 0,
  amount numeric(18, 2) NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_inv_inbound_order_line_order ON inventory.inv_inbound_order_line(inbound_order_id);

CREATE TABLE IF NOT EXISTS inventory.inv_outbound_order_line (
  id bigserial CONSTRAINT pk_inv_outbound_order_line PRIMARY KEY,
  outbound_order_id bigint NOT NULL REFERENCES inventory.inv_outbound_order(id),
  item_id bigint NOT NULL REFERENCES inventory.inv_item(id),
  quantity numeric(18, 2) NOT NULL,
  unit_price numeric(18, 2) NOT NULL DEFAULT 0,
  amount numeric(18, 2) NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_inv_outbound_order_line_order ON inventory.inv_outbound_order_line(outbound_order_id);

CREATE TABLE IF NOT EXISTS inventory.inv_stocktake_order_line (
  id bigserial CONSTRAINT pk_inv_stocktake_order_line PRIMARY KEY,
  stocktake_order_id bigint NOT NULL REFERENCES inventory.inv_stocktake_order(id),
  item_id bigint NOT NULL REFERENCES inventory.inv_item(id),
  book_quantity numeric(18, 2) NOT NULL,
  actual_quantity numeric(18, 2) NOT NULL,
  difference_quantity numeric(18, 2) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_inv_stocktake_order_line_order ON inventory.inv_stocktake_order_line(stocktake_order_id);

CREATE TABLE IF NOT EXISTS inventory.inv_reimbursement_record (
  id bigserial CONSTRAINT pk_inv_reimbursement_record PRIMARY KEY,
  oa_instance_id bigint NOT NULL,
  related_inbound_order_id bigint REFERENCES inventory.inv_inbound_order(id),
  related_inbound_oa_instance_id bigint,
  applicant_user_id bigint NOT NULL REFERENCES iam.iam_user(id),
  amount numeric(18, 2) NOT NULL,
  status varchar(30) NOT NULL DEFAULT 'approved',
  created_at timestamptz NOT NULL DEFAULT now(),
  approved_at timestamptz
);
CREATE INDEX IF NOT EXISTS idx_inv_reimbursement_record_oa ON inventory.inv_reimbursement_record(oa_instance_id);
CREATE INDEX IF NOT EXISTS idx_inv_reimbursement_record_inbound ON inventory.inv_reimbursement_record(related_inbound_order_id);

INSERT INTO iam.iam_permission(code, name, domain, resource_type)
VALUES
  ('iam:user:create', '创建用户', 'IAM', 'api'),
  ('iam:user:update', '更新用户', 'IAM', 'api'),
  ('iam:user:disable', '禁用用户', 'IAM', 'api'),
  ('iam:role:write', '维护角色', 'IAM', 'api'),
  ('iam:permission:write', '维护权限', 'IAM', 'api'),
  ('iam:user-permission:write', '用户单独授权', 'IAM', 'api'),
  ('oa:process:read', '查看流程定义', 'OA', 'api'),
  ('oa:process:write', '维护流程定义', 'OA', 'api'),
  ('oa:instance:create', '发起流程', 'OA', 'api'),
  ('oa:instance:read', '查看流程', 'OA', 'api'),
  ('oa:task:read', '查看待办', 'OA', 'api'),
  ('oa:task:approve', '审批任务', 'OA', 'api'),
  ('oa:task:urge', '催办任务', 'OA', 'api'),
  ('oa:reminder:config', '配置提醒', 'OA', 'api'),
  ('inventory:warehouse:read', '查看仓库', 'Inventory', 'api'),
  ('inventory:warehouse:write', '维护仓库', 'Inventory', 'api'),
  ('inventory:item:read', '查看物资', 'Inventory', 'api'),
  ('inventory:item:write', '维护物资', 'Inventory', 'api'),
  ('inventory:price:read', '查看价格', 'Inventory', 'api'),
  ('inventory:price:write', '维护价格', 'Inventory', 'api'),
  ('inventory:inbound:create', '发起入库', 'Inventory', 'api'),
  ('inventory:inbound:approve', '审批入库', 'Inventory', 'api'),
  ('inventory:outbound:create', '发起出库', 'Inventory', 'api'),
  ('inventory:outbound:approve', '审批出库', 'Inventory', 'api'),
  ('inventory:stock:read', '查看库存', 'Inventory', 'api'),
  ('inventory:stock:write', '维护库存', 'Inventory', 'api'),
  ('audit:read', '查看审计', 'Audit', 'api'),
  ('ai-access:read', '查看 AI 接入', 'AI Access', 'api')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.iam_role(code, name, description)
VALUES
  ('SYSTEM_ADMIN', '系统管理员', '管理用户、角色、权限、审计配置'),
  ('OA_ADMIN', 'OA 管理员', '管理 OA 流程定义、节点、提醒策略'),
  ('INVENTORY_ADMIN', '物资管理员', '管理物资档案、库存、入库、出库、盘点'),
  ('FINANCE_APPROVER', '财务审批人', '负责报销审批'),
  ('DEPARTMENT_MANAGER', '部门负责人', '作为汇报上级审批人'),
  ('EMPLOYEE', '普通员工', '发起 OA、查看自己的流程'),
  ('AI_CALLER', 'AI 调用方', '仅访问 MCP 白名单只读接口')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.iam_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM iam.iam_role r
JOIN iam.iam_permission p ON (
  r.code = 'SYSTEM_ADMIN'
  OR (r.code = 'OA_ADMIN' AND p.code LIKE 'oa:%')
  OR (r.code = 'INVENTORY_ADMIN' AND p.code LIKE 'inventory:%')
  OR (r.code = 'FINANCE_APPROVER' AND p.code IN ('oa:task:read', 'oa:task:approve', 'oa:instance:read'))
  OR (r.code = 'DEPARTMENT_MANAGER' AND p.code IN ('oa:task:read', 'oa:task:approve', 'oa:instance:read'))
  OR (r.code = 'EMPLOYEE' AND p.code IN ('oa:instance:create', 'oa:instance:read', 'oa:task:urge', 'inventory:item:read', 'inventory:stock:read'))
  OR (r.code = 'AI_CALLER' AND p.code IN ('inventory:item:read', 'inventory:stock:read', 'inventory:price:read'))
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

UPDATE iam.iam_user
SET password_hash = CASE
  WHEN username = 'admin' THEN '67727a41b5b1d4dfca981e4045b1bb2f1e7fef0e3e8825c028949d186cad4c00'
  ELSE '4d4f26369171994f3a46776ee2d88494fb9955800a5bb6261c016c4bb9f30b56'
END
WHERE password_hash IS NULL;

INSERT INTO oa.oa_process_definition(code, name, version, description)
VALUES
  ('inbound_material', '物资入库 OA', 1, '汇报上级审批后由物资管理员审批并触发入库'),
  ('outbound_material', '物资出库 OA', 1, '汇报上级审批后由物资管理员审批并触发出库'),
  ('reimbursement', '报销 OA', 1, '关联已通过入库 OA 后走上级和财务审批')
ON CONFLICT (code, version) DO NOTHING;


INSERT INTO oa.oa_process_node(process_definition_id, node_code, node_name, node_type, sort_order, assignee_mode)
SELECT d.id, v.node_code, v.node_name, 'approval', v.sort_order, v.assignee_mode
FROM oa.oa_process_definition d
JOIN (VALUES
  ('inbound_material', 'manager_approve', '汇报上级审批', 10, 'SUPERVISOR'),
  ('inbound_material', 'inventory_approve', '物资管理员审批', 20, 'ROLE'),
  ('outbound_material', 'manager_approve', '汇报上级审批', 10, 'SUPERVISOR'),
  ('outbound_material', 'inventory_approve', '物资管理员审批', 20, 'ROLE'),
  ('reimbursement', 'manager_approve', '汇报上级审批', 10, 'SUPERVISOR'),
  ('reimbursement', 'finance_approve', '财务审批', 20, 'ROLE')
) AS v(process_code, node_code, node_name, sort_order, assignee_mode) ON v.process_code = d.code
ON CONFLICT (process_definition_id, node_code) DO NOTHING;

INSERT INTO oa.oa_process_node_assignee(node_id, assignee_role_id)
SELECT n.id, r.id
FROM oa.oa_process_node n
JOIN oa.oa_process_definition d ON d.id = n.process_definition_id
JOIN iam.iam_role r ON (
  (d.code IN ('inbound_material', 'outbound_material') AND n.node_code = 'inventory_approve' AND r.code = 'INVENTORY_ADMIN')
  OR (d.code = 'reimbursement' AND n.node_code = 'finance_approve' AND r.code = 'FINANCE_APPROVER')
)
WHERE NOT EXISTS (
  SELECT 1 FROM oa.oa_process_node_assignee a WHERE a.node_id = n.id AND a.assignee_role_id = r.id
);

INSERT INTO oa.oa_reminder_policy(process_definition_id, node_id, interval_minutes, max_remind_count, cooldown_minutes_for_urge)
SELECT d.id, n.id, 120, 3, 30
FROM oa.oa_process_definition d
JOIN oa.oa_process_node n ON n.process_definition_id = d.id
WHERE NOT EXISTS (
  SELECT 1 FROM oa.oa_reminder_policy p WHERE p.process_definition_id = d.id AND p.node_id = n.id
);

INSERT INTO inventory.inv_warehouse(code, name, location, warehouse_type)
VALUES
  ('MEDICAL_MAIN', '医疗用品库', '默认库位', 'medical'),
  ('NON_MEDICAL_MAIN', '非医疗用品库', '默认库位', 'non_medical')
ON CONFLICT (code) DO NOTHING;

INSERT INTO ai_access.ai_api_allowlist(api_code, method, path, scope)
VALUES
  ('inventory.price.list', 'GET', '/api/v1/inventory/prices', 'inventory.read'),
  ('oa.process.status.read', 'GET', '/api/v1/oa/instances', 'oa.read')
ON CONFLICT (api_code) DO NOTHING;

GRANT SELECT ON ALL TABLES IN SCHEMA iam, oa, inventory, audit, ai_access TO his_ai_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA oa GRANT SELECT ON TABLES TO his_ai_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA inventory GRANT SELECT ON TABLES TO his_ai_readonly;
