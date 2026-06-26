# 企业微信自建应用 H5 接入 OA 计划（2026-06-26）

文件名：`wecom-h5-oa-mobile-integration20260626.plan.md`  
项目：`HIS`  
目标分支：`main` 最新代码拉取后新建开发分支  
范围：企业微信自建应用、移动端 H5 OA、企业微信 OAuth、企业微信应用消息通知、OA 手机端发起/审批/催办

---

## 0. 依据与约束

本计划依据当前仓库最新代码、现有 `plans/` 计划文档格式、`design/` 文档和当前 OA / IAM / 通知 / 前端实现编写。

本轮计划继续遵守项目既有约束：

1. **先计划后执行**：每轮需求先生成或引用计划文件，再推进文档、代码、测试。
2. **文档驱动**：执行顺序为需求文档 → 设计文档 → 开发文档 → 代码 → 测试，禁止跳过文档直接写代码。
3. **权限可审计**：用户、角色、权限、OA 发起、审批、催办、通知、附件上传等关键行为必须记录审计或消息日志。
4. **AI 只读**：AI/LLM 不允许发起 OA、审批、驳回、催办、上传附件或写库存。
5. **模块化单体优先**：首期继续采用模块化单体，不拆微服务。
6. **前端轻量**：移动端 H5 必须适配企业微信手机工作台，不是简单打开 PC 管理后台网页。
7. **HIS 是唯一流程状态源**：不接企业微信原生审批，不做双审批引擎，不做企业微信审批状态同步。

> 说明：当前通过 GitHub 连接器无法直接列出 `.codex` 目录；但仓库现有计划文件集中在 `plans/` 目录，且已有计划文件明确写明计划需依据 `.codex/rules`、`AGENTS.md`、`README.MD`、`GUIDE.md`、`design/` 和既有 `plans/` 编写。因此本计划按当前可读取到的 `plans/` 样式和约束重新整理。

---

## 1. 本轮需求清单

| # | 需求 | 计划处理方式 |
|---|---|---|
| 1 | 不使用企业微信原生审批 | 明确企业微信只作为入口、登录和通知通道，HIS OA 仍为唯一流程引擎 |
| 2 | 新建企业微信自建应用 H5 发起 OA | 企业微信工作台入口配置为 HIS 移动端 `/m/oa` |
| 3 | 不是简单打开网页，而是适配手机页面 | 新增独立移动端路由、布局、表单、审批详情页和底部操作栏 |
| 4 | 支持手机端发起入库、出库、报销 | 复用现有 HIS OA 发起接口，新增移动端表单 |
| 5 | 支持手机端待办审批 | 复用现有待办、详情、审批、驳回接口，新增移动端审批页 |
| 6 | 企业微信中自动识别用户 | 新增企业微信 OAuth 登录，使用 `wecomUserId` 映射 HIS 用户 |
| 7 | 流程流转后企业微信通知处理人 | 改造 `OaNotificationService`，新增企业微信应用消息发送 |
| 8 | 角色节点需要通知多人 | 对 `ROLE` 节点按角色查找全部 enabled 用户并发送通知 |
| 9 | 催办通过企业微信提醒 | 复用当前催办接口，消息链接跳转移动审批页 |
| 10 | 保持 PC 管理端不受影响 | 新增 `/m/*` 移动端路由和移动端鉴权，不破坏现有 `/oa/*` PC 页面 |

---

## 2. 当前代码现状

### 2.1 OA 流程能力

当前后端 `ProcessRuntimeService` 已提供可发起流程列表：

- `inbound_material` / `inventory_inbound`：物资入库 OA
- `outbound_material` / `inventory_outbound`：物品领用
- `reimbursement` / `reimbursement`：报销 OA

对应发起接口当前已经存在：

- `POST /api/v1/oa/instances/inventory-inbound`
- `POST /api/v1/oa/instances/inventory-outbound`
- `POST /api/v1/oa/instances/reimbursement`

当前也已有：

