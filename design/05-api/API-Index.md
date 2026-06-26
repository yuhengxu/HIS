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
| `inventory.inbound.unlinked-reimbursement` | GET | `/api/v1/inventory/inbound-orders/unlinked-for-reimbursement` | `finance:reimbursement:create` | 否 | 报销可关联的未关联入库单 |
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
| `oa.instance.mine` | GET | `/api/v1/oa/instances/mine` | 登录用户 / 本人流程 | 否 |
| `oa.instance.revoke` | POST | `/api/v1/oa/instances/{instanceId}/revoke` | 发起人本人 + `oa:instance:read` | 否 |
| `oa.claimable-material.search` | GET | `/api/v1/oa/instances/claimable-materials/search` | `inventory:item:read` | 否 |
| `oa.task.todo` | GET | `/api/v1/oa/tasks/todo` | `oa:task:read` | 否 |
| `oa.task.handled` | GET | `/api/v1/oa/tasks/handled` | `oa:task:read` + 本人已处理任务 | 否 |
| `oa.task.detail` | GET | `/api/v1/oa/tasks/{taskId}` | `oa:task:read` + 当前可处理人 | 否 |
| `oa.task.approve` | POST | `/api/v1/oa/tasks/{taskId}/approve` | `oa:task:approve` | 否 |
| `oa.task.reject` | POST | `/api/v1/oa/tasks/{taskId}/reject` | `oa:task:approve` | 否 |
| `oa.instance.urge` | POST | `/api/v1/oa/instances/{instanceId}/urge` | `oa:task:urge` | 否 |

说明：起草人确认任务复用 `oa.task.approve` / `oa.task.reject`，仅允许发起人处理自己的确认任务，不授予全局 `oa:task:approve` 能力；AI 不允许调用。

### Inventory

| API | 方法 | 路径 | 权限 | AI 可调用 |
|---|---|---|---|---|
| `inventory.warehouse.list` | GET | `/api/v1/inventory/warehouses` | `inventory:warehouse:read` | 否 |
| `inventory.item.list` | GET | `/api/v1/inventory/items` | `inventory:item:read` | 是，只读 |
| `inventory.item.create` | POST | `/api/v1/inventory/items` | `inventory:item:write` | 否 |
| `inventory.price.list` | GET | `/api/v1/inventory/prices` | `inventory:price:read` | 是，只读 |
| `inventory.stock-summary.list` | GET | `/api/v1/inventory/stock-summary` | `inventory:stock:read` | 是，只读 |
| `inventory.stock.list` | GET | `/api/v1/inventory/stocks` | `inventory:stock:read` | 是，只读 |
| `inventory.stock.create` | POST | `/api/v1/inventory/stocks` | `inventory:stock:write` | 否 |
| `inventory.stock.adjust` | POST | `/api/v1/inventory/stocks/adjust` | `SYSTEM_ADMIN` + `inventory:stock:write` | 否 |
| `inventory.stock-txn.list` | GET | `/api/v1/inventory/stock-transactions` | `inventory:stock:read` | 是，只读 |
| `inventory.inbound.list` | GET | `/api/v1/inventory/inbound-orders` | `inventory:inbound:create` | 否 |
| `inventory.outbound.list` | GET | `/api/v1/inventory/outbound-orders` | `inventory:outbound:create` | 否 |
| `inventory.reimbursement.list` | GET | `/api/v1/inventory/reimbursements` | `inventory:price:read` | 否 |

说明：`inventory.stock-summary.list` 为“物资库”总库存，按仓库 + 物资聚合；`inventory.stock.list` 为“物资明细库”，返回 `ownerUserId`、`ownerName`。`inventory.stock-txn.list` 返回物资、仓库、操作人名称。`SYSTEM_ADMIN` 可读取全部普通/特殊物资、库存与流水；`INVENTORY_ADMIN` 仅可读取普通物资、普通物资库存与流水；普通角色不可访问库存查询、库存流水、物资档案后台。`inventory.stock.adjust` 允许系统管理员直接改库存并生成 `ADJUST` 流水。AI 调用仍只允许只读查询并继承特殊物资过滤边界。物品领用使用 `oa.claimable-material.search` 按仓库类型过滤可申领物资。

