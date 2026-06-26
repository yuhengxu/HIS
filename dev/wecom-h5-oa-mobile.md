# 企业微信自建应用 H5 OA 接入说明

依据：`plans/wecom-h5-oa-mobile-integration20260626.plan.md`。

## 企业微信后台配置

1. 创建企业微信自建应用。
2. 设置应用可见范围，确保审批人、物资管理员、财务审批人可接收应用消息。
3. 配置网页授权可信域名，域名应与 `HIS_PUBLIC_BASE_URL` 一致。
4. 将应用主页配置为：`{HIS_PUBLIC_BASE_URL}/m/oa`。
5. 保存 CorpID、AgentId、Secret 到部署环境变量，不写入 Git。

## 环境变量

```bash
WECOM_ENABLED=true
WECOM_CORP_ID=wwxxxxxxxxxxxx
WECOM_AGENT_ID=1000002
WECOM_SECRET=xxxxxxxx
WECOM_BASE_URL=https://qyapi.weixin.qq.com
HIS_PUBLIC_BASE_URL=http://82.156.67.222:15173
```

配置为空或 `WECOM_ENABLED=false` 时，系统仍可启动，企业微信消息记录为 skipped。

## OAuth 登录

- 前端 `/m/oa/login` 获取企业微信回调 `code` 后调用 `POST /api/v1/wecom/auth/login`。
- 后端用应用 `access_token` 调用企业微信 `user/getuserinfo` 获取 `UserId`。
- `UserId` 匹配 `iam_user.wecom_user_id` 后签发 `mobileToken`。
- 本地联调用 `mock:employee.wecom` 可直接模拟企业微信 UserId。

## 移动端页面

- `/m/oa` 首页
- `/m/oa/start` 发起中心
- `/m/oa/start/inbound` 物资入库
- `/m/oa/start/outbound` 物品领用
- `/m/oa/start/reimbursement` 报销
- `/m/oa/todo` 我的待办
- `/m/oa/tasks/{taskId}` 审批详情
- `/m/oa/mine` 我发起的
- `/m/oa/handled` 我的已办

## 通知链路

- 待办到达：后端解析任务处理人并发送企业微信 textcard。
- 角色节点：通知拥有该角色的全部 enabled 用户。
- 催办：发送催办 textcard，链接到 `/m/oa/tasks/{taskId}`。
- 未绑定企业微信 ID 或发送失败：记录日志，不阻断 OA。

## 测试

- `mock:employee.wecom` 登录普通员工。
- 发起入库 OA，确认直属上级产生待办。
- 上级审批后，物资管理员角色节点产生通知日志。
- 企业微信配置关闭时，PC 管理端和 OA 内部流程仍可使用。
