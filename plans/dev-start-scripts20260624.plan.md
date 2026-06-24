# 前后端一键启动脚本计划

## 目标

- 为开发环境提供前后端一键启动入口。
- 同时覆盖 Linux/macOS shell 与 Windows PowerShell。
- 默认启动后端 Spring Boot 与 Web 管理端 Vite dev server。
- 尽量不依赖代理，沿用项目默认直连策略。

## 范围

- 新增开发启动脚本。
- 更新开发文档中的启动说明。
- 不修改业务代码、数据库结构、权限模型和 API 行为。

## 执行顺序

1. 检查后端 Maven 启动方式、前端 npm scripts、端口与代理配置。
2. 新增 Linux/macOS 一键启动脚本。
3. 新增 Windows PowerShell 一键启动脚本。
4. 在开发文档中补充使用方式。
5. 做脚本语法与基础命令验证。

## 验收标准

- Linux/macOS 可通过一个脚本同时启动后端与 Web 管理端。
- Windows 可通过一个 PowerShell 脚本同时启动后端与 Web 管理端。
- 脚本在退出时尽量清理子进程。
- 文档说明包含启动命令、默认端口和前置条件。

## 风险

- 脚本只覆盖本地开发启动，不替代 Docker Compose 部署。
- 后端本地启动仍需要可用的 PostgreSQL 与 Redis，或使用项目后续补充的本地 profile。
