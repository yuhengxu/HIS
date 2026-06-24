# 一键启动脚本 permission denied 排障计划

## 目标

- 修复 `scripts/dev-start.sh` 执行时报 `permission denied` 的问题。
- 确认脚本权限、目录权限、挂载选项和换行格式。
- 补充文档中的权限修复说明。

## 范围

- 检查并修复 `scripts/dev-start.sh` 的执行权限。
- 必要时检查 `scripts/` 目录权限与当前工作区挂载选项。
- 不修改业务代码、API、数据库或权限模型。

## 执行顺序

1. 检查脚本文件权限、目录权限、文件类型和挂载选项。
2. 修复脚本执行位与换行格式。
3. 验证 `--help` 和 `status` 可执行。
4. 更新开发文档中的常见权限问题处理方式。

## 验收标准

- `scripts/dev-start.sh --help` 可直接执行。
- `scripts/dev-start.sh status` 可直接执行。
- 文档包含 `chmod +x scripts/dev-start.sh` 与 `bash scripts/dev-start.sh ...` 备用方式。

## 风险

- 如果用户从 Windows 文件系统或 noexec 挂载运行，执行位可能仍不可用；此时需使用 `bash scripts/dev-start.sh start` 方式绕过。
