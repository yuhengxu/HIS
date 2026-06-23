# 物资管理开发说明

依据：`Data-Dictionary.md` §4、`API-Index.md` §3、`Permission-Design.md` §2。

## 首期目标

- 物资分类、物资档案、供应商、仓库。
- 库存余额查询。
- 入库、出库、盘点与库存流水。

## 权限要求

- 查询类接口使用 `inventory:*:read`。
- 写入类接口使用 `inventory:*:write`。
- AI 仅允许调用物资档案列表和库存查询两个只读接口。
