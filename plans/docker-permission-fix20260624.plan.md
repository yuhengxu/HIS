# Docker daemon 权限排障计划

## 目标

- 解决一键启动脚本执行 `docker compose up -d postgres redis` 时无法连接 Docker daemon socket 的权限问题。
- 明确当前用户是否已加入 `docker` 组，以及当前登录会话是否已经生效。
- 增强脚本对 Docker 权限错误的提示。

## 范围

- 检查 `/var/run/docker.sock` 权限、当前用户组、Docker 服务状态。
- 必要时将当前用户加入 `docker` 组。
- 更新启动脚本的 Docker 权限提示。
- 不修改业务代码、API、数据库结构或权限模型。

## 执行顺序

1. 检查 Docker 服务状态、socket 权限、用户组与当前会话组。
2. 若用户未加入 `docker` 组，则加入。
3. 若已加入但当前会话未生效，则给出 `newgrp docker` 或重新登录方案。
4. 脚本中增加 Docker 权限错误提示，避免只输出底层 socket 错误。
5. 验证 `docker compose ps` 或 `docker ps` 权限。

## 验收标准

- 明确说明当前失败原因。
- 当前终端可通过可执行方案启动，或明确需要重新登录/`newgrp docker`。
- 脚本文档/提示能引导用户处理 Docker socket 权限。

## 风险

- 将用户加入 `docker` 组等同于给予该用户较高本机容器管理权限。
- 当前 shell 的组权限不会自动刷新，可能需要重新登录或使用 `newgrp docker`。
