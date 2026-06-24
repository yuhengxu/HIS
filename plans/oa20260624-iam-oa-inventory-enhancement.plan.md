# OA + 用户权限 + 物资管理增强任务计划

文件名：`oa20260624-iam-oa-inventory-enhancement.plan.md`  
项目：`yuhengxu/HIS`  
目标分支：`main` 最新代码拉取后新建开发分支  
日期：2026-06-24  
范围：用户管理、角色权限、OA 流程发起、物资档案权限、OA 与物资关联、图片/凭证上传

---

## 0. 依据与约束

本计划依据仓库现有 `.codex/rules`、`AGENTS.md`、`README.MD`、`GUIDE.md`、`design/` 和既有 `plans/oa20260623.plan.md` 编写。

必须遵守以下项目约束：

1. **先计划后执行**：每轮需求先生成或引用 `plans/` 下执行计划，再推进文档、代码、测试。
2. **文档驱动**：执行顺序为需求文档 → 设计文档 → 开发文档 → 代码 → 测试，禁止跳过文档直接写代码。
3. **权限可审计**：用户、角色、权限、物资、OA 审批、附件上传等关键行为必须记录审计日志。
4. **AI 只读**：AI/LLM 在数据库层仅允许 `SELECT`，不得写权限、写库存、发起审批或上传附件。
5. **模块化单体优先**：首期继续采用共享平台基础层 + OA 模块 + 物资模块的模块化单体实现，不拆微服务。
6. **前端轻量**：Web 管理端、微信小程序、企业微信小程序均需考虑低性能设备与弱网场景。

---

## 1. 本轮需求清单

| # | 需求 | 计划处理方式 |
|---|---|---|
| 1 | 拉取最新版本 | 本地执行 `git checkout main && git pull --ff-only`，确认最新提交后新建开发分支 |
| 2 | 用户管理，新建用户功能完善 | 完善后端用户创建、初始密码、角色分配、上级、企业微信 ID、启停用、审计；补齐前端表单 |
| 3 | 角色权限支持新增/删除/修改，并对每个权限给出说明 | 完善角色 CRUD、权限点字典、权限说明、角色-权限绑定、删除约束、审计 |
| 4 | 只有系统管理员角色能看到用户管理、角色权限 | 前端菜单、路由、按钮与后端接口均增加 `SYSTEM_ADMIN` 强校验 |
| 5 | OA 流程支持新建 OA 流程，可选择创建哪一个 OA 流程 | 区分“流程定义管理”和“流程实例发起”；新增 OA 发起入口，选择入库/出库/报销等流程后创建实例 |
| 6 | 物资档案只有系统管理员、物资管理员可新增修改删除 | 物资档案写操作只允许 `SYSTEM_ADMIN`、`INVENTORY_ADMIN`；其他角色只读或无权 |
| 7 | OA 创建时可关联物资档案物资；无该物资则创建新物资 | OA 表单支持物资选择；不存在时提交新物资信息，按权限即时创建或走审批后创建正式物资档案 |
| 8 | OA 流程、物资档案、库存查询支持上传物资图片 | 建通用附件/图片能力；物资图片与 OA 表单、库存查询关联，限制格式、大小、权限 |
| 9 | 报销流程上传报销凭证 | 报销 OA 表单增加必填凭证附件，审批记录与报销记录可追溯 |

---

## 2. 执行分支与版本同步

### 2.1 拉取最新代码

开发前必须先执行：

```bash
git checkout main
git pull --ff-only origin main
git status
```

确认工作区干净后创建分支：

```bash
git checkout -b feature/oa-iam-inventory-upload-20260624
```

### 2.2 最新版本确认项

- 确认默认分支为 `main`。
- 确认当前最新提交已包含上一轮 OA + 物资管理第一阶段文档和基础代码。
- 确认 `plans/oa20260623.plan.md` 已存在，但本轮需求属于增强计划，应新增本文件，不直接覆盖旧计划。

---

## 3. 文档更新任务

### 3.1 更新 `design/02-requirements/PRD.md`

新增或补充以下章节：

