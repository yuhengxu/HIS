# ECMS 项目深度分析

> 分析文档：`devcase/AnalogyRslt/ecms-main.md`（上传 git）  
> 源码路径（本地）：`devcase/ecms-main/`（不上传 git）
> 项目：[NabsCodes/ecms](https://github.com/NabsCodes/ecms)  
> 定位：**MERN 栈养老护理管理 MVP 原型**

---

## 1. 系统架构

### 1.1 总体架构

```
Client (React 17 + CRA)
  ├── 营销首页 (Tailwind / twin.macro)
  └── 后台面板 (Paper Dashboard + Bootstrap)
        ↓ Axios REST (JWT in localStorage)
Server (Express 4 + Node.js)
  ├── /api/auth    认证
  ├── /api/user    用户/合同
  ├── /api/invoice 发票
  └── /api/schedule 排班
        ↓ Mongoose 5
MongoDB
```

### 1.2 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | React 17, React Router v5, Axios, Reactstrap, Bootstrap 5, Tailwind 2 |
| 后端 | Express 4, Mongoose 5, bcryptjs, jsonwebtoken |
| 数据库 | MongoDB |
| 部署 | 前端 Vercel，后端 Render |

### 1.3 角色与访问控制

| 角色 | 权限范围 |
|------|----------|
| admin | 全量 CRUD |
| client | 只读个人数据 |
| caregiver | 只读个人数据 + 查看排班 |

认证：POST `/api/auth` → JWT（24h）→ localStorage → AuthContext 校验

---

## 2. 设计方案

### 2.1 数据模型

**单用户表 + 嵌入式子文档**（非多表关联）：

| 模型 | 关键字段 |
|------|----------|
| Users | email, role, status, staff_type, password(bcrypt), Medical{}, Contracts[], NextOfKin{}, EmergencyContact{} |
| Schedule | start_date, end_date, week, schedule{0..6: [{name, email}×5]} |
| Invoice | email, amount, description, date_created |

### 2.2 前端 UI 设计

- 营销层：Hero、Features、Director 介绍
- 后台层：Paper Dashboard 模板（Sidebar + Navbar + 主内容）
- 页面模式：列表(all) → 新增(add) → 详情(view) 三态切换

### 2.3 认证设计缺陷

- JWT Bearer（非 Cookie）
- 无角色权限校验中间件 — 任何已登录用户可调用任意 API

---

## 3. 功能模块详解

### 3.1 认证模块（Auth）

- **路径/入口**:
  - 后端：`server/routes/api/auth.js`
  - 前端：`client/src/views/auth/Login.js`、`client/src/context/AuthContext.js`
  - 中间件：`server/middleware/auth.js`
- **核心数据表/模型**: JWT payload `{ id }`；Users 集合（password bcrypt 哈希）
- **功能点清单**:
  1. `POST /api/auth` — 邮箱+密码登录，返回 `{ auth: true, token }`（86400s）
  2. `GET /api/auth/user` — auth 中间件验证 JWT，返回用户（排除 password）
  3. 前端启动：`AuthContext` → `GET /api/user/loggedIn` → 有效则拉取用户详情
  4. Axios 全局 Header：`Authorization: token`（无 Bearer 前缀）
  5. 角色路由分流：admin/client/caregiver → 对应 Layout
  6. **Bug**：登录成功后硬编码跳转 `/admin/dashboard`（不区分角色）
  7. **未实现**：Register 页面存在但未挂路由；forgotPassword/passwordReset/verifyAcct 前端定义但后端无
- **与其他模块的关联**: 所有 API 的 auth 中间件入口

### 3.2 用户管理模块（User）

- **路径/入口**:
  - 后端：`server/routes/api/user.js`
  - 前端 Admin：`views/admin/CareGiver.js`、`views/admin/Client.js`
  - 前端 Profile：`views/admin/UserPage.js`（三角色共用结构）
- **核心数据表/模型**: Users 集合（见 §2.1）
- **功能点清单**:
  1. `POST /api/user/add_user` — **无认证**公开注册（first_name, email, last_name, dob, role, address, phone, password）
  2. `PATCH /api/user/update_user/:id` — auth，更新任意字段（password 则 bcrypt）
  3. `GET /api/user/show_users/:role` — auth，按角色分页（10条/页，Query: current, search）
  4. `GET /api/user/show_all_users` — auth，全部用户分页
  5. `GET /api/user/show_user_by_email/:email` — auth，按邮箱查单用户
  6. `GET /api/user/get_counts` — auth，返回 `{ users, contracts, clients }`
  7. `GET /api/user/get_counts/:email` — auth，返回 `{ invoices, contracts }`
  8. `GET /api/user/loggedIn` — 无 auth，验证 token 有效性
  9. **Admin 护理员 CRUD 表单字段**：first_name, last_name, email, phone, dob, address, password, staff_type(Permanent/Contract Staff/Volunteer), status(active/suspended/on-leave/in-active), NextOfKin(name/phone/address/relationship), EmergencyContact(同上)
  10. **Admin 客户 CRUD 表单字段**：同上 + Medical(blood_group: A+/A-/B+/B-/AB+/AB-/O+/O-, genotype: AA/AS/AC/SS/SC, allergy, others)
  11. **Profile 编辑**：email, phone, first_name, last_name, address, NextOfKin, EmergencyContact
  12. 三态切换：列表(all) → 新增(add) → 详情(view)
- **与其他模块的关联**: → 合同（嵌入式 Contracts[]）、发票（email 关联）、排班（caregiver email）

### 3.3 合同管理模块（Contract）

- **路径/入口**:
  - 后端：`GET /api/user/show_all_contracts`（合同从 Users.Contracts[] 聚合）
  - 前端 Admin：`views/admin/Contract.js`
  - 前端 Client/Caregiver：`views/client/Contract.js`、`views/caregiver/Contract.js`（只读）
- **核心数据表/模型**: Users.Contracts[] — `{ start_date, end_date, description }`
- **功能点清单**:
  1. Admin 合同 CRUD：email + Find User → start_date, end_date, description
  2. `checkDates()` 逻辑：valid（end_date ≥ today）/ expired / noContracts 三分组
  3. Admin 合同列表：分类筛选 valid/expired/noContracts + 分页
  4. Client 查看个人合同（只读）
  5. Client "Renew?" 按钮 — **未实现**续签逻辑
  6. Caregiver 查看关联合约（只读）
  7. Dashboard 统计卡片引用合同数量
- **与其他模块的关联**: 嵌入 Users 文档，无独立集合；无关联护理员字段

### 3.4 排班管理模块（Schedule）

- **路径/入口**:
  - 后端：`server/routes/api/schedule.js`
  - 前端 Admin：`views/admin/Schedule.js`
  - 前端 Caregiver Dashboard：展示本周排班
- **核心数据表/模型**: Schedule 集合 — `{ start_date(String), end_date(String), week(String), schedule: { 0..6: [{name, email}×5] } }`
- **功能点清单**:
  1. `POST /api/schedule/add_schedule` — **无认证**；Body: first, last, week
  2. 算法：随机选 7 名 status=active 的 caregiver → 7 天 × 每天 5 人循环轮转
  3. 前置条件：恰好 7 名 active caregiver，否则失败
  4. `GET /api/schedule/show_all_schedules` — auth，分页，按 start_date/week 降序
  5. Admin 生成表单：start_date（自动推算 end_date + week）
  6. Admin 查看：7 天 × 5 名 staff 表格
  7. Caregiver Dashboard：展示本周排班预览
  8. **缺陷**：不考虑技能/可用性/位置；日期存 String 非 Date
- **与其他模块的关联**: 引用 Users(caregiver) 的 name/email；无与合同/客户的关联

### 3.5 发票管理模块（Invoice）

- **路径/入口**:
  - 后端：`server/routes/api/invoice.js`
  - 前端 Admin：`views/admin/Invoice.js`
  - 前端 Client/Caregiver：`views/client/Invoice.js`、`views/caregiver/Invoice.js`（只读）
- **核心数据表/模型**: Invoices 集合 — `{ email, amount(Number), description, date_created }`
- **功能点清单**:
  1. `POST /api/invoice/add_invoice` — auth，创建发票
  2. `GET /api/invoice/show_invoices` — auth，**Bug**：读取 `req.params.role` 但路由无 `:role`
  3. `GET /api/invoice/show_invoice/:email` — auth，按邮箱查发票 + 分页
  4. `PATCH /api/invoice/update_invoice` — auth，Body 含 `_id` 更新
  5. Admin 表单：email + Find User → amount, description
  6. Client/Caregiver 只读查看个人发票
  7. Dashboard 统计：发票数量
- **与其他模块的关联**: 通过 email 关联 Users；无支付/状态字段

### 3.6 Admin Dashboard 模块

- **路径/入口**: `views/admin/Dashboard.js`；路由 `/admin/dashboard`
- **功能点清单**:
  1. 统计卡片：users/contracts/clients 数量（`GET /api/user/get_counts`）
  2. 合同预览列表
  3. 排班预览

### 3.7 Client Dashboard 模块

- **路径/入口**: `views/client/Dashboard.js`；路由 `/client/dashboard`
- **功能点清单**:
  1. 个人合同/发票数量统计（`GET /api/user/get_counts/:email`）
  2. 快捷导航

### 3.8 Caregiver Dashboard 模块

- **路径/入口**: `views/caregiver/Dashboard.js`；路由 `/caregiver/dashboard`
- **功能点清单**:
  1. 个人统计
  2. 本周排班预览

### 3.9 营销首页模块

- **路径/入口**: `views/Homepage.js`；路由 `/`
- **功能点清单**:
  1. Hero 区域
  2. 4 项静态服务展示（Proper Health Care 等）
  3. Director 介绍
  4. Features 区块
  5. 无后端 API 依赖

### 3.10 未实现/死代码模块

- **路径/入口**: `server/routes/api/service.js`（**未挂载**）
- **功能点清单**:
  1. `POST /add_service` — 创建 Service（**Service 模型不存在**）
  2. `GET /show_store_services` — 按 user.id 过滤
  3. `PATCH /update_service` — 更新
  4. `authDetail` 中间件 — 定义但未被引用

---

## 4. 系统优点

1. 三角色 Layout/路由分离清晰（admin/client/caregiver 独立 routes + Layout）
2. 嵌入式 NextOfKin、EmergencyContact、Medical、Contracts 适合养老基础信息建模
3. Paper Dashboard 后台 UI 体验一致
4. JWT + bcrypt 认证基础可用
5. 合同 valid/expired/noContracts 三分组思路可扩展
6. 排班自动生成机制可运行（虽算法简单）

---

## 5. 系统不足

### 5.1 安全缺陷

- `POST /api/schedule/add_schedule`、`POST /api/user/add_user` 无认证
- 无 RBAC 中间件 — 任何已登录用户可调用 admin 级 API
- JWT 存 localStorage（XSS 风险）
- 无输入校验、无 rate limiting

### 5.2 代码 Bug

- 登录成功后硬编码跳转 `/admin/dashboard`
- `show_invoices` 引用不存在的 role 参数
- 搜索功能前端 setSearch 但后端未使用
- Client Contract "Renew?" 未实现
- Schedule.week schema 默认值为 `true`（类型错误）

### 5.3 架构局限

- 单表承载所有角色（admin/client/caregiver 共用 Users）
- 合同无关联护理员
- 排班算法随机，不考虑技能/可用性/位置
- Medical 字段无 Schema 约束（空对象 `{}`）
- CareGiver.js 与 Client.js ~1200 行高度重复
- 日期存 String 而非 Date

---

## 6. 开发参考索引

| 模块 | 关键文件 | 可复用模式 |
|------|----------|------------|
| 认证 | `server/routes/api/auth.js`、`AuthContext.js` | JWT + Context 角色分流 |
| 用户 | `server/models/User.js`、`CareGiver.js`、`Client.js` | 嵌入式子文档（Medical/Contracts/Kin） |
| 合同 | `Contract.js`、`show_all_contracts` | valid/expired 三分组 |
| 排班 | `schedule.js`、`Schedule.js` | 周期排班生成（需改进算法） |
| 发票 | `invoice.js`、`Invoice.js` | email 关联的简单计费 |
| 路由 | `Router.js`、`admin_routes.js` 等 | 三角色独立路由表 |
| UI | Paper Dashboard Layout | 后台面板模板 |

---

## 对本项目的借鉴价值

| 可借鉴 | 需规避 |
|--------|--------|
| 三角色前端路由架构 | 无 RBAC 的 API 设计 |
| 老人/护理员/家属/合同实体模型 | 单表混合角色 |
| Paper Dashboard UI 集成 | 前后端不一致的死代码 |
| 合同状态分类思路 | 随机排班算法 |
| 嵌入式 Medical/Kin 字段设计 | localStorage JWT 存储 |
