# IAM 用户与角色权限开发说明

依据：`plans/oa20260624-iam-oa-inventory-enhancement.plan.md` §5、§6、§7；`PRD.md` §10-§11。

## 模块边界

- 后端包：`com.health.platform.iam`
- 前端页面：`/iam/users`、`/iam/roles`、`/iam/permissions`
- 仅 `SYSTEM_ADMIN` 角色可访问用户管理与角色权限菜单及对应接口。

## 接口契约

| 接口 | 方法 | 路径 | 角色/权限 |
|---|---|---|---|
| 用户列表 | GET | `/api/v1/iam/users` | `SYSTEM_ADMIN` + `iam:user:read` |
| 新建用户 | POST | `/api/v1/iam/users` | `SYSTEM_ADMIN` + `iam:user:create` |
| 重置密码 | POST | `/api/v1/iam/users/{id}/reset-password` | `SYSTEM_ADMIN` + `iam:user:update` |
| 角色 CRUD | * | `/api/v1/iam/roles/**` | `SYSTEM_ADMIN` + 对应 IAM 权限 |
| 权限点列表 | GET | `/api/v1/iam/permissions` | `SYSTEM_ADMIN` + `iam:permission:read` |

## 数据与审计

- 用户默认密码 `qwer1234`，保存 SHA-256 哈希。
- 角色/用户变更写入 `auditService`。
- 角色删除采用软删除；系统内置角色不可删。

## 前端页面

- 用户列表 + 新建/编辑弹窗 + 重置密码 + 启停用。
- 角色列表 + 权限域分组勾选 + 权限说明展示。

## 测试清单

- [ ] 非 `SYSTEM_ADMIN` 调用 IAM 管理接口返回 403
- [ ] 新建用户默认密码哈希正确
- [ ] 用户名重复返回校验错误
- [ ] 系统角色不可删除
- [ ] 权限点返回中文说明
