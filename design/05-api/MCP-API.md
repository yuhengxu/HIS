# 大模型 / MCP 接入接口

## 1. 接入原则

- AI 只能调用本文件登记的接口。
- AI 数据库账号仅允许 `SELECT`。
- AI 调用必须写入 `ai_call_log`。
- AI 不继承普通用户权限，不能审批、催办、写库存、改权限。

## 2. 第一阶段授权接口

| API | 方法 | 路径 | Scope | 限制 |
|---|---|---|---|---|
| `inventory.item.list` | GET | `/api/v1/inventory/items` | `inventory.read` | 只读，不返回敏感个人信息 |
| `inventory.stock.list` | GET | `/api/v1/inventory/stocks` | `inventory.read` | 只读，仅库存余额与物资基础信息 |
| `inventory.price.list` | GET | `/api/v1/inventory/prices` | `inventory.read` | 只读，仅价格记录 |
| `oa.process.status.read` | GET | `/api/v1/oa/instances` | `oa.read` | 只读，仅流程状态，不允许审批 |

## 3. 禁止接口

- `oa.task.approve`
- `oa.task.reject`
- `oa.instance.create`
- `inventory.inbound.create`
- `inventory.outbound.create`
- `iam.user.write`
- `iam.permission.write`

## 4. SQL 禁止项

AI 数据库账号禁止执行 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`CREATE`。
