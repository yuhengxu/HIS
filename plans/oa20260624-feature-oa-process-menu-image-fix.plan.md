# OA流程定义、菜单管理、物资/库存图片补齐计划

文件名：`oa20260624-feature-oa-process-menu-image-fix.plan.md`  
项目：HIS + 康养平台  
日期：2026-06-24  
目标分支：`feature/oa-iam-inventory-upload-20260624`  
计划类型：基于 feature 分支的缺口补齐计划  
范围：OA流程定义CRUD、流程内容/审批节点灵活配置、菜单管理、物资档案与库存查询图片上传

---

## 0. 分支与基线

### 0.1 拉取分支

开发前执行：

```bash
git fetch origin
git checkout feature/oa-iam-inventory-upload-20260624
git pull origin feature/oa-iam-inventory-upload-20260624
```

如果本地不存在该分支：

```bash
git fetch origin feature/oa-iam-inventory-upload-20260624
git checkout -b feature/oa-iam-inventory-upload-20260624 origin/feature/oa-iam-inventory-upload-20260624
```

### 0.2 已确认的项目基线

当前分支已有以下设计基础：

1. 项目要求文档驱动、计划驱动、权限可审计、数据可追溯。
2. `.codex/rules/project-constitution.md` 要求每轮先生成或引用 `plans/` 执行计划，再推进文档、代码、测试。
3. `.codex/rules/his-ai-architect.md` 要求按“需求分析 → 业务分析 → 数据分析 → 权限分析 → AI分析 → 系统设计 → 开发设计 → 编码实现”顺序执行。
4. `design/02-requirements/PRD.md` 已覆盖用户管理增强、角色权限维护、OA发起增强、OA与物资联动、物资图片与报销凭证上传。
5. `design/03-architecture/Permission-Design.md` 已覆盖 RBAC + 用户单独授权模型，以及部分物资图片上传权限。
6. `design/04-database/Data-Dictionary.md` 已包含 `file_attachment`、`inv_item_image`、`oa_instance_attachment`、`inv_reimbursement_voucher`、`oa_form_material_draft` 等附件相关表方向。

### 0.3 本轮用户反馈缺口

1. OA流程定义里要支持新增、修改、删除流程。
2. OA流程定义里要支持对流程内容修改，增加灵活的审批节点。
3. 新增菜单管理，按角色、按人物设置可以看到的菜单列表。
4. 物资档案和库存查询里仍然没有办法传图片。

本计划不重复上一轮 IAM / OA / Inventory 全量计划，而是针对上述四个缺口进行补齐。

---

## 1. 执行原则

本轮属于结构性变更，涉及流程定义、流程节点、菜单权限、附件上传、文件访问、前端页面和接口权限，必须先补文档再编码。

执行顺序：

1. 新增本计划到 `plans/`。
2. 更新 `design/02-requirements/PRD.md`。
3. 更新 `design/03-architecture/Permission-Design.md`。
4. 更新 `design/04-database/Data-Dictionary.md`、`Table-Index.md`、`ERD.md`。
5. 更新 `design/05-api/API-Index.md`、`OpenAPI.yaml`。
6. 更新 `dev/` 模块开发文档。
7. 后端实现。
8. 前端实现。
9. 单元测试、集成测试、权限测试、上传测试、回归测试。

禁止直接跳到代码实现。

---

## 2. 需求拆解

## 2.1 OA流程定义新增、修改、删除

### 2.1.1 目标

OA流程不能只内置固定的入库、出库、报销流程，也不能只停留在“定义”概念层面。需要在页面和接口上真实支持流程定义的新增、修改、删除、启停用和发布。

### 2.1.2 功能范围

| 功能 | 要求 |
|---|---|
| 流程定义列表 | 展示流程编码、名称、版本、状态、是否内置、创建人、更新时间 |
| 新增流程 | 支持创建新的流程定义，配置基础信息和初始节点 |
| 修改流程基础信息 | 支持修改名称、说明、启用状态、适用业务类型 |
| 修改流程内容 | 支持修改表单schema、审批节点、节点处理人、提醒策略 |
| 删除流程 | 未产生实例的流程允许删除；已产生实例的流程只能停用/软删除 |
| 启用/停用流程 | 停用后不能新发起，但历史实例保留 |
| 发布流程 | 草稿确认后发布为可发起流程 |
| 复制流程 | 可从已有流程复制出新流程，减少配置成本 |

### 2.1.3 版本规则

