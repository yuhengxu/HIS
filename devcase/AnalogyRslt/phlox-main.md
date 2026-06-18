# Phlox 项目深度分析

> 分析文档：`devcase/AnalogyRslt/phlox-main.md`（上传 git）  
> 源码路径（本地）：`devcase/phlox-main/`（不上传 git）
> 项目：[bloodworks-io/phlox](https://github.com/bloodworks-io/phlox)  
> 版本：1.0.5 | 许可：MIT  
> 定位：**本地优先（local-first）AI 辅助医疗文书系统**（实验性，非认证医疗器械）

---

## 1. 系统架构

### 1.1 总体分层

```
Client: React 18 + Vite + Chakra UI (+ Tauri 2 桌面壳)
    ↓ REST API + SSE 流式
Backend: FastAPI + uvicorn + APScheduler
    ├── NLP 流水线（转录/模板/信件/推理）
    ├── ChatEngine + Tool Executor
    └── MCP 客户端
    ↓
Data: SQLCipher 加密 SQLite + ChromaDB 向量库(Docker)
    ↓
AI: Ollama/OpenAI/llama.cpp + Whisper + ONNX MiniLM Embeddings
```

### 1.2 部署模式差异

| 模式 | 特点 |
|------|------|
| Docker/Podman | 端口 5000，含 ChromaDB/RAG/Agent/MCP 全功能，默认无认证 |
| Tauri 桌面 | 捆绑 llama.cpp+whisper.cpp，口令解锁，**无 RAG**，仅转录/笔记/信件 |

### 1.3 API 路由总览

| 前缀 | 职责 |
|------|------|
| `/api/note` | 患者/就诊 CRUD、任务、推理 |
| `/api/transcribe` | 音频/文档转结构化笔记 |
| `/api/chat` | 聊天、视觉文档 |
| `/api/rag` | 文档集合管理 |
| `/api/templates` | 临床模板 CRUD |
| `/api/letter` | 信件生成 |
| `/api/dashboard` | Todo、健康检查 |
| `/api/config/*` | 全局配置、模型、MCP |

---

## 2. 设计方案

### 2.1 设计哲学

- **本地优先**：数据不出机器（SQLCipher + 可选完全离线）
- **管理性自动化**：减轻文书负担，**不做临床决策**
- **适配小模型**：分字段并发 + JSON Schema + refinement 多 pass

### 2.2 模板驱动文档生成

每个 Field 可配置：system_prompt、format_schema（bullet/numbered/narrative）、persistent（跨就诊携带）、adaptive_refinement_instructions

内置模板：**Phlox**、**SOAP**、**Progress Note**

### 2.3 转录 → 笔记流水线

```
音频/文档 → Whisper 转写 → 按模板字段并发 LLM 处理
  → JSON 结构化提取 → 字段级 refinement
  → 合并 persistent 字段 → 返回前端
```

### 2.4 Agent 工具架构

- ChatEngine 协调 LLM + ChromaDB + Tool Executor
- Interleaved thinking：多轮 tool call 链式推理
- 12 内置工具 + 动态 MCP 工具
- PubMed/Wikipedia 默认禁用（PHI 外泄风险）

---

## 3. 功能模块详解

### 3.1 患者与就诊管理

- **路径/入口**:
  - 后端：`server/api/patient.py`（prefix `/api/note`）
  - 前端：`src/pages/PatientDetails.jsx`（路由 `/new-note`、`/note/:id`）
- **核心数据表/模型**: `patients` 表 — id, name, dob, ur_number, gender, encounter_date, template_key, template_data(JSON), raw_transcription, jobs_list(JSON), reasoning_output(JSON), primary_condition, encounter_summary, final_letter
- **功能点清单**:
  1. `POST /api/note/save` — 保存/更新就诊（Last,First 姓名、DOB、UR、性别、日期）
  2. `GET /api/note/list?date=&detailed=` — 按日期侧边栏列表
  3. `GET /api/note/id/{id}` — 就诊详情
  4. `GET /api/note/id/{id}/history` — 历史就诊
  5. `GET /api/note/search?ur_number=` — UR 号搜索复用患者历史
  6. `GET /api/note/history` — 跨 UR 历史
  7. `GET /api/note/summary/{id}` — 摘要
  8. `DELETE /api/note/id/{id}` — 删除
  9. 前端 ConfirmLeaveModal — 未保存离开确认
  10. UR 搜索 → 自动填充 persistent 字段
- **与其他模块的关联**: → 模板（template_key/template_data）、转录、任务、推理、信件

### 3.2 医疗转录（Scribe）

- **路径/入口**:
  - 后端：`server/api/transcribe.py`（prefix `/api/transcribe`）
  - 前端：PatientDetails 内 Scribe 面板 + TranscriptionPanel
- **功能点清单**:
  1. `POST /api/transcribe/audio` — 浏览器录音/音频文件 → Whisper 转写
  2. `POST /api/transcribe/dictate` — 单字段口述
  3. `POST /api/transcribe/reprocess` — 不换音频只重跑 LLM
  4. `POST /api/transcribe/process-document` — PDF/Word/图片 → 混合管道
  5. `POST /api/transcribe/process-document-visual` — Vision OCR
  6. `POST /api/transcribe/process-document-from-text` — 纯文本输入
  7. 前端 MediaRecorder 录音：暂停/继续/计时
  8. Ambient / Dictate 模式 — 影响 LLM 提示策略
  9. Whisper：外部 API 或 whisper.cpp（桌面版）
  10. 转写 → 按模板字段并发 LLM → JSON 结构化 → 字段级 refinement
  11. 查看原始转录：TranscriptionPanel
- **与其他模块的关联**: → 模板字段、patient.raw_transcription

### 3.3 临床笔记模板

- **路径/入口**:
  - 后端：`server/api/templates.py` + `server/database/config/defaults/templates.py`
  - Schema：`server/schemas/templates.py`
  - 前端：Settings Template Panel
- **核心数据表/模型**: `clinical_templates` — template_key(PK, 版本化), template_name, fields(JSON), deleted
- **功能点清单**:
  1. 三套内置：phlox_01（Phlox）、soap_01（SOAP）、progress_01（Progress Note）
  2. `GET/POST /api/templates` — 模板 CRUD
  3. `POST /api/templates/generate` — 从示例笔记 AI 生成模板
  4. `GET/POST /api/templates/default` — 默认模板
  5. `GET/DELETE /api/templates/{template_key}` — 单模板
  6. 版本控制：内容变更 → `{base_key}_{n+1}` 新版本，旧版 soft delete
  7. 字段 Schema：field_key, field_name, field_type, required, persistent, system_prompt, format_schema, style_example, refinement_rules, adaptive_refinement_instructions
  8. format_schema.type：bullet / numbered / narrative
  9. Plan 字段强制编号 → 驱动 jobs 提取
  10. `POST .../adaptive-instructions/reset|consolidate` — 从用户编辑学习偏好
  11. persistent 字段跨就诊携带（get_persistent_fields）
- **与其他模块的关联**: → 转录流水线、任务提取、就诊 template_data

### 3.4 任务管理（Jobs & Todo）

- **路径/入口**:
  - 后端：patient.py 的 jobs 端点 + `server/api/dashboard.py`
  - 前端：ClinicSummary、OutstandingJobs、Dashboard Todo Panel
- **核心数据表/模型**: patients.jobs_list(JSON)、patients.all_jobs_completed；todos 表
- **功能点清单**:
  1. Plan 编号列表 → 自动生成 jobs_list
  2. `POST /api/note/update-jobs-list` / `update-jobs` — 任务更新
  3. `GET /api/note/outstanding-jobs` — 跨日未完成任务
  4. `GET /api/note/incomplete-jobs-count` — 未完成计数
  5. `/clinic-summary` — 按日期展示所有患者摘要+jobs
  6. `/outstanding-jobs` — 按日期分组汇总
  7. Dashboard Todo CRUD：`POST/GET /api/dashboard/todos`、`PUT/DELETE /api/dashboard/todos/{id}`
  8. Agent 工具：get_patient_jobs、list_outstanding_jobs、complete_job、todo_list
- **与其他模块的关联**: ← 模板 Plan 字段、→ Agent 工具

### 3.5 信件/通信（Correspondence）

- **路径/入口**:
  - 后端：`server/api/letter.py`（prefix `/api/letter`）
  - 前端：PatientDetails Letter Panel + Settings Letter Panel
- **核心数据表/模型**: `letter_templates` — id, name, instructions；patients.final_letter
- **功能点清单**:
  1. `POST /api/letter/generate` — 一键生成（Ambient/Dictate 双模式）
  2. `POST /api/letter/save` / `GET /api/letter/fetch-letter`
  3. 内置模板：GP Letter、Specialist Referral、Discharge、Brief Update、Dictation
  4. 信件模板 CRUD：`GET/POST /api/letter/templates`、`GET/PUT/DELETE /templates/{id}`
  5. `POST /api/letter/templates/reset` — 重置默认
  6. LLM 交互式 refine
- **与其他模块的关联**: ← 当前就诊笔记+转录

### 3.6 AI 聊天

- **路径/入口**:
  - 后端：`server/api/chat.py`（prefix `/api/chat`）
  - 前端：PatientDetails Chat Panel + LandingPage DashboardChat
- **功能点清单**:
  1. `POST /api/chat` — 主聊天 SSE 流式
  2. 患者视图：引用当前笔记+转录
  3. Quick Chat 3 个可配置快捷问题（user_settings）
  4. `POST /api/chat/upload-image` — 图片上传
  5. `POST /api/chat/analyze-document-visual` — 视觉文档分析
  6. `POST /api/chat/respond-visual` — 视觉回复
  7. `GET/POST /api/chat/vision-capability` — 视觉能力开关
  8. 工具：transcript_search、get_relevant_literature
- **与其他模块的关联**: → ChatEngine + Tool Executor + RAG

### 3.7 Agent Dashboard

- **路径/入口**: `src/pages/LandingPage.jsx`（路由 `/`）
- **功能点清单**:
  1. ChatGPT 风格 DashboardChat 对话界面
  2. PDF/图片拖拽上传
  3. 内嵌 Todo Panel
  4. 完整 tool calling（12 内置 + MCP 动态）
  5. Interleaved thinking 多轮推理
  6. 免责声明弹窗
- **与其他模块的关联**: → 全部 Agent 工具

### 3.8 RAG 文档知识库（仅 Docker）

- **路径/入口**:
  - 后端：`server/api/rag.py`（prefix `/api/rag`）
  - 前端：`src/pages/Rag.jsx`（路由 `/rag`）
- **功能点清单**:
  1. `GET /api/rag/files` / `collection_files/{name}` — 集合浏览
  2. `POST /api/rag/modify` — 修改集合
  3. `POST /api/rag/extract-pdf-info` — PDF 元数据提取
  4. `POST /api/rag/commit-to-vectordb` — 入库
  5. `DELETE /api/rag/delete-collection/{name}` / `delete-file` / `clear-database`
  6. `GET /api/rag/suggestions` — 搜索建议
  7. 语义分块 + ONNX MiniLM Embeddings + ChromaDB 向量检索
  8. 前端 DocumentExplorer + Uploader
- **与其他模块的关联**: → get_relevant_literature 工具

### 3.9 内置 Agent 工具（12 类）

- **路径/入口**: `server/utils/chat/tools/registry.py` + `executor.py`
- **功能点清单**:

  | 工具名 | 用途 | 默认状态 |
  |--------|------|----------|
  | transcript_search | 搜索当前转录/对话 | 启用 |
  | get_relevant_literature | ChromaDB 文献检索 | 启用（需 Docker） |
  | pubmed_search | PubMed 检索 | **禁用** |
  | wiki_search | Wikipedia 检索 | **禁用** |
  | get_previous_encounter | 按 UR/姓名查历史 | 启用 |
  | direct_response | 非医学直答 | 启用 |
  | create_note | Agent 创建新就诊 | 启用 |
  | get_patient_jobs | 某患者未完成任务 | 启用 |
  | todo_list | 全局 Todo CRUD | 启用 |
  | search_patient_notes | 模糊搜索历史笔记 | 启用 |
  | list_outstanding_jobs | 跨患者未完成任务 | 启用 |
  | complete_job | 标记 job 完成 | 启用 |

  1. 动态 MCP 工具：`mcp_{server_name}_{tool_name}`
  2. `user_settings.disabled_tools` 过滤
  3. reasoning 场景 exclude_chat_only 排除 transcript/direct_response
  4. **注意**：search_patient.py 存在但未注册

- **与其他模块的关联**: ChatEngine 统一调度

### 3.10 MCP 外部工具集成

- **路径/入口**:
  - 配置：`server/database/config/mcp_manager.py` + `server/api/config/mcp.py`
  - 客户端：`server/utils/mcp/client.py`
  - 执行：`server/utils/chat/tools/mcp_tool.py`
- **核心数据表/模型**: `mcp_servers` — id, name, url, description, server_version, enabled, allow_sensitive_data
- **功能点清单**:
  1. Settings 配置 MCP 服务器 URL
  2. SSE 传输 → ClientSession.initialize/list_tools/call_tool
  3. `GET/POST /api/config/mcp` — 列表/新增
  4. `GET/PUT/DELETE /api/config/mcp/{server_id}` — CRUD
  5. `POST /api/config/mcp/{server_id}/toggle|test` — 启停/连接测试
  6. `POST /api/config/mcp/refresh-tools` — 刷新工具缓存
  7. PHI 过滤：allow_sensitive_data=False 时 sanitize_query_for_external_search
  8. 工具命名：mcp_{sanitized_server_name}_{tool.name}
- **与其他模块的关联**: → registry.get_tools_definition 合并到 LLM tools

### 3.11 教育性临床推理

- **路径/入口**: `POST /api/note/{note_id}/reasoning/stream`（SSE）
- **功能点清单**:
  1. Generate Reasoning 按钮 → SSE 流式
  2. 输出：案例摘要、文献关联、标准检查参考、文档 QA
  3. 结果持久化 reasoning_output(JSON)
  4. ClinicSummary PatientTable 预览 reasoning
- **与其他模块的关联**: ← 当前就诊笔记+转录+模板

### 3.12 诊所摘要

- **路径/入口**: `src/pages/ClinicSummary.jsx`（路由 `/clinic-summary`）
- **功能点清单**:
  1. 按 selectedDate 拉 `/api/note/list?detailed=true`
  2. PatientTable：摘要、jobs、reasoning 预览
  3. 日视图全患者概览

### 3.13 设置模块

- **路径/入口**: `src/pages/Settings.jsx`（路由 `/settings`）
- **七大面板**:
  1. User — 姓名/专科/默认模板/Quick Chat/ disabled_tools
  2. Model — LLM/Whisper/Embedding 选择与参数
  3. Prompt — 分 category 的 system prompt
  4. Template — 模板 CRUD + adaptive 指令
  5. Letter — 信件模板管理
  6. Chat — 聊天配置
  7. Local Model Manager — llama.cpp/whisper.cpp 下载（桌面版）
  8. MCP — 外部工具服务器配置
- **API**: `/api/config/global`、`/models`、`/prompts`、`/user`、`/system`、`/validate-url`

### 3.14 桌面版特有（Tauri）

- **功能点清单**:
  1. SQLCipher 口令解锁 Setup/Unlock 流程
  2. llama.cpp/whisper.cpp 本地模型下载管理
  3. 本地 request token 防 CSRF
  4. **无 RAG/ChromaDB**
  5. 仅 Apple Silicon 支持

---

## 4. 系统优点

1. **真正的 local-first**：SQLCipher + 桌面可完全离线
2. **分字段 LLM 流水线**适配小模型，降低幻觉
3. **Adaptive refinement** 从用户编辑中学习偏好
4. **12 内置工具 + MCP** 可扩展 Agent 架构
5. **模板系统细致**：persistent 字段、Plan→Jobs 自动化
6. **Schema 迁移 + 自动备份** + CI/CodeQL

---

## 5. 系统不足

### 5.1 合规

- 非认证医疗器械，不满足 HIPAA/GDPR
- Docker 默认 0.0.0.0 + 无认证
- 无 RBAC、无审计日志

### 5.2 功能

- 患者管理极基础，无床位/医嘱/检验/HIS 集成
- 桌面 vs Docker 功能割裂
- 桌面仅 Apple Silicon
- 无多用户/多诊所

### 5.3 与国内场景差距

- 无中文/本地化
- 无国标病历格式、医保接口
- 无护理评估/交接班专项模块

---

## 6. 开发参考索引

| 模块 | 关键文件 | 可复用模式 |
|------|----------|------------|
| 转录流水线 | `server/api/transcribe.py` | Whisper → 分字段并发 LLM → JSON |
| 模板引擎 | `server/schemas/templates.py`、`defaults/templates.py` | persistent + adaptive refinement |
| 任务自动化 | patient.jobs_list + Plan 编号 | Plan→Jobs 提取 |
| Agent 工具 | `tools/registry.py`、`executor.py` | 12 内置 + MCP 动态注册 |
| MCP 集成 | `utils/mcp/client.py`、`mcp_tool.py` | SSE 传输 + PHI 过滤 |
| 聊天 | `server/api/chat.py` | SSE 流式 + tool calling |
| RAG | `server/api/rag.py` | ChromaDB + MiniLM Embeddings |
| 信件 | `server/api/letter.py` | 模板驱动 + LLM refine |
| 推理 | patient.reasoning/stream | 教育性 SSE 推理 |
| 数据库 | `server/database/` | SQLCipher + schema v5 迁移 |
| 前端 | `src/pages/PatientDetails.jsx` | 多面板（Scribe/Summary/Chat/Letter） |
| 桌面 | Tauri 壳 | local-first + 口令解锁 |

---

## 对本项目的借鉴价值

| 可借鉴 | 需规避 |
|--------|--------|
| 模板引擎 + 分字段 LLM 流水线 | Docker 无认证部署 |
| Adaptive refinement 学习机制 | 当作完整 HIS/EMR |
| MCP + Tool Calling 架构 | 英文-only 的 UI/模板 |
| Plan→Jobs 任务自动化 | 非合规医疗决策输出 |
| local-first + SQLCipher 安全模型 | 桌面/Docker 功能不一致 |
