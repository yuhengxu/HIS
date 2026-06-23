# AI 原生接入架构

## 1. 设计依据

- `README.MD` §3.3
- `SAD.md` §5
- `Permission-Design.md` §4
- `MCP-API.md`

## 2. 首期能力边界

- AI 不直接写业务库。
- AI 数据库账号 `his_ai_readonly` 仅授予 `SELECT`。
- AI 只能调用 `MCP-API.md` 登记的接口。
- 首期 AI 可调用接口仅覆盖物资档案列表与库存查询。

## 3. 调用链路

1. AI 调用方携带 scope 请求后端 MCP/API 网关。
2. 后端校验 `ai_api_allowlist`。
3. 后端执行只读业务查询。
4. 后端写入 `ai_call_log`。
5. 返回脱敏后的只读结果。

## 4. 审计要求

每次 AI 调用记录调用方、接口编码、scope、结果状态、请求摘要和时间。