流程定义必须版本化：

```text
流程编码 code 稳定不变
流程版本 version 递增
流程实例绑定 process_definition_id + version
```

规则：

1. 草稿流程可以直接修改。
2. 已发布但未产生实例的流程可以直接修改。
3. 已发布且已产生实例的流程，修改流程内容时必须生成新版本。
4. 历史流程实例继续使用旧版本，不受新版本影响。
5. 删除已产生实例的流程只做软删除或停用，不允许物理删除。

---

## 2.2 OA流程内容和审批节点灵活修改

### 2.2.1 目标

OA流程定义需要支持管理员编辑流程内容和审批节点。首期不做复杂BPMN，但必须支持足够灵活的顺序审批配置。

### 2.2.2 节点能力

| 能力 | 首期要求 |
|---|---|
| 新增节点 | 在流程中增加审批节点 |
| 修改节点 | 修改节点名称、处理人模式、审批策略、驳回策略 |
| 删除节点 | 删除草稿节点；已发布版本通过新版本变更 |
| 排序 | 支持上移、下移或直接设置排序值 |
| 节点类型 | `APPROVAL`审批、`CC`抄送，首期默认审批节点 |
| 处理人模式 | 指定用户、指定角色、汇报上级、发起人自选 |
| 审批策略 | 首期支持任一人审批；预留会签/顺序会签 |
| 驳回策略 | 驳回到发起人、驳回到上一节点、终止流程 |
| 提醒策略 | 节点级提醒频率、超时提醒、催办冷却时间 |
| 条件扩展 | 预留金额阈值、物资类型等条件字段，首期不实现复杂分支引擎 |

### 2.2.3 处理人模式

| 模式 | 说明 |
|---|---|
| `USER` | 指定一个或多个具体用户 |
| `ROLE` | 指定一个或多个角色，任一拥有该角色的 enabled 用户可处理 |
| `SUPERVISOR` | 自动解析发起人的 `report_to_user_id` |
| `INITIATOR_SELECTED` | 发起流程时由发起人选择审批人，需在候选范围内 |

### 2.2.4 异常规则

1. 节点没有处理人时，流程定义不可发布。
2. 汇报上级节点如果发起人没有上级，流程不可提交或进入配置异常，不允许静默跳过。
3. 角色节点如果角色下没有 enabled 用户，流程定义不可发布或发起时报错。
4. 删除节点后必须重新校验节点顺序。
5. 流程至少需要一个审批节点。

---

## 2.3 菜单管理

### 2.3.1 目标

新增系统菜单管理能力，使系统管理员可以控制不同角色、不同用户登录后能看到哪些菜单。

### 2.3.2 菜单管理范围

| 功能 | 要求 |
|---|---|
| 菜单树维护 | 支持新增、修改、删除一级/二级/三级菜单 |
| 菜单基础字段 | 菜单编码、名称、路径、组件、图标、排序、端类型、状态 |
| 按角色配置菜单 | 给某个角色勾选可见菜单 |
| 按用户配置菜单 | 给某个用户额外加授或撤销菜单 |
| 当前用户菜单 | 登录后返回当前用户最终可见菜单树 |
| 菜单与权限点绑定 | 菜单可绑定一个或多个权限点，用于按钮显示和路由守卫 |
| 审计 | 菜单增删改、角色菜单配置、用户菜单配置都必须记录审计日志 |

### 2.3.3 菜单可见规则

```text
最终可见菜单 = 角色菜单 + 用户菜单加授 - 用户菜单撤销
```

注意：

```text
菜单可见性 != 接口访问权限
```

菜单只决定前端是否展示入口，接口权限仍必须由后端权限系统强校验。不能因为菜单隐藏就省略接口权限校验，也不能因为菜单被加授就自动获得接口写权限。

### 2.3.4 菜单分类建议

```text
系统管理
├── 用户管理
├── 角色权限
└── 菜单管理

OA管理
├── OA发起
├── 我的流程
├── 待办审批
└── 流程定义

物资管理
├── 物资档案
├── 库存查询
├── 入库管理
└── 出库管理
```

默认可见关系：

