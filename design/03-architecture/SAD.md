# 系统架构设计（SAD）

## 1. 架构原则（已定）

- **产品层双模块**：HIS 医院信息系统、康养服务系统，均可独立部署。
- **模块内模块化单体**：各模块内部优先模块化单体，禁止为先进而微服务。
- **演进式拆分**：用户量、团队规模、模块边界稳定后，才允许单模块演进为微服务。
- **共享层**：统一账户体系（SSO）、共享 API 网关与 AI 接入层。
- **依据文档**：`README.MD` §3.1、§3.2、§3.3、§4；`GUIDE.md` §3.3。

## 2. 首期技术栈

| 层级 | 技术选型 | 说明 |
|---|---|---|
| Web 管理端 | Vue 3.5 + TypeScript 6 + Vite 8 + Element Plus 2 | 用于 OA、用户管理、角色权限、物资管理后台 |
| 移动端 | uni-app Vue 3 通道 + Vue 3.5 + Vite 7.3 | 优先编译微信小程序与企业微信小程序；Vite 版本按 DCloud Vue 3 通道 peer 约束固定 |
| 后端 | Java 25 LTS + Spring Boot 4.1 + Spring Security + MyBatis-Plus | 企业级权限、审计、事务与长期维护优先 |
| API | REST + OpenAPI 3.0 | 所有业务接口同步维护接口文档与权限矩阵 |
| 数据库 | PostgreSQL 16 | 支持复杂事务、JSONB、全文检索、报表查询与审计 |
| 缓存/会话 | Redis 7 | 用于会话、验证码、限流与热点缓存 |
| 部署 | Docker Compose | 首期满足 Windows/Linux、局域网/云端一键部署 |

## 3. 首期模块划分

首期后端仍采用一个 Spring Boot 应用承载模块化单体，但代码边界必须体现「共享平台 + HIS + 康养」三层关系。HIS 与康养是并列独立业务域，不允许把康养放在 HIS 下。

| Java 包 | 层级 | 职责 |
|---|---|---|
| `com.health.platform` | 共享平台能力 | IAM、认证授权、审计、AI 接入、API 网关适配、系统公共响应、OA 基座、共享物资能力 |
| `com.health.his` | HIS 独立业务域 | 门诊、住院、医嘱、收费、检验检查等医院信息系统业务 |
| `com.health.kangyang` | 康养独立业务域 | 院内康养服务、上门服务、康养师、服务机构、家属协同等康养业务 |

首期已实现能力先归入共享平台层：

| 平台子域 | 职责 |
|---|---|
| `platform.iam` | 用户、角色、权限、SSO、登录认证 |
| `platform.oa` | 基础 OA 权限点，后续扩展审批、通知、公告 |
| `platform.inventory` | 共享物资分类、物资档案、供应商、仓库、库存、入库、出库、盘点、库存流水 |
| `platform.audit` | 操作日志、权限变更日志、AI 调用审计 |
| `platform.ai-access` | AI 可调用接口白名单、只读查询边界、调用审计 |

## 4. 部署边界

- 首期以 `backend`、`admin-web`、`postgres`、`redis` 四个容器组成最小部署单元。
- HIS 与康养模块共享 `platform` 账户体系；后续可按部署包拆分，但不得复制账户体系。
- 单库内按 schema 隔离共享域与业务域：`iam`、`oa`、`inventory`、`audit`、`ai_access`、`his`、`kangyang`。

## 5. AI 接入边界

- AI 数据库账号仅授予 `SELECT` 权限。
- AI 仅允许调用 `design/05-api/MCP-API.md` 中登记的授权接口。
- 所有 AI 调用必须记录调用方、接口、权限 scope、请求摘要和结果状态。
