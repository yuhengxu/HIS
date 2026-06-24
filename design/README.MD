# design · 正式设计

正式立项后的设计文档目录。文档编写顺序：需求 → 设计 → 开发 → 测试 → 部署，为 AI 辅助开发提供结构化约束。

## 目录结构与联动关系

| 目录 | 文件 | 用途 | 联动文档 |
|---|---|---|---|
| **01-vision/** | Product-Vision.md | 产品愿景与项目章程：背景、核心目标、范围界定与成功标准 | PRD.md、Roadmap.md |
| **02-requirements/** | PRD.md | 产品需求文档：HIS 模块、康养（院内+上门）、账户体系、部署模式、非功能需求（性能/AI/安全）、验收标准 | Product-Vision.md、SAD.md、Deployment.md、AI-Architecture.md、Data-Dictionary.md、OpenAPI.yaml、API-Index.md、Roadmap.md |
| **03-architecture/** | SAD.md | 系统架构设计：HIS + 康养双模块独立部署、模块内模块化单体、技术选型、共享账户与 API 网关 | PRD.md、Data-Dictionary.md、ERD.md、OpenAPI.yaml、API-Index.md、MCP-API.md、Coding-Standards.md、Release-Process.md |
| | Deployment.md | 部署与运维方案：Windows/Linux、Docker 打包、局域网/云部署、监控与灾备 | PRD.md、Release-Process.md |
| | AI-Architecture.md | AI 原生接入：AI 服务层、能力分层（只读/受限写入）、模型沙箱、日志审计 | PRD.md、MCP-API.md、Permission-Design.md |
| | Permission-Design.md | 权限与安全：RBAC 模型、OAuth2/JWT、AI 专用权限、加密与审计合规 | SAD.md、AI-Architecture.md、API-Index.md、MCP-API.md |
| **04-database/** | Data-Dictionary-Standard.md | 数据字典编写规范：表/字段/索引命名约定 | Data-Dictionary.md、Table-Index.md |
| | Data-Dictionary.md | 数据字典：各业务域表结构、字段含义、主键与外键 | Table-Index.md、ERD.md、OpenAPI.yaml、API-Index.md、SAD.md、PRD.md |
| | Table-Index.md | 数据表与索引清单：全表及索引汇总，便于检索与复用 | Data-Dictionary.md、ERD.md |
| | ERD.md | 实体关系图：核心业务域 ER 图及关系说明 | Data-Dictionary.md、Table-Index.md |
| **05-api/** | OpenAPI.yaml | OpenAPI 3.0 规范：核心 REST 接口定义 | API-Index.md、MCP-API.md、Data-Dictionary.md、Permission-Design.md |
| | API-Index.md | 接口索引与权限矩阵：接口清单、角色权限、速率限制 | OpenAPI.yaml、MCP-API.md、Permission-Design.md |
| | MCP-API.md | 大模型/MCP 接入接口：AI 可调用接口子集、权限 scopes | OpenAPI.yaml、API-Index.md、AI-Architecture.md、Permission-Design.md |
| **06-research/** | OpenSource-HIS-AI-Research.md | 开源 HIS/康养/AI 项目调研对比（参见 doc/02） | SAD.md、Product-Vision.md、Data-Dictionary.md |
| **07-development/** | Coding-Standards.md | 编码规范：语言/框架约定、目录结构、命名与提交规范 | SAD.md、AI-Dev-Rules.md |
| | AI-Dev-Rules.md | AI 辅助开发约束：文档引用规则、AI 可写/只读边界、输出格式 | 所有 design/ 文档 |
| | Release-Process.md | 发布与 CI/CD：分支策略、构建打包、测试门禁、回滚 | Deployment.md |
| | Test-Strategy.md | 测试策略与用例：测试范围、P0 验收用例、AI 权限边界测试 | PRD.md、OpenAPI.yaml、Permission-Design.md、AI-Dev-Rules.md |
| | Roadmap.md | 实施路线图：分阶段迭代计划、里程碑、试点与推广路径 | Product-Vision.md、PRD.md |

> **联动规则**：变更任一列的文档时，须检查"联动文档"列中列出的文件是否需要同步更新。表结构变更时 04-database 内四个文件须保持同步。接口变更时 05-api 内三个文件须保持同步。


## 修订记录

**仅记录跨目录的结构性变更**（目录新增/删除/重组、顶层约定变更）。单文档内容变更由 Git 追踪，不在此登记。

| 版本 | 日期 | 修订人 | 变更摘要 | 联动更新 |
|---|---|---|---|---|
| 0.4.0 | 2026-06-17 | - | README 升格为项目宪法，更新文档优先级 | README.MD、GUIDE.md |
| 0.5.0 | 2026-06-17 | - | 合并子目录 README 到此文件，简化修订追踪为两级 | 01-vision ~ 07-development/README.MD、GUIDE.md |
| 0.6.0 | 2026-06-17 | - | 明确双模块+模块化单体架构；新增 Test-Strategy.md | README.MD、SAD.md、his-ai-architect.mdc、dev/README.MD |
| 0.7.0 | 2026-06-23 | Codex | 锁定首期技术栈并建立 OA/物资工程骨架 | SAD.md、Deployment.md、Permission-Design.md、04-database、05-api、07-development、dev/README.MD |
| 0.8.0 | 2026-06-23 | Codex | 执行 OA 第一阶段计划，补齐 OA 流程、企业微信提醒、物资审批库存联动设计 | PRD.md、Permission-Design.md、04-database、05-api、dev/oa、dev/inventory |