| 菜单 | 默认角色 |
|---|---|
| 用户管理 | `SYSTEM_ADMIN` |
| 角色权限 | `SYSTEM_ADMIN` |
| 菜单管理 | `SYSTEM_ADMIN` |
| 流程定义 | `SYSTEM_ADMIN`、`OA_ADMIN` |
| OA发起 | `EMPLOYEE`、`OA_ADMIN`、`SYSTEM_ADMIN` |
| 待办审批 | 有审批权限的用户 |
| 物资档案 | `SYSTEM_ADMIN`、`INVENTORY_ADMIN`可写；其他有读权限用户只读 |
| 库存查询 | `SYSTEM_ADMIN`、`INVENTORY_ADMIN`、有库存读权限用户 |

---

## 2.4 物资档案和库存查询图片上传

### 2.4.1 目标

当前分支已有附件表设计方向，但物资档案和库存查询页面仍无法真正上传图片。本轮要把图片上传打通到页面、接口、文件存储、数据库关联、权限校验和展示。

### 2.4.2 物资档案图片

物资档案支持：

| 功能 | 要求 |
|---|---|
| 上传主图 | 每个物资支持一张主图 |
| 上传多图 | 每个物资支持多张详情图 |
| 图片预览 | 新增/编辑页、详情页可预览 |
| 列表缩略图 | 物资档案列表显示主图缩略图 |
| 删除图片 | 仅解除图片与物资的业务关联，默认不物理删除文件 |
| 替换主图 | 上传新主图后旧主图变为普通图片或解除主图关系 |
| 权限控制 | 仅 `SYSTEM_ADMIN`、`INVENTORY_ADMIN` 可维护图片 |

### 2.4.3 库存查询图片

库存查询支持：

| 功能 | 要求 |
|---|---|
| 展示物资主图 | 库存列表/详情展示物资档案主图 |
| 上传库存现场图 | 对某个库存记录上传货架图、现场图、库位图 |
| 查看库存图片 | 库存详情页可查看多张库存图片 |
| 删除库存图片 | 有权限用户可删除库存图片关联 |
| 权限控制 | 仅 `SYSTEM_ADMIN`、`INVENTORY_ADMIN` 可上传/删除库存图片 |
| 只读用户 | 有 `inventory:stock:read` 仅能查看，不显示上传/删除按钮 |

### 2.4.4 文件限制

| 项 | 要求 |
|---|---|
| 文件类型 | `image/jpeg`、`image/png`、`image/webp` |
| 单文件大小 | 默认不超过 5MB |
| 数量限制 | 物资主图1张；物资详情图最多10张；库存现场图最多20张 |
| 存储方式 | 首期可本地存储，预留OSS/S3字段 |
| 安全 | 禁止按原文件名直接落盘；禁止路径穿越；访问文件要做权限校验 |

---

## 3. 文档更新计划

### 3.1 新增计划

新增：

```text
plans/oa20260624-feature-oa-process-menu-image-fix.plan.md
```

### 3.2 更新 `design/02-requirements/PRD.md`

新增或补充：

```text
## 15. OA流程定义维护增强
## 16. OA流程节点灵活配置
## 17. 菜单管理
## 18. 物资档案与库存查询图片上传补齐
```

补充验收标准：

1. 系统管理员/OA管理员可以新增、修改、删除/停用流程定义。
2. 已产生实例的流程不允许物理删除，只能停用或软删除。
3. 已发布且产生实例的流程修改时必须生成新版本。
4. 流程内容支持新增、修改、删除、排序审批节点。
5. 节点处理人支持指定用户、指定角色、汇报上级、发起人自选。
6. 系统管理员可以维护菜单树。
7. 系统管理员可以按角色配置菜单。
8. 系统管理员可以按用户加授/撤销菜单。
9. 当前用户登录后只返回最终可见菜单。
10. 菜单隐藏不能替代后端接口鉴权。
11. 物资档案可上传主图和多图。
12. 库存查询可展示物资图，并可上传库存现场图。
13. 无权限用户不能上传、删除图片。

### 3.3 更新 `design/03-architecture/Permission-Design.md`

新增权限点：

```text
oa:process-definition:create
oa:process-definition:update
oa:process-definition:delete
oa:process-definition:publish
oa:process-node:write

menu:read
menu:write
menu:role-bind
menu:user-bind

file:upload
file:read
file:delete

inventory:item:image:write
inventory:stock:image:write
```

补充：

1. 菜单权限计算公式。
2. 菜单权限与接口权限的边界。
3. 图片上传的业务二次鉴权规则。
4. AI 不允许调用流程定义写接口、菜单管理接口、上传接口、删除接口。

### 3.4 更新 `design/04-database/Data-Dictionary.md`

补充或明确：

