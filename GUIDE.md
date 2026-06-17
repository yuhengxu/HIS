# HIS + 康养平台 · 项目工作指引

> **每轮对话入口。** 先读本文档了解当前阶段与核心约束，再根据需要加载对应文档，确保工作不偏离项目目标。

---

## 1. 项目核心约束（不可偏离）

以下约束来自 [README.MD](./README.MD)（**项目宪法**），所有决策与实现均不得违背：

| 约束 | 说明 |
|---|---|
| 双模块 | 系统分为 **HIS 医院信息系统** 与 **康养服务系统**，均可独立部署 |
| 统一账户 | 两模块共用一套账户体系（SSO） |
| 康养双场景 | 康养含 **院内服务** 与 **上门服务** |
| 部署灵活 | 支持本地局域网与云端部署；Windows / Linux 均可运行 |
| AI 原生 | 从设计起预留 AI 接入；数据库层 AI **只读**；接口层限制 AI 可调用权限 |
| 前端轻量 | 网页端 + 微信/企业微信小程序；适配低端设备，界面简洁、避免卡顿 |
| 高并发 | 架构与实现须考虑并发高峰场景 |
| 文档驱动 | **需求 → 设计 → 开发文档 → 代码**，禁止跳过文档直接开发 |
| 数据可追溯 | 表/字段/索引须有说明与清单；接口须有文档、权限与索引 |
| 文档联动 | 变更须更新关联文档，并在对应 README 修订记录中登记 |

---

## 2. 文档体系

```
HIS/
├── GUIDE.md              ← 本文档（每轮对话入口）
├── README.MD             ← 项目宪法（初心与不可违背的约束）
├── doc/                  ← 构思规划（立项前，参考用）
├── design/               ← 正式设计（立项后，权威依据）
│   ├── README.MD         ← 设计总览：目录、文件清单、联动关系
│   ├── 01-vision/        ← 愿景与范围
│   ├── 02-requirements/  ← 需求
│   ├── 03-architecture/  ← 架构设计
│   ├── 04-database/      ← 数据库设计
│   ├── 05-api/           ← 接口设计
│   ├── 06-research/      ← 调研参考
│   └── 07-development/   ← 开发规范
└── dev/                  ← 开发文档（模块任务拆解，见 dev/README.MD）
```

| 目录 | 定位 | 权威性 | 何时读取 |
|---|---|---|---|
| [README.MD](./README.MD) | 项目宪法：初心与不可违背的约束 | **最高**，约束来源 | 每轮对话 |
| [doc/](./doc/) | 构思、规划、调研 | 参考，非最终依据 | 规划/调研阶段；design 未覆盖时补充参考 |
| [design/](./design/) | 正式需求与设计 | **权威**，开发以此为准 | 按需检索（见 §4） |
| `dev/` | 开发任务与模块说明 | 开发执行依据 | 进入编码阶段 |

**优先级**：`README.MD`（宪法）> `design/`（正式设计）> `doc/`（构思参考）。冲突时以优先级高者为准并向下修订。

---

## 3. 各阶段工作指引

### 3.1 规划阶段

**目标**：明确方向与范围，输出可落地的设计输入。

| 步骤 | 动作 | 读取文档 |
|---|---|---|
| 1 | 理解项目边界与约束 | [README.MD](./README.MD)、本文档 §1 |
| 2 | 查阅前期构思与调研 | [doc/00 项目规划](./doc/00%20医院信息系统（HIS）与康养服务系统项目规划.md)、[doc/01 技术架构规划](./doc/01%20HIS与康养服务系统技术架构规划.md)、[doc/02 开源调研](./doc/02%20HIS%20+%20康养平台开源项目调研报告.md) |
| 3 | 对照设计目录总览 | [design/README.MD](./design/README.MD) |
| 4 | 输出/更新愿景 | [design/01-vision/Product-Vision.md](./design/01-vision/Product-Vision.md) |
| 5 | 更新路线图 | [design/07-development/Roadmap.md](./design/07-development/Roadmap.md) |

**完成标准 checklist**：

- [ ] 愿景文档明确项目背景、核心目标与系统范围
- [ ] Roadmap 有分阶段里程碑与关键交付物
- [ ] 三大目标（原生 AI HIS、标准化康养、康养到家平台）均有对应落地路径

---

### 3.2 需求阶段

**目标**：将规划转化为可验收的需求。