- 我的待办：`GET /api/v1/oa/tasks/todo`
- 我的已办：`GET /api/v1/oa/tasks/handled`
- 待办详情：`GET /api/v1/oa/tasks/{taskId}`
- 审批通过：`POST /api/v1/oa/tasks/{taskId}/approve`
- 审批驳回：`POST /api/v1/oa/tasks/{taskId}/reject`
- 催办：`POST /api/v1/oa/instances/{instanceId}/urge`
- 撤销：`POST /api/v1/oa/instances/{instanceId}/revoke`

### 2.2 OA 流程闭环

当前默认流程已经包含“起草人确认”节点：

- 入库：汇报上级审批 → 物资管理员审批 → 起草人确认
- 出库：汇报上级审批 → 物资管理员审批 → 起草人确认
- 报销：汇报上级审批 → 财务审批 → 起草人确认

业务生效点在起草人最终确认之后，而不是最后一个审批人审批后立即生效。

### 2.3 用户与企业微信映射

当前 `UserRecord` 已包含：

- `reportToUserId`
- `wecomUserId`
- `departmentName`
- `roleCodes`

当前 `IamStore.createUser(...)` 已支持创建用户时传入 `wecomUserId`，并设置初始密码 `qwer1234`。

### 2.4 当前前端鉴权方式

当前 `frontend/admin/src/api/http.ts` 通过：

```ts
localStorage.getItem('his.currentUserId')
```

读取当前用户，并在请求头中加入：

```http
X-User-Id: <currentUserId>
```

该方式适合当前 PC 管理端开发阶段，但不适合作为企业微信正式登录态。企业微信 H5 应新增 OAuth 登录和移动端 token。

### 2.5 当前通知能力

当前 `OaNotificationService` 仅在内存中记录：

- `OA_TASK_ARRIVED`
- `OA_TASK_MANUAL_URGE`
- `OA_TASK_AUTO_REMIND`

尚未真正调用企业微信发送消息。

### 2.6 当前通知目标缺口

当前 `ProcessRuntimeService.targetUserId(...)` 对以下节点有效：

- `USER`
- `INITIATOR_SELECTED`
- `SUPERVISOR`

但对 `ROLE` 节点返回 `null`。而默认流程中的物资管理员审批、财务审批均为角色节点，因此必须新增“角色节点收件人解析”。

---

## 3. 需求分析

本轮目标是建设一套企业微信自建应用 H5 OA 入口，让员工可以直接在企业微信手机端完成：

1. 自动登录 HIS。
2. 查看 OA 首页。
3. 发起物资入库。
4. 发起物品领用 / 出库。
5. 发起报销。
6. 查看我的待办。
7. 查看待办详情。
8. 手机端审批通过 / 驳回。
9. 查看我发起的流程。
10. 对自己发起的流程催办。
11. 流程流转后通过企业微信通知下一个处理人。

非目标：

1. 不接企业微信原生审批模板。
2. 不让企业微信审批作为流程引擎。
3. 不做企业微信审批回调转 HIS OA。
4. 不做企业微信与 HIS 双向审批状态同步。
5. 不重构现有 OA 流程定义能力。

---

## 4. 业务分析

### 4.1 企业微信定位

企业微信自建应用只承担三件事：

1. **入口**：员工从企业微信工作台进入 HIS OA。
2. **身份识别**：通过企业微信 OAuth 获取企业微信用户 ID，并映射 HIS 用户。
3. **消息触达**：待办到达、催办、自动提醒时发送企业微信应用消息。

HIS 承担：

1. 流程定义。
2. 流程实例。
3. 节点流转。
4. 权限判断。
5. 审批通过 / 驳回。
6. 起草人确认。
7. 入库 / 出库 / 报销业务生效。
8. 审计与消息日志。

### 4.2 移动端页面定位

移动端不是 PC 管理后台缩放版，而是独立 H5 工作台：

- 禁止展示 PC 左侧菜单。
- 禁止用大表格展示待办。
- 列表统一用卡片。
- 表单统一单列布局。
- 审批按钮固定底部。
- 上传、搜索、选择控件适配手机操作。

### 4.3 流程触达规则

待办到达时：

- 指定用户节点：通知该用户。
- 汇报上级节点：通知解析出的直属上级。
- 角色节点：通知拥有该角色的所有 enabled 用户。
- 起草人确认节点：通知流程发起人。

催办时：

