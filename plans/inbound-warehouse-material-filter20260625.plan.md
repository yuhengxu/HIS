# 入库仓库物资过滤修复计划（2026-06-25）

## 目标

- 修复“发起入库 OA 选择非医疗物资仓库时仍出现医疗物资”的问题。
- 前后端同时约束：入库已有物资选择必须按仓库类型过滤。

## 原因

- 入库页使用 `inventoryApi.items()` 获取物资档案列表，没有传入仓库，也没有按仓库类型过滤。
- 已有的 `claimable-materials` 接口用于出库申领，入库没有对应的仓库过滤接口。

## 执行清单

- [x] 后端增加入库可选物资搜索接口，按仓库类型过滤。
- [x] 前端入库页改用该接口。
- [x] 切换仓库时清空已选物资并重新加载。
- [x] 测试/构建/重启验证。

## 验证记录

- `mvn test` 通过，37 个测试成功。
- `npm run build` 通过；仍有 Vite/Rolldown 依赖注释和 chunk 体积警告，不影响构建。
- 已重启开发栈。
- 接口验证：
  - `/api/v1/oa/instances/inbound-materials/search?warehouseId=2` 只返回非医疗物资“办公用纸”。
  - `/api/v1/oa/instances/inbound-materials/search?warehouseId=1` 只返回医疗物资“医用口罩”。