| 步骤 | 动作 | 读取文档 |
|---|---|---|
| 1 | 确认愿景与范围 | [design/01-vision/Product-Vision.md](./design/01-vision/Product-Vision.md) |
| 2 | 编写/更新 PRD | [design/02-requirements/PRD.md](./design/02-requirements/PRD.md) |
| 3 | 参考前期需求描述 | [doc/00 项目规划 §2](./doc/00%20医院信息系统（HIS）与康养服务系统项目规划.md) |
| 4 | 需求变更时联动 | 见 [design/README.MD](./design/README.MD) 联动关系 |

**PRD 必含**：HIS 模块功能、康养（院内+上门）功能、账户体系、部署模式、非功能需求（性能/AI/安全）、验收标准。

**完成标准 checklist**：

- [ ] 用户角色已定义（医生、护士、患者/家属、管理员、康养机构、护工等）
- [ ] HIS 核心流程（挂号→就诊→处方→缴费等）的用户故事已覆盖
- [ ] 康养双场景（院内+上门）的用户故事已覆盖
- [ ] 康养到家全链路（下单→派单→服务→评价→结算）已描述
- [ ] 非功能需求已量化（性能指标、安全等级、并发量级）
- [ ] 每个 P0 需求有 Given/When/Then 验收条件
- [ ] AI 接入需求已明确（哪些业务环节需要 AI 辅助）
- [ ] 下游架构/数据库/接口设计可据此展开

---

### 3.3 设计阶段

**目标**：将需求转化为可开发的技术方案。

| 步骤 | 动作 | 读取文档 |
|---|---|---|
| 1 | 确认 PRD | [design/02-requirements/PRD.md](./design/02-requirements/PRD.md) |
| 2 | 系统架构 | [design/03-architecture/SAD.md](./design/03-architecture/SAD.md) |
| 3 | 部署方案 | [design/03-architecture/Deployment.md](./design/03-architecture/Deployment.md) |
| 4 | AI 接入 | [design/03-architecture/AI-Architecture.md](./design/03-architecture/AI-Architecture.md) |
| 5 | 权限安全 | [design/03-architecture/Permission-Design.md](./design/03-architecture/Permission-Design.md) |
| 6 | 数据库 | [design/04-database/](./design/04-database/)（规范 → 字典 → 索引 → ERD，四者同步） |
| 7 | 接口 | [design/05-api/](./design/05-api/)（OpenAPI ↔ 索引 ↔ MCP，三者同步） |
| 8 | 参考调研 | [design/06-research/](./design/06-research/)、[doc/02 开源调研](./doc/02%20HIS%20+%20康养平台开源项目调研报告.md) |
| 9 | 设计变更联动 | [design/README.MD](./design/README.MD) 联动关系表 |

**完成标准 checklist**：

- [ ] SAD 明确技术选型及选型理由（含候选方案对比）
- [ ] 部署方案覆盖局域网部署、云部署、Docker 一键部署三种模式
- [ ] AI 架构含调用链路、RAG 架构、模型管理与审计日志
- [ ] AI 数据只读与接口权限约束已在架构中体现
- [ ] 权限模型（RBAC）覆盖所有用户角色，AI 有独立权限层级
- [ ] 数据库四文件齐全且互相对齐（字典 ↔ 索引 ↔ ERD）
- [ ] 接口三文件齐全且互相对齐（OpenAPI ↔ 索引 ↔ MCP）
- [ ] 所有接口有文档、权限标注和速率限制

---

### 3.4 开发阶段

**目标**：在文档约束下实现代码，并保持文档与代码一致。

| 步骤 | 动作 | 读取文档 |
|---|---|---|
| 1 | 确认设计基线 | [design/02-requirements/PRD.md](./design/02-requirements/PRD.md)、[design/03-architecture/SAD.md](./design/03-architecture/SAD.md) |
| 2 | 编码规范 | [design/07-development/Coding-Standards.md](./design/07-development/Coding-Standards.md) |
| 3 | AI 辅助约束 | [design/07-development/AI-Dev-Rules.md](./design/07-development/AI-Dev-Rules.md) |
| 4 | 查表/字段 | [design/04-database/Table-Index.md](./design/04-database/Table-Index.md)、[Data-Dictionary.md](./design/04-database/Data-Dictionary.md) |
| 5 | 查接口 | [design/05-api/API-Index.md](./design/05-api/API-Index.md)、[OpenAPI.yaml](./design/05-api/OpenAPI.yaml) |
| 6 | 查权限 | [design/03-architecture/Permission-Design.md](./design/03-architecture/Permission-Design.md) |
| 7 | 发布流程 | [design/07-development/Release-Process.md](./design/07-development/Release-Process.md) |
| 8 | 开发文档 | `dev/`（待建，按模块存放开发说明与任务拆解） |