- 找到当前 pending task。
- 使用与任务到达相同的收件人解析规则。
- 向当前处理人发送催办消息。
- 记录催办审计与消息日志。

---

## 5. 数据分析

### 5.1 暂不新增核心业务表

本轮优先沿用当前内存模型和已有 Store 结构，不重构持久化层。

### 5.2 建议新增运行时模型

可先以内存 Store 实现，后续迁移数据库：

```text
WeComSessionRecord
- token
- userId
- wecomUserId
- expiresAt
- createdAt

WeComMessageLogRecord
- id
- taskId
- instanceId
- messageType
- hisUserId
- wecomUserId
- status
- errorMessage
- sentAt
```

### 5.3 用户映射

使用当前 `UserRecord.wecomUserId` 字段作为唯一映射依据。

要求：

1. `wecomUserId` 不为空才允许企业微信自动登录。
2. 企业微信返回的 UserId 在 HIS 中找不到用户时，移动端显示“当前企业微信账号未绑定 HIS 用户”。
3. 企业微信通知时，如果目标 HIS 用户未绑定 `wecomUserId`，记录失败日志，不阻断 OA 主流程。

---

## 6. 权限分析

### 6.1 移动端登录权限

企业微信 OAuth 只负责识别“是谁”，不能绕过 HIS 权限系统。

登录成功后，所有接口仍按现有权限点判断：

- `oa:instance:create`
- `oa:instance:read`
- `oa:task:read`
- `oa:task:approve`
- `oa:task:urge`
- `inventory:inbound:create`
- `inventory:outbound:create`
- `finance:reimbursement:create`
- `oa:attachment:write`

### 6.2 移动端接口鉴权

PC 端继续兼容：

```http
X-User-Id: <id>
```

企业微信 H5 新增：

```http
Authorization: Bearer <mobileToken>
```

后端解析优先级：

1. 优先解析 `Authorization`。
2. 无 `Authorization` 时兼容 `X-User-Id`。
3. 最终仍统一得到 `actorUserId`。

### 6.3 角色节点通知

角色节点通知只是“提醒拥有该角色的用户”，不改变审批权限。

审批时仍调用 `canHandle(...)` 判断：

- 当前用户必须拥有对应角色。
- 当前 task 必须是 pending。
- 当前用户必须有 `oa:task:approve` 或起草确认读权限。

---

## 7. AI 分析

本轮不新增 AI 可写能力。

AI/LLM 不允许：

1. 发起 OA。
2. 审批 OA。
3. 驳回 OA。
4. 催办。
5. 上传附件。
6. 发送企业微信消息。
7. 修改用户企业微信绑定。
8. 写库存或写报销数据。

AI 如访问移动端相关数据，只能通过已授权只读接口，并继承调用方权限过滤。

---

## 8. 系统设计

### 8.1 后端模块设计

新增包：

```text
backend/src/main/java/com/health/platform/wecom/
```

建议新增类：

```text
WeComProperties
WeComTokenService
WeComOAuthService
WeComMessageClient
WeComAuthController
WeComSessionService
WeComSessionRecord
WeComMessageResult
```

通知侧新增或改造：

```text
OaNotificationRecipientResolver
OaNotificationMessageBuilder
OaNotificationService
```

### 8.2 企业微信配置

配置项：

```yaml
wecom:
  enabled: true
  corp-id: ${WECOM_CORP_ID:}
  agent-id: ${WECOM_AGENT_ID:}
  secret: ${WECOM_SECRET:}
  base-url: ${WECOM_BASE_URL:https://qyapi.weixin.qq.com}
  his-public-base-url: ${HIS_PUBLIC_BASE_URL:}
```

要求：

1. 配置为空时系统仍可启动。
2. `wecom.enabled=false` 时不调用企业微信。
3. `secret` 只能来自环境变量，不进 Git。
4. 发送失败不影响 OA 主流程。

### 8.3 企业微信 OAuth 登录

新增接口：

```text
GET  /api/v1/wecom/auth/url?redirect=/m/oa
POST /api/v1/wecom/auth/login
GET  /api/v1/wecom/auth/me
POST /api/v1/wecom/auth/logout
```

登录流程：

