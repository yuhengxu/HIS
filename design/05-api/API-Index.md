# API 接口索引

## 1. 系统接口

| API | 方法 | 路径 | 权限 | AI 可调用 | 说明 |
|---|---|---|---|---|---|
| `system.health` | GET | `/api/v1/system/health` | 匿名 | 否 | 服务健康检查 |
| `system.modules` | GET | `/api/v1/system/modules` | 匿名 | 否 | 首期模块清单 |

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
| `inventory.stock.list` | GET | `/api/v1/inventory/stocks` | `inventory:stock:read` | 是，只读 | 库存查询 |
| `inventory.inbound.create` | POST | `/api/v1/inventory/inbound-orders` | `inventory:inbound:write` | 否 | 创建入库单 |
| `inventory.outbound.create` | POST | `/api/v1/inventory/outbound-orders` | `inventory:outbound:write` | 否 | 创建出库单 |
| `inventory.stocktake.create` | POST | `/api/v1/inventory/stocktake-orders` | `inventory:stocktake:write` | 否 | 创建盘点单 |

## 4. 速率限制

- 匿名系统接口：每 IP 每分钟 60 次。
- 登录业务接口：每用户每分钟 300 次。
- AI 可调用接口：每调用方每分钟 60 次，并记录 `ai_call_log`。
