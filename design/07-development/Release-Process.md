# 发布与 CI/CD

## 1. 首期发布方式

- 使用 Docker Compose 交付。
- 后端镜像由 `backend/Dockerfile` 构建。
- Web 管理端镜像由 `frontend/admin/Dockerfile` 构建。
- PostgreSQL 初始化脚本位于 `infra/db/init/`。

## 2. 发布门禁

- 后端测试通过。
- 前端构建通过。
- OpenAPI 文档与 `API-Index.md` 同步。
- 数据字典与初始化 DDL 同步。
- AI 只读账号权限验证通过。