```text
## 10. 用户管理增强
## 11. 角色权限维护增强
## 12. OA 流程发起增强
## 13. OA 与物资档案联动
## 14. 物资图片与报销凭证上传
```

必须补充 Given/When/Then 验收条件：

- 系统管理员创建用户后，新用户初始密码为 `qwer1234`。
- 非 `SYSTEM_ADMIN` 用户无法看到用户管理、角色权限菜单。
- 非 `SYSTEM_ADMIN` 用户直接调用用户/角色权限接口时后端拒绝。
- 系统管理员可以新增、修改、删除或禁用角色。
- 每个权限点在前端和接口返回中都有中文说明。
- 用户在 OA 发起页可以选择发起“物资入库 OA / 物资出库 OA / 报销 OA”。
- OA 表单选择物资时可从物资档案搜索；未找到时可录入新物资信息。
- 非物资管理员发起新物资时，先形成 OA 内的物资草稿，审批通过后由系统创建正式物资档案。
- 物资档案写操作仅 `SYSTEM_ADMIN`、`INVENTORY_ADMIN` 可执行。
- 报销 OA 未上传报销凭证时不可提交。
- OA、物资档案、库存查询中的图片上传须校验文件类型、大小和权限。

### 3.2 更新 `design/03-architecture/Permission-Design.md`

补充权限说明、菜单可见性规则和接口强校验规则。

核心规则：

```text
用户管理菜单：仅 SYSTEM_ADMIN 可见
角色权限菜单：仅 SYSTEM_ADMIN 可见
物资档案新增/修改/删除：仅 SYSTEM_ADMIN、INVENTORY_ADMIN 可执行
OA 流程定义维护：SYSTEM_ADMIN、OA_ADMIN
OA 流程实例发起：拥有 oa:instance:create 的用户
报销凭证上传：发起报销 OA 的用户或具备 oa:attachment:write 的处理人
物资图片上传：SYSTEM_ADMIN、INVENTORY_ADMIN；OA 中的新物资图片随 OA 审批流转
```

### 3.3 更新 `design/04-database/Data-Dictionary.md`

补充附件、图片、凭证、角色权限增强相关表字段：

- `iam_role`：补充 `status`、`sort_order`、`is_system`、`deleted_at`。
- `iam_permission`：补充 `name`、`description`、`domain`、`resource_type`、`is_system`、`enabled`。
- `iam_role_permission`：补充唯一约束和审计字段。
- `file_attachment`：通用附件表。
- `inv_item_image`：物资图片关联表。
- `oa_instance_attachment`：OA 实例附件关联表。
- `inv_reimbursement_voucher`：报销凭证关联表。
- `oa_form_material_draft`：OA 中临时创建的新物资草稿。

### 3.4 更新 `design/04-database/Table-Index.md`

补齐唯一索引、普通索引和外键：

- `uk_iam_role_code`
- `uk_iam_permission_code`
- `uk_iam_role_permission_role_permission`
- `idx_file_attachment_biz`
- `idx_inv_item_image_item`
- `idx_oa_instance_attachment_instance`
- `idx_reimbursement_voucher_record`
- `idx_oa_form_material_draft_instance`

### 3.5 更新 `design/05-api/API-Index.md` 与 `OpenAPI.yaml`

新增或补充以下 API 分组：

- IAM 用户管理 API
- 角色管理 API
- 权限点查询 API
- 角色权限绑定 API
- OA 流程定义查询 API
- OA 流程实例发起 API
- OA 表单物资搜索/草稿创建 API
- 物资档案图片上传 API
- OA 附件上传 API
- 报销凭证上传 API

### 3.6 更新 `dev/` 开发文档

建议新增：

```text
dev/iam-user-role-permission.md
dev/oa-process-start-and-material-link.md
dev/inventory-item-image-and-attachment.md
```

开发文档需写明模块边界、接口契约、权限点、表结构、前端页面、测试清单。

---

## 4. 权限点清单与说明

本轮必须保证权限点可在前端“角色权限”页面展示，并给出中文说明。