```text
oa_process_definition
oa_process_node
oa_process_node_assignee
sys_menu
iam_role_menu
iam_user_menu_override
file_attachment
inv_item_image
inv_stock_image
```

其中 `inv_stock_image` 是本轮建议新增，用于解决库存查询图片上传缺口。若采用统一附件关系表，则需明确 `biz_type=inventory_stock` 的关系，不再单独建表。

### 3.5 更新 `design/04-database/Table-Index.md`

补充索引：

```text
uk_oa_process_definition_code_version(code, version)
idx_oa_process_definition_status(status)
idx_oa_process_node_definition_sort(process_definition_id, sort_order)
uk_sys_menu_code(code)
idx_sys_menu_parent(parent_id)
uk_iam_role_menu(role_id, menu_id)
uk_iam_user_menu_override(user_id, menu_id)
idx_file_attachment_biz(biz_type, biz_id)
idx_inv_item_image_item(item_id)
idx_inv_stock_image_stock(stock_id)
```

### 3.6 更新 `design/05-api/API-Index.md` 和 `OpenAPI.yaml`

新增接口分组：

```text
OA流程定义接口
OA流程节点接口
菜单管理接口
当前用户菜单接口
文件上传接口
物资图片接口
库存图片接口
```

每个接口必须标注功能说明、调用方、权限要求、请求参数、返回参数、错误码、AI是否可调用。

---

## 4. 数据库设计

## 4.1 OA流程定义

扩展 `oa_process_definition`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 主键 |
| `code` | varchar(80) | 流程编码 |
| `name` | varchar(120) | 流程名称 |
| `version` | int | 流程版本 |
| `status` | varchar(30) | `draft`、`enabled`、`disabled`、`archived` |
| `is_builtin` | boolean | 是否系统内置 |
| `form_schema` | jsonb | 表单schema |
| `description` | text | 说明 |
| `created_by` | bigint | 创建人 |
| `updated_by` | bigint | 更新人 |
| `published_at` | timestamptz | 发布时间 |
| `created_at` | timestamptz | 创建时间 |
| `updated_at` | timestamptz | 更新时间 |
| `deleted_at` | timestamptz | 软删除时间 |

唯一约束：

```text
uk_oa_process_definition_code_version(code, version)
```

## 4.2 OA流程节点

扩展 `oa_process_node`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 主键 |
| `process_definition_id` | bigint | 流程定义ID |
| `node_key` | varchar(80) | 节点稳定编码 |
| `node_name` | varchar(120) | 节点名称 |
| `node_type` | varchar(30) | `APPROVAL`、`CC`、`START`、`END` |
| `sort_order` | int | 节点顺序 |
| `assignee_mode` | varchar(30) | `USER`、`ROLE`、`SUPERVISOR`、`INITIATOR_SELECTED` |
| `approve_policy` | varchar(30) | `ANY_ONE`、`ALL`、`SEQUENTIAL` |
| `reject_policy` | varchar(30) | `BACK_TO_START`、`BACK_TO_PREVIOUS`、`TERMINATE` |
| `condition_json` | jsonb | 条件配置，首期可为空 |
| `reminder_json` | jsonb | 节点提醒配置 |
| `config_json` | jsonb | 扩展配置 |
| `created_at` | timestamptz | 创建时间 |
| `updated_at` | timestamptz | 更新时间 |
| `deleted_at` | timestamptz | 软删除时间 |

## 4.3 菜单表

新增 `sys_menu`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 主键 |
| `parent_id` | bigint | 父菜单ID |
| `code` | varchar(80) | 菜单编码 |
| `name` | varchar(120) | 菜单名称 |
| `client_type` | varchar(30) | `web_admin`、`wecom_miniapp`、`wechat_miniapp` |
| `path` | varchar(255) | 前端路由 |
| `component` | varchar(255) | 前端组件 |
| `icon` | varchar(80) | 图标 |
| `sort_order` | int | 排序 |
| `visible` | boolean | 是否可见 |
| `status` | varchar(30) | `enabled`、`disabled` |
| `permission_codes` | jsonb | 绑定权限点 |
| `created_by` | bigint | 创建人 |
| `updated_by` | bigint | 更新人 |
| `created_at` | timestamptz | 创建时间 |
| `updated_at` | timestamptz | 更新时间 |
| `deleted_at` | timestamptz | 软删除时间 |

新增 `iam_role_menu`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 主键 |
| `role_id` | bigint | 角色ID |
| `menu_id` | bigint | 菜单ID |
| `created_by` | bigint | 创建人 |
| `created_at` | timestamptz | 创建时间 |

