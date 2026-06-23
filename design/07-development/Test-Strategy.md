# 测试策略

## 测试依据

- 功能验收：[PRD.md](../02-requirements/PRD.md) Given/When/Then 条件
- 接口契约：[OpenAPI.yaml](../05-api/OpenAPI.yaml)、[API-Index.md](../05-api/API-Index.md)
- 权限边界：[Permission-Design.md](../03-architecture/Permission-Design.md)、[AI-Dev-Rules.md](./AI-Dev-Rules.md)
- AI 只读约束：AI 生成 SQL 仅允许 SELECT

## 首期测试范围

- 后端：JUnit 5 + Spring Boot Test；后续引入 Testcontainers 验证 PostgreSQL/Redis。
- 前端：Vitest 覆盖权限菜单、表单校验、物资列表状态逻辑；Playwright 覆盖核心流程。
- API：OpenAPI 契约校验，确保接口索引和权限矩阵一致。
- AI：验证 MCP 白名单和数据库只读账号，非 `SELECT` 操作必须失败。
- 部署：Docker Compose 在 Windows/Linux、局域网环境可启动。