| 权限点 | 权限名称 | 说明 | 默认角色 |
|---|---|---|---|
| `iam:user:read` | 用户查看 | 查看用户列表、用户详情、用户角色与单独授权 | `SYSTEM_ADMIN` |
| `iam:user:create` | 用户新增 | 新建系统用户，设置基础信息、角色、汇报上级、企业微信 ID | `SYSTEM_ADMIN` |
| `iam:user:update` | 用户修改 | 修改用户资料、状态、角色、单独授权、汇报上级 | `SYSTEM_ADMIN` |
| `iam:user:disable` | 用户停用 | 停用用户登录与业务操作权限，保留审计数据 | `SYSTEM_ADMIN` |
| `iam:user:delete` | 用户删除 | 软删除用户，仅允许无未完结业务或满足归档条件时执行 | `SYSTEM_ADMIN` |
| `iam:role:read` | 角色查看 | 查看角色列表、角色详情、已绑定权限 | `SYSTEM_ADMIN` |
| `iam:role:create` | 角色新增 | 新增业务角色，设置角色编码、名称、说明和初始权限 | `SYSTEM_ADMIN` |
| `iam:role:update` | 角色修改 | 修改角色名称、说明、状态、排序和权限绑定 | `SYSTEM_ADMIN` |
| `iam:role:delete` | 角色删除 | 删除或禁用非系统内置角色；已有用户绑定时禁止硬删 | `SYSTEM_ADMIN` |
| `iam:permission:read` | 权限查看 | 查看权限点、权限域、权限说明，用于角色配置 | `SYSTEM_ADMIN` |
| `iam:permission:write` | 权限维护 | 新增、修改、启停权限点；系统内置权限不允许随意删除 | `SYSTEM_ADMIN` |
| `iam:user-permission:write` | 用户单独授权 | 对单个用户加授或撤销某个权限点 | `SYSTEM_ADMIN` |
| `oa:process:read` | OA 流程定义查看 | 查看可发起流程和流程定义详情 | `SYSTEM_ADMIN`、`OA_ADMIN`、`EMPLOYEE` |
| `oa:process:write` | OA 流程定义维护 | 新增、修改、启停 OA 流程定义和节点 | `SYSTEM_ADMIN`、`OA_ADMIN` |
| `oa:instance:create` | OA 流程发起 | 在 OA 发起页选择具体流程并提交申请 | `SYSTEM_ADMIN`、`OA_ADMIN`、`INVENTORY_ADMIN`、`EMPLOYEE` |
| `oa:instance:read` | OA 实例查看 | 查看本人发起、本人待办或授权范围内的 OA 实例 | 按角色与数据范围授权 |
| `oa:task:read` | OA 待办查看 | 查看本人待办、已办、相关审批任务 | 按角色与数据范围授权 |
| `oa:task:approve` | OA 审批处理 | 对本人或本角色待办进行通过、驳回、转交等处理 | 审批角色/处理人 |
| `oa:task:urge` | OA 催办 | 发起人对当前处理人执行催办，受冷却时间限制 | 流程发起人 |
| `oa:reminder:config` | OA 提醒配置 | 配置流程或节点级提醒频率、冷却时间、机器人 | `SYSTEM_ADMIN`、`OA_ADMIN` |
| `oa:attachment:write` | OA 附件上传 | 在 OA 表单或审批过程中上传物资图片、凭证等附件 | 发起人/处理人，按流程限制 |
| `inventory:item:read` | 物资档案查看 | 查看物资档案、图片、分类、规格、单位、价格摘要 | 按业务角色授权 |
| `inventory:item:write` | 物资档案维护 | 新增、修改、删除物资档案和物资图片 | `SYSTEM_ADMIN`、`INVENTORY_ADMIN` |
| `inventory:stock:read` | 库存查询 | 查询库存余额、库存流水、物资图片 | `SYSTEM_ADMIN`、`INVENTORY_ADMIN`、授权用户、AI 只读白名单 |
| `inventory:stock:write` | 库存调整 | 执行库存调整、盘点差异处理；首期谨慎开放 | `SYSTEM_ADMIN`、`INVENTORY_ADMIN` |
| `inventory:inbound:create` | 入库申请 | 发起物资入库 OA 或创建入库草稿 | `SYSTEM_ADMIN`、`INVENTORY_ADMIN`、授权用户 |
| `inventory:inbound:approve` | 入库审批 | 入库 OA 审批通过后生成入库单和库存流水 | 审批节点处理人 |
| `inventory:outbound:create` | 出库申请 | 发起物资出库 OA 或创建出库草稿 | `SYSTEM_ADMIN`、`INVENTORY_ADMIN`、授权用户 |
| `inventory:outbound:approve` | 出库审批 | 出库 OA 审批通过后生成出库单和库存流水 | 审批节点处理人 |
| `inventory:image:write` | 物资图片维护 | 上传、替换、删除物资主图和附图 | `SYSTEM_ADMIN`、`INVENTORY_ADMIN` |
| `finance:reimbursement:create` | 报销申请 | 发起报销 OA，填写金额、关联入库 OA、上传凭证 | 授权用户 |
| `finance:reimbursement:approve` | 报销审批 | 处理报销审批节点，确认凭证与金额 | `FINANCE_APPROVER` 或节点处理人 |
| `audit:read` | 审计查看 | 查看用户、权限、物资、库存、OA、附件操作日志 | `SYSTEM_ADMIN` |
| `ai-access:read` | AI 接入查看 | 查看 AI 可调用接口白名单和调用审计 | `SYSTEM_ADMIN`、`AI_CALLER` 只读 |