```text
/m/oa
  ↓
检测无 mobileToken
  ↓
跳转 /m/oa/login?redirect=/m/oa
  ↓
获取企业微信 OAuth code
  ↓
POST /api/v1/wecom/auth/login
  ↓
后端用 code 换 wecomUserId
  ↓
根据 wecomUserId 找 HIS 用户
  ↓
签发 mobileToken
  ↓
前端保存 his.mobileToken
  ↓
进入 /m/oa
```

### 8.4 企业微信消息通知

消息发送使用企业微信自建应用消息，优先用 `textcard`。

消息链接统一指向：

```text
${HIS_PUBLIC_BASE_URL}/m/oa/tasks/{taskId}
```

待办消息：

```text
标题：你有一个新的 OA 待办
内容：流程名称、当前节点、发起人、摘要
按钮：查看审批
```

催办消息：

```text
标题：OA 催办提醒
内容：发起人催办了当前待处理流程
按钮：查看审批
```

### 8.5 移动端路由设计

新增移动端路由：

```text
/m/oa                         企业微信 OA 首页
/m/oa/login                   企业微信 OAuth 登录中间页
/m/oa/start                   发起流程选择页
/m/oa/start/inbound           发起物资入库
/m/oa/start/outbound          发起物品领用 / 出库
/m/oa/start/reimbursement     发起报销
/m/oa/todo                    我的待办
/m/oa/handled                 我的已办
/m/oa/mine                    我发起的
/m/oa/tasks/:taskId           待办详情 / 审批页
/m/oa/instances/:instanceId   流程详情
```

### 8.6 移动端组件设计

新增目录：

```text
frontend/admin/src/mobile/
```

建议结构：

```text
mobile/
  MobileLayout.vue
  MobileTopBar.vue
  MobileTabBar.vue
  mobile.css
  pages/
    MobileOaHome.vue
    MobileOaLogin.vue
    MobileStartCenter.vue
    MobileInboundApply.vue
    MobileOutboundApply.vue
    MobileReimbursementApply.vue
    MobileTodoTasks.vue
    MobileHandledTasks.vue
    MobileStartedInstances.vue
    MobileTaskDetail.vue
```

---

## 9. 开发设计

### 9.1 文档更新

更新：

```text
design/02-requirements/PRD.md
design/03-architecture/Permission-Design.md
design/05-api/API-Index.md
design/05-api/OpenAPI.yaml
```

新增开发文档：

```text
dev/wecom-h5-oa-mobile.md
```

内容包括：

1. 企业微信后台配置说明。
2. 环境变量说明。
3. OAuth 登录流程。
4. 移动端页面说明。
5. 消息通知链路。
6. 权限和异常处理。
7. 测试清单。

### 9.2 后端开发

#### 9.2.1 企业微信配置与 Token

新增：

```text
WeComProperties
WeComTokenService
```

实现：

1. 获取 access_token。
2. 本地缓存 token。
3. 提前 5 分钟过期。
4. 企业微信异常返回记录错误。

#### 9.2.2 企业微信 OAuth

新增：

```text
WeComAuthController
WeComOAuthService
WeComSessionService
```

实现：

1. 生成 OAuth URL。
2. 使用 code 获取企业微信 UserId。
3. 根据 `wecomUserId` 查 HIS 用户。
4. 签发移动端 token。
5. `/api/v1/wecom/auth/me` 返回当前用户信息。

#### 9.2.3 移动端鉴权

新增：

```text
MobileAuthFilter
MobileSessionStore
```

改造：

```text
SecurityContextUtil 或请求用户解析逻辑
```

要求：

1. 兼容当前 `X-User-Id`。
2. 新增 `Authorization` 解析。
3. PC 管理端不受影响。

#### 9.2.4 消息通知

新增：

```text
WeComMessageClient
OaNotificationRecipientResolver
OaNotificationMessageBuilder
```

改造：

```text
OaNotificationService
```

要求：

1. `taskArrived` 发送企业微信应用消息。
2. `manualUrge` 发送企业微信催办消息。
3. `autoRemind` 预留自动提醒发送。
4. `ROLE` 节点按角色查找全部用户。
5. 未绑定 `wecomUserId` 记录失败，不中断流程。
6. 企业微信发送失败记录失败，不中断流程。

