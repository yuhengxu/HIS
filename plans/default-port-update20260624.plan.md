# 默认端口调整计划

## 目标

- 后端默认端口从 `8080` 调整为 `18080`。
- Web 管理端默认端口从 `5173` 调整为 `15173`。
- 启动脚本、开发配置、Docker 配置和文档中的默认端口保持一致。

## 范围

- 修改开发启动脚本 Linux/macOS 与 Windows PowerShell 版本。
- 修改后端 Spring Boot 默认端口。
- 修改 Web 管理端 Vite 默认端口与后端代理默认端口。
- 修改 Dockerfile、Compose 与 Nginx 后端代理端口。
- 更新开发文档与相关计划文档中的默认端口说明。
- 不修改业务逻辑、数据库结构、API 定义或权限模型。

## 执行步骤

1. 扫描旧端口 `8080`、`5173` 的引用位置。
2. 将配置默认端口统一替换为 `18080`、`15173`。
3. 保留环境变量覆盖能力：`BACKEND_PORT`、`ADMIN_WEB_PORT`。
4. 运行脚本语法检查。
5. 使用新默认端口启动验证，检查健康接口与前端页面。
6. 更新验证记录。

## 验收标准

- [x] `./scripts/dev-start.sh start --no-infra --skip-install` 默认启动到 `18080` 与 `15173`。
- [x] 启动成功输出 `SUCCESS`。
- [x] `curl http://localhost:18080/api/v1/system/health` 返回 `UP`。
- [x] `curl -I http://localhost:15173` 返回 `200 OK`。
- [ ] `./scripts/dev-start.sh stop` 可停止脚本记录的进程。本轮为保留公网访问服务，未执行停止。

## 验证记录

- `bash -n scripts/dev-start.sh` 通过。
- `START_TIMEOUT_SECONDS=120 ./scripts/dev-start.sh start --no-infra --skip-install` 使用默认端口启动成功，并输出 `SUCCESS`。
- 本地后端健康检查返回 `UP`。
- 本地 Web 管理端返回 `200 OK`。
- `ss` 确认监听：`*:18080`、`0.0.0.0:15173`。
- 本机网卡地址为 `10.2.0.2`，公网 IP `82.156.67.222` 通过云侧 NAT/EIP 转发。

## 风险

- 公网访问仍依赖云服务器安全组或外层网络策略放行 `15173` 与 `18080`。
- 如果旧端口上仍有其它任务运行，本轮不会主动停止无关进程。
