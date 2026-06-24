# 本地开发脚本启动停止重启参数补全计划

## 目标

- 补全前后端一键脚本的参数说明。
- 支持显式启动、停止、重启与状态查看。
- Linux/macOS shell 与 Windows PowerShell 行为保持一致。
- 更新开发文档，使开发人员能明确知道每个参数的用途。

## 范围

- 修改 `scripts/dev-start.sh`。
- 修改 `scripts/dev-start.ps1`。
- 更新 `dev/README.MD`。
- 不修改业务代码、接口、数据库或权限模型。

## 执行顺序

1. 复查现有脚本启动方式与参数。
2. 设计统一 action：`start`、`stop`、`restart`、`status`。
3. 为本地开发进程增加 pid 文件管理。
4. 保持原有 `--no-infra`、`--skip-install` 行为。
5. 补全文档中的命令、参数、示例和注意事项。
6. 验证脚本语法、帮助输出与状态命令。

## 验收标准

- `scripts/dev-start.sh start|stop|restart|status` 可识别。
- `scripts/dev-start.ps1 -Action start|stop|restart|status` 可识别。
- `--help` / `-Help` 输出包含启动、停止、重启、状态与参数说明。
- 文档包含默认行为、端口、pid 文件位置、基础设施控制说明。

## 风险

- 停止脚本只管理由脚本记录 pid 的前后端进程，不主动杀掉其它占用同端口的进程。
- Docker Compose 的数据库与 Redis 默认随 start 启动；stop 是否停止基础设施由参数控制。
