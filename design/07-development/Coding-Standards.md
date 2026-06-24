# 编码规范

## 1. 依据文档

- `SAD.md` §2、§3
- `Permission-Design.md`
- `Data-Dictionary-Standard.md`

## 2. 后端

- Java 25 LTS + Spring Boot 4.1。
- 包名根路径采用品牌中性命名：`com.health`。
- 共享平台能力放在 `com.health.platform`。
- HIS 独立业务域放在 `com.health.his`。
- 康养独立业务域放在 `com.health.kangyang`。
- 按模块划分包：`iam`、`oa`、`inventory`、`audit`、`aiaccess`、`system`。
- Controller 只处理 HTTP 入参出参，业务逻辑放 Service。
- 所有写操作必须记录审计日志。
- AI 相关代码必须显式校验 allowlist 和 scope。

## 3. 前端

- Web 管理端使用 Vue 3 + TypeScript + Vite + Element Plus。
- 移动端使用 uni-app + Vue 3。
- 页面权限由后端权限点驱动，前端只做展示控制，不作为安全边界。

## 4. 数据库

- 表结构以 `design/04-database/` 为准。
- DDL 不得绕过数据字典与索引清单。
- AI 数据库账号只读。
