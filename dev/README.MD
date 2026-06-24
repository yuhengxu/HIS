# dev · 开发文档

编码阶段的模块任务拆解与开发说明。design/ 定义「做什么、怎么设计」，dev/ 定义「怎么实现、当前进度」。

## 与 design/ 的分工

| 目录 | 定位 | 示例 |
|---|---|---|
| [design/](../design/) | 需求与设计依据（权威） | PRD、SAD、数据字典、OpenAPI |
| **dev/**（本目录） | 开发执行与任务追踪 | 模块开发说明、Sprint 任务、接口实现清单 |

## 目录规划（待建）

```
dev/
├── README.MD              ← 本文件
├── his/                   ← HIS 模块开发文档
├── kangyang/              ← 康养模块开发文档
├── platform/              ← 共享平台层（账户、网关、AI 接入、审计、OA、共享物资）开发文档
├── oa/                    ← OA 基座开发文档
└── inventory/             ← 物资管理开发文档
```

## 首期工程骨架

| 目录 | 说明 | 依据 |
|---|---|---|
| `backend/` | Java 25 LTS + Spring Boot 4.1 模块化单体 | `SAD.md` §2、§3 |
| `frontend/admin/` | Vue 3.5 + TypeScript 6 + Vite 8 + Element Plus 2 Web 管理端 | `SAD.md` §2 |
| `frontend/mobile/` | uni-app Vue 3 通道 + Vue 3.5 移动端骨架 | `SAD.md` §2 |
| `infra/db/init/` | PostgreSQL schema 与 AI 只读账号初始化 | `Deployment.md` §3 |
| `docker-compose.yml` | 首期一键部署入口 | `Deployment.md` |

## 后端包边界

首期后端是一个 Spring Boot 模块化单体，但包结构必须反映独立业务边界：

| Java 包 | 说明 |
|---|---|
| `com.health.platform` | 共享平台能力：IAM、认证、审计、AI 接入、OA 基座、共享物资、公共响应 |
| `com.health.his` | HIS 独立业务域 |
| `com.health.kangyang` | 康养独立业务域 |

开发约束：

- HIS 与康养互为并列业务域，不允许互相作为父包或子包。
- 共享能力只能放在 `platform` 层，业务模块通过接口调用共享能力。
- 新增业务能力前，先在 design/ 中确认归属，再创建对应包与开发文档。

## 本地一键启动

依据 `SAD.md` §2、§4 与 `Deployment.md`，本地开发可使用脚本同时启动后端与 Web 管理端。

### Linux/macOS

```bash
chmod +x scripts/dev-start.sh
scripts/dev-start.sh start
```

### Windows PowerShell

```powershell
powershell -ExecutionPolicy Bypass -File scripts/dev-start.ps1 -Action start
```

默认行为：

- 先通过 `docker compose up -d postgres redis` 启动 PostgreSQL 与 Redis。
- 启动后端：`backend/` 下执行 `mvn spring-boot:run`，端口 `18080`。
- 启动 Web 管理端：`frontend/admin/` 下执行 `npm run dev -- --host 0.0.0.0`，端口 `15173`。
- 启动前会检查端口占用，默认检查 `18080`、`15173`、`5432`、`6379`。
- 脚本会清理当前进程中的代理环境变量，保持项目默认直连策略。
- 前后端进程 pid 记录在 `.run/backend.pid` 与 `.run/admin-web.pid`。
- 前后端日志写入 `.run/logs/backend.log` 与 `.run/logs/admin-web.log`。
- `start` 为后台运行模式，脚本会等待后端健康检查和前端页面就绪，输出 `SUCCESS` 后退出。
- 后端与前端开发服务均绑定 `0.0.0.0`，默认公网访问地址按 `82.156.67.222` 输出。

### 启动、停止、重启、状态

Linux/macOS：

```bash
scripts/dev-start.sh start
scripts/dev-start.sh stop
scripts/dev-start.sh restart
scripts/dev-start.sh status
```

Windows PowerShell：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/dev-start.ps1 -Action start
powershell -ExecutionPolicy Bypass -File scripts/dev-start.ps1 -Action stop
powershell -ExecutionPolicy Bypass -File scripts/dev-start.ps1 -Action restart
powershell -ExecutionPolicy Bypass -File scripts/dev-start.ps1 -Action status
```

参数说明：

| 参数 | Linux/macOS | Windows PowerShell | 说明 |
|---|---|---|---|
| 启动 | `start` | `-Action start` | 启动 PostgreSQL、Redis、后端与 Web 管理端；未指定 action 时默认启动 |
| 停止 | `stop` | `-Action stop` | 停止 `.run/*.pid` 记录的后端与 Web 管理端进程 |
| 重启 | `restart` | `-Action restart` | 先执行停止，再执行启动 |
| 状态 | `status` | `-Action status` | 查看 `.run/*.pid` 记录的进程状态，并尝试显示 PostgreSQL/Redis Compose 状态 |
| 跳过基础设施启动 | `--no-infra` | `-NoInfra` | `start` / `restart` 时不执行 `docker compose up -d postgres redis` |
| 停止基础设施 | `--stop-infra` | `-StopInfra` | `stop` / `restart` 时同时执行 `docker compose stop postgres redis` |
| 跳过依赖安装 | `--skip-install` | `-SkipInstall` | 即使 `frontend/admin/node_modules` 不存在，也不自动执行 `npm install` |
| 帮助 | `--help` | `-Help` | 输出完整参数说明 |

可选参数：

```bash
scripts/dev-start.sh start --no-infra
scripts/dev-start.sh restart --stop-infra
scripts/dev-start.sh start --skip-install
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts/dev-start.ps1 -Action start -NoInfra
powershell -ExecutionPolicy Bypass -File scripts/dev-start.ps1 -Action restart -StopInfra
powershell -ExecutionPolicy Bypass -File scripts/dev-start.ps1 -Action start -SkipInstall
```

注意事项：

- `stop` 只停止由脚本记录 pid 的进程，不会主动杀掉其它占用 `18080` 或 `15173` 的进程。
- 如手动结束进程导致 pid 文件过期，`stop` 会清理 stale pid 文件。
- Docker Compose 的 PostgreSQL 与 Redis 默认随 `start` 启动，但不会随 `stop` 停止，除非显式传入 `--stop-infra` / `-StopInfra`。

### 端口冲突处理

启动脚本会在启动前检查端口占用：

| 服务 | 默认端口 | 环境变量 |
|---|---:|---|
| 后端 | `18080` | `BACKEND_PORT` |
| Web 管理端 | `15173` | `ADMIN_WEB_PORT` |
| PostgreSQL | `5432` | `POSTGRES_PORT` |
| Redis | `6379` | `REDIS_PORT` |
| 公网地址输出 | `82.156.67.222` | `PUBLIC_HOST` |
| 启动等待秒数 | `90` | `START_TIMEOUT_SECONDS` |

如果后端或前端端口已被占用，脚本会输出占用进程并停止启动。处理方式：

```bash
ss -ltnp | grep ':15173'
ss -ltnp | grep ':18080'
```

停止已有进程后重新启动，或临时改端口：

```bash
BACKEND_PORT=19080 ADMIN_WEB_PORT=16173 ./scripts/dev-start.sh start --no-infra
```

前端开发服务会把 `/api` 代理到当前 `BACKEND_PORT`，也可以显式指定：

```bash
VITE_API_PROXY_TARGET=http://localhost:19080 BACKEND_PORT=19080 ADMIN_WEB_PORT=16173 ./scripts/dev-start.sh start --no-infra
```

公网访问默认输出：

```text
Admin web public: http://82.156.67.222:15173
Backend public:   http://82.156.67.222:18080
```

如果云服务器安全组或系统防火墙未放行端口，脚本本地启动成功后公网仍无法访问。需要至少放行 Web 管理端端口 `15173`，如需公网直接访问后端 API，再放行 `18080`。

如果 PostgreSQL/Redis 端口已由本项目 Docker Compose 服务占用，脚本会复用；如果被其它服务占用，脚本会停止并提示。已有基础设施时可以跳过基础设施启动：

```bash
./scripts/dev-start.sh start --no-infra
```

### 常见权限问题

如果 Linux/macOS 执行脚本时报 `permission denied`，先修复目录与脚本执行权限：

```bash
chmod 755 scripts
chmod +x scripts/dev-start.sh
scripts/dev-start.sh status
```

如果当前文件系统不允许直接执行脚本，例如部分 Windows 挂载目录或 `noexec` 挂载，可用 `bash` 显式执行：

```bash
bash scripts/dev-start.sh start
bash scripts/dev-start.sh stop
```

如果启动 PostgreSQL/Redis 时报 Docker socket `permission denied`，说明当前 shell 没有 Docker daemon 访问权限。先检查当前会话是否已生效 `docker` 组：

```bash
id
groups
docker ps
```

如果 `ubuntu` 已经在 `docker` 组，但当前 `id` 没显示 `docker`，在当前终端执行：

```bash
newgrp docker
./scripts/dev-start.sh start
```

或者退出 SSH 重新登录后再启动。临时绕过方式：

```bash
sudo docker compose up -d postgres redis
./scripts/dev-start.sh start --no-infra
```

如果 Docker 拉取 `postgres:16` 或 `redis:7-alpine` 时访问 Docker Hub 超时，先验证直连与代理：

```bash
curl -I --connect-timeout 10 https://registry-1.docker.io/v2/
curl -I --proxy http://127.0.0.1:7890 https://registry-1.docker.io/v2/
```

当直连超时、代理可用时，为 Docker daemon 配置代理：

```bash
sudo mkdir -p /etc/systemd/system/docker.service.d
sudo tee /etc/systemd/system/docker.service.d/http-proxy.conf >/dev/null <<'EOF'
[Service]
Environment="HTTP_PROXY=http://127.0.0.1:7890"
Environment="HTTPS_PROXY=http://127.0.0.1:7890"
Environment="NO_PROXY=localhost,127.0.0.1,::1"
EOF
sudo systemctl daemon-reload
sudo systemctl restart docker
```

验证镜像拉取：

```bash
sudo docker pull redis:7-alpine
sudo docker pull postgres:16
```

如果 PostgreSQL 容器日志出现 `cannot open directory '/docker-entrypoint-initdb.d/': Permission denied`，说明宿主机初始化 SQL 目录权限过紧，需要让容器只读访问：

```bash
chmod 755 infra infra/db infra/db/init
chmod 644 infra/db/init/*.sql
sudo docker compose rm -sf postgres
sudo docker volume rm his_postgres-data
sudo docker compose up -d postgres redis
```

## 编写规则

- 开发任务须引用 design/ 对应文档章节（如「依据 PRD.md §3.2」）
- 涉及表/接口/权限变更时，先更新 design/ 再编码
- 单模块开发文档不得违背 [README.MD](../README.MD) 宪法约束
