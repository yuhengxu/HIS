# OA 撤销与物资库/明细库拆分计划（2026-06-25）

## 1. 需求分析

- 发起人对自己发起且未结束的 OA 流程需要支持撤销。
- 物资库存需要区分“物资库总库存”和“物资明细库/入库人明细”。
- 入库按入库人记录明细，同时汇总到仓库 + 物资维度的总库存。
- 物品领用任何有发起权限的人都能申请，出库按仓库总库存校验与扣减，不应按申请人本人库存校验。
- 物品领用选择仓库后，物资下拉只能显示该仓库类型匹配的物资。

## 2. 业务分析

- OA 撤销只适用于发起人自己的运行中流程，撤销后待办不应继续出现在审批人工作台。
- 库存查询当前保留“本人入库明细/管理员全量”的可见规则。
- 物品领用使用仓库总库存，避免郭晓入库 10、徐东入库 20 时其他人无法领用 25 的问题。

## 3. 数据分析

- 当前 `StockRecord` 已代表入库人明细库存，键为 `ownerUserId:warehouseId:itemId`。
- 新增运行时聚合查询，按 `warehouseId:itemId` 汇总所有明细库存作为物资库总库存。
- 出库流水 `operatorUserId` 记录领用申请人；总库存扣减时按明细批次依次扣减，保留明细可追踪。
- OA 实例新增或复用状态表达撤销态；如枚举缺失则补充 `REVOKED`。

## 4. 权限分析

- 撤销接口：发起人本人 + `oa:instance:read`，仅运行中流程可撤销。
- 库存明细查询：保持系统管理员看全部，其他用户看本人入库明细。
- 物品领用：有 `oa:instance:create` / `inventory:outbound:create` 的用户可申请；出库校验使用总库存。

## 5. AI 分析

- 不新增 AI 可调用写接口。
- 总库存/明细库存读接口仍是只读；AI 如读取，必须继承调用方过滤边界。

## 6. 系统设计

- OA：
  - 新增撤销接口和前端按钮。
  - 撤销后实例进入撤销/结束状态，相关待办不再可处理。
- Inventory：
  - 增加总库存视图/可领用物资查询。
  - 出库时按仓库 + 物资总量校验，按明细批次扣减。
  - 物品领用页面按仓库类型过滤物资下拉。

## 7. 开发设计

- 后端：
  - `InstanceStatus` 增加 `REVOKED`，`ProcessRuntimeService` 增加撤销方法。
  - `OaInstanceController` 暴露撤销接口。
  - `InventoryStore` 增加总库存计算、按仓库类型搜索可领用物资、跨入库人扣减库存。
  - `InventoryStockService` 暴露领用物资搜索。
  - 测试覆盖流程撤销、总库存出库、仓库类型过滤。
- 前端：
  - API 增加撤销与可领用物资查询。
  - 工作台“我发起的流程”增加撤销按钮。
  - 物品领用按仓库加载匹配物资，切换仓库时清空物资选择。
- 文档：
  - 更新 API 索引、权限设计、数据字典说明。

## 8. 执行清单

- [x] 后端 OA 撤销
- [x] 后端库存总库/明细库与出库扣减
- [x] 前端撤销与物品领用过滤
- [x] 文档同步
- [x] 测试、构建、重启与验证

## 9. 验证记录

- `backend`: `mvn test` 通过，33 个测试成功。
- `frontend/admin`: `npm run build` 通过；Vite/Rolldown 对依赖包 `@vueuse/core` 的 PURE 注释和 chunk 体积给出警告，不影响构建结果。
- 已重启开发栈：`./scripts/dev-start.sh restart --no-infra --skip-install`。
- 验证接口：
  - `/api/v1/inventory/stock-summary` 返回仓库 + 物资维度总库存。
  - `/api/v1/oa/instances/claimable-materials/search?warehouseId=1` 仅返回医疗物资。
  - `/api/v1/oa/instances/claimable-materials/search?warehouseId=2` 仅返回非医疗物资。
  - `/api/v1/oa/instances/{instanceId}/revoke` 可撤销本人运行中流程，撤销后审批人待办消失。
