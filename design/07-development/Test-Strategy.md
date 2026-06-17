# 测试策略

<!-- TODO: 测试范围、测试分层（单元/集成/UAT）、P0 验收用例、AI 权限边界自动化测试 -->

## 测试依据

- 功能验收：[PRD.md](../02-requirements/PRD.md) Given/When/Then 条件
- 接口契约：[OpenAPI.yaml](../05-api/OpenAPI.yaml)、[API-Index.md](../05-api/API-Index.md)
- 权限边界：[Permission-Design.md](../03-architecture/Permission-Design.md)、[AI-Dev-Rules.md](./AI-Dev-Rules.md)
- AI 只读约束：AI 生成 SQL 仅允许 SELECT