新增 `iam_user_menu_override`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 主键 |
| `user_id` | bigint | 用户ID |
| `menu_id` | bigint | 菜单ID |
| `effect` | varchar(20) | `grant`、`deny` |
| `reason` | varchar(255) | 原因 |
| `created_by` | bigint | 创建人 |
| `created_at` | timestamptz | 创建时间 |

## 4.4 图片附件

当前分支已有 `file_attachment`、`inv_item_image` 方向，本轮需要补齐库存图片。

推荐新增 `inv_stock_image`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 主键 |
| `stock_id` | bigint | 库存记录ID |
| `attachment_id` | bigint | 附件ID |
| `usage_type` | varchar(30) | `stock_scene`、`shelf`、`location` |
| `sort_order` | int | 排序 |
| `created_by` | bigint | 创建人 |
| `created_at` | timestamptz | 创建时间 |
| `deleted_at` | timestamptz | 软删除时间 |

保留 `inv_item_image`：

| 字段 | 说明 |
|---|---|
| `item_id` | 物资ID |
| `attachment_id` | 附件ID |
| `usage_type` | `main_image`、`gallery` |
| `sort_order` | 排序 |

也可以使用统一附件关系表，但为了和当前分支已有 `inv_item_image` 设计保持一致，建议采用 `file_attachment + inv_item_image + inv_stock_image`。

---

## 5. 接口设计

## 5.1 OA流程定义接口

| 方法 | 路径 | 说明 | 权限 | AI |
|---|---|---|---|---|
| `GET` | `/api/v1/oa/process-definitions` | 查询流程定义列表 | `oa:process:read` | 否 |
| `POST` | `/api/v1/oa/process-definitions` | 新增流程定义 | `oa:process-definition:create` | 否 |
| `GET` | `/api/v1/oa/process-definitions/{id}` | 查看流程定义详情 | `oa:process:read` | 否 |
| `PUT` | `/api/v1/oa/process-definitions/{id}` | 修改流程定义基础信息 | `oa:process-definition:update` | 否 |
| `DELETE` | `/api/v1/oa/process-definitions/{id}` | 删除/停用流程定义 | `oa:process-definition:delete` | 否 |
| `POST` | `/api/v1/oa/process-definitions/{id}/publish` | 发布流程定义 | `oa:process-definition:publish` | 否 |
| `POST` | `/api/v1/oa/process-definitions/{id}/copy` | 复制流程定义 | `oa:process-definition:create` | 否 |

## 5.2 OA流程节点接口

| 方法 | 路径 | 说明 | 权限 | AI |
|---|---|---|---|---|
| `GET` | `/api/v1/oa/process-definitions/{id}/nodes` | 查询流程节点 | `oa:process:read` | 否 |
| `PUT` | `/api/v1/oa/process-definitions/{id}/nodes` | 全量保存流程节点 | `oa:process-node:write` | 否 |
| `POST` | `/api/v1/oa/process-definitions/{id}/nodes/validate` | 校验节点配置 | `oa:process-node:write` | 否 |

## 5.3 菜单管理接口

| 方法 | 路径 | 说明 | 权限 | AI |
|---|---|---|---|---|
| `GET` | `/api/v1/system/menus` | 查询菜单树 | `menu:read` | 否 |
| `POST` | `/api/v1/system/menus` | 新增菜单 | `menu:write` | 否 |
| `PUT` | `/api/v1/system/menus/{id}` | 修改菜单 | `menu:write` | 否 |
| `DELETE` | `/api/v1/system/menus/{id}` | 删除菜单 | `menu:write` | 否 |
| `GET` | `/api/v1/system/roles/{roleId}/menus` | 查询角色菜单 | `menu:read` | 否 |
| `PUT` | `/api/v1/system/roles/{roleId}/menus` | 保存角色菜单 | `menu:role-bind` | 否 |
| `GET` | `/api/v1/system/users/{userId}/menu-overrides` | 查询用户菜单覆盖 | `menu:read` | 否 |
| `PUT` | `/api/v1/system/users/{userId}/menu-overrides` | 保存用户菜单覆盖 | `menu:user-bind` | 否 |
| `GET` | `/api/v1/me/menus` | 查询当前用户可见菜单 | 登录用户 | 否 |

## 5.4 物资图片接口

