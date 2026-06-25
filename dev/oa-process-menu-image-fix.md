# OA 流程定义、菜单与图片补齐开发说明

依据：`plans/oa20260624-feature-oa-process-menu-image-fix.plan.md`；`PRD.md` §15-§18。

## 模块边界

- OA 流程定义：`OaProcessDefinitionService`、`OaProcessDefinitionController`
- 菜单管理：`MenuService`、`SystemMenuController`、`MeController`
- 物资/库存图片：`InventoryStockService` 图片方法 + `AttachmentService`

## 关键规则

1. 流程修改产生实例时保存节点会生成新版本。
2. 内置流程不可删除；有实例的流程仅软删除/归档。
3. 菜单可见性独立于接口权限，后端接口仍强校验。
4. 物资/库存图片仅 `SYSTEM_ADMIN`、`INVENTORY_ADMIN` 可写。

## 测试清单

- [ ] 流程定义 CRUD、发布、节点保存
- [ ] 有实例流程保存节点生成新版本
- [ ] 菜单角色绑定与用户撤销
- [ ] 物资主图/多图上传
- [ ] 库存现场图上传
- [ ] 普通员工无法上传图片
