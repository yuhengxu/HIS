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

## 7. 权限与菜单规则

依据：`plans/oa20260624-iam-oa-inventory-enhancement.plan.md` §4、§7。

| 能力 | 可见/可执行角色 | 接口强校验 |
|---|---|---|
| 用户管理菜单 | 仅 `SYSTEM_ADMIN` | `/api/v1/iam/users/**` 需 `SYSTEM_ADMIN` |
| 角色权限菜单 | 仅 `SYSTEM_ADMIN` | `/api/v1/iam/roles/**`、`/api/v1/iam/permissions/**` 需 `SYSTEM_ADMIN` |
| 物资档案写操作 | `SYSTEM_ADMIN`、`INVENTORY_ADMIN` | 写接口双重校验角色 + `inventory:item:write`；特殊物资标签仅 `SYSTEM_ADMIN` 可见可改 |
| OA 流程定义维护 | `SYSTEM_ADMIN`、`OA_ADMIN` | `oa:process:write` |
| OA 流程实例发起 | 拥有 `oa:instance:create` 的用户 | 发起接口校验权限 |
| OA 流程撤销 | 流程发起人本人 | `/api/v1/oa/instances/{instanceId}/revoke` 仅允许撤销运行中/待配置流程，撤销后待办取消 |
| 工作台本人流程与待办读取 | `SYSTEM_ADMIN`、`OA_ADMIN`、`INVENTORY_ADMIN`、`FINANCE_APPROVER`、`DEPARTMENT_MANAGER`、`EMPLOYEE` | 本人流程 `/api/v1/oa/instances/mine` 校验登录用户；待办 `/api/v1/oa/tasks/todo` 校验 `oa:task:read` |
| 工作台已办读取 | 拥有 `oa:task:read` 的当前用户 | `/api/v1/oa/tasks/handled` 仅返回 `claimed_by_user_id` 等于当前用户的已处理任务 |
| OA 连续同人审批 | 当前节点处理人与下一节点解析处理人相同 | 当前人通过后，下一连续同人节点自动标记通过并记录审计；起草人确认节点不自动跳过 |
| 顶层管理员无上级审批 | 无汇报上级且具备 IAM 管理员权限的发起人 | 汇报上级节点解析为空时自动标记通过并进入下一节点；非管理员或普通用户无上级仍进入待配置 |
| 普通角色库存后台访问 | 普通角色无权限 | `EMPLOYEE`、`DEPARTMENT_MANAGER`、`FINANCE_APPROVER` 不授予 `inventory:item:read` / `inventory:stock:read` |
| 库存与流水读取 | `SYSTEM_ADMIN` 可读全部；`INVENTORY_ADMIN` 仅可读普通物资 | `/api/v1/inventory/stocks`、`/api/v1/inventory/stock-transactions` 按物资标签过滤；特殊物资仅系统管理员可见 |
| 物资库总库存读取 | `SYSTEM_ADMIN` 可读全部；`INVENTORY_ADMIN` 仅可读普通物资 | `/api/v1/inventory/stock-summary` 返回仓库 + 物资维度总库存，不暴露入库人明细 |
| 管理员直接库存调整 | 仅 `SYSTEM_ADMIN` | `/api/v1/inventory/stocks/adjust` 不走 OA，但必须生成 `ADJUST` 库存流水和审计 |
| 物品领用出库 | 拥有 `oa:instance:create` 的用户可申请 | 领用物资按仓库类型过滤；审批通过后按仓库总库存校验并扣减明细库存 |
| 部门负责人物资档案查看 | `DEPARTMENT_MANAGER` | 仅授予 `inventory:item:read`，不授予物资档案写权限 |
| 菜单展示 | 当前用户服务端菜单树 | 左侧菜单读取 `/api/v1/me/menus`，按管理类、OA 类、库存类多层目录展示；角色菜单配置支持多层目录勾选 |
| 报销凭证上传 | 发起人或 `oa:attachment:write` | 报销提交校验 `voucherAttachmentIds` |
| 物资图片上传 | `SYSTEM_ADMIN`、`INVENTORY_ADMIN`；OA 新物资图片随 OA 流转 | `inventory:image:write` / `oa:attachment:write` |

权限点说明必须在角色权限页面和 `/api/v1/iam/permissions` 返回中提供中文 `name` 与 `description`。

## 8. OA 基本流转规则

依据：`plans/oa-draft-confirm-drag-nodes20260625.plan.md` §2、§6。

OA 标准闭环为：起草节点发起 -> 一个或多个审批节点审批 -> 起草人确认 -> 流程结束。

| 场景 | 流转结果 | 业务生效 |
|---|---|---|
| 审批节点通过，后续仍有审批节点 | 进入下一个审批节点 | 否 |
| 最后一个审批节点通过 | 回到起草人确认节点 | 否 |
| 任一审批节点驳回 | 回到起草人确认节点 | 否 |
| 起草人确认通过 | 流程标记为已结束/通过 | 是，触发入库、领用、报销等业务监听 |
| 起草人确认驳回/关闭 | 流程标记为已驳回 | 否 |
| 发起人无汇报上级且具备 IAM 管理员权限 | 视为组织最高级，该汇报上级节点自动通过，继续后续节点 | 否 |
| 发起人无汇报上级但不具备 IAM 管理员权限 | 视为组织层级配置缺失，流程进入待配置状态 | 否 |

起草人确认任务只能由流程发起人本人处理。该确认动作不授予发起人全局审批权限，仅允许处理自己的确认任务。

流程定义节点配置要求：

- 至少包含一个审批节点。
- 系统默认流程内置“起草人确认”节点。
- 保存流程节点时，如果缺少“起草人确认”节点，系统自动追加。
- 节点排序以流程定义页面拖拽后的顺序为准，保存时重新生成排序号。

## 9. 流程定义、菜单与图片权限（本轮补齐）

依据：`plans/oa20260624-feature-oa-process-menu-image-fix.plan.md` §3.3。

新增权限点：

| 权限点 | 说明 | AI 可调用 |
|---|---|---|
| `oa:process-definition:create` | 新增/复制流程定义 | 否 |
| `oa:process-definition:update` | 修改流程定义基础信息 | 否 |
| `oa:process-definition:delete` | 删除/停用流程定义 | 否 |
| `oa:process-definition:publish` | 发布流程定义 | 否 |
| `oa:process-node:write` | 保存流程节点配置 | 否 |
| `menu:read` | 查看菜单树与绑定关系 | 否 |
| `menu:write` | 维护菜单树 | 否 |
| `menu:role-bind` | 角色菜单绑定 | 否 |
| `menu:user-bind` | 用户菜单加授/撤销 | 否 |
| `file:upload` | 通用文件上传 | 否 |
| `file:read` | 读取文件 | 否 |
| `file:delete` | 删除文件关联 | 否 |
| `inventory:item:image:write` | 物资图片维护 | 否 |
| `inventory:stock:image:write` | 库存现场图维护 | 否 |

菜单可见公式：`最终可见菜单 = 角色菜单 + 用户菜单加授 - 用户菜单撤销`。菜单只控制前端入口，接口权限仍独立校验。图片上传须业务二次鉴权（角色 + 权限点）。