### 9.3 前端开发

#### 9.3.1 request 改造

改造：

```text
frontend/admin/src/api/http.ts
```

逻辑：

```ts
const mobileToken = localStorage.getItem('his.mobileToken')
const currentUserId = localStorage.getItem('his.currentUserId')

headers: {
  'Content-Type': 'application/json',
  ...(mobileToken ? { Authorization: `Bearer ${mobileToken}` } : {}),
  ...(!mobileToken && currentUserId ? { 'X-User-Id': currentUserId } : {}),
}
```

#### 9.3.2 企业微信 API

新增：

```text
frontend/admin/src/api/wecom.ts
```

包含：

```ts
authUrl(redirect: string)
login(code: string)
me()
logout()
```

#### 9.3.3 移动端页面

新增：

1. `MobileOaHome.vue`
2. `MobileOaLogin.vue`
3. `MobileStartCenter.vue`
4. `MobileInboundApply.vue`
5. `MobileOutboundApply.vue`
6. `MobileReimbursementApply.vue`
7. `MobileTodoTasks.vue`
8. `MobileTaskDetail.vue`
9. `MobileStartedInstances.vue`
10. `MobileHandledTasks.vue`

#### 9.3.4 移动端样式

新增：

```text
frontend/admin/src/mobile/mobile.css
```

样式要求：

1. 单列布局。
2. 卡片列表。
3. 底部固定操作栏。
4. 适配 375px、390px、414px 宽度。
5. 按钮高度不低于 44px。
6. 表单控件适合触屏。

---

## 10. 执行清单

### 本轮 MVP 执行补充

- 企业微信官方联调依赖真实 `WECOM_CORP_ID`、`WECOM_AGENT_ID`、`WECOM_SECRET`、可信域名和应用主页配置；本轮先实现可配置接入、mock code 本地验证和关闭配置不影响系统。
- 移动端 H5 先复用 `frontend/admin` Vite 应用，新增 `/m/oa` 路由与 `frontend/admin/src/mobile/` 页面，不新建独立前端项目。
- 移动端接口优先覆盖 OA 发起、待办、详情、审批、驳回、催办、撤销、物资选择、入库单选择、附件上传。
- 企业微信通知先支持内存消息日志和真实发送 Client；配置关闭时记录 `skipped`，配置开启时调用企业微信应用消息接口，发送失败不阻断 OA。
- `.env.example` 已补齐默认端口 `BACKEND_PORT=18080`、`ADMIN_WEB_PORT=15173` 以及企业微信环境变量。
- 参考资料：
  - 企业微信网页授权登录官方文档：`https://developer.work.weixin.qq.com/document/path/91022`
  - 企业微信应用消息官方/镜像文档：`https://qiyeweixin.apifox.cn/api-10061358`
  - 企业微信 access_token 官方文档：`https://developer.work.weixin.qq.com/document/path/91039`

### 第一阶段：重新生成计划与文档

- [x] 新增本计划文件到 `plans/wecom-h5-oa-mobile-integration20260626.plan.md`
- [x] 更新 `design/02-requirements/PRD.md`
- [x] 更新 `design/03-architecture/Permission-Design.md`
- [x] 更新 `design/05-api/API-Index.md`
- [x] 更新 `design/05-api/OpenAPI.yaml`
- [x] 新增 `dev/wecom-h5-oa-mobile.md`

### 第二阶段：移动端页面骨架

- [x] 新增 `/m/oa` 路由分组
- [x] 新增 `MobileLayout`
- [x] 新增移动端首页
- [x] 新增移动端发起流程选择页
- [x] 新增移动端待办列表页
- [x] 保证 PC 管理端路由不受影响

### 第三阶段：移动端发起 OA

- [x] 新增移动端入库表单
- [x] 新增移动端出库 / 领用表单
- [x] 新增移动端报销表单
- [x] 复用现有 OA 发起接口
- [x] 接入物资搜索接口
- [x] 接入附件 / 凭证上传能力
- [x] 提交成功后跳转移动端“我发起的”

### 第四阶段：移动端审批与催办