---

## 5. 用户管理增强设计

### 5.1 后端能力

用户创建需要支持以下字段：

| 字段 | 必填 | 说明 |
|---|---|---|
| `username` | 是 | 登录名，全局唯一 |
| `displayName` | 是 | 显示名称 |
| `phone` | 否 | 手机号，建议唯一校验 |
| `departmentName` | 否 | 首期简化部门字段 |
| `reportToUserId` | 否 | 汇报上级，用于 OA 上级审批节点 |
| `wecomUserId` | 否 | 企业微信用户 ID，用于后续个人提醒 |
| `roleIds` | 否 | 用户角色列表 |
| `permissionOverrides` | 否 | 用户单独加授/撤销权限 |
| `status` | 是 | `enabled` / `disabled` |

新建用户规则：

1. 默认初始密码为 `qwer1234`。
2. 数据库只保存密码哈希，不保存明文。
3. 首次登录建议强制修改密码；若当前系统未实现登录页，可先保留 `must_change_password` 字段或 TODO。
4. 用户名重复时返回明确错误码。
5. 新建、修改、停用、删除、重置密码均记录审计日志。
6. 删除采用软删除，不物理删除审计相关用户。

### 5.2 前端页面

用户管理页仅 `SYSTEM_ADMIN` 可见，包含：

- 用户列表：用户名、姓名、角色、部门、上级、状态、最后登录时间。
- 新增用户弹窗/页面：填写基础信息，勾选角色。
- 编辑用户：修改资料、角色、上级、企业微信 ID、状态。
- 单独授权：权限点加授/撤销，必须填写原因。
- 重置密码：重置为 `qwer1234` 或生成临时密码；首期按需求使用 `qwer1234`。

---

## 6. 角色权限增强设计

### 6.1 角色 CRUD

角色管理页仅 `SYSTEM_ADMIN` 可见。

必须支持：

1. 新增角色：编码、名称、说明、排序、启用状态、权限点。
2. 修改角色：名称、说明、排序、启停、权限点。
3. 删除角色：
   - 系统内置角色不允许删除，只允许视情况禁用非关键角色。
   - 已绑定用户的角色不允许直接删除，需要先解除用户绑定或转为禁用。
   - 删除采用软删除。
4. 角色权限绑定：批量勾选权限点，保存时增量更新。
5. 审计：新增、修改、删除、权限绑定变化必须记录操作人、目标角色、变化内容。

### 6.2 权限说明展示

权限点列表必须包含：

- 权限编码
- 权限名称
- 权限域
- 资源类型
- 权限说明
- 默认建议角色
- 是否系统内置
- 是否启用

前端角色编辑页中，权限按权限域分组展示：`IAM`、`OA`、`Inventory`、`Finance`、`Audit`、`AI Access`。

---

## 7. 系统管理员菜单与接口强校验

### 7.1 前端校验

非 `SYSTEM_ADMIN` 用户登录后：