| 方法 | 路径 | 说明 | 权限 | AI |
|---|---|---|---|---|
| `POST` | `/api/v1/inventory/items/{itemId}/images` | 上传物资图片 | `inventory:item:image:write` | 否 |
| `GET` | `/api/v1/inventory/items/{itemId}/images` | 查询物资图片 | `inventory:item:read` | 否 |
| `PUT` | `/api/v1/inventory/items/{itemId}/images/{imageId}/main` | 设置主图 | `inventory:item:image:write` | 否 |
| `DELETE` | `/api/v1/inventory/items/{itemId}/images/{imageId}` | 删除物资图片关联 | `inventory:item:image:write` | 否 |

## 5.5 库存图片接口

| 方法 | 路径 | 说明 | 权限 | AI |
|---|---|---|---|---|
| `POST` | `/api/v1/inventory/stocks/{stockId}/images` | 上传库存图片 | `inventory:stock:image:write` | 否 |
| `GET` | `/api/v1/inventory/stocks/{stockId}/images` | 查询库存图片 | `inventory:stock:read` | 否 |
| `DELETE` | `/api/v1/inventory/stocks/{stockId}/images/{imageId}` | 删除库存图片关联 | `inventory:stock:image:write` | 否 |

## 5.6 通用文件接口

| 方法 | 路径 | 说明 | 权限 | AI |
|---|---|---|---|---|
| `POST` | `/api/v1/files/upload` | 通用上传 | `file:upload` + 业务二次鉴权 | 否 |
| `GET` | `/api/v1/files/{fileId}` | 读取文件 | `file:read` + 业务可见性 | 否 |
| `DELETE` | `/api/v1/files/{fileId}` | 删除/标记删除 | `file:delete` + 业务二次鉴权 | 否 |

---

## 6. 后端开发任务

## 6.1 OA流程定义

新增/完善：

```text
OaProcessDefinitionController
OaProcessDefinitionService
OaProcessNodeService
OaProcessDefinitionRepository / Mapper
OaProcessNodeRepository / Mapper
OaProcessDefinitionValidator
```

实现重点：

1. 流程定义CRUD。
2. 流程发布。
3. 流程复制。
4. 流程删除/停用保护。
5. 流程版本化。
6. 节点配置保存。
7. 节点合法性校验。
8. 审计日志。

## 6.2 菜单管理

新增：

```text
SystemMenuController
SystemMenuService
RoleMenuService
UserMenuOverrideService
CurrentUserMenuService
```

实现重点：

1. 菜单树维护。
2. 角色菜单绑定。
3. 用户菜单加授/撤销。
4. 当前用户菜单计算。
5. 菜单与权限点绑定。
6. 审计日志。

## 6.3 文件与图片

新增/完善：

```text
FileAttachmentController
FileAttachmentService
FileStorageService
LocalFileStorageService
InventoryItemImageService
InventoryStockImageService
```

实现重点：

1. 上传文件类型校验。
2. 文件大小校验。
3. 文件安全落盘。
4. 附件元数据保存。
5. 物资图片关联。
6. 库存图片关联。
7. 图片删除只解除关联。
8. 图片访问权限校验。
9. 列表查询返回缩略图/访问URL。

---

## 7. 前端开发任务

## 7.1 OA流程定义页面

新增/完善页面：

```text
OA管理 / 流程定义
├── 流程定义列表
├── 新增流程弹窗/页面
├── 编辑流程基础信息
├── 编辑表单内容
├── 编辑审批节点
├── 发布流程
└── 删除/停用流程
```

节点编辑首期采用表格 + 表单配置方式，不做拖拽流程设计器。

字段：节点名称、节点类型、处理人模式、指定用户/角色、审批策略、驳回策略、提醒配置、排序。

## 7.2 菜单管理页面

新增：

```text
系统管理 / 菜单管理
├── 菜单树维护
├── 角色菜单配置
└── 用户菜单配置
```

要求：

1. 支持菜单树展示。
2. 支持新增同级/子级菜单。
3. 支持绑定路由、组件、图标、排序。
4. 支持绑定权限点。
5. 支持角色勾选菜单。
6. 支持用户菜单加授/撤销。
7. 保存前二次确认。

## 7.3 物资档案图片

物资档案新增/编辑页增加：

1. 主图上传。
2. 多图上传。
3. 图片预览。
4. 图片删除。
5. 设置主图。
6. 无权限用户隐藏上传和删除按钮。

物资档案列表增加：

1. 主图缩略图列。
2. 无图片占位图。