**完成标准 checklist**：

- [ ] 实现与 PRD/设计一致
- [ ] AI 生成的代码附文档溯源注释（依据 PRD.md §X、SAD.md §Y）
- [ ] AI 生成的 SQL 仅含 SELECT（禁止 INSERT/UPDATE/DELETE/DROP/ALTER/CREATE）
- [ ] 表/接口/权限变更已回写 design 文档
- [ ] 代码已通过编码规范检查

---

### 3.5 测试阶段

**目标**：验证实现与需求、设计的一致性，确保系统质量。

| 步骤 | 动作 | 依据文档 |
|---|---|---|
| 1 | 编写测试策略与测试用例 | [07-development/Test-Strategy.md](./design/07-development/Test-Strategy.md)、PRD.md（验收标准） |
| 2 | 单元测试 | Data-Dictionary.md、API-Index.md |
| 3 | 集成测试 | OpenAPI.yaml（接口契约）、Permission-Design.md（权限校验） |
| 4 | 性能/压力测试 | 非功能需求中的性能指标 |
| 5 | 安全测试 | Permission-Design.md、AI-Architecture.md（AI 权限边界） |
| 6 | AI 行为测试 | AI-Dev-Rules.md（AI 是否遵守只读/接口限制） |
| 7 | UAT 验收测试 | PRD.md（业务验收标准） |

**完成标准 checklist**：

- [ ] 测试策略文档已编写并评审
- [ ] P0 用户故事的测试用例已覆盖（单元 + 集成 + UAT）
- [ ] AI 数据库只读约束已通过自动化测试验证
- [ ] AI 接口权限边界已通过测试验证
- [ ] 性能指标达标（并发量、响应时间等）
- [ ] 安全测试通过（权限越界、数据加密、审计日志）

---

### 3.6 部署与运维阶段

**目标**：将系统交付到目标环境，保障稳定运行。

| 步骤 | 动作 | 依据文档 |
|---|---|---|
| 1 | 准备部署环境 | Deployment.md（Windows/Linux、局域网/云） |
| 2 | 执行部署 | Release-Process.md（构建打包、部署流程） |
| 3 | 上线验证 | 测试阶段 checklist |
| 4 | 监控与告警 | Deployment.md（监控方案） |
| 5 | 灾备与回滚 | Deployment.md、Release-Process.md（回滚策略） |

**完成标准 checklist**：

- [ ] 一键部署脚本/容器镜像已验证
- [ ] 局域网部署与云部署均已验证
- [ ] 监控告警已配置（服务健康、数据库、AI 调用）
- [ ] 灾备与回滚流程已演练
- [ ] 运维手册已编写

---

## 4. 工作启动流程（每轮对话）

**原则：轻量入口，按需检索。** 不做全量文档加载，由 AI 根据任务类型自行检索所需文档。

```
□ 1. 读取 GUIDE.md（本文档），确认当前阶段
□ 2. 读取 README.MD，确认核心约束
□ 3. 根据任务类型检索对应文档（见 §5 速查表）
□ 4. 若涉及变更，查阅 design/README.MD 联动关系表
□ 5. 任务完成后，若涉及结构性变更，更新 design/README.MD 修订记录
```

**检索策略**：

- **顶层入口**（始终读取）：GUIDE.md + README.MD
- **设计总览**（需了解全局时读取）：design/README.MD
- **具体文档**（按需读取）：根据 §5 速查表按文件路径检索
- **检索方式**：先查索引（Table-Index.md、API-Index.md），再按需读详情

---

## 5. 按任务类型速查