- 不显示“用户管理”菜单。
- 不显示“角色权限”菜单。
- 不能通过直接输入 URL 打开对应页面。
- 不能看到用户新增、角色新增、权限配置按钮。

### 7.2 后端校验

后端接口不得只依赖前端隐藏菜单，必须强制校验：

```text
/system/users/**              -> requireRole(SYSTEM_ADMIN)
/system/roles/**              -> requireRole(SYSTEM_ADMIN)
/system/permissions/**        -> requireRole(SYSTEM_ADMIN)
/system/user-permissions/**   -> requireRole(SYSTEM_ADMIN)
```

如果未来允许超级管理员给某用户单独授权 IAM 权限，也不得突破“只有系统管理员角色能看到用户管理、角色权限”的产品要求。本轮以角色硬校验为准。

---

## 8. OA 流程发起增强

### 8.1 解决问题

当前设计已有“OA 流程定义”，但用户需求强调：OA 流程里要支持“新建 OA 流程”，即业务用户可以选择创建哪一个流程实例，而不是只维护流程定义。

因此需要明确两个概念：

| 概念 | 面向对象 | 说明 |
|---|---|---|
| 流程定义 | 管理员 | 配置流程模板、节点、处理人、提醒策略 |
| 流程实例 | 业务用户 | 选择某个流程定义后提交表单，生成一条实际 OA 流程 |

### 8.2 OA 发起页

新增 OA 发起入口：

```text
OA 中心
├── 发起流程
│   ├── 物资入库 OA
│   ├── 物资出库 OA
│   └── 报销 OA
├── 我的申请
├── 我的待办
├── 我的已办
└── 流程定义管理（SYSTEM_ADMIN / OA_ADMIN）
```

发起流程逻辑：

1. 查询启用状态的流程定义。
2. 用户选择流程类型。
3. 根据流程类型加载对应表单 schema。
4. 用户填写表单并上传附件。
5. 提交后创建 `oa_process_instance`。
6. 解析第一个节点处理人。
7. 创建待办任务并发送企业微信提醒。

### 8.3 内置流程

首期必须可选择发起：

- `MATERIAL_INBOUND`：物资入库 OA。
- `MATERIAL_OUTBOUND`：物资出库 OA。
- `REIMBURSEMENT`：报销 OA。

后续可以通过流程定义管理新增更多流程，但本轮重点是让用户能在 OA 中真正发起实例。

---

## 9. OA 与物资档案关联

### 9.1 物资选择

入库 OA、出库 OA 表单中的物资行支持：

- 按物资编码、名称、规格搜索物资档案。
- 选择已有物资后带出单位、规格、参考价格、图片。
- 支持多行物资明细。

### 9.2 无物资时创建新物资

由于“物资档案只有系统管理员、物资管理员可以新增修改删除”和“OA 中无物资时创建新物资”存在权限边界，需要按以下规则实现：

| 发起人权限 | 处理方式 |
|---|---|
| `SYSTEM_ADMIN` / `INVENTORY_ADMIN` | 可在 OA 表单中即时创建正式物资档案，并关联到 OA 明细 |
| 其他授权发起人 | 在 OA 表单中填写新物资信息，先保存为 `oa_form_material_draft`，审批通过且到达物资管理员确认节点后创建正式物资档案 |

新物资草稿字段：

- 物资名称
- 分类
- 类型：医疗 / 非医疗
- 规格
- 单位
- 默认价格
- 供应商
- 物资图片
- 创建原因

审批通过后的处理：

1. 校验是否已有同名同规格物资，避免重复建档。
2. 若不存在，创建 `inv_item`。
3. 关联 `inv_item_image`。
4. 将 OA 明细中的草稿 ID 回填为正式 `item_id`。
5. 继续生成入库单/出库单/报销记录。

---

## 10. 物资档案权限控制

物资档案写操作仅允许：

```text
SYSTEM_ADMIN
INVENTORY_ADMIN
```

需要控制的动作：

- 新增物资档案
- 修改物资基础信息
- 删除/停用物资档案
- 上传/替换/删除物资图片
- 维护物资价格
- 维护物资分类、供应商、仓库中与物资档案强相关的内容

