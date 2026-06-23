# 部署与运维方案

## 1. 部署目标

- 支持 Windows 与 Linux。
- 支持本地局域网部署与云端部署。
- 前端、后端、数据库、缓存通过 Docker Compose 一键启动。
- 依据文档：`README.MD` §3.2、§4；`SAD.md` §2。

## 2. 首期容器

| 服务 | 镜像/构建 | 端口 | 说明 |
|---|---|---|---|
| `postgres` | `postgres:16` | `5432` | 主业务库，初始化 schema 与 AI 只读账号 |
| `redis` | `redis:7-alpine` | `6379` | 会话、限流、缓存 |
| `backend` | `backend/Dockerfile` | `8080` | Spring Boot 模块化单体 |
| `admin-web` | `frontend/admin/Dockerfile` | `8081` | Web 管理端静态资源 + Nginx |

## 3. 配置原则

- 所有敏感配置通过环境变量传入，不提交真实密码。
- 局域网部署可使用 `docker compose up -d` 启动全部服务。
- 云端部署首期仍使用 Docker Compose，后续可迁移到 Kubernetes。
- 数据库初始化脚本位于 `infra/db/init/`，仅用于新环境初始化。

## 4. 健康检查

- 后端健康检查：`GET /api/v1/system/health`。
- Spring Actuator：`GET /actuator/health`。
- Nginx 静态站点以容器存活和首页返回为首期检查。

## 5. 运维基线

- PostgreSQL 数据卷必须独立挂载并纳入备份。
- 生产环境必须替换默认密码。
- 生产环境不得暴露 PostgreSQL/Redis 公网端口。
