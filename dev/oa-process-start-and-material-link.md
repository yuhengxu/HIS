# OA 流程发起与物资联动开发说明

依据：`plans/oa20260624-iam-oa-inventory-enhancement.plan.md` §8-§9；`PRD.md` §12-§13。

## 模块边界

- 后端包：`com.health.platform.oa`
- 前端页面：`/oa/start`、`/oa/inbound`、`/oa/outbound`、`/oa/reimbursement`

## 接口契约

| 接口 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 可发起流程 | GET | `/api/v1/oa/instances/startable` | 返回入库/出库/报销 |
| 物资搜索 | GET | `/api/v1/oa/instances/materials/search` | OA 表单选物资 |
| 发起入库 | POST | `/api/v1/oa/instances/inventory-inbound` | 支持 `itemId` 或 `newMaterial` |
| 发起报销 | POST | `/api/v1/oa/instances/reimbursement` | 必填 `voucherAttachmentIds` |
| 物资草稿 | POST | `/api/v1/oa/instances/{id}/material-drafts` | 非管理员新物资草稿 |

## 权限与业务规则

- 发起需 `oa:instance:create`。
- 管理员/物资管理员可在 OA 中即时创建正式物资。
- 普通员工新物资写入 `oa_form_material_draft`，审批通过后转 `inv_item`。

## 测试清单

- [ ] 发起页展示三类流程
- [ ] 报销无凭证不可提交
- [ ] 普通员工不能直接写正式物资档案
- [ ] 审批通过后草稿转正式物资并继续库存流程
