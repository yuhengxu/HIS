# API 接口索引

## 1. 系统接口

| API | 方法 | 路径 | 权限 | AI 可调用 | 说明 |
|---|---|---|---|---|---|
| `system.health` | GET | `/api/v1/system/health` | 匿名 | 否 | 服务健康检查 |
| `system.modules` | GET | `/api/v1/system/modules` | 匿名 | 否 | 首期模块清单 |
| `auth.login` | POST | `/api/v1/auth/login` | 匿名 | 否 | 登录，返回当前用户与权限 |

## 2. IAM

| API | 方法 | 路径 | 权限 | AI 可调用 | 说明 |
|---|---|---|---|---|---|
| `iam.user.list` | GET | `/api/v1/iam/users` | `iam:user:read` | 否 | 用户列表 |
| `iam.role.list` | GET | `/api/v1/iam/roles` | `iam:role:read` | 否 | 角色列表 |
| `iam.permission.list` | GET | `/api/v1/iam/permissions` | `iam:permission:read` | 否 | 权限点列表 |
| `iam.user-permission.update` | PUT | `/api/v1/iam/users/{userId}/permission-overrides` | `iam:user-permission:write` | 否 | 用户单独授权 |

## 3. Inventory

| API | 方法 | 路径 | 权限 | AI 可调用 | 说明 |
|---|---|---|---|---|---|
| `inventory.item.list` | GET | `/api/v1/inventory/items` | `inventory:item:read` | 是，只读 | 物资档案列表 |
| `inventory.item.create` | POST | `/api/v1/inventory/items` | `inventory:item:write` | 否 | 新增物资档案，ID 自增 |
| `inventory.stock.list` | GET | `/api/v1/inventory/stocks` | `inventory:stock:read` | 是，只读 | 库存查询 |
| `inventory.stock.create` | POST | `/api/v1/inventory/stocks` | `inventory:stock:write` | 否 | 新增库存，ID 自增 |
| `inventory.inbound.create` | POST | `/api/v1/inventory/inbound-orders` | `inventory:inbound:write` | 否 | 创建入库单 |
| `inventory.outbound.create` | POST | `/api/v1/inventory/outbound-orders` | `inventory:outbound:write` | 否 | 创建出库单 |
| `inventory.stocktake.create` | POST | `/api/v1/inventory/stocktake-orders` | `inventory:stocktake:write` | 否 | 创建盘点单 |

## 4. 速率限制

- 匿名系统接口：每 IP 每分钟 60 次。
- 登录业务接口：每用户每分钟 300 次。
- AI 可调用接口：每调用方每分钟 60 次，并记录 `ai_call_log`。

## 5. 第一阶段完整接口清单

### IAM

| API | 方法 | 路径 | 权限 | AI 可调用 |
|---|---|---|---|---|
| `iam.user.create` | POST | `/api/v1/iam/users` | `iam:user:create` | 否 |
| `iam.user.list` | GET | `/api/v1/iam/users` | `iam:user:read` | 否 |
| `iam.user.get` | GET | `/api/v1/iam/users/{userId}` | `iam:user:read` | 否 |
| `iam.user.update` | PUT | `/api/v1/iam/users/{userId}` | `iam:user:update` | 否 |
| `iam.user.disable` | PUT | `/api/v1/iam/users/{userId}/status` | `iam:user:disable` | 否 |
| `iam.user.roles.update` | PUT | `/api/v1/iam/users/{userId}/roles` | `iam:user:update` | 否 |
| `iam.user.permission.override` | PUT | `/api/v1/iam/users/{userId}/permission-overrides` | `iam:user-permission:write` | 否 |
| `iam.role.list` | GET | `/api/v1/iam/roles` | `iam:role:read` | 否 |
| `iam.role.write` | POST/PUT | `/api/v1/iam/roles` | `iam:role:write` | 否 |
| `iam.permission.list` | GET | `/api/v1/iam/permissions` | `iam:permission:read` | 否 |
| `iam.me.permissions` | GET | `/api/v1/iam/me/permissions` | 登录用户 | 否 |
| `auth.login` | POST | `/api/v1/auth/login` | 匿名 | 否 |

### OA

| API | 方法 | 路径 | 权限 | AI 可调用 |
|---|---|---|---|---|
| `oa.process.list` | GET | `/api/v1/oa/process-definitions` | `oa:process:read` | 否 |
| `oa.process.write` | POST/PUT | `/api/v1/oa/process-definitions` | `oa:process:write` | 否 |
| `oa.instance.inventory-inbound.create` | POST | `/api/v1/oa/instances/inventory-inbound` | `oa:instance:create` | 否 |
| `oa.instance.inventory-outbound.create` | POST | `/api/v1/oa/instances/inventory-outbound` | `oa:instance:create` | 否 |
| `oa.instance.reimbursement.create` | POST | `/api/v1/oa/instances/reimbursement` | `oa:instance:create` | 否 |
| `oa.instance.list` | GET | `/api/v1/oa/instances` | `oa:instance:read` | 是，只读 |
| `oa.task.todo` | GET | `/api/v1/oa/tasks/todo` | `oa:task:read` | 否 |
| `oa.task.approve` | POST | `/api/v1/oa/tasks/{taskId}/approve` | `oa:task:approve` | 否 |
| `oa.task.reject` | POST | `/api/v1/oa/tasks/{taskId}/reject` | `oa:task:approve` | 否 |
| `oa.instance.urge` | POST | `/api/v1/oa/instances/{instanceId}/urge` | `oa:task:urge` | 否 |

### Inventory

| API | 方法 | 路径 | 权限 | AI 可调用 |
|---|---|---|---|---|
| `inventory.warehouse.list` | GET | `/api/v1/inventory/warehouses` | `inventory:warehouse:read` | 否 |
| `inventory.item.list` | GET | `/api/v1/inventory/items` | `inventory:item:read` | 是，只读 |
| `inventory.item.create` | POST | `/api/v1/inventory/items` | `inventory:item:write` | 否 |
| `inventory.price.list` | GET | `/api/v1/inventory/prices` | `inventory:price:read` | 是，只读 |
| `inventory.stock.list` | GET | `/api/v1/inventory/stocks` | `inventory:stock:read` | 是，只读 |
| `inventory.stock.create` | POST | `/api/v1/inventory/stocks` | `inventory:stock:write` | 否 |
| `inventory.stock-txn.list` | GET | `/api/v1/inventory/stock-transactions` | `inventory:stock:read` | 是，只读 |
| `inventory.inbound.list` | GET | `/api/v1/inventory/inbound-orders` | `inventory:inbound:create` | 否 |
| `inventory.outbound.list` | GET | `/api/v1/inventory/outbound-orders` | `inventory:outbound:create` | 否 |
| `inventory.reimbursement.list` | GET | `/api/v1/inventory/reimbursements` | `inventory:price:read` | 否 |
