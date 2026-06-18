# OpenMRS Core 项目深度分析

> 分析文档：`devcase/AnalogyRslt/openmrs-core-master.md`（上传 git）  
> 源码路径（本地）：`devcase/openmrs-core-master/`（不上传 git）
> 项目：[openmrs/openmrs-core](https://github.com/openmrs/openmrs-core)  
> 版本：3.0.0-SNAPSHOT | Java 21 | MPL 2.0  
> 定位：**患者中心化开源 EMR 平台内核**（非完整 HIS 产品）

---

## 1. 系统架构

### 1.1 Maven 多模块分层

| 模块 | 职责 |
|------|------|
| `bom/` | 依赖版本管理（Spring 7、Hibernate 7.3、Hibernate Search 8.3） |
| `api/` | 核心业务 API — 领域模型、Service、DAO、模块框架 |
| `web/` | Filter、Servlet、初始化向导 |
| `webapp/` | 打包 openmrs.war |
| `liquibase/` | Schema 迁移 |
| `test-suite/` | 示例模块（api + omod） |

### 1.2 技术栈（3.0）

- Java 21 + Spring Framework 7.0.8
- Hibernate ORM 7.3.6 + Envers（审计）+ Search 8.3.2
- Infinispan 15.2（二级缓存）
- JobRunr（定时任务）
- MariaDB 10.11
- GraalVM JavaScript 25.0.3
- 部署：单体 WAR → Tomcat/Jetty

### 1.3 运行时架构

```
webapp (openmrs.war)
  ├── Filters: 初始化/CSRF/GZIP/ModuleFilter
  ├── 外部模块 omod (REST/FHIR/UI 等 — 不在 core 仓库)
  ↓
api/ — Context 门面 → ServiceContext → *Service → *DAO → DB
  ↓
MariaDB + Liquibase + Envers + Lucene/ES 索引
```

**核心访问**：`Context.getXxxService()` 静态门面

### 1.4 模块扩展架构（Platform 核心）

- 每模块 = 独立 ClassLoader + `config.xml`
- 可声明：privileges、globalProperties、advicePoints(AOP)、mappingFiles、liquibase.xml
- 分 api（可被依赖）和 omod（Web 入口）
- REST/FHIR/UI 均在**独立模块**中

---

## 2. 设计方案

### 2.1 领域驱动 + Service/DAO 分层

```
Controller/Module → Service (@Authorized) → DAO (Hibernate) → DB
```

### 2.2 统一领域对象模型

| 基类 | 含义 | 生命周期 |
|------|------|----------|
| `OpenmrsObject` | 所有实体根 | UUID |
| `OpenmrsMetadata` | 配置/字典 | Retire（停用） |
| `OpenmrsData` | 临床/业务数据 | Void（作废，保留审计） |
| `BaseCustomizableData` | 可扩展属性 | Attribute 机制 |

**设计哲学**：
- 临床数据不可物理删除，Void + 新版本 Obs 修正
- Concept 字典作为术语 backbone

### 2.3 临床数据模型

```
Patient (extends Person)
  └── Visit (就诊时间段)
        └── Encounter (一次医患交互)
              ├── Obs (观察/检验/问卷)
              ├── Order (DrugOrder/TestOrder/ReferralOrder)
              ├── Diagnosis
              └── Allergy
  └── Condition (长期健康问题)
  └── PatientProgram → PatientState (项目/工作流)
```

### 2.4 可扩展性设计

1. **CustomDatatype / AttributeType** — 扩展字段无需改表
2. **@Handler** — Validator/SaveHandler/VoidHandler 拦截生命周期
3. **AdvicePoint** — 模块 AOP 织入核心 Service
4. **GlobalProperty** — 运行时配置
5. **DomainService (3.0)** — UUID 统一查询

### 2.5 横切关注点

| 能力 | 实现 |
|------|------|
| 权限 | `@Authorized` + Role/Privilege |
| 审计 | Hibernate Envers |
| 搜索 | Hibernate Search（Soundex/全文） |
| 调度 | JobRunr |
| 存储 | StorageService (Local/S3) |
| 互操作 | HL7Service (HL7 v2.5) |

---

## 3. 功能模块详解

### 3.1 患者服务（PatientService）

- **路径/入口**: `api/src/main/java/org/openmrs/api/PatientService.java` → `PatientServiceImpl.java` → `HibernatePatientDAO.java`
- **核心数据表/模型**: `Patient`（extends Person）、`PatientIdentifier`、`PatientIdentifierType`、`Allergy`、`Allergen`
- **功能点清单**:
  1. `savePatient` / `getPatient` / `getPatientByUuid` — CRUD
  2. `voidPatient` / `unvoidPatient` / `purgePatient` — 生命周期
  3. `getPatients` — 多签名搜索（姓名/标识符/分页）
  4. `getPatientByExample` / `getDuplicatePatientsByAttributes` — 去重
  5. `savePatientIdentifier` / `checkPatientIdentifiers` — 标识符管理
  6. `getAllPatientIdentifierTypes` / retire/unretire — 标识符类型
  7. `getIdentifierValidator` / `getDefaultIdentifierValidator` — Luhn/Verhoeff 校验
  8. `mergePatients` — 患者合并
  9. `processDeath` / `saveCauseOfDeathObs` — 死亡处理
  10. `saveAllergy` / `voidAllergy` / `removeAllergy` — 过敏管理
- **与其他模块的关联**: → PersonService（人口学）、ObsService（死亡 Obs）、EncounterService、VisitService

### 3.2 人员服务（PersonService）

- **路径/入口**: `PersonService.java` → `PersonServiceImpl.java` → `HibernatePersonDAO.java`
- **核心数据表/模型**: `Person`、`PersonName`、`PersonAddress`、`PersonAttribute`、`PersonAttributeType`、`Relationship`、`RelationshipType`
- **功能点清单**:
  1. `savePerson` / `getPerson` / `voidPerson` — 人员 CRUD
  2. `savePersonName` / `voidPersonName` / `parsePersonName` — 姓名
  3. `savePersonAddress` — 地址
  4. `saveRelationship` / `getRelationshipsByPerson` — 关系
  5. `saveRelationshipType` / `getRelationshipMap` — 关系类型
  6. `savePersonAttributeType` / `getAllPersonAttributeTypes` — 扩展属性
  7. `getPeople` / `getSimilarPeople` — 搜索
  8. `savePersonMergeLog` — 合并日志
- **与其他模块的关联**: Patient extends Person；Provider extends Person

### 3.3 概念字典（ConceptService）

- **路径/入口**: `ConceptService.java` → `ConceptServiceImpl.java` → `HibernateConceptDAO.java`
- **核心数据表/模型**: `Concept`、`ConceptName`、`ConceptDescription`、`ConceptAnswer`、`ConceptSet`、`ConceptClass`、`ConceptDatatype`、`Drug`、`ConceptReferenceTerm`、`ConceptMap`
- **功能点清单**:
  1. `saveConcept` / `getConcept` / `getConceptByUuid` — 概念 CRUD
  2. `getConcepts(ConceptSearchCriteria)` — 搜索
  3. `retireConcept` / `purgeConcept` — 停用/删除
  4. `saveDrug` / `getDrug` / `getDrugsByConcept` — 药品
  5. `ConceptClass` / `ConceptDatatype` / `ConceptSource` — 元数据
  6. `ConceptReferenceTerm` / `ConceptMapType` — LOINC/SNOMED 映射
  7. `ConceptAnswer` / `ConceptSet` — 答案/集合
  8. `ConceptReferenceRange` — 参考范围
  9. `ConceptProposal` — 概念提案
- **与其他模块的关联**: 所有 Obs/Order/Diagnosis 的术语 backbone

### 3.4 观察值（ObsService）

- **路径/入口**: `ObsService.java` → `ObsServiceImpl.java` → `HibernateObsDAO.java`
- **核心数据表/模型**: `Obs`（核心宽表）、`ObsReferenceRange`；Complex Obs Handler（Text/Image/Binary/Media）
- **功能点清单**:
  1. `saveObs` / `getObs` / `getObsByUuid` — CRUD
  2. `voidObs` / `unvoidObs` / `purgeObs` — 作废/修正
  3. `getRevisionObs` — 获取修订版本
  4. `getObservations` — 多条件查询
  5. `getObservationCount` / `getObservationsByPersonAndConcept`
  6. `getComplexObs` — 复杂观察值（影像/文档）
  7. `registerHandler` / `getHandlers` — ComplexObsHandler 注册
  8. ObsGroup — 嵌套观察组
- **与其他模块的关联**: → Encounter（encounter_id）、Concept（concept_id）、Form（form_namespace_and_path）

### 3.5 就诊（EncounterService）

- **路径/入口**: `EncounterService.java` → `EncounterServiceImpl.java`
- **核心数据表/模型**: `Encounter`、`EncounterType`、`EncounterRole`、`EncounterProvider`
- **功能点清单**:
  1. `saveEncounter` / `getEncounter` / `getEncounterByUuid` — CRUD
  2. `voidEncounter` / `unvoidEncounter` / `purgeEncounter`
  3. `getEncounters` / `getEncountersByPatient` — 查询
  4. `getEncountersNotAssignedToAnyVisit` — 未分配 Visit
  5. `saveEncounterType` / `saveEncounterRole` — 类型/角色
  6. `canViewEncounter` / `canEditEncounter` — 权限
  7. `filterEncountersByViewPermissions` — 数据级过滤
  8. `transferEncounter` / `getEncounterVisitHandlers` — Visit 关联
- **与其他模块的关联**: → Visit、Obs、Order、Diagnosis、Form

### 3.6 访问（VisitService）

- **路径/入口**: `VisitService.java` → `VisitServiceImpl.java`
- **核心数据表/模型**: `Visit`、`VisitType`、`VisitAttribute`、`VisitAttributeType`
- **功能点清单**:
  1. `saveVisit` / `getVisit` / `endVisit` — CRUD
  2. `voidVisit` / `unvoidVisit` / `purgeVisit`
  3. `getVisits(VisitSearchCriteria)` / `getVisitsByPatient`
  4. `getActiveVisitsByPatient` — 活跃访问
  5. `saveVisitType` / retire/unretire — 类型管理
  6. `stopVisits` — 批量结束
- **与其他模块的关联**: 一个 Visit 包含多个 Encounter；VisitAttribute 扩展字段

### 3.7 医嘱（OrderService）

- **路径/入口**: `OrderService.java` → `OrderServiceImpl.java`
- **核心数据表/模型**: `Order`（抽象）→ `DrugOrder`、`TestOrder`、`ReferralOrder`；`OrderGroup`、`OrderType`、`OrderFrequency`、`CareSetting`、`OrderSet`、`OrderSetMember`
- **功能点清单**:
  1. `saveOrder` / `saveRetrospectiveOrder` — 新建/回溯
  2. `voidOrder` / `unvoidOrder` / `purgeOrder`
  3. `getOrders(OrderSearchCriteria)` / `getActiveOrders`
  4. `getRevisionOrder` / `getDiscontinuationOrder` — 修订/停止链
  5. `discontinueOrder` — 停止医嘱
  6. `getOrderType` / `saveOrderType` / `getOrderFrequency` / `getCareSetting`
  7. `updateOrderFulfillerStatus` — 履行状态
  8. `getNextOrderNumberSeedSequenceValue` — 医嘱号
  9. Order 状态：NEW → REVISE → DISCONTINUE
- **与其他模块的关联**: → Encounter、Concept/Drug、MedicationDispenseService

### 3.8 诊断与状况（DiagnosisService / ConditionService）

- **路径/入口**: `DiagnosisService.java`、`ConditionService.java`
- **核心数据表/模型**: `Diagnosis`（+ Attribute）、`Condition`（+ ClinicalStatus/VerificationStatus 枚举）
- **功能点清单**:
  1. Diagnosis: `save` / `voidDiagnosis` / `getDiagnoses(Patient/Encounter)`
  2. Diagnosis: `DiagnosisAttributeType` CRUD
  3. Condition: `saveCondition` / `voidCondition` / `endCondition`
  4. Condition: `getActiveConditions` / `getAllConditions` / `getConditionsByEncounter`
  5. FHIR Condition 映射支持
- **与其他模块的关联**: → Encounter、Concept、Patient

### 3.9 药品发放（MedicationDispenseService）

- **路径/入口**: `MedicationDispenseService.java`（since 2.6）
- **核心数据表/模型**: `MedicationDispense`
- **功能点清单**:
  1. `saveMedicationDispense` / `getMedicationDispense`
  2. `getMedicationDispenseByCriteria` — 条件查询
  3. `voidMedicationDispense` / `unvoidMedicationDispense` / `purgeMedicationDispense`
- **与其他模块的关联**: → DrugOrder 履行

### 3.10 程序与工作流（ProgramWorkflowService）

- **路径/入口**: `ProgramWorkflowService.java`
- **核心数据表/模型**: `Program`、`ProgramWorkflow`、`ProgramWorkflowState`、`PatientProgram`、`PatientState`、`ConceptStateConversion`
- **功能点清单**:
  1. `saveProgram` / `getProgram` / `retireProgram` — 项目管理
  2. `savePatientProgram` / `voidPatientProgram` / `getPatientPrograms`
  3. `getWorkflow` / `getState` — 工作流/状态
  4. `ConceptStateConversion` — 状态转换规则
  5. `ProgramAttributeType` / `PatientProgramAttribute` — 扩展属性
- **与其他模块的关联**: 慢病/康复/HIV/TB 等项目管理；可映射康养路径

### 3.11 队列（CohortService）

- **路径/入口**: `CohortService.java`
- **核心数据表/模型**: `Cohort`、`CohortMembership`
- **功能点清单**:
  1. `saveCohort` / `voidCohort` / `getCohort`
  2. `getCohortsContainingPatient` — 患者所属队列
  3. `addPatientToCohort` / `removePatientFromCohort`
- **与其他模块的关联**: 人群筛选 → 报表/干预/AI 批处理

### 3.12 表单（FormService）

- **路径/入口**: `FormService.java`
- **核心数据表/模型**: `Form`、`FormField`、`Field`、`FieldType`、`FieldAnswer`、`FormResource`（JSON Schema）
- **功能点清单**:
  1. `saveForm` / `getForm` / `duplicateForm` / `retireForm`
  2. `saveField` / `saveFormField` / `getFields`
  3. `mergeDuplicateFields`
  4. `FormResource` CRUD — JSON Schema 资源
  5. `checkIfFormsAreLocked`
- **与其他模块的关联**: → Obs（form_namespace_and_path 关联）

### 3.13 位置与提供者（LocationService / ProviderService）

- **路径/入口**: `LocationService.java`、`ProviderService.java`
- **核心数据表/模型**: `Location`（+ Tag/Attribute）、`Provider`（+ Attribute/Role）
- **功能点清单**:
  1. Location: CRUD + 标签 + 地址模板 + 层级（`getRootLocations`）
  2. Provider: CRUD + `ProviderAttributeType` + `ProviderRole`
  3. EncounterProvider 关联
- **与其他模块的关联**: → Encounter、Visit、Order

### 3.14 用户与安全（UserService）

- **路径/入口**: `UserService.java`
- **核心数据表/模型**: `User`、`Role`、`Privilege`
- **功能点清单**:
  1. `createUser` / `saveUser` / `getUser` / `getUserByUsername`
  2. `changePassword`（多签名）/ `changePasswordUsingSecretAnswer`
  3. `saveRole` / `savePrivilege` / `getRole` / `getPrivilege`
  4. `setUserProperty` / `saveUserProperties`
  5. `retireUser` / `purgeUser`
- **与其他模块的关联**: `@Authorized` 注解驱动所有 Service 权限

### 3.15 系统管理（AdministrationService）

- **路径/入口**: `AdministrationService.java`
- **核心数据表/模型**: `GlobalProperty`、`ImplementationId`
- **功能点清单**:
  1. `getGlobalProperty` / `saveGlobalProperty` / `getAllGlobalProperties`
  2. `getSystemVariables` / `getSystemInformation`
  3. `getImplementationId` / `getAllowedLocales`
  4. `executeSQL` — 管理员工具
  5. `validate(Object, Errors)` — 触发 Validator
  6. 模块管理（通过 ModuleFactory）
- **与其他模块的关联**: 全局配置驱动所有模块行为

### 3.16 HL7 集成（HL7Service）

- **路径/入口**: `HL7Service.java`；Handler：`org/openmrs/hl7/handler/`（ORUR01Handler、ADTA28Handler）
- **核心数据表/模型**: `HL7InQueue`、`HL7InArchive`、`HL7InError`
- **功能点清单**:
  1. `parseHL7String` / `processHL7Message`
  2. `processHL7InQueue` — 队列批处理
  3. `resolvePatientId` / `resolvePersonId` / `resolveLocationId`
  4. HL7InQueue/Archive/Error CRUD
- **与其他模块的关联**: → Patient/Person/Location 解析

### 3.17 调度（SchedulerService）

- **路径/入口**: `SchedulerService.java` → `JobRunrSchedulerService`
- **核心数据表/模型**: JobRunr 表（含 `shedlock`）
- **功能点清单**:
  1. `schedule(TaskData)` / `scheduleRecurrently`
  2. `getTasks` / `deleteTask`
  3. 旧 API（@Deprecated）：`scheduleTask(TaskDefinition)`
- **与其他模块的关联**: 定时提醒/批处理/数据同步

### 3.18 存储（StorageService）

- **路径/入口**: `StorageService.java` → `LocalStorageService` / `S3StorageService`
- **功能点清单**:
  1. `getData` / `saveData` / `purgeData`
  2. `getMetadata` / `getKeys`
- **与其他模块的关联**: Complex Obs 大对象存储

### 3.19 规则引擎（LogicService）

- **路径/入口**: `LogicService.java`（**core 无实现，需 Logic 模块**）
- **功能点清单**:
  1. `addRule` / `getRule` / `eval`（单患者/队列/Cohort）
  2. `getLogicDataSources` / `parse`
- **与其他模块的关联**: 临床决策支持（需额外模块）

### 3.20 UUID 统一查询（DomainService — 3.0 新增）

- **路径/入口**: `DomainService.java`
- **功能点清单**:
  1. `fetchByUuid(Class<T>, String uuid)` — 泛型 UUID 查询
  2. `getDomainTypes()` — 已注册域类型
- **与其他模块的关联**: REST/FHIR 模块的 UUID 入口

### 3.21 Handler/Validator 框架

- **路径/入口**: `org/openmrs/api/handler/`（Save/Void/Unvoid/Retire Handler）；`org/openmrs/validator/`（55+ Validator）
- **功能点清单**:
  1. `@Handler(supports=..., order=...)` 注解注册
  2. `RequiredDataAdvice` AOP 拦截 save/void/retire
  3. `PatientSaveHandler` — 标识符反向关联
  4. `PatientValidator extends PersonValidator` — 校验链
  5. `EncounterVisitHandler` — 自动分配 Visit
  6. `ComplexObsHandler` — Text/Image/Binary/Media
  7. `IdentifierValidator` — Luhn/Verhoeff
- **与其他模块的关联**: 所有 Service 写操作的生命周期拦截

### 3.22 模块框架（Module Framework）

- **路径/入口**: `org/openmrs/module/ModuleFactory.java`；示例：`test-suite/module/`
- **config.xml 元素**: id/name/version/activator/privileges/globalProperties/advice/mappingFiles/messages/conditionalResources
- **功能点清单**:
  1. `.omod` 文件加载 → 独立 ClassLoader
  2. `ModuleActivator.started()` / `stopped()` 生命周期
  3. Spring 上下文合并（moduleApplicationContext.xml）
  4. 模块 liquibase.xml 数据库迁移
  5. ModuleFilter/ModuleServlet/ModuleResourcesServlet Web 集成
  6. AdvicePoint AOP 织入
- **与其他模块的关联**: 所有扩展功能通过模块实现

### 3.23 Web 层（Core 内）

- **路径/入口**: `webapp/src/main/webapp/WEB-INF/web.xml`
- **Filter 链**: charsetFilter → StartupError → Initialization → Update → multipart → HibernateFilter → OpenmrsFilter → CSRFGuard → ModuleFilter → GZIP
- **Servlet**: DispatcherServlet（`*.htm/form/list/portlet/field`、`/ws/*`）、ModuleServlet（`/moduleServlet/*`）
- **功能点清单**:
  1. InitializationFilter — 首次安装向导
  2. UpdateFilter — 版本升级
  3. OpenmrsFilter — UserContext 线程绑定
  4. `/health/alive` — 健康检查
- **与其他模块的关联**: 临床 UI/REST/FHIR 均不在 core，需外部 omod

### 3.24 Liquibase Schema

- **路径/入口**: `api/src/main/resources/org/openmrs/liquibase/`
- **三组 Changelog**: schema-only（~7000 行 createTable）、core-data（初始字典）、update-to-latest（增量）
- **版本矩阵**: 1.9.x → 3.0.x snapshots + updates
- **功能点清单**:
  1. `ChangeLogDetective` — 检测 DB 版本
  2. `DatabaseUpdater` — 执行迁移
  3. 模块独立 liquibase.xml
  4. PostgreSQL 扩展（fuzzystrmatch/uuid-ossp）
  5. Envers 审计表自动生成

---

## 4. 系统优点

1. **模块化 Platform** — 20 年社区验证，全球数千部署，最适合 HIS 扩展
2. **临床领域模型成熟** — Patient→Visit→Encounter→Obs/Order 链路清晰
3. **Concept 字典灵活** — 可配置任意临床/康养指标，无需改表
4. **Void/Retire + Envers** — 满足医疗合规审计
5. **扩展点丰富** — Handler/Validator/AOP/Attribute/Module 利于 AI/康养低侵入扩展
6. **技术现代化（3.0）** — Spring 7 + Hibernate 7 + Java 21 + JobRunr + S3

---

## 5. 系统不足

1. **Core 只是 Platform** — 无 UI/API/FHIR，直接部署仅见初始化向导
2. **WAR 单体** — 非 Spring Boot 原生，水平扩展需整体复制
3. **Context 静态门面** — 不利于单元测试
4. **三层 ORM 栈** — Hibernate + Envers + Search 运维复杂
5. **Obs 表性能瓶颈** — 典型宽表设计
6. **无中国 HIS 标准** — 无医保/DRG/计费/中医/康养领域模型
7. **LogicService 无 core 实现** — 规则引擎需额外模块
8. **RBAC 无 ABAC** — 无数据级权限/国密/等保

---

## 6. 开发参考索引

| 模块 | 关键文件 | 可复用模式 |
|------|----------|------------|
| 患者 | `api/.../PatientService.java`、`Patient.java` | Person 继承 + Identifier 体系 |
| 观察值 | `ObsService.java`、`Obs.java` | Concept 驱动的灵活指标 |
| 就诊 | `EncounterService.java`、`VisitService.java` | Visit→Encounter 双层 |
| 医嘱 | `OrderService.java`、`DrugOrder.java` | NEW/REVISE/DISCONTINUE 生命周期 |
| 概念字典 | `ConceptService.java`、`Concept.java` | 术语 backbone + ReferenceTerm 映射 |
| 程序 | `ProgramWorkflowService.java` | 状态机式慢病/康复路径 |
| 队列 | `CohortService.java` | 人群筛选 |
| 扩展 | `@Handler`、`BaseCustomizableData` | Attribute 扩展 + AOP Handler |
| 模块 | `ModuleFactory.java`、`config.xml` | omod 热插拔 |
| 审计 | Envers、`Voidable`/`Retireable` | 软删除 + 历史版本 |
| 调度 | `JobRunrSchedulerService` | 定时任务 |
| 存储 | `StorageService` | Local/S3 大对象 |
| UUID | `DomainService.java` | 3.0 统一 UUID 查询 |

---

## 面向 HIS + 康养 + AI 的扩展建议

```
AI 层（自研）— 推理/风险评分/NLP/知识图谱
康养层（自研）— 健康档案/评估量表/IoT/随访
HIS 层（OpenMRS 模块）— webservices.rest + fhir2 + referenceapplication
Platform 层（openmrs-core）— Patient/Obs/Encounter/Order/Concept/Program
```

**可复用 Core 能力**：Patient+Visit 统一主索引、Concept+Obs 健康指标库、ProgramWorkflow 康复路径、Cohort 高危分组、JobRunr AI 批处理、Handler 拦截临床事件触发 AI

**需自研**：REST/FHIR 网关、现代前端、康养领域模型、AI 推理服务、中国区合规