其他用户即使拥有 `inventory:item:read`，也只能查看，不允许写。

后端接口建议：

```text
GET    /api/inventory/items              -> inventory:item:read
GET    /api/inventory/items/{id}         -> inventory:item:read
POST   /api/inventory/items              -> requireRole(SYSTEM_ADMIN, INVENTORY_ADMIN)
PUT    /api/inventory/items/{id}         -> requireRole(SYSTEM_ADMIN, INVENTORY_ADMIN)
DELETE /api/inventory/items/{id}         -> requireRole(SYSTEM_ADMIN, INVENTORY_ADMIN)
POST   /api/inventory/items/{id}/images  -> requireRole(SYSTEM_ADMIN, INVENTORY_ADMIN)
```

---

## 11. 图片与附件上传设计

### 11.1 通用附件表

新增 `file_attachment`：

| 字段 | 说明 |
|---|---|
| `id` | 附件 ID |
| `biz_type` | 业务类型：`OA`、`INVENTORY_ITEM`、`STOCK_QUERY`、`REIMBURSEMENT` |
| `biz_id` | 业务 ID，可为空，临时上传后再绑定 |
| `usage_type` | 用途：`material_image`、`reimbursement_voucher`、`oa_attachment` |
| `original_name` | 原始文件名 |
| `storage_path` | 存储路径 |
| `content_type` | MIME 类型 |
| `size_bytes` | 文件大小 |
| `checksum` | 文件哈希，用于去重和完整性校验 |
| `uploaded_by` | 上传人 |
| `created_at` | 上传时间 |
| `deleted_at` | 软删除时间 |

首期可使用本地 Docker volume 或服务器本地目录存储，预留对象存储接口。

### 11.2 文件校验

物资图片：

- 允许：`.jpg`、`.jpeg`、`.png`、`.webp`
- 单文件大小：建议不超过 5MB
- 数量：物资主图 1 张，附图最多 5 张

报销凭证：

- 允许：`.jpg`、`.jpeg`、`.png`、`.webp`、`.pdf`
- 单文件大小：建议不超过 10MB
- 数量：至少 1 个，最多 10 个

### 11.3 上传场景

| 场景 | 上传内容 | 权限 |
|---|---|---|
| OA 入库/出库 | 物资图片、补充说明附件 | 发起人或当前处理人，按流程状态限制 |
| 物资档案 | 物资主图、附图 | `SYSTEM_ADMIN`、`INVENTORY_ADMIN` |
| 库存查询 | 物资图片补充/维护入口 | 只有 `SYSTEM_ADMIN`、`INVENTORY_ADMIN` 显示上传按钮；普通用户只看图 |
| 报销 OA | 报销凭证 | 报销发起人必传；审批人只读或追加说明附件 |

---

## 12. 报销 OA 凭证设计

报销流程表单字段：

| 字段 | 必填 | 说明 |
|---|---|---|
| `reimbursementTitle` | 是 | 报销标题 |
| `amount` | 是 | 报销金额 |
| `relatedInboundInstanceId` | 否 | 关联已审批通过的入库 OA |
| `reason` | 是 | 报销事由 |
| `voucherAttachmentIds` | 是 | 报销凭证附件 ID 列表 |

提交规则：

1. 没有报销凭证不可提交。
2. 关联入库 OA 时，只能选择已审批通过且当前用户有权查看的入库 OA。
3. 报销金额应与入库金额进行提示性校验，首期可不强制一致，但必须给审批人展示差异。
4. 审批通过后生成 `inv_reimbursement_record` 和 `inv_reimbursement_voucher`。
5. 报销流程不直接变更库存。

---

## 13. 后端开发任务拆解

### 13.1 IAM 模块

- [ ] 完善用户实体、DTO、VO、Mapper、Service、Controller。
- [ ] 新增用户创建逻辑，默认密码 `qwer1234`，保存哈希。
- [ ] 支持用户角色分配、上级、企业微信 ID、启停用。
- [ ] 增加用户单独授权/撤销。
- [ ] 增加审计日志。
- [ ] 增加后端权限拦截器或注解：`requireRole(SYSTEM_ADMIN)`。