## 7.4 库存查询图片

库存查询页增加：

1. 物资主图展示。
2. 库存现场图入口。
3. 库存图片上传按钮。
4. 库存图片预览。
5. 库存图片删除。
6. 无权限用户仅可查看，不显示上传/删除入口。

---

## 8. 测试计划

## 8.1 OA流程定义测试

1. 新增流程定义成功。
2. 修改草稿流程成功。
3. 发布流程成功。
4. 删除未产生实例的流程成功。
5. 删除已产生实例的流程时不允许物理删除。
6. 已产生实例的流程修改时生成新版本。
7. 历史流程实例仍绑定旧版本。
8. 节点为空时不可发布。
9. 角色节点无可用审批人时不可发布或不可发起。
10. 汇报上级节点在发起人无上级时不可发起或进入配置异常。

## 8.2 菜单管理测试

1. SYSTEM_ADMIN 可访问菜单管理。
2. 非 SYSTEM_ADMIN 不能访问菜单管理接口。
3. 角色绑定菜单后，该角色用户可见菜单。
4. 用户加授菜单后，即使角色无该菜单也可见。
5. 用户撤销菜单后，即使角色有该菜单也不可见。
6. 菜单隐藏后，直接调用无权限接口仍被后端拒绝。
7. 删除父菜单时校验子菜单处理逻辑。

## 8.3 图片上传测试

1. INVENTORY_ADMIN 可上传物资主图。
2. INVENTORY_ADMIN 可上传物资多图。
3. INVENTORY_ADMIN 可上传库存现场图。
4. 普通员工不能上传物资图片。
5. 普通员工不能上传库存图片。
6. 只读库存用户可查看图片但不能删除。
7. 非图片文件上传失败。
8. 超过大小限制上传失败。
9. 删除图片后业务页面不再展示。
10. 文件访问接口不能越权读取。

## 8.4 回归测试

1. 用户管理仍正常。
2. 角色权限仍正常。
3. OA入库流程仍可发起、审批、入库。
4. OA出库流程仍可发起、审批、出库。
5. 报销流程仍要求上传凭证。
6. 物资档案新增、修改、删除权限仍只对 SYSTEM_ADMIN / INVENTORY_ADMIN 开放。
7. AI 仍不能调用写接口、审批接口、上传接口。

---

## 9. 验收标准

## 9.1 OA流程定义

- Given OA管理员进入流程定义页面 When 点击新增流程并保存 Then 新流程定义创建成功。
- Given OA管理员编辑流程内容 When 增加审批节点并发布 Then 新发起流程按新节点流转。
- Given 流程已有历史实例 When 修改流程节点 Then 系统生成新版本，不影响历史实例。
- Given 流程已有历史实例 When 删除流程 Then 系统拒绝物理删除，只允许停用/软删除。
- Given 节点处理模式为角色 When 该角色任意 enabled 用户审批 Then 流程进入下一节点。
- Given 节点处理模式为汇报上级 When 发起人没有上级 Then 系统不允许静默跳过审批。

## 9.2 菜单管理

- Given 系统管理员 When 新增菜单 Then 菜单树保存成功。
- Given 系统管理员 When 为角色勾选菜单 Then 该角色用户重新登录后可见菜单。
- Given 系统管理员 When 对用户单独撤销菜单 Then 该用户即使角色有菜单也不可见。
- Given 普通员工 When 访问菜单管理 Then 页面不可见且接口返回无权限。
- Given 用户无接口权限 When 直接调用接口 Then 后端拒绝，不受菜单显示影响。

## 9.3 物资与库存图片

- Given 物资管理员 When 在物资档案上传主图 Then 物资列表和详情展示该图片。
- Given 物资管理员 When 在物资档案上传多图 Then 详情页可预览多张图片。
- Given 物资管理员 When 在库存查询上传库存现场图 Then 库存详情展示该图片。
- Given 普通员工 When 进入库存查询 Then 可查看有权限库存数据和图片，但无上传/删除入口。
- Given 上传非法文件类型 When 提交 Then 系统返回明确错误。
- Given 上传超大图片 When 提交 Then 系统返回明确错误。
- Given 删除图片 When 删除成功 Then 业务页面不再展示，审计日志保留。

---

## 10. 实施顺序

### Step 1：确认分支

```bash
git fetch origin
git checkout feature/oa-iam-inventory-upload-20260624
git pull origin feature/oa-iam-inventory-upload-20260624
```

### Step 2：提交计划文件

