# HIS + 康养平台开源项目调研报告

## 文档目的

结合《HIS + 康养服务平台项目说明书》，调研当前全球具有代表性的开源医院信息系统（HIS）、康养服务系统以及 AI 医疗项目，为后续系统设计、架构规划和产品研发提供参考依据。

# 一、开源 HIS 系统调研

## 1. OpenEMR

### 项目地址

https://github.com/openemr/openemr

### Star

约 5200+

### 技术栈

- PHP
- MySQL
- Bootstrap
- REST API
- FHIR

### 核心功能

- 门诊管理
- 住院管理
- EMR电子病历
- 药房管理
- 检验管理
- 医保管理
- 财务收费
- 预约管理

### 优势

OpenEMR 是全球最成熟的开源 HIS/EHR 系统之一。

特别值得研究：

- 患者档案设计
- 医疗业务流程
- 权限体系
- FHIR标准接口
- 数据模型设计

### 参考价值

★★★★★

## 2. OpenMRS

### 项目地址

https://github.com/openmrs/openmrs-core

### 技术栈

- Java
- Spring
- PostgreSQL

### 核心特点

采用高度模块化设计：

- Patient
- Encounter
- Observation
- Order
- Diagnosis
- FHIR

### 优势

适合作为大型平台架构参考：

```
Core
 ├─ HIS
 ├─ 康养
 ├─ AI
 ├─ 上门服务
 └─ 本地生活服务
```

特别适合未来平台化扩展。

### 参考价值

★★★★★

## 3. Hospital Management EMR

### 项目地址

https://github.com/opensource-emr/hospital-management-emr

### 技术栈

- PHP
- MySQL

### 功能

- 患者管理
- 医生管理
- 收费管理
- 药房管理

### 优势

代码量较小。

适合作为：

- 表结构参考
- 权限设计参考
- 快速原型参考

### 参考价值

★★★☆☆

# 二、康养服务系统调研

目前全球开源康养系统相对较少。

更多属于养老机构管理系统。

## 1. ECMS (Elderly Care Management System)

### 项目地址

https://github.com/NabsCodes/ecms

### 技术栈

- React
- NodeJS
- MongoDB

### 功能

- 老人档案
- 护理员管理
- 服务记录
- 家属沟通

### 可借鉴内容

业务实体设计：

```
老人
护理员
家属
护理计划
服务记录
```

### 参考价值

★★★★☆

## 2. LibreHealth EHR

### 项目地址

https://github.com/LibreHealthIO

### 技术栈

- Java
- OpenMRS衍生

### 功能

- 长期护理
- 慢病管理
- 社区医疗
- 健康管理

### 可借鉴内容

未来康养业务场景：

```
医院
养老院
护理机构
居家养老
```

统一管理模式。

### 参考价值

★★★★☆

## 3. HospitalRun

### 项目地址

https://github.com/HospitalRun/hospitalrun-frontend

### 技术栈

- React
- CouchDB

### 特点

Offline First 架构

支持：

```
离线使用
联网同步
```

### 适用场景

- 养老院
- 社区卫生站
- 边远地区医疗

### 参考价值

★★★★☆

# 三、AI + HIS 项目调研

未来系统规划为：

```
AI Native HIS
```

因此重点关注 AI 医疗方向。

## 1. LangCare

### 官网

https://www.langcare.ai

### 定位

FHIR + MCP + AI Agent

### 架构

```
HIS
 ↓
FHIR
 ↓
MCP Server
 ↓
Agent
 ↓
LLM
```

### 功能

连接：

- GPT
- Claude
- Gemini

实现：

- 医疗问答
- 医疗Agent
- 医疗知识库

### 可借鉴内容

未来 AI 中台设计：

```
AI Gateway
FHIR Adapter
MCP Server
Prompt Engine
```

### 参考价值

★★★★★

## 2. AI Hospital

### 项目地址

https://github.com/LibertFan/AI_Hospital

### 论文

https://arxiv.org/abs/2402.09742

### 架构

```
Patient Agent

Doctor Agent

Exam Agent

Chief Doctor Agent
```

### 应用场景

未来可扩展：

```
AI导诊
AI医生
AI护士
AI客服
AI质控
```

### 参考价值

★★★★★

# 四、AI + 康养项目调研

目前属于新兴方向。

## 1. Phlox

### 项目地址

https://github.com/bloodworks-io/phlox

### 功能

- AI病历生成
- AI护理记录
- AI总结报告
- AI任务管理

### 康养价值

护理员录入：

```
体温
血压
饮食
睡眠
护理情况
```

AI自动生成：

```
护理日报
护理周报
护理月报
异常提醒
```

### 参考价值

★★★★★

## 2. Clinfo.ai

### 论文

https://arxiv.org/abs/2310.16146

### 技术路线

```
知识库
 ↓
向量库
 ↓
RAG
 ↓
LLM
```

### 应用方向

建设：

- 康养知识库
- 护理知识库
- 医疗知识库

### 参考价值

★★★★★

# 五、项目最终参考架构

建议采用：

## HIS业务层

参考：

- OpenEMR
- OpenMRS

负责：

```
门诊
住院
药房
检验
收费
电子病历
```

## 康养业务层

参考：

- ECMS
- LibreHealth

负责：

```
机构养老
长期护理
健康管理
家属端
```

## 到家服务平台

参考：

- 美团
- 京东到家

负责：

```
陪诊
陪护
康复
助餐
助洁
护理上门
```

## AI中台

参考：

- LangCare
- AI Hospital
- Phlox

负责：

```
AI导诊
AI医生
AI护士
AI客服
AI分析师
AI运营助手
```

# 六、未来目标架构

```
HIS Core
    │
    ├─ 门诊
    ├─ 住院
    ├─ 药房
    ├─ 检验
    └─ 财务

康养 Core
    │
    ├─ 入院养老
    ├─ 护理管理
    ├─ 健康管理
    └─ 家属端

到家服务平台
    │
    ├─ 护工
    ├─ 陪诊
    ├─ 康复
    ├─ 保洁
    └─ 助餐

AI Platform
    │
    ├─ MCP
    ├─ RAG
    ├─ Agent
    ├─ AI医生
    ├─ AI护士
    ├─ AI客服
    └─ AI运营助手
```

# 七、推荐学习优先级

| 优先级 | 项目        | 用途        |
| ------ | ----------- | ----------- |
| P0     | OpenEMR     | HIS业务模型 |
| P0     | OpenMRS     | 系统架构    |
| P0     | LangCare    | AI中台      |
| P0     | AI Hospital | Agent体系   |
| P1     | ECMS        | 康养业务    |
| P1     | LibreHealth | 长期护理    |
| P1     | Phlox       | AI护理      |
| P2     | HospitalRun | 离线架构    |
| P2     | Clinfo.ai   | RAG知识库   |

# 结论

项目最佳技术路线：

```
OpenMRS架构
      +
OpenEMR业务
      +
ECMS康养模型
      +
美团到家模式
      +
LangCare AI中台
      +
AI Hospital Agent体系
```

最终形成：

“中国版 AI Native HIS + 康养平台 + 到家服务平台”。

该方案兼顾：

- 医疗机构
- 养老机构
- 社区护理
- 居家养老
- 本地生活服务
- AI智能运营

具备长期平台化发展潜力。