- [x] 新增移动端任务详情页
- [x] 支持审批通过
- [x] 支持审批驳回
- [x] 支持起草人确认
- [x] 支持我发起的流程催办
- [x] 支持已办查看
- [x] 支持撤销本人运行中流程

### 第五阶段：企业微信 OAuth 登录

- [x] 新增企业微信配置
- [x] 新增 Token 服务
- [x] 新增 OAuth 登录接口
- [x] 新增移动端 session/token
- [x] 前端 `/m/oa/login` 接入 OAuth
- [x] `request` 支持 mobileToken
- [x] 未绑定 `wecomUserId` 时显示明确提示

### 第六阶段：企业微信消息通知

- [x] 新增企业微信消息 Client
- [x] 改造 `OaNotificationService`
- [x] 新增通知收件人解析器
- [x] 支持 USER 节点通知
- [x] 支持 SUPERVISOR 节点通知
- [x] 支持 INITIATOR_SELECTED 节点通知
- [x] 支持 ROLE 节点多人通知
- [x] 催办消息跳转移动审批页
- [x] 发送失败记录日志且不阻断 OA

### 第七阶段：测试、构建与验证

- [x] 后端单元测试
- [x] 前端构建
- [ ] 手机端宽度适配验证
- [ ] 企业微信 OAuth 联调
- [ ] 企业微信消息发送联调
- [ ] 入库全链路验证
- [ ] 出库全链路验证
- [ ] 报销全链路验证

本轮验证记录（2026-06-26）：

- `backend mvn test`：通过，44 tests，0 failures，0 errors。
- `frontend/admin npm run build`：通过，存在依赖注释与 chunk size 警告，不阻断构建。
- `./scripts/dev-start.sh restart --no-infra --skip-install`：通过，后台启动后输出 SUCCESS。
- `POST /api/v1/wecom/auth/login` 使用 `mock:employee.wecom`：通过，返回 mobileToken。
- `GET /api/v1/wecom/auth/me` 携带 `Authorization: Bearer <mobileToken>`：通过。
- `GET /api/v1/oa/instances/mine` 携带 `Authorization: Bearer <mobileToken>`：通过。
- 公网 `http://82.156.67.222:15173/m/oa/login`：返回 200。
- 手机端宽度截图验证：未执行，当前前端项目未安装 Playwright；已保留为待验证项。
- 企业微信 OAuth 与消息真实联调：待企业微信后台配置真实 CorpID、AgentId、Secret、可信域名和应用主页后执行。

---

## 11. 测试计划

### 11.1 后端测试

新增或补充测试：

```text
WeComTokenServiceTest
WeComOAuthServiceTest
WeComSessionServiceTest
OaNotificationRecipientResolverTest
OaNotificationServiceTest
ProcessRuntimeServiceTest
```

测试点：

1. 企业微信未启用时，不发送消息。
2. token 缓存未过期时不重复获取。
3. OAuth code 换取 wecomUserId 后能匹配 HIS 用户。
4. 未绑定用户登录失败。
5. USER 节点通知指定人。
6. SUPERVISOR 节点通知直属上级。
7. ROLE 节点通知所有角色用户。
8. INITIATOR_SELECTED 节点通知发起人。
9. 企业微信发送失败不阻断 OA 发起/审批。
10. 催办会发送企业微信催办消息。

### 11.2 前端测试

测试点：

1. `/m/oa` 不显示 PC 侧边栏。
2. 移动端首页可正常进入发起、待办、我发起的。
3. 移动端入库表单可提交。
4. 移动端出库表单可提交。
5. 移动端报销必须上传凭证。
6. 移动端审批详情可同意/驳回。
7. 移动端催办可调用接口。
8. mobileToken 存在时请求带 `Authorization`。
9. mobileToken 不存在时 PC 端继续使用 `X-User-Id`。

### 11.3 联调测试

最小联调链路：

```text
企业微信中打开 HIS OA 自建应用
  → 自动登录
  → 手机端发起物资入库
  → 流转到直属上级
  → 直属上级收到企业微信通知
  → 点击通知进入手机审批页
  → 审批通过
  → 流转到物资管理员
  → 物资管理员收到企业微信通知
  → 手机端继续审批
  → 流程回到起草人确认
  → 起草人收到企业微信通知
  → 起草人确认
  → 入库业务生效
```