新增：

```text
plans/oa20260624-feature-oa-process-menu-image-fix.plan.md
```

### Step 3：补齐设计文档

按顺序更新：

1. `design/02-requirements/PRD.md`
2. `design/03-architecture/Permission-Design.md`
3. `design/04-database/Data-Dictionary.md`
4. `design/04-database/Table-Index.md`
5. `design/04-database/ERD.md`
6. `design/05-api/API-Index.md`
7. `design/05-api/OpenAPI.yaml`
8. `dev/` 模块开发文档

### Step 4：数据库迁移

新增/调整：

1. OA流程定义状态、版本、表单schema字段。
2. OA流程节点配置字段。
3. 菜单表。
4. 角色菜单关系表。
5. 用户菜单覆盖表。
6. 库存图片关联表或统一附件关联。
7. 图片相关索引和唯一约束。

### Step 5：后端实现

1. OA流程定义CRUD。
2. OA流程节点保存与校验。
3. 菜单管理。
4. 当前用户菜单树。
5. 文件上传。
6. 物资图片。
7. 库存图片。
8. 审计日志。

### Step 6：前端实现

1. 流程定义页面。
2. 节点配置页面。
3. 菜单管理页面。
4. 角色菜单配置。
5. 用户菜单配置。
6. 物资图片上传。
7. 库存图片上传。

### Step 7：测试与回归

1. 单元测试。
2. 接口测试。
3. 权限测试。
4. 上传安全测试。
5. 前端手工验收。
6. OA入库/出库/报销全链路回归。

---

## 11. 风险清单

| 风险 | 影响 | 应对 |
|---|---|---|
| 流程定义修改影响历史实例 | 历史审批记录不一致 | 强制流程版本化，实例绑定版本 |
| 删除流程导致审计断链 | 历史实例无法解释 | 已产生实例的流程只能停用/软删除 |
| 节点配置过度复杂 | 首期开发周期失控 | 首期只做顺序审批，复杂分支预留字段 |
| 菜单权限和接口权限混淆 | 越权访问 | 菜单只控制可见性，接口必须后端鉴权 |
| 用户菜单加授/撤销计算错误 | 菜单显示异常 | 明确公式并做单元测试 |
| 图片上传存在安全风险 | 任意文件上传、越权访问 | 限制类型/大小/路径，文件访问二次鉴权 |
| 图片过大导致页面卡顿 | 物资/库存页面加载慢 | 限制大小，列表加载缩略图 |
| 本地存储不适合长期生产 | 多机部署文件不同步 | 首期本地存储，预留 OSS/S3 适配 |
| AI 误调用上传/写接口 | 安全边界破坏 | 上传、菜单、流程定义写接口全部 AI=false |

---

## 12. 本轮不做范围

本轮不做：

1. 拖拽式BPMN流程设计器。
2. 复杂条件分支、并行网关、排他网关。
3. 图片AI识别。
4. 图片自动压缩/CDN分发。
5. 多租户菜单隔离。
6. 菜单灰度发布。
7. 对象存储多云完整适配。
8. AI自动创建流程或自动审批。

---

## 13. 完成自检清单

- [ ] 是否已基于 `feature/oa-iam-inventory-upload-20260624` 分支开发。
- [ ] 是否新增本计划到 `plans/`。
- [ ] 是否补齐 PRD。
- [ ] 是否补齐权限设计。
- [ ] 是否补齐数据库字典、索引、ERD。
- [ ] 是否补齐 API 文档和 OpenAPI。
- [ ] OA流程定义是否支持新增、修改、删除/停用。
- [ ] OA流程内容是否支持新增/修改/删除/排序审批节点。
- [ ] 流程修改是否支持版本化。
- [ ] 菜单管理是否支持菜单树维护。
- [ ] 菜单是否支持按角色配置。
- [ ] 菜单是否支持按用户加授/撤销。
- [ ] 当前用户菜单树是否正确。
- [ ] 后端接口是否独立做权限校验。
- [ ] 物资档案是否可以上传主图和多图。
- [ ] 库存查询是否可以上传和展示库存图片。
- [ ] 普通员工是否无法上传/删除物资和库存图片。
- [ ] 文件上传是否限制类型和大小。
- [ ] 图片访问是否有权限校验。
- [ ] 审计日志是否覆盖流程定义、菜单配置、图片上传/删除。
- [ ] AI 是否无法调用写接口、上传接口、菜单接口、流程定义接口。
