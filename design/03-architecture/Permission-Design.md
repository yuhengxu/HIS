# 权限与安全设计

## 1. 权限模型

首期采用 RBAC + 用户单独授权模型。

| 概念 | 说明 |
|---|---|
| 用户 | 系统登录主体，属于统一账户体系 |
| 角色 | 一组权限的集合，如系统管理员、OA 管理员、物资管理员 |
| 权限点 | 可访问的页面、菜单、按钮或接口能力 |
| 用户授权 | 对单个用户额外加授或撤销某个权限点 |

最终权限 = 角色权限 + 用户额外加授 - 用户单独撤销。

## 2. 首期权限域

| 权限域 | 示例权限 |
|---|---|
| IAM | 用户查看、用户维护、角色维护、权限维护 |
| OA | OA 首页访问、公告查看、公告维护、审批查看 |
| Inventory | 物资查看、物资维护、入库、出库、盘点 |
| Audit | 审计日志查看、权限变更日志查看 |
| AI Access | AI 接口索引查看、AI 调用审计查看 |

## 3. 审计要求

- 用户、角色、权限、用户单独授权的变更必须记录审计日志。
- 物资入库、出库、盘点、库存调整必须记录库存流水与操作审计。
- AI 调用必须记录调用方、授权 scope、接口、结果状态。

## 4. AI 权限边界

- AI 不继承普通用户的全部权限。
- AI 只允许调用 `MCP-API.md` 中登记的授权接口。
- AI 数据库账号仅允许 `SELECT`，禁止写入和 DDL。

## 5. 第一阶段权限点

最终权限规则保持：角色权限 + 用户额外加授 - 用户单独撤销。

| 权限域 | 权限点 |
|---|---|
| IAM | `iam:user:create`、`iam:user:update`、`iam:user:disable`、`iam:role:write`、`iam:permission:write`、`iam:user-permission:write` |
| OA | `oa:process:read`、`oa:process:write`、`oa:instance:create`、`oa:instance:read`、`oa:task:read`、`oa:task:approve`、`oa:task:urge`、`oa:reminder:config` |
| Inventory | `inventory:warehouse:read`、`inventory:warehouse:write`、`inventory:item:read`、`inventory:item:write`、`inventory:price:read`、`inventory:price:write`、`inventory:inbound:create`、`inventory:inbound:approve`、`inventory:outbound:create`、`inventory:outbound:approve`、`inventory:stock:read`、`inventory:stock:write` |
| Audit | `audit:read` |
| AI Access | `ai-access:read` |

## 6. 第一阶段初始化角色

| 角色 | 说明 |
|---|---|
| `SYSTEM_ADMIN` | 系统管理员，管理用户、角色、权限、审计配置 |
| `OA_ADMIN` | OA 管理员，管理 OA 流程定义、节点、提醒策略 |
| `INVENTORY_ADMIN` | 物资管理员，管理物资档案、库存、入库、出库、盘点 |
| `FINANCE_APPROVER` | 财务审批人，负责报销审批 |
| `DEPARTMENT_MANAGER` | 部门负责人，可作为汇报上级审批人 |
| `EMPLOYEE` | 普通员工，可发起 OA、查看自己的流程 |
| `AI_CALLER` | AI 调用方，仅可访问 MCP 白名单只读接口 |

汇报上级不是角色，而是 `iam_user.report_to_user_id` 用户关系字段。

## 7. 增强版权限与菜单规则

依据：`plans/oa20260624-iam-oa-inventory-enhancement.plan.md` §4、§7。

| 能力 | 可见/可执行角色 | 接口强校验 |
|---|---|---|
| 用户管理菜单 | 仅 `SYSTEM_ADMIN` | `/api/v1/iam/users/**` 需 `SYSTEM_ADMIN` |
| 角色权限菜单 | 仅 `SYSTEM_ADMIN` | `/api/v1/iam/roles/**`、`/api/v1/iam/permissions/**` 需 `SYSTEM_ADMIN` |
| 物资档案写操作 | `SYSTEM_ADMIN`、`INVENTORY_ADMIN` | 写接口双重校验角色 + `inventory:item:write` |
| OA 流程定义维护 | `SYSTEM_ADMIN`、`OA_ADMIN` | `oa:process:write` |
| OA 流程实例发起 | 拥有 `oa:instance:create` 的用户 | 发起接口校验权限 |
| 报销凭证上传 | 发起人或 `oa:attachment:write` | 报销提交校验 `voucherAttachmentIds` |
| 物资图片上传 | `SYSTEM_ADMIN`、`INVENTORY_ADMIN`；OA 新物资图片随 OA 流转 | `inventory:image:write` / `oa:attachment:write` |

权限点说明必须在角色权限页面和 `/api/v1/iam/permissions` 返回中提供中文 `name` 与 `description`。
