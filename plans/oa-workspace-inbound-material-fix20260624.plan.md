# OA 工作台与入库新增物资修复计划（2026-06-24）

## 1. 需求分析

- `employee` 发起物资入库 OA 后，工作台必须能看到自己发起的流程。
- 工作台加载不能因为待办或发起流程任一接口失败而整体空白。
- 物资入库 OA 需要恢复“新增物资”能力。
- 新增物资仍应遵守权限边界：普通员工通过 OA 提交新物资草稿，不直接写物资档案；管理员或物资管理员可直接落档。

## 2. 业务分析

- 发起人视角需要在工作台看到本人全部 OA 流程，包括起草、审批中、结束、驳回。
- 入库申请支持两种路径：
  - 选择已有物资。
  - 填写新增物资信息后发起 OA。

## 3. 数据分析

- 本轮不新增数据库表。
- 继续复用 `oa_form_material_draft` / 内存 `OaMaterialDraftRecord` 承载普通员工发起的新物资草稿。
- 流程实例仍以 `initiator_user_id` 作为本人流程过滤依据。

## 4. 权限分析

- `oa.instance.mine` 应允许拥有 `oa:instance:create` 的发起人读取本人流程，避免被管理级实例读取语义误伤。
- 新增物资草稿由 `oa:instance:create` + `inventory:inbound:create` 的入库流程承载。
- 物资档案直接创建仍限定 `SYSTEM_ADMIN` / `INVENTORY_ADMIN`。

## 5. AI 分析

- 本轮接口不开放 AI 调用。
- AI 不获得新增物资或发起 OA 的写权限。
- 本人流程查询涉及个人业务数据，AI 不可调用。

## 6. 系统设计

- 后端补充本人流程查询权限语义与测试。
- 前端工作台分别加载待办与本人流程，任何一侧失败都给出提示，不影响另一侧展示。
- 入库 OA 页面恢复新增物资表单，并与已有物资下拉二选一提交。

## 7. 开发设计

- 更新 `ProcessRuntimeService.myInstances` 权限校验。
- 更新 `Dashboard.vue` 加载逻辑与错误提示。
- 更新 `InboundApply.vue`，增加“选择已有/新增物资”切换、表单校验和提交体。
- 更新 API 文档中 `oa.instance.mine` 的权限与 AI 边界。
- 增加后端单元测试覆盖 `employee` 发起后可查询本人流程。

## 8. 执行清单

- [x] 修复本人流程查询权限与测试
- [x] 修复工作台前端加载健壮性
- [x] 恢复入库 OA 新增物资表单
- [x] 更新 API/权限文档
- [x] 运行前后端验证

## 9. 验证记录

- 后端 `mvn test` 通过：26 个测试，0 失败，0 错误。
- 前端 `npm run build` 通过。
- 已重启开发服务：前端 `15173`，后端 `18080`。
- 使用 `employee / qwer1234` 验证：
  - 登录返回权限包含 `oa:task:read`、`oa:instance:read`、`oa:instance:create`。
  - `/api/v1/oa/tasks/todo` 返回成功，不再 403。
  - 发起已有物资入库后，`/api/v1/oa/instances/mine` 可见该流程。
  - 发起新增物资入库后，`/api/v1/oa/instances/mine` 可见该流程，表单包含 `materialDraftId`。
