# Docker Hub 镜像拉取超时排障计划

## 目标

- 解决 `docker compose up -d postgres redis` 拉取 Docker Hub 镜像超时的问题。
- 遵循网络策略：优先直连；直连不通时再为 Docker daemon 启用代理。
- 保持业务代码、API、数据库结构和权限模型不变。

## 范围

- 检查 Docker Hub 直连、代理连通性和本机代理端口。
- 必要时配置 Docker daemon systemd 代理。
- 验证 `redis:7-alpine` 与 `postgres:16` 镜像拉取或 Compose 启动。
- 补充开发文档中的 Docker 镜像拉取超时处理说明。

## 执行顺序

1. 检查 Docker Hub 直连是否可用。
2. 检查本机可用代理端口是否能访问 Docker Hub。
3. 若直连不可用而代理可用，配置 Docker daemon 代理 drop-in。
4. 重启 Docker daemon 并验证代理配置生效。
5. 拉取 `redis:7-alpine` 与 `postgres:16`，再验证 Compose 基础设施启动。
6. 更新文档中的排障说明。

## 验收标准

- 能明确说明直连或代理路径的测试结果。
- Docker daemon 能成功拉取 Redis/PostgreSQL 基础镜像。
- `docker compose up -d postgres redis` 可运行。
- 文档包含 Docker Hub 超时处理方式。

## 风险

- Docker daemon 代理配置会影响 Docker 拉取镜像的网络路径。
- 如果本机代理进程停止，Docker 后续拉取外部镜像会失败，需要恢复直连配置或重启代理。