### 13.2 Role / Permission 模块

- [ ] 角色列表、详情、新增、修改、删除/禁用。
- [ ] 权限点列表，包含中文说明。
- [ ] 角色权限绑定保存。
- [ ] 系统内置角色和权限保护。
- [ ] 删除约束和软删除。
- [ ] 操作审计。

### 13.3 OA 模块

- [ ] 区分流程定义和流程实例。
- [ ] 新增流程发起接口：获取可发起流程列表。
- [ ] 新增流程实例创建接口。
- [ ] 入库、出库、报销三类表单 schema。
- [ ] OA 表单物资搜索和新物资草稿保存。
- [ ] 当前节点任务生成、审批、驳回、催办。
- [ ] 附件绑定 OA 实例。

### 13.4 Inventory 模块

- [ ] 物资档案写接口限制 `SYSTEM_ADMIN`、`INVENTORY_ADMIN`。
- [ ] 物资图片上传、设置主图、删除图片。
- [ ] 库存查询返回物资图片信息。
- [ ] OA 审批通过后创建物资、入库单、出库单、库存流水。
- [ ] 重复物资校验。

### 13.5 Attachment 模块

- [ ] 通用上传接口。
- [ ] 文件类型、大小、数量校验。
- [ ] 临时附件与业务附件绑定。
- [ ] 文件下载/预览接口。
- [ ] 附件权限校验。
- [ ] 删除附件采用软删除。

---

## 14. 前端开发任务拆解

### 14.1 用户管理

- [ ] 用户列表页。
- [ ] 新增用户弹窗/页面。
- [ ] 编辑用户弹窗/页面。
- [ ] 角色勾选组件。
- [ ] 上级用户选择组件。
- [ ] 用户单独授权组件。
- [ ] 非 `SYSTEM_ADMIN` 隐藏菜单和路由拦截。

### 14.2 角色权限

- [ ] 角色列表页。
- [ ] 新增/编辑角色表单。
- [ ] 权限点按权限域分组展示。
- [ ] 每个权限显示名称、编码、说明。
- [ ] 系统内置角色保护提示。

### 14.3 OA 发起

- [ ] OA 发起中心。
- [ ] 流程卡片：入库、出库、报销。
- [ ] 动态表单渲染。
- [ ] 物资搜索选择组件。
- [ ] 新物资录入组件。
- [ ] 附件上传组件。
- [ ] 报销凭证必填校验。

### 14.4 物资档案与库存查询

- [ ] 物资档案列表/详情显示图片。
- [ ] 物资图片上传、主图设置、删除。
- [ ] 库存查询显示物资图片。
- [ ] 只有 `SYSTEM_ADMIN`、`INVENTORY_ADMIN` 显示图片上传和物资维护按钮。

---

## 15. 测试计划

### 15.1 单元测试

- [ ] 用户创建：默认密码哈希、用户名唯一、角色绑定。
- [ ] 角色 CRUD：系统角色不可删、已有用户绑定角色不可硬删。
- [ ] 权限计算：角色权限 + 用户加授 - 用户撤销。
- [ ] 物资写权限：非管理员、非物资管理员被拒绝。
- [ ] OA 物资草稿：无物资时创建草稿并审批后转正式物资。
- [ ] 附件校验：类型、大小、数量限制。
- [ ] 报销 OA：无凭证不可提交。

### 15.2 接口测试

- [ ] 非 `SYSTEM_ADMIN` 调用用户管理接口返回 403。
- [ ] 非 `SYSTEM_ADMIN` 调用角色权限接口返回 403。
- [ ] `INVENTORY_ADMIN` 可以新增物资，普通员工不能新增物资档案。
- [ ] 普通员工可发起 OA，但不能直接写正式物资档案。
- [ ] 报销 OA 上传凭证后可提交。
- [ ] 库存查询接口返回物资图片 URL，但不暴露无权附件。

### 15.3 集成测试

