-- Based on design/04-database/Data-Dictionary.md and Table-Index.md.
CREATE SCHEMA IF NOT EXISTS iam;
CREATE SCHEMA IF NOT EXISTS oa;
CREATE SCHEMA IF NOT EXISTS inventory;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS ai_access;

CREATE TABLE IF NOT EXISTS iam.iam_user (
  id bigserial CONSTRAINT pk_iam_user PRIMARY KEY,
  username varchar(64) NOT NULL,
  display_name varchar(80) NOT NULL,
  phone varchar(32),
  status varchar(20) NOT NULL DEFAULT 'enabled',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_iam_user_username UNIQUE (username)
);
CREATE INDEX IF NOT EXISTS idx_iam_user_status ON iam.iam_user(status);

CREATE TABLE IF NOT EXISTS iam.iam_role (
  id bigserial CONSTRAINT pk_iam_role PRIMARY KEY,
  code varchar(64) NOT NULL,
  name varchar(80) NOT NULL,
  description varchar(255),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_iam_role_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS iam.iam_permission (
  id bigserial CONSTRAINT pk_iam_permission PRIMARY KEY,
  code varchar(120) NOT NULL,
  name varchar(120) NOT NULL,
  domain varchar(40) NOT NULL,
  resource_type varchar(40) NOT NULL,
  description varchar(255),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uk_iam_permission_code UNIQUE (code)
);
CREATE INDEX IF NOT EXISTS idx_iam_permission_domain ON iam.iam_permission(domain);

CREATE TABLE IF NOT EXISTS iam.iam_user_role (
  id bigserial CONSTRAINT pk_iam_user_role PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES iam.iam_user(id),
  role_id bigint NOT NULL REFERENCES iam.iam_role(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uk_iam_user_role_user_role UNIQUE (user_id, role_id)
);
CREATE INDEX IF NOT EXISTS idx_iam_user_role_role_id ON iam.iam_user_role(role_id);

CREATE TABLE IF NOT EXISTS iam.iam_role_permission (
  id bigserial CONSTRAINT pk_iam_role_permission PRIMARY KEY,
  role_id bigint NOT NULL REFERENCES iam.iam_role(id),
  permission_id bigint NOT NULL REFERENCES iam.iam_permission(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uk_iam_role_permission_role_permission UNIQUE (role_id, permission_id)
);
CREATE INDEX IF NOT EXISTS idx_iam_role_permission_permission_id ON iam.iam_role_permission(permission_id);

CREATE TABLE IF NOT EXISTS iam.iam_user_permission_override (
  id bigserial CONSTRAINT pk_iam_user_permission_override PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES iam.iam_user(id),
  permission_id bigint NOT NULL REFERENCES iam.iam_permission(id),
  effect varchar(20) NOT NULL,
  reason varchar(255),
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uk_iam_user_permission_override_user_permission UNIQUE (user_id, permission_id)
);
CREATE INDEX IF NOT EXISTS idx_iam_user_permission_override_effect ON iam.iam_user_permission_override(effect);

CREATE TABLE IF NOT EXISTS oa.oa_permission_group (
  id bigserial CONSTRAINT pk_oa_permission_group PRIMARY KEY,
  code varchar(64) NOT NULL,
  name varchar(80) NOT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uk_oa_permission_group_code UNIQUE (code)
);
CREATE INDEX IF NOT EXISTS idx_oa_permission_group_sort_order ON oa.oa_permission_group(sort_order);

CREATE TABLE IF NOT EXISTS inventory.inv_category (
  id bigserial CONSTRAINT pk_inv_category PRIMARY KEY,
  parent_id bigint REFERENCES inventory.inv_category(id),
  code varchar(64) NOT NULL,
  name varchar(120) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_inv_category_code UNIQUE (code)
);
CREATE INDEX IF NOT EXISTS idx_inv_category_parent_id ON inventory.inv_category(parent_id);

CREATE TABLE IF NOT EXISTS inventory.inv_item (
  id bigserial CONSTRAINT pk_inv_item PRIMARY KEY,
  category_id bigint REFERENCES inventory.inv_category(id),
  code varchar(64) NOT NULL,
  name varchar(120) NOT NULL,
  specification varchar(120),
  unit varchar(32) NOT NULL,
  safety_stock numeric(18, 2) NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'enabled',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_inv_item_code UNIQUE (code)
);
CREATE INDEX IF NOT EXISTS idx_inv_item_category_id ON inventory.inv_item(category_id);

CREATE TABLE IF NOT EXISTS inventory.inv_supplier (
  id bigserial CONSTRAINT pk_inv_supplier PRIMARY KEY,
  code varchar(64) NOT NULL,
  name varchar(120) NOT NULL,
  contact_name varchar(80),
  contact_phone varchar(32),
  status varchar(20) NOT NULL DEFAULT 'enabled',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_inv_supplier_code UNIQUE (code)
);
CREATE INDEX IF NOT EXISTS idx_inv_supplier_status ON inventory.inv_supplier(status);

CREATE TABLE IF NOT EXISTS inventory.inv_warehouse (
  id bigserial CONSTRAINT pk_inv_warehouse PRIMARY KEY,
  code varchar(64) NOT NULL,
  name varchar(120) NOT NULL,
  location varchar(255),
  status varchar(20) NOT NULL DEFAULT 'enabled',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  CONSTRAINT uk_inv_warehouse_code UNIQUE (code)
);
CREATE INDEX IF NOT EXISTS idx_inv_warehouse_status ON inventory.inv_warehouse(status);

CREATE TABLE IF NOT EXISTS inventory.inv_stock (
  id bigserial CONSTRAINT pk_inv_stock PRIMARY KEY,
  warehouse_id bigint NOT NULL REFERENCES inventory.inv_warehouse(id),
  item_id bigint NOT NULL REFERENCES inventory.inv_item(id),
  quantity numeric(18, 2) NOT NULL DEFAULT 0,
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uk_inv_stock_warehouse_item UNIQUE (warehouse_id, item_id)
);
CREATE INDEX IF NOT EXISTS idx_inv_stock_item_id ON inventory.inv_stock(item_id);

CREATE TABLE IF NOT EXISTS inventory.inv_stock_txn (
  id bigserial CONSTRAINT pk_inv_stock_txn PRIMARY KEY,
  txn_type varchar(30) NOT NULL,
  biz_no varchar(64) NOT NULL,
  warehouse_id bigint NOT NULL REFERENCES inventory.inv_warehouse(id),
  item_id bigint NOT NULL REFERENCES inventory.inv_item(id),
  quantity_delta numeric(18, 2) NOT NULL,
  operator_user_id bigint REFERENCES iam.iam_user(id),
  occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_inv_stock_txn_biz_no ON inventory.inv_stock_txn(biz_no);
CREATE INDEX IF NOT EXISTS idx_inv_stock_txn_item_time ON inventory.inv_stock_txn(item_id, occurred_at);

CREATE TABLE IF NOT EXISTS inventory.inv_inbound_order (
  id bigserial CONSTRAINT pk_inv_inbound_order PRIMARY KEY,
  order_no varchar(64) NOT NULL,
  supplier_id bigint REFERENCES inventory.inv_supplier(id),
  warehouse_id bigint NOT NULL REFERENCES inventory.inv_warehouse(id),
  status varchar(20) NOT NULL DEFAULT 'draft',
  created_by bigint REFERENCES iam.iam_user(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  approved_at timestamptz,
  CONSTRAINT uk_inv_inbound_order_no UNIQUE (order_no)
);
CREATE INDEX IF NOT EXISTS idx_inv_inbound_order_status ON inventory.inv_inbound_order(status);

CREATE TABLE IF NOT EXISTS inventory.inv_outbound_order (
  id bigserial CONSTRAINT pk_inv_outbound_order PRIMARY KEY,
  order_no varchar(64) NOT NULL,
  warehouse_id bigint NOT NULL REFERENCES inventory.inv_warehouse(id),
  status varchar(20) NOT NULL DEFAULT 'draft',
  created_by bigint REFERENCES iam.iam_user(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  approved_at timestamptz,
  CONSTRAINT uk_inv_outbound_order_no UNIQUE (order_no)
);
CREATE INDEX IF NOT EXISTS idx_inv_outbound_order_status ON inventory.inv_outbound_order(status);

CREATE TABLE IF NOT EXISTS inventory.inv_stocktake_order (
  id bigserial CONSTRAINT pk_inv_stocktake_order PRIMARY KEY,
  order_no varchar(64) NOT NULL,
  warehouse_id bigint NOT NULL REFERENCES inventory.inv_warehouse(id),
  status varchar(20) NOT NULL DEFAULT 'draft',
  created_by bigint REFERENCES iam.iam_user(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  completed_at timestamptz,
  CONSTRAINT uk_inv_stocktake_order_no UNIQUE (order_no)
);
CREATE INDEX IF NOT EXISTS idx_inv_stocktake_order_status ON inventory.inv_stocktake_order(status);

CREATE TABLE IF NOT EXISTS audit.audit_operation_log (
  id bigserial CONSTRAINT pk_audit_operation_log PRIMARY KEY,
  actor_user_id bigint REFERENCES iam.iam_user(id),
  action varchar(120) NOT NULL,
  resource_type varchar(80) NOT NULL,
  resource_id varchar(80),
  request_id varchar(80),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_operation_log_actor_time ON audit.audit_operation_log(actor_user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_operation_log_resource ON audit.audit_operation_log(resource_type, resource_id);

CREATE TABLE IF NOT EXISTS audit.audit_permission_change_log (
  id bigserial CONSTRAINT pk_audit_permission_change_log PRIMARY KEY,
  actor_user_id bigint REFERENCES iam.iam_user(id),
  target_user_id bigint REFERENCES iam.iam_user(id),
  target_role_id bigint REFERENCES iam.iam_role(id),
  permission_id bigint REFERENCES iam.iam_permission(id),
  change_type varchar(40) NOT NULL,
  reason varchar(255),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_permission_change_log_target_time ON audit.audit_permission_change_log(target_user_id, created_at);

CREATE TABLE IF NOT EXISTS ai_access.ai_api_allowlist (
  id bigserial CONSTRAINT pk_ai_api_allowlist PRIMARY KEY,
  api_code varchar(120) NOT NULL,
  method varchar(10) NOT NULL,
  path varchar(255) NOT NULL,
  scope varchar(80) NOT NULL,
  enabled boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uk_ai_api_allowlist_api_code UNIQUE (api_code)
);
CREATE INDEX IF NOT EXISTS idx_ai_api_allowlist_scope ON ai_access.ai_api_allowlist(scope);

CREATE TABLE IF NOT EXISTS ai_access.ai_call_log (
  id bigserial CONSTRAINT pk_ai_call_log PRIMARY KEY,
  caller varchar(120) NOT NULL,
  api_code varchar(120) NOT NULL,
  scope varchar(80) NOT NULL,
  status varchar(30) NOT NULL,
  request_summary text,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_ai_call_log_api_time ON ai_access.ai_call_log(api_code, created_at);

INSERT INTO ai_access.ai_api_allowlist(api_code, method, path, scope)
VALUES
  ('inventory.item.list', 'GET', '/api/v1/inventory/items', 'inventory.read'),
  ('inventory.stock.list', 'GET', '/api/v1/inventory/stocks', 'inventory.read')
ON CONFLICT (api_code) DO NOTHING;

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'his_ai_readonly') THEN
    EXECUTE 'CREATE ROLE his_ai_readonly LOGIN PASSWORD ''his_ai_readonly_password''';
  END IF;
END
$$;

GRANT USAGE ON SCHEMA iam, oa, inventory, audit, ai_access TO his_ai_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA iam, oa, inventory, audit, ai_access TO his_ai_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA iam GRANT SELECT ON TABLES TO his_ai_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA oa GRANT SELECT ON TABLES TO his_ai_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA inventory GRANT SELECT ON TABLES TO his_ai_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT SELECT ON TABLES TO his_ai_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA ai_access GRANT SELECT ON TABLES TO his_ai_readonly;