## 6. 扩展 API

依据：`plans/oa20260624-iam-oa-inventory-enhancement.plan.md` §3.5。

| API | 方法 | 路径 | 权限/角色 | AI 可调用 |
|---|---|---|---|---|
| `iam.user.reset-password` | POST | `/api/v1/iam/users/{id}/reset-password` | `SYSTEM_ADMIN` | 否 |
| `iam.role.delete` | DELETE | `/api/v1/iam/roles/{code}` | `SYSTEM_ADMIN` | 否 |
| `iam.me.roles` | GET | `/api/v1/iam/me/roles` | 登录用户 | 否 |
| `oa.instance.startable` | GET | `/api/v1/oa/instances/startable` | `oa:instance:create` | 否 |
| `oa.material.search` | GET | `/api/v1/oa/instances/materials/search` | `inventory:item:read` | 否 |
| `oa.material-draft.create` | POST | `/api/v1/oa/instances/{id}/material-drafts` | `oa:instance:create` | 否 |
| `inventory.item.update` | PUT | `/api/v1/inventory/items/{id}` | `SYSTEM_ADMIN`/`INVENTORY_ADMIN` | 否 |
| `inventory.item.image.add` | POST | `/api/v1/inventory/items/{id}/images` | `inventory:image:write` | 否 |
| `attachment.upload` | POST | `/api/v1/attachments` | 按用途 | 否 |
## 7. 扩展 API（流程/菜单/图片）

依据：`plans/oa20260624-feature-oa-process-menu-image-fix.plan.md` §5。

| API | 方法 | 路径 | 权限 |
|---|---|---|---|
| `oa.process-definition.create` | POST | `/api/v1/oa/process-definitions` | `oa:process-definition:create` |
| `oa.process-definition.nodes.save` | PUT | `/api/v1/oa/process-definitions/{id}/nodes` | `oa:process-node:write` |
| `oa.process-definition.publish` | POST | `/api/v1/oa/process-definitions/{id}/publish` | `oa:process-definition:publish` |
| `system.menu.list` | GET | `/api/v1/system/menus` | `menu:read` |
| `system.menu.role.bind` | PUT | `/api/v1/system/menus/roles/{roleCode}` | `menu:role-bind` |
| `me.menus` | GET | `/api/v1/me/menus` | 登录用户 |
| `inventory.item.images` | GET/POST | `/api/v1/inventory/items/{id}/images` | 读/写分离 |
| `inventory.stock.images` | GET/POST | `/api/v1/inventory/stocks/{id}/images` | 读/写分离 |

## 8. 企业微信 H5 OA API

依据：`plans/wecom-h5-oa-mobile-integration20260626.plan.md` §8、§9。

| API | 方法 | 路径 | 权限 | AI 可调用 |
|---|---|---|---|---|
| `wecom.auth.url` | GET | `/api/v1/wecom/auth/url` | 匿名 | 否 |
| `wecom.auth.login` | POST | `/api/v1/wecom/auth/login` | 企业微信 OAuth code + `wecomUserId` 绑定 | 否 |
| `wecom.auth.me` | GET | `/api/v1/wecom/auth/me` | `Authorization: Bearer <mobileToken>` | 否 |
| `wecom.auth.logout` | POST | `/api/v1/wecom/auth/logout` | `Authorization: Bearer <mobileToken>` | 否 |

说明：移动端 `/m/oa` 复用现有 OA 和库存接口；移动端 token 只替代请求身份识别方式，不改变业务权限点。企业微信消息发送由后端流程流转触发，不对 AI 或前端开放直接发送接口。
