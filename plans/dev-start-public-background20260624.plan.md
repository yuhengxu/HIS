# 公网访问与后台启动改造计划

## 目标

- Web 管理端和后端启动后可通过公网 IP `82.156.67.222` 访问。
- 一键启动脚本不再卡在 Spring Boot/Vite 前台界面。
- 启动完成后输出 `SUCCESS`、本地访问地址、公网访问地址、日志路径和停止命令。

## 范围

- 修改 Linux/macOS 启动脚本。
- 修改 Windows PowerShell 启动脚本。
- 更新开发文档。
- 不修改业务代码、API、数据库结构或权限模型。

## 默认访问地址

- 后端本地：`http://localhost:18080`
- 后端公网：`http://82.156.67.222:18080`
- Web 管理端本地：`http://localhost:15173`
- Web 管理端公网：`http://82.156.67.222:15173`

## 执行顺序

1. 检查当前脚本启动方式和监听地址。
2. 将前后端启动改为后台进程，pid 写入 `.run/`。
3. 将日志写入 `.run/logs/`。
4. 启动后等待端口就绪，成功后输出访问地址。
5. 确保后端和前端均监听 `0.0.0.0`。
6. 更新文档中的公网访问、后台启动、停止与查看日志说明。
7. 验证脚本语法和状态命令。

## 验收标准

- [x] `scripts/dev-start.sh start` 启动后退出命令行，不阻塞。
- [x] 成功启动时输出 `SUCCESS`。
- [x] 输出本地和公网访问地址。
- [x] `scripts/dev-start.sh stop` 可停止脚本启动的前后端进程。
- [x] 端口冲突仍会在启动前拦截。

## 验证记录

- `bash -n scripts/dev-start.sh` 通过。
- `BACKEND_PORT=18080 ADMIN_WEB_PORT=15173 START_TIMEOUT_SECONDS=120 ./scripts/dev-start.sh start --no-infra --skip-install` 通过，并输出 `SUCCESS`。
- `curl http://localhost:18080/api/v1/system/health` 返回 `UP`。
- `curl -I http://localhost:15173` 返回 `200 OK`。
- `ss` 确认后端与前端监听公网网卡：`*:18080`、`0.0.0.0:15173`。
- 本机 `ufw` 为 inactive，`iptables INPUT` 默认 ACCEPT，未发现针对测试端口的本机拒绝规则。
- `curl --noproxy '*' http://82.156.67.222:15173` 已可访问 Web 管理端；后端健康检查可通过前端 `/api` 代理访问。

## 风险

- 公网访问还依赖云服务器安全组、防火墙放行 `18080` 与 `15173`。
- 本轮只保证服务监听公网网卡，不修改云厂商安全组。