- [ ] 入库 OA：选择已有物资 → 审批通过 → 入库单生成 → 库存增加。
- [ ] 入库 OA：填写新物资 → 审批通过 → 物资档案创建 → 库存增加。
- [ ] 出库 OA：选择已有物资 → 审批通过 → 出库单生成 → 库存扣减。
- [ ] 报销 OA：关联入库 OA + 上传凭证 → 审批通过 → 报销记录生成。
- [ ] 企业微信提醒失败不阻断 OA 主流程。

### 15.4 前端验收

- [ ] `SYSTEM_ADMIN` 可看到用户管理、角色权限。
- [ ] 非 `SYSTEM_ADMIN` 不可见且无法通过 URL 访问。
- [ ] 权限点说明完整展示。
- [ ] OA 发起页可选择发起哪类 OA。
- [ ] 物资图片在 OA、物资档案、库存查询中可展示。
- [ ] 报销凭证上传体验可用。

---

## 16. 验收标准汇总

本轮完成后必须满足：

1. 代码基于最新 `main` 分支开发。
2. 用户管理的新建用户功能可用，默认密码 `qwer1234`，密码哈希存储。
3. 角色权限支持新增、修改、删除/禁用，每个权限点都有说明。
4. 用户管理、角色权限只对 `SYSTEM_ADMIN` 可见且接口强校验。
5. OA 中可以选择发起物资入库、物资出库、报销流程实例。
6. 物资档案新增、修改、删除只允许 `SYSTEM_ADMIN`、`INVENTORY_ADMIN`。
7. OA 表单可关联已有物资；无物资时可走新物资草稿/创建流程。
8. OA、物资档案、库存查询支持物资图片上传/展示。
9. 报销 OA 必须上传报销凭证。
10. 文档、数据库、接口、权限、前端、测试均同步更新。
11. 所有权限、物资、OA、附件关键操作有审计日志。
12. AI 相关接口保持只读边界，不引入 AI 写权限。

---

## 17. 风险与处理

| 风险 | 影响 | 处理 |
|---|---|---|
| OA 中创建新物资与物资档案写权限冲突 | 普通用户可能绕过物资管理员直接建档 | 普通用户只创建物资草稿，审批通过并经物资管理员节点确认后创建正式档案 |
| 附件上传带来安全风险 | 恶意文件、超大文件、越权访问 | 限制类型/大小/数量，附件鉴权，软删除，后续可接入病毒扫描 |
| 角色删除影响历史审批和审计 | 历史数据不可解释 | 系统角色不可删，业务角色优先禁用，已有绑定时禁止硬删 |
| 菜单隐藏不足以保证安全 | 用户可直接调接口 | 后端所有敏感接口强制 `SYSTEM_ADMIN` 或指定角色校验 |
| 报销金额与入库金额关系复杂 | 财务规则不清导致验收争议 | 首期只做提示性校验和审批展示，不自动阻断，后续由财务规则扩展 |
| 库存查询上传图片语义不清 | 查询页可能承担维护功能 | 查询页只展示图片；上传按钮仅对物资管理员/系统管理员显示，本质调用物资图片维护能力 |

---

## 18. 建议执行顺序

1. 拉取最新代码，新建开发分支。
2. 更新本计划到 `plans/oa20260624-iam-oa-inventory-enhancement.plan.md`。
3. 更新 PRD、权限设计、数据字典、索引、API 文档、dev 文档。
4. 实现 IAM 用户管理增强。
5. 实现角色权限 CRUD 与权限说明展示。
6. 实现菜单/路由/接口的系统管理员强校验。
7. 实现通用附件上传能力。
8. 实现物资图片上传与展示。
9. 实现 OA 发起中心与流程实例创建。
10. 实现 OA 表单物资关联与新物资草稿。
11. 实现报销 OA 凭证上传与校验。
12. 补齐单元测试、接口测试、集成测试。
13. 自检：业务、权限、AI、安全、性能。
14. 提交代码并发起 PR。

---

## 19. 提交建议

建议拆分提交：

```text
chore(plan): add oa iam inventory enhancement plan
docs: update iam oa inventory permission and api design
feat(iam): enhance user role permission management
feat(file): add attachment upload support
feat(inventory): add item image permissions and display
feat(oa): add process start and material linkage
feat(finance): require reimbursement vouchers in oa flow
test: add iam oa inventory attachment permission tests
```
