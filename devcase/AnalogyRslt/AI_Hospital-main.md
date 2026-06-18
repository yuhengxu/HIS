# AI Hospital 项目深度分析

> 分析文档：`devcase/AnalogyRslt/AI_Hospital-main.md`（上传 git）  
> 源码路径（本地）：`devcase/AI_Hospital-main/`（不上传 git）
> 论文：[AI Hospital: Interactive Evaluation and Collaboration of LLMs as Intern Doctors](https://arxiv.org/abs/2402.09742)  
> 定位：**多 Agent 模拟的 LLM 临床诊断评测与协作框架**（非生产级 HIS）

---

## 1. 系统架构

### 1.1 架构概览

```
入口层 (run.py + options.py + register.py)
    ↓
场景层 (Scenario.Consultation / Scenario.CollaborativeConsultation)
    ↓
Agent 层 (Doctor / Patient / Reporter / Host + 7 种 LLM 后端)
    ↓
引擎层 (Engine: GPT / Qwen / WenXin / ChatGLM / MiniMax / HuatuoGPT / HF)
    ↓
数据层 (patients.json ~506 病例 + outputs/*.jsonl)
    ↓
评估层 (eval.py MVME 五维评分 + eval_db.py ICD-10 + eval_show.py Bootstrap)
```

### 1.2 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Python 3 |
| LLM | openai、dashscope、zhipuai、requests、transformers+torch |
| 数据 | JSON 病例库、JSONL 对话/评估结果 |
| 并发 | ThreadPoolExecutor |
| 评估 | bootstrapped、fuzzywuzzy、xlrd |

### 1.3 模块划分

| 模块 | 路径 | 职责 |
|------|------|------|
| 入口 | `src/run.py` | CLI 参数解析，串行/并行运行 |
| 注册中心 | `src/utils/register.py` | `@register_class` 装饰器 + 全局 registry（22 项） |
| Agent | `src/agents/` | 医生、患者、检查员、主任医生 |
| 引擎 | `src/engine/` | 统一 `get_response(messages)` 接口 |
| 场景 | `src/hospital/` | 两种工作流编排 |
| 数据 | `src/data/` | MVME 基准病例 + 协作医生配置 |
| 评估 | `src/evaluate/` | 三维评估体系 |

---

## 2. 设计方案

### 2.1 核心设计模式

1. **注册表模式** — Agent/Engine/Scenario 通过字符串别名动态实例化
2. **策略模式** — Engine 抽象 + 多 LLM 后端；Doctor 基类 + 各模型子类
3. **模板方法** — Consultation/CollaborativeConsultation 定义固定流程骨架
4. **Prompt 驱动结构化输出** — `#字段名#` 标记 + 正则解析（无 function calling）
5. **记忆管理** — Agent.memories 单会话；Doctor.memories[patient_id] 按患者分组

### 2.2 Agent 角色设计

| Agent | 别名 | 角色 |
|-------|------|------|
| Doctor | Agent.Doctor.* | 实习医生：问诊、诊断、协作修订 |
| Patient | Agent.Patient.GPT | 模拟患者（固定 GPT 后端） |
| Reporter | Agent.Reporter.GPT | 检查数据库管理员 |
| ReporterV2 | Agent.Reporter.GPTV2 | 带检查项目 NER 预处理 |
| Host | Agent.Host.GPT | 主任医生/会诊主持人 |

### 2.3 三条核心数据流

**A. 交互式诊断（MVME）**：Patient + Doctor + Reporter 多轮对话 → 结构化总结（#症状#/#辅助检查#/#诊断结果#/#诊断依据#/#治疗方案#）

**B. 协作会诊**：Host 汇总多医生意见 → Parallel / Parallel_with_Critique 讨论 → 最终会诊结论

**C. 评估**：GPT-4 Judge 五维 A-D 评分 + ICD-10 Set F1 + Bootstrap 置信区间

---

## 3. 功能模块详解

### 3.1 Engine 层（LLM 后端）

- **路径/入口**: `src/engine/`；注册表 22 项中的 8 个 Engine 别名
- **核心数据表/模型**: 无持久化；messages 列表 `[{role, content}]`
- **功能点清单**:

  | 类 | 别名 | 后端 | 关键行为 |
  |----|------|------|----------|
  | Engine | Engine.Base | 抽象 | `get_response(messages)` |
  | GPTEngine | Engine.GPT | OpenAI Chat Completions | BadRequest→16k fallback；RateLimit 重试 |
  | QwenEngine | Engine.Qwen | DashScope Generation.call | seed 固定 |
  | WenXinEngine | Engine.WenXin | 百度 ERNIE-Bot4 REST | get_access_token()；system 参数 |
  | ChatGLMEngine | Engine.ChatGLM | 智谱 sse_invoke 流式 | 流式拼接 |
  | MiniMaxEngine | Engine.MiniMax | MiniMax chatcompletion_pro | 需 bot_setting |
  | HuatuoGPTEngine | Engine.HuatuoGPT | 本地 HuatuoChat | AutoModelForCausalLM |
  | HFEngine | Engine.HF | 通用 HuggingFace | model.chat(tokenizer, messages) |

  1. 统一接口：`get_response(messages) → str`
  2. 各 Engine 独立 API Key/endpoint 配置（CLI 参数）
  3. **Bug**：`Engine.GPTV2` 被 ReporterV2 引用但未实现

- **与其他模块的关联**: 所有 Agent 的 LLM 调用出口

### 3.2 Doctor Agent 模块

- **路径/入口**: `src/agents/doctor.py` + 7 个子类（gpt/chatglm/minimax/wenxin/qwen/huatuogpt/hf）
- **核心数据表/模型**: `memories[patient_id]` 对话历史；`diagnosis[patient_id]` 结构化诊断
- **功能点清单**:
  1. 系统 Prompt：逐步问诊、单次一问、必要时开检查、最终五段结构化输出
  2. `doctor_greet()` — 固定开场白「您好，有哪里不舒服？」
  3. `speak(content, patient_id)` — 维护 per-patient 对话记忆 → LLM → 写回
  4. `parse_diagnosis(text)` — 正则提取 `#症状#/#辅助检查#/#诊断结果#/#诊断依据#/#治疗方案#`
  5. `get_diagnosis_by_patient_id(patient_id, key)` — 取全部或单字段
  6. `load_diagnosis(dict/jsonl/evaluation_file)` — 从多种来源加载诊断
  7. `memorize(message, patient_id)` / `forget(patient_id)` — 按患者维度记忆
  8. `revise_diagnosis_by_symptom_and_examination(patient, summary)` — Host 汇总后修订
  9. `revise_diagnosis_by_others(patient, doctors, host_critique, mode)` — 分发 Parallel/Critique
  10. `revise_diagnosis_by_others_in_parallel(...)` — 并行接收多医生意见
  11. `revise_diagnosis_by_others_in_parallel_with_critique(...)` — 批判性讨论修订
  12. 7 个子类差异：Engine 绑定 + speak/get_response 消息格式适配
- **与其他模块的关联**: → Engine、Patient（对话）、Reporter（检查查询）、Host（协作修订）

### 3.3 Patient Agent 模块

- **路径/入口**: `src/agents/patient.py`；别名 `Agent.Patient.GPT`
- **核心数据表/模型**: 注入 `profile` + `medical_record`（现病史/既往史/个人史）
- **功能点清单**:
  1. 注入 profile：`<病情陈述>`/`<性别>`/`<年龄>`/`<工作与生活>`/`<说法方式>`
  2. 注入 medical_record：现病史、既往史、个人史（ground truth，仅 Patient 可见）
  3. `speak(role, content)` — 前缀 `<医生>`/`<检查员>` 调用 GPT
  4. `parse_role_content(response)` — 解析 `<对医生讲>`/`<对检查员讲>` 路由目标
  5. 收到诊断后加 `<结束>` 标记终止对话
  6. 硬绑定 GPT 后端（不可配置其他 Engine）
- **与其他模块的关联**: → Doctor（问诊）、Reporter（检查查询）

### 3.4 Reporter Agent 模块

- **路径/入口**: `src/agents/reporter.py`；别名 `Agent.Reporter.GPT` / `Agent.Reporter.GPTV2`
- **核心数据表/模型**: 注入 `#查体#` + `#辅助检查#`（来自 medical_record ground truth）
- **功能点清单**:
  1. `speak(medical_records, content)` — 注入查体/辅助检查 + few-shot → GPT 回复
  2. 忠实回复：仅返回 medical_record 中存在的检查结果
  3. **ReporterV2** 额外：`parse_examination_queries(query)` — LLM NER 预处理检查申请
  4. `parse_content(response)` — 提取 `#检查项目#` 段
  5. **Bug**：ReporterV2 依赖未实现的 `Engine.GPTV2`；`super(Reporter, self)` 写错
- **与其他模块的关联**: → Doctor（检查结果反馈）、Patient（检查查询路由）

### 3.5 Host Agent 模块

- **路径/入口**: `src/agents/host.py`；别名 `Agent.Host.GPT`
- **功能点清单**:
  1. `summarize_symptom_and_examination(doctors, patient, reporter)` — 汇总多医生症状/检查
  2. 矛盾时追问 Patient/Reporter 消歧
  3. `parse_symptom_and_examination(response)` — 解析四段（症状/辅助检查/询问病人/询问检查员）
  4. `edit_symptom_and_examination(structure_result)` — 据追问回答修正
  5. `measure_agreement(doctors, patient, discussion_mode)` — 输出 `#结束#` 或 `#继续#` + 争议点
  6. `summarize_diagnosis(doctors, patient)` — 最终会诊结论
  7. **Bug**：变量名 typo（query_to_repoter）
- **与其他模块的关联**: → Doctor（汇总/讨论/最终结论）、Patient/Reporter（消歧）

### 3.6 Scenario.Consultation（MVME 交互评测）

- **路径/入口**: `src/hospital/consultation.py`；别名 `Scenario.Consultation`
- **CLI 参数**:
  - `--patient_database`、`--patient`、`--doctor`、`--reporter`
  - `--max_conversation_turn`（默认 10）、`--max_workers`、`--delay_between_tasks`
  - `--save_path`、`--ff_print`、`--parallel`
- **功能点清单**:
  1. 加载 patients.json → 实例化 Patient/Doctor/Reporter
  2. `remove_processed_patients()` — 读 jsonl 已处理 ID，过滤并 shuffle（断点续跑）
  3. `run()` / `parallel_run()` — 串行或 ThreadPoolExecutor 并行
  4. `_diagnosis(patient)` 核心循环：
     - Turn 0：Doctor 固定问候
     - 每轮：Patient 回复 → parse_role_content 路由
     - 对医生：Doctor.speak；对检查员：Reporter.speak → 结果喂 Doctor
     - 含 `<结束>` 或达 max turn → break
     - 最后 Doctor 回答 medical_director_summary_query（强制五段输出）
  5. `save_dialog_info()` — 追加 jsonl（patient_id、agent/engine 名、dialog_history、time）
- **与其他模块的关联**: 编排 Patient + Doctor + Reporter + Engine

### 3.7 Scenario.CollaborativeConsultation（协作会诊）

- **路径/入口**: `src/hospital/collaborative_consultation.py`
- **CLI 参数**:
  - `--scenario Scenario.CollaborativeConsultation`
  - `--doctor_database`、`--number_of_doctors`（默认 2）
  - `--host`、`--discussion_mode`（Parallel | Parallel_with_Critique）
  - `--max_discussion_turn`（默认 4）
- **功能点清单**:
  1. 从 doctors.json 加载多 Doctor → 各自 load_diagnosis
  2. Host.summarize_symptom_and_examination → 必要时消歧
  3. 各 Doctor.revise_diagnosis_by_symptom_and_examination
  4. Host.measure_agreement → 若 `#继续#`，循环 max_discussion_turn：
     - 各 Doctor.revise_diagnosis_by_others
     - 再次 measure_agreement
  5. Host.summarize_diagnosis → 保存 jsonl（含 diagnosis_in_discussion 全过程）
- **与其他模块的关联**: 编排 Host + 多 Doctor + Patient/Reporter

### 3.8 数据层

- **路径/入口**: `src/data/patients.json`、`src/data/collaborative_doctors/doctors.json`
- **patients.json schema（~506 条）**:
  - 元数据：local, title, author, department, diseases, time, read_num, comment_num, url
  - 标识：id（整数）
  - 三份病历：raw_medical_record, reformed_text_medical_record, medical_record（**场景实际使用**）
  - 模拟患者：profile（`<病情陈述>`/`<性别>`/`<年龄>`/`<工作与生活>`/`<说法方式>`）
  - medical_record 子字段：一般资料, 主诉, 现病史, 既往史, 查体, 辅助检查, 初步诊断, 诊断依据, 鉴别诊断, 诊治经过, 诊断结果, 分析总结
- **doctors.json**: doctor_name, diagnosis_filepath, 可选 doctor_openai_model_name/evaluation_filepath/doctor_key
- **预置 outputs/**: dialog_history, onestep, collaboration_history, evaluation

### 3.9 评估层

#### eval.py — MVME 五维 LLM-as-Judge
- **路径/入口**: `src/evaluate/eval.py`
- **功能点清单**:
  1. `load_reference_diagnosis` — 从 patients.json 取 ground truth
  2. `build_onestep_platform` / `build_dialog_platform` / `build_collaborative_discussion_platform`
  3. `evaluate()` / `parallel_evaluate()` — 断点续跑 + 线程池
  4. `evaluate_one()` — GPT-4 Judge 对比专家 vs 实习医生
  5. `parse_response()` — 解析五维 A-D 选项
  6. **五维指标**：① 病人症状掌握 ② 医学检查完整性 ③ 诊断结果一致性 ④ 诊断依据一致性 ⑤ 治疗方案一致性

#### eval_db.py — ICD-10 Set F1
- **功能点清单**:
  1. GPT 标准化疾病名（`##` 分隔）
  2. fuzzywuzzy 匹配 ICD-10 xls
  3. 输出 Set Recall/Precision/F1
  4. **依赖 ICD-10 数据库文件未随仓库提供**

#### eval_show.py — Bootstrap 置信区间
- **功能点清单**:
  1. A=4/B=3/C=2/D=1 映射
  2. Bootstrap 10000 次 95% CI
  3. 交互式/单步结果展示

### 3.10 注册表与 CLI

- **路径/入口**: `src/utils/register.py`、`src/utils/options.py`、`src/run.py`
- **22 个注册项**: 3 Scenario + 8 Engine + 11 Agent
- **run.py**: 解析 args → registry.get_class(scenario)(args) → run()/parallel_run()
- **run.sh**: 6 组模型 MVME 复现（GPT-4/3.5/WenXin/Qwen/Baichuan/HuatuoGPT）
- **run_md.sh**: 协作会诊复现（Parallel_with_Critique, 2 doctors, 4 turns）
- **eval.sh**: eval.py + eval_show.py 串联

---

## 4. 系统优点

1. **MVME 五维评测**比单一诊断准确率更全面
2. **多 Agent 角色分离**：Patient/Reporter/Host 隔离信息，防止 ground truth 泄露
3. **可插拔 LLM 架构**：Registry + 策略模式，新增后端只需 Engine + Doctor 子类
4. **协作诊断创新**：Parallel_with_Critique 争议驱动 MDT 讨论
5. **工程实用**：断点续跑、并行加速、结构化输出
6. **中文医学场景适配**：Prompt、病历、评估均为中文

---

## 5. 系统不足

### 5.1 代码 Bug

| 问题 | 位置 |
|------|------|
| `Engine.GPTV2` 未实现 | reporter.py |
| `super(Reporter, self)` 错误 | reporter.py:88 |
| 变量名 typo | host.py |
| 条件判断 bug | doctor.py:72 |
| 裸 except / raise 字符串 | 多处 |

### 5.2 依赖与环境

- `requirements.txt` 严重不完整（缺 jsonlines、tqdm、torch 等）
- ICD-10 数据库文件未随仓库提供
- 无 Web UI / API 服务，纯 CLI 批处理

### 5.3 架构局限

- Patient/Reporter/Host 硬绑定 GPT
- 无 RAG/知识库，纯 Prompt 工程
- `#字段#` + 正则解析脆弱
- 评估本身依赖 GPT-4 Judge，存在 evaluator bias
- 506 病例规模偏小

---

## 6. 开发参考索引

| 模块 | 关键文件 | 可复用模式 |
|------|----------|------------|
| 注册表 | `utils/register.py` | @register_class 动态实例化 |
| Engine | `engine/gpt.py` 等 | 策略模式统一 get_response |
| Doctor | `agents/doctor.py` | per-patient 记忆 + 结构化输出解析 |
| Patient | `agents/patient.py` | 角色路由 `<对医生讲>`/`<对检查员讲>` |
| Reporter | `agents/reporter.py` | ground truth 隔离 + 忠实回复 |
| Host | `agents/host.py` | MDT 汇总 + 争议驱动讨论 |
| MVME 场景 | `hospital/consultation.py` | 多轮对话 + 断点续跑 |
| 协作场景 | `hospital/collaborative_consultation.py` | Parallel_with_Critique |
| 评估 | `evaluate/eval.py` | LLM-as-Judge 五维评分 |
| ICD-10 | `evaluate/eval_db.py` | Set F1 模糊匹配 |
| 数据 | `data/patients.json` | 结构化病历 + profile 分离 |

---

## 对本项目的借鉴价值

| 可借鉴 | 需规避 |
|--------|--------|
| Agent 角色划分（导诊/医生/护士/质控） | 直接用于生产诊断 |
| 结构化诊断输出格式（#字段#） | Prompt/正则解析方案 |
| Parallel_with_Critique 协作机制 | 不完整的依赖声明 |
| MVME 多维度评测思路 | 无 API/Web 的 CLI 架构 |
| Registry 可插拔后端 | GPT-4 Judge evaluator bias |
