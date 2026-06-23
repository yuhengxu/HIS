# 大模型 / MCP 接入接口

## 1. 接入原则

- AI 只能调用本文件登记的接口。
- AI 数据库账号仅允许 `SELECT`。
- AI 调用必须写入 `ai_call_log`。

## 2. 首期授权接口

| API | 方法 | 路径 | Scope | 限制 |
|---|---|---|---|---|
| `inventory.item.list` | GET | `/api/v1/inventory/items` | `inventory.read` | 只读，不返回敏感个人信息 |
| `inventory.stock.list` | GET | `/api/v1/inventory/stocks` | `inventory.read` | 只读，仅库存余额与物资基础信息 |

## 3. 禁止项

- AI 不得调用入库、出库、盘点、权限变更等写接口。
- AI 不得直接执行 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`CREATE`。