| 任务类型 | 核心文档 | 补充参考 |
|---|---|---|
| 讨论产品方向 | README、01-vision/Product-Vision.md、07-development/Roadmap.md | doc/00 |
| 写/改需求 | 02-requirements/PRD.md、01-vision/Product-Vision.md | doc/00、README |
| 架构设计 | 02-requirements/PRD.md、03-architecture/SAD.md、Deployment.md | doc/01、06-research |
| AI 接入设计 | 03-architecture/AI-Architecture.md、05-api/MCP-API.md、03-architecture/Permission-Design.md | doc/01 |
| 数据库设计 | 02-requirements/PRD.md、04-database/Data-Dictionary-Standard.md、Data-Dictionary.md、Table-Index.md、ERD.md | doc/01 |
| 接口设计 | 02-requirements/PRD.md、05-api/OpenAPI.yaml、API-Index.md、03-architecture/Permission-Design.md | doc/01 |
| 写代码 | 02-requirements/PRD.md、03-architecture/SAD.md、07-development/Coding-Standards.md、AI-Dev-Rules.md、04-database/Table-Index.md、05-api/API-Index.md | 对应模块 dev 文档 |
| 测试 | 07-development/Test-Strategy.md、02-requirements/PRD.md、05-api/OpenAPI.yaml、03-architecture/Permission-Design.md | AI-Dev-Rules.md |
| 部署运维 | 03-architecture/Deployment.md、07-development/Release-Process.md | doc/01 |
| 调研参考 | 06-research/OpenSource-HIS-AI-Research.md | doc/02 |

---

## 6. 偏离处理与反馈闭环

### 6.1 开发中的偏离

出现以下情况时，**停止实现**，先更新文档再继续：

| 情况 | 处理方式 |
|---|---|
| 实现需求不在 PRD 中 | 先更新 PRD，再更新受影响的设计文档 |
| 技术方案与 SAD 不一致 | 先修订 SAD 及联动文档 |
| 新增/变更表或字段 | 同步更新 Data-Dictionary、Table-Index、ERD |
| 新增/变更接口 | 同步更新 OpenAPI、API-Index、MCP-API（若 AI 可调用） |
| 变更权限模型 | 同步更新 Permission-Design、API-Index |
| 愿景/范围重大调整 | 先修订 README.MD（宪法），再从 01-vision 起逐级向下联动 |

**小偏离（可先编码、提交时同步文档）**须同时满足以下全部条件：

- 不涉及新功能需求（已在 PRD 或设计文档中有依据）
- 不新增/变更数据库表、字段或索引
- 不新增/变更 API 路径、参数或权限
- 不新增/变更用户角色或权限模型
- 不改变模块边界（HIS / 康养）或部署模式
- 变更范围限于 1-2 个代码文件

典型示例：修复已有逻辑的 bug、UI 文案/样式微调、性能优化（不改变接口契约）。

**不属于小偏离**（须先更新文档再编码）：新增业务功能、新增表/接口/角色、跨模块调用变更、AI 可调用接口变更。

### 6.2 上线后的反馈闭环

线上问题或用户反馈驱动文档修订的流程：

```
线上问题 / 用户反馈
       ↓
定位根因所在文档层级：
  需求层（PRD）？设计层（SAD/DB/API）？开发规范层（Coding-Standards）？
       ↓
修订该层文档 → 检查联动关系 → 向下逐级更新受影响文档
       ↓
编码修复 → 测试验证 → 部署上线
       ↓
更新 design/README.MD 修订记录（若为结构性变更）
```

**关键原则**：问题修复不是从代码开始，而是从**定位文档层级**开始。代码层面的 bug 修复也要回查是否是设计或需求层面的疏漏——如果是，先修文档再修代码。

### 6.3 已上线版本的迭代

版本迭代（v1.0 → v1.1）时，建议从 `01-vision/Product-Vision.md` 和 `07-development/Roadmap.md` 出发，逐级审视文档是否需要修订，而非直接跳到编码。

---

## 7. 文档修订

1. **结构性变更**（目录新增/删除/重组、顶层约定变更）：在 [design/README.MD](./design/README.MD) 修订记录中追加一行。
2. **单文档内容变更**：由 Git 自然追踪，不额外维护修订记录。
3. 若本文档结构性变更，在下方登记：

| 版本 | 日期 | 修订人 | 变更摘要 | 联动更新 |
|---|---|---|---|---|
| 1.0.0 | 2026-06-17 | - | 初版：阶段指引、必读清单、偏离处理 | README.MD、design/README.MD |
| 1.1.0 | 2026-06-17 | - | README 升格为项目宪法，更新文档优先级 | README.MD |
| 2.0.0 | 2026-06-17 | - | 重构：轻量入口+按需检索；新增测试/部署阶段；checklist化完成标准；补充反馈闭环 | design/README.MD、design/01~07/README.MD |
| 2.1.0 | 2026-06-17 | - | 明确双模块+模块化单体；补充小偏离判定；Test-Strategy/dev 落点 | README.MD、design/README.MD、dev/README.MD、his-ai-architect.mdc |