---

## 12. 验收标准

### 12.1 功能验收

- [ ] 企业微信工作台可打开 HIS OA 移动端。
- [x] 移动端页面为手机适配布局，不出现 PC 侧边栏。
- [ ] 企业微信 OAuth 可自动登录 HIS 用户。
- [x] 未绑定 `wecomUserId` 的用户无法登录，并显示明确提示。
- [x] 手机端可发起物资入库 OA。
- [x] 手机端可发起物品领用 / 出库 OA。
- [x] 手机端可发起报销 OA。
- [x] 手机端可查看我的待办。
- [x] 手机端可查看待办详情。
- [x] 手机端可审批通过。
- [x] 手机端可审批驳回。
- [x] 手机端可处理起草人确认。
- [x] 手机端可查看我发起的流程。
- [x] 手机端可催办。
- [x] 手机端可查看已办。

### 12.2 通知验收

- [ ] 发起入库后，直属上级收到企业微信通知。
- [ ] 上级审批后，物资管理员收到企业微信通知。
- [ ] 发起出库后，直属上级收到企业微信通知。
- [ ] 出库流转到物资管理员后，物资管理员收到企业微信通知。
- [ ] 发起报销后，直属上级收到企业微信通知。
- [ ] 报销流转到财务审批后，财务审批人收到企业微信通知。
- [ ] 流程回到起草人确认时，起草人收到企业微信通知。
- [ ] 发起人催办时，当前处理人收到企业微信催办通知。
- [x] 企业微信发送失败不阻断 OA 主流程。

### 12.3 权限验收

- [ ] 没有 `oa:instance:create` 的用户无法发起 OA。
- [ ] 没有 `oa:task:read` 的用户无法查看待办。
- [ ] 没有处理权限的用户无法审批别人的待办。
- [ ] 角色节点只有拥有对应角色的用户可以审批。
- [ ] AI 调用方不能调用发起、审批、驳回、催办接口。

### 12.4 构建验收

- [x] `backend mvn test` 通过。
- [x] `frontend/admin npm run build` 通过。
- [x] 本地开发栈可重启。
- [x] 企业微信配置关闭时，系统仍可正常使用 PC 管理端和 OA 内部流程。

---

## 13. 风险与处理

| 风险 | 说明 | 处理 |
|---|---|---|
| 企业微信 OAuth 配置复杂 | CorpID、AgentId、Secret、可信域名、回调地址容易配置错误 | 新增 `dev/wecom-h5-oa-mobile.md`，写清配置步骤 |
| 用户未绑定 wecomUserId | 企业微信用户无法映射 HIS 用户 | 登录页展示明确错误，并提示联系管理员绑定 |
| 角色节点通知多人 | 当前 targetUserId 对 ROLE 返回 null | 新增 `OaNotificationRecipientResolver` |
| 企业微信发送失败影响流程 | 外部接口不稳定 | 发送失败只记录日志，不阻断 OA |
| mobileToken 与 PC 登录冲突 | 当前 PC 使用 X-User-Id | token 优先，X-User-Id 兼容；移动端和 PC 端 localStorage key 分离 |
| 手机端页面复用 PC 布局体验差 | PC 侧边栏、大表格不适合手机 | 新增独立 `/m/*` 路由和 MobileLayout |
| 附件上传移动端体验不足 | 报销必须上传凭证 | 移动端附件上传需作为第二阶段重点验证 |

---

## 14. 最小可交付版本

MVP 只要求跑通以下链路：

```text
企业微信工作台
  → 打开 HIS OA H5
  → 企业微信 OAuth 自动登录
  → 手机端发起物资入库
  → 直属上级收到企业微信通知
  → 点击通知进入手机审批页
  → 上级审批通过
  → 物资管理员收到企业微信通知
  → 物资管理员手机端审批通过
  → 起草人收到确认通知
  → 起草人手机端确认
  → 入库业务生效
```

MVP 完成后，再扩展出库、报销、自动提醒、通知日志页面和更完整的移动端体验优化。
