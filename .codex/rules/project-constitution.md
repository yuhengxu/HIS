# HIS + Kangyang Project Constitution

Adapted from `.cursor/rules/project-constitution.mdc`.

## System Boundary

- The system has two modules: HIS hospital information system and Kangyang service system.
- The two modules can be deployed independently.
- They share one SSO account system.
- Kangyang covers both in-hospital services and home-visit services.

## Deployment And Environment

- Support Windows and Linux.
- Support local LAN deployment and cloud deployment.
- Frontend, backend, and database must support one-command deployment, preferably with Docker.

## AI Native And Safety Hard Constraints

- AI/LLM database access is read-only.
- AI/LLM may only execute `SELECT` at the database layer.
- AI/LLM must never execute `INSERT`, `UPDATE`, `DELETE`, `DROP`, `ALTER`, or `CREATE`.
- AI may only call the authorized interface subset defined in `MCP-API.md`.
- Permissions must be auditable.
- All interfaces must have documentation, permission annotations, and index lists.

## Frontend Constraints

- Cover web, WeChat Mini Program, and Enterprise WeChat Mini Program.
- Keep clients lightweight for older phones and low-end computers.
- Design for high concurrency.

## Engineering Constraints

- Document-driven flow: requirements document -> design document -> development document -> code.
- Do not skip documents and write code directly.
- Code output must state the source document section it is based on.
- Tables, fields, primary keys, and indexes must have explanations and traceable lists.
- When code changes affect tables, APIs, or permissions, update corresponding documents under `design/`.

## 计划驱动执行 / Plan Driven Execution

- 每轮请求先在 `plans/` 生成或引用执行计划。
- 按计划顺序推进文档、代码、测试。
- 结构性偏离必须先更新计划或说明原因。
