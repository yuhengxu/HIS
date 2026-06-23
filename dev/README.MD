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
├── care/                  ← 康养模块开发文档
├── shared/                ← 共享层（账户、网关、AI 接入）开发文档
├── oa/                    ← OA 基座开发文档
└── inventory/             ← 物资管理开发文档
```

## 首期工程骨架

| 目录 | 说明 | 依据 |
|---|---|---|
| `backend/` | Java 21 + Spring Boot 3 模块化单体 | `SAD.md` §2、§3 |
| `frontend/admin/` | Vue 3 + TypeScript + Vite + Element Plus Web 管理端 | `SAD.md` §2 |
| `frontend/mobile/` | uni-app + Vue 3 移动端骨架 | `SAD.md` §2 |
| `infra/db/init/` | PostgreSQL schema 与 AI 只读账号初始化 | `Deployment.md` §3 |
| `docker-compose.yml` | 首期一键部署入口 | `Deployment.md` |

## 编写规则

- 开发任务须引用 design/ 对应文档章节（如「依据 PRD.md §3.2」）
- 涉及表/接口/权限变更时，先更新 design/ 再编码
- 单模块开发文档不得违背 [README.MD](../README.MD) 宪法约束
