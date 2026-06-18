# HospitalRun Frontend 项目深度分析

> 分析文档：`devcase/AnalogyRslt/hospitalrun-frontend-master.md`（上传 git）  
> 源码路径（本地）：`devcase/hospitalrun-frontend-master/`（不上传 git）
> 项目：[HospitalRun/hospitalrun-frontend](https://github.com/HospitalRun/hospitalrun-frontend)  
> 版本：2.0.0-alpha.7（2020，已停滞）  
> 定位：**Offline First 发展中国家医院 HIS 前端**

---

## 1. 系统架构

### 1.1 Offline First 架构

```
React 17 SPA
  ├── Redux Toolkit（UI/用户/面包屑）
  ├── React Query v2（异步数据）
  └── Service Worker（PWA 静态缓存）
        ↓
Repository 抽象层（6 个 Repository）
        ↓
relationalDb (PouchDB local_hospitalrun, IndexedDB)
        ↓ live sync, retry
PouchDB → CouchDB HTTP (hospitalrun 库)
```

### 1.2 技术栈

| 层级 | 技术 |
|------|------|
| UI | React 17, TypeScript 3.8, Bootstrap 5, @hospitalrun/components |
| 路由 | react-router-dom v5 |
| 本地 DB | PouchDB 7.2 + relational-pouch + pouchdb-find |
| 远程 | CouchDB（HTTP 直连，非 REST API） |
| i18n | 12 语言（含 zhCN，部分不完整） |

### 1.3 数据模型（relational-pouch）

| 实体 | 存储 | 关系 |
|------|------|------|
| patient | 独立文档 + **内嵌数组** | hasMany → appointment/lab/medication/imaging |
| appointment/lab/medication/imaging | 独立文档 | belongsTo → patient |
| incident | 独立文档 | 无关联 |

内嵌在 Patient 中：allergies、diagnoses、notes、carePlans、careGoals、visits、relatedPersons

---

## 2. 设计方案

### 2.1 分层

```
View → Hooks (React Query / Redux) → Repository → relationalDb → sync → CouchDB
```

### 2.2 权限设计

- `Permissions` 枚举 31 项细粒度权限
- `PrivateRoute` 按权限守卫
- **缺陷**：权限在 initialState 硬编码为全部，未从 CouchDB roles 映射

### 2.3 同步机制

- `localDb.sync(serverDb, { live: true, retry: true })`
- 全部读写走 localDb，后台推送到 CouchDB
- **无冲突解决 UI**，无 sync 进度反馈

---

## 3. 功能模块详解

### 3.1 Dashboard 模块

- **路径/入口**: `src/dashboard/Dashboard.tsx`；路由 `/`
- **核心数据表/模型**: 无独立实体
- **功能点清单**:
  1. 占位页面（无实际统计/Widget）
  2. Navbar + Sidebar 导航入口
- **与其他模块的关联**: 导航 hub

### 3.2 患者模块（Patients — 核心最大模块）

- **路径/入口**:
  - 路由：`/patients/*`（`src/patients/Patients.tsx`）
  - 子路由：search/new/edit/:id/:id/*（ViewPatient 内嵌 Tab）
  - Repository：`src/shared/db/PatientRepository.ts`
  - Hooks：27 个（`src/patients/hooks/`）
  - Redux：`patients-slice.ts`、`patient-slice.ts`
- **核心数据表/模型**:
  - Patient 主实体：sex, dateOfBirth, isApproximateDateOfBirth, preferredLanguage, occupation, type, code(P前缀), index, bloodType, phoneNumbers[], emails[], addresses[]
  - 内嵌：relatedPersons[], allergies[], diagnoses[], notes[], carePlans[], careGoals[], visits[]
- **功能点清单**:

  **3.2.1 患者 CRUD**
  1. `/patients` — 搜索列表（需 `read:patients`）
  2. `/patients/new` — 新建（需 `write:patients`）：姓名/性别/DOB/血型/职业/联系方式
  3. `/patients/edit/:id` — 编辑
  4. 自动生成 P 前缀编号（PatientRepository.save）
  5. 重复检测：按姓名+生日（search）

  **3.2.2 基本信息 Tab**（`/patients/:id` — GeneralInformation）
  6. 姓名（fullName/family/given）、性别、DOB、血型、职业、语言、联系方式

  **3.2.3 关联人**（`/patients/:id/relatedpersons`）
  7. 添加/列表/删除 RelatedPerson（id, patientId, type）
  8. Hooks: useAddPatientRelatedPerson, useRemovePatientRelatedPerson, usePatientRelatedPersons

  **3.2.4 过敏**（`/patients/:id/allergies/*`）
  9. 添加/列表/详情 Allergy（id, name）
  10. Hooks: useAddAllergy, usePatientAllergies, useAllergy

  **3.2.5 诊断**（`/patients/:id/diagnoses/*`）
  11. 添加/列表/详情 Diagnosis
  12. 字段：name, diagnosisDate, onsetDate, abatementDate, status(active/recurrence/relapse/inactive/remission/resolved), note, visit
  13. Hooks: useAddPatientDiagnosis, usePatientDiagnoses, useDiagnosis

  **3.2.6 笔记**（`/patients/:id/notes`）
  14. 添加/列表/软删除 Note（id, date, text, deleted）
  15. Hooks: useAddPatientNote, usePatientNotes, usePatientNote

  **3.2.7 护理计划**（`/patients/:id/care-plans/*`）
  16. 添加/列表/详情 CarePlan
  17. 字段：status(draft/active/on hold/revoked/completed/unknown), intent(proposal/plan/order/option), title, description, startDate, endDate, diagnosisId, note
  18. Hooks: useAddCarePlan, usePatientCarePlans, useCarePlan

  **3.2.8 护理目标**（`/patients/:id/care-goals/*`）
  19. 添加/列表/详情 CareGoal
  20. 字段：status(7种), achievementStatus(6种), priority(high/medium/low), description, startDate, dueDate, note
  21. Hooks: useAddCareGoal, usePatientCareGoals, useCareGoal

  **3.2.9 就诊**（`/patients/:id/visits/*`）
  22. 添加/列表/详情 Visit
  23. 字段：startDateTime, endDateTime, type, status(planned/arrived/triaged/in progress/on leave/finished/cancelled), reason, location
  24. Hooks: useAddVisit, usePatientVisits, useVisit

  **3.2.10 历史 Tab**（`/patients/:id/history`）
  25. 聚合化验+预约为 PatientHistoryRecord（type: Appointment/Lab, date, info, recordId）

  **3.2.11 患者内嵌子模块 Tab**
  26. `/patients/:id/appointments` — 患者预约列表
  27. `/patients/:id/medications` — 患者用药列表
  28. `/patients/:id/labs` — 患者化验列表

- **与其他模块的关联**: 所有临床子实体的中心；hasMany → appointment/lab/medication/imaging

### 3.3 预约模块（Appointments）

- **路径/入口**:
  - 路由：`/appointments/*`（`src/scheduling/appointments/Appointments.tsx`）
  - Repository：`AppointmentRepository.ts`
  - Hooks：5 个（`src/scheduling/hooks/`）
- **核心数据表/模型**: Appointment — startDateTime, endDateTime, patient(patientId), location, reason, type
- **功能点清单**:
  1. `/appointments` — 列表（需 `read:appointments`）
  2. `/appointments/new` — 创建（需 `write:appointments`）
  3. `/appointments/edit/:id` — 编辑
  4. `/appointments/:id` — 详情
  5. 删除（需 `delete:appointment`）
  6. 字段：startDateTime, endDateTime, patient, location, reason, type
  7. Hooks: useAppointments, useAppointment, useScheduleAppointment, useUpdateAppointment, useDeleteAppointment
- **与其他模块的关联**: belongsTo → patient

### 3.4 化验模块（Labs）

- **路径/入口**:
  - 路由：`/labs/*`（`src/labs/Labs.tsx`）
  - Repository：`LabRepository.ts`
  - Hooks：6 个
- **核心数据表/模型**: Lab — code(L前缀), patient, type, requestedBy, notes[], result, status, requestedOn, completedOn, canceledOn, visitId
- **功能点清单**:
  1. `/labs` — 搜索列表（需 `read:labs`）
  2. `/labs/new` — 创建（需 `write:labs`）
  3. `/labs/:id` — 详情（需 `read:lab`）
  4. 录入 result、notes
  5. 完成（需 `complete:lab`）/ 取消（需 `cancel:lab`）
  6. 状态流转：requested → completed | canceled
  7. Hooks: useLab, useLabsSearch, useRequestLab, useUpdateLab, useCompleteLab, useCancelLab
- **与其他模块的关联**: belongsTo → patient；PatientRepository.getLabs(patientId)

### 3.5 用药模块（Medications）

- **路径/入口**:
  - 路由：`/medications/*`（`src/medications/Medications.tsx`）
  - Repository：`MedicationRepository.ts`
  - Redux：`medication-slice.ts`（详情页用 Redux，列表用 React Query — 架构不一致）
  - Hooks：1 个 search + slice actions
- **核心数据表/模型**: Medication — medication, status(8种), intent(8种), priority, patient, requestedBy, requestedOn, completedOn, canceledOn, notes, quantity{value,unit}
- **功能点清单**:
  1. `/medications` — 搜索列表
  2. `/medications/new` — 创建（需 `write:medications`）
  3. `/medications/:id` — 详情
  4. 编辑 status/intent/priority/quantity
  5. 完成（`complete:medication`）/ 取消（`cancel:medication`）
  6. MedicationStatus 枚举：8 种 FHIR 风格状态
  7. Hooks: useMedicationSearch
- **与其他模块的关联**: belongsTo → patient

### 3.6 影像模块（Imagings）

- **路径/入口**:
  - 路由：`/imaging/*`（`src/imagings/Imagings.tsx`）
  - Repository：`ImagingRepository.ts`
  - Hooks：3 个
- **核心数据表/模型**: Imaging — code(I前缀), patient, type, status 等
- **功能点清单**:
  1. `/imaging` — 搜索列表（需 `read:imagings`）
  2. `/imaging/new` — 创建（需 `write:imaging`）
  3. **缺少** `/imaging/:id` 详情页
  4. **缺少** 完成/取消流程
  5. Hooks: useImagingSearch, useImagingRequest, useRequestImaging
- **与其他模块的关联**: belongsTo → patient（功能不完整）

### 3.7 事件模块（Incidents）

- **路径/入口**:
  - 路由：`/incidents/*`（`src/incidents/Incidents.tsx`）
  - Repository：`IncidentRepository.ts`
  - Hooks：4 个
- **核心数据表/模型**: Incident — 独立文档，无 patient 关联
- **功能点清单**:
  1. `/incidents` — 列表（需 `read:incidents`）
  2. `/incidents/new` — 报告事件（需 `write:incident`）
  3. `/incidents/:id` — 详情（需 `read:incident`）
  4. 解决事件（需 `resolve:incident`）
  5. `/incidents/visualize` — 月度折线图可视化（需 `read:incident_widgets`）
  6. 按状态筛选
  7. **CSV 导出**
  8. Hooks: useIncidents, useIncident, useReportIncident, useResolveIncident
- **与其他模块的关联**: 独立模块，无 patient 关联

### 3.8 设置模块（Settings）

- **路径/入口**: `src/settings/Settings.tsx`；路由 `/settings`
- **功能点清单**:
  1. 语言切换（12 语言，含 zhCN）
  2. 无其他配置项

### 3.9 认证模块（Auth）

- **路径/入口**: `src/user/user-slice.ts`；CouchDB session
- **功能点清单**:
  1. CouchDB session 恢复（pouchdb-authentication）
  2. **alpha.6 移除登录页**
  3. logout 仍跳转 `/login`（**死链**）
  4. 权限在 initialState 硬编码为全部 31 项
- **与其他模块的关联**: PrivateRoute 守卫所有模块

### 3.10 Repository 层（共享基础设施）

- **路径/入口**: `src/shared/db/`
- **功能点清单**:

  **基类 Repository<T>**
  1. find(id) / findAll(sort?) / count()
  2. search(criteria) / save(entity) / saveOrUpdate(entity) / delete(entity)
  3. 自动生成 UUID、createdAt/updatedAt

  **6 个具体 Repository**
  | Repository | 特有方法 | Code 前缀 |
  |-----------|---------|----------|
  | PatientRepository | search(text), getAppointments/Labs/Medications | P |
  | AppointmentRepository | searchPatientAppointments | — |
  | LabRepository | search, findAllByPatientId | L |
  | MedicationRepository | search, findAllByPatientId | — |
  | ImagingRepository | search | I |
  | IncidentRepository | search({ status }) | — |

- **与其他模块的关联**: 所有模块的数据访问层

### 3.11 权限体系（Permissions）

- **路径/入口**: `src/shared/model/Permissions.ts`
- **31 项权限枚举**:

  | 类别 | 权限 |
  |------|------|
  | 患者 | read:patients, write:patients |
  | 预约 | read:appointments, write:appointments, delete:appointment |
  | 过敏 | write:allergy |
  | 诊断 | write:diagnosis |
  | 化验 | write:labs, cancel:lab, complete:lab, read:lab, read:labs |
  | 事件 | read:incidents, read:incident, write:incident, resolve:incident, read:incident_widgets |
  | 护理计划 | write:care_plan, read:care_plan |
  | 护理目标 | write:care_goal, read:care_goal |
  | 用药 | write:medications, cancel:medication, complete:medication, read:medication, read:medications |
  | 就诊 | write:visit, read:visit |
  | 影像 | write:imaging, read:imagings |

---

## 4. 系统优点

1. **真正的 Offline First** — 适合网络不稳定的发展中国家/偏远场景
2. **CouchDB/PouchDB 成熟同步栈** — 无需自建 REST API
3. **relational-pouch 关系建模** — NoSQL 上的 belongsTo/hasMany
4. **Repository 抽象清晰** — 统一 CRUD 接口
5. **患者模块功能丰富** — 7 类内嵌子实体，部分借鉴 FHIR 枚举
6. **测试覆盖较好** — 大量 `__tests__/`（536 文件中约半数为测试）
7. **12 语言 i18n**（含中文）

---

## 5. 系统不足

### 5.1 项目成熟度

- alpha.7，2020 年后停滞
- Dashboard 占位；无计费/药房/库存/报表

### 5.2 认证与权限

- 登录页移除但 logout 死链
- 权限全部硬编码，RBAC 形同虚设

### 5.3 Offline Sync 局限

- 无冲突解决 UI
- 无 sync 进度/错误反馈
- 全量 sync，大库性能差
- Patient 内嵌数组膨胀影响 sync

### 5.4 技术债务

- react-scripts 3.4、TypeScript 3.8、React Query v2
- react 17 与 react-dom 16 版本冲突
- Medications 模块 Redux/RQ 混用
- Imagings 模块功能不完整
- zhCN 翻译不完整

---

## 6. 开发参考索引

| 模块 | 关键文件 | 可复用模式 |
|------|----------|------------|
| 离线架构 | `shared/config/pouchdb.ts` | PouchDB live sync |
| Repository | `shared/db/Repository.ts` | 统一 CRUD 抽象 |
| 关系建模 | relational-pouch schema | belongsTo/hasMany on NoSQL |
| 患者 | `patients/`（27 hooks） | 内嵌子实体 + Tab 路由 |
| 权限 | `Permissions.ts`、`PrivateRoute` | 细粒度权限枚举 |
| 事件 | `incidents/visualize/` | CSV 导出 + 折线图 |
| i18n | `shared/locales/` | 12 语言框架 |
| 测试 | `__tests__/` | Repository/Hook 单元测试 |

---

## 对本项目的借鉴价值

| 可借鉴 | 需规避 |
|--------|--------|
| Offline First + CouchDB 同步思路 | 直接用于生产（项目已停滞） |
| Repository + relational-pouch 模式 | 权限硬编码 |
| 患者子域建模（过敏/诊断/护理计划） | Redux/RQ 混用 |
| 事件报告 + CSV 导出 | 无冲突处理的 sync |
| 12 语言 i18n 框架 | 内嵌+独立文档混合存储 |
