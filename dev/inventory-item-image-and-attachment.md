# 物资图片与附件上传开发说明

依据：`plans/oa20260624-iam-oa-inventory-enhancement.plan.md` §10-§11；`PRD.md` §14。

## 模块边界

- 后端包：`com.health.platform.attachment`、`com.health.platform.inventory`
- 通用表：`file_attachment`、`inv_item_image`、`oa_instance_attachment`、`inv_reimbursement_voucher`

## 接口契约

| 接口 | 方法 | 路径 | 权限 |
|---|---|---|---|
| 上传附件 | POST | `/api/v1/attachments` | 按 `usageType` 校验 |
| 下载/预览 | GET | `/api/v1/attachments/{id}/content` | 上传者或授权读角色 |
| 物资绑图 | POST | `/api/v1/inventory/items/{id}/images` | `SYSTEM_ADMIN` / `INVENTORY_ADMIN` |

## 文件校验

- 物资图片：jpg/jpeg/png/webp，≤ 5MB
- 报销凭证：jpg/jpeg/png/webp/pdf，≤ 10MB，至少 1 个

## 前端

- 报销 OA 凭证上传组件
- 物资档案主图展示
- 库存查询只读展示图片；上传按钮仅管理员可见

## 测试清单

- [ ] 非法文件类型被拒绝
- [ ] 超大文件被拒绝
- [ ] 非管理员无法维护物资图片
