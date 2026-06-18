# OpenEMR 项目深度分析

> 分析文档：`devcase/AnalogyRslt/openemr-master.md`（上传 git）  
> 源码路径（本地）：`devcase/openemr-master/`（不上传 git）
> 项目：[openemr/openemr](https://github.com/openemr/openemr)  
> 版本：8.0.0.3 | PHP ≥8.2 | GPL-3.0  
> 定位：**全球最成熟的开源 HIS/EHR 之一**

---

## 1. 系统架构

### 1.1 混合演进型单体

```
入口层 (index.php → sites/{site}/sqlconf.php)
    ↓
表现层 interface/ — 传统 PHP + SASS/Bootstrap + Twig/Smarty
    ↓
API 层 apis/ + oauth2/ — Symfony HttpKernel + OAuth2/OIDC
    ↓
业务服务层 src/ — OpenEMR\Services\* + RestControllers + FHIR
    ↓
模块层 interface/modules/ — Laminas MVC + Custom Modules
    ↓
遗留核心 library/ — ADODB + 全局 inc.php + FeeSheet/HL7/billing
    ↓
数据层 MySQL/MariaDB — 281 张表 + Doctrine Migrations
```

### 1.2 技术栈

| 层次 | 技术 |
|------|------|
| 语言 | PHP 8.2+ |
| Web | 遗留 PHP + Laminas MVC 3 + Symfony 7 |
| ORM/DB | ADODB + Doctrine DBAL/ORM/Migrations |
| 前端 | Bootstrap + SASS + Webpack + Node.js 24 |
| 认证 | GACL + OAuth2/OIDC + MFA(TOTP/U2F) |
| 互操作 | FHIR R4 + HL7 v2 + X12 EDI + C-CDA + SMART on FHIR v2.2 |

### 1.3 三套 API 端点

| 端点 | 前缀 | 用途 |
|------|------|------|
| Standard REST | `/apis/{site}/api/*` | 患者/就诊/处方等 CRUD |
| FHIR R4 | `/apis/{site}/fhir/*` | 30 Resource US Core 8.0 |
| Patient Portal API | `/apis/{site}/portal/*` | 患者门户专用 |

### 1.4 目录职责

| 目录 | 职责 | 规模 |
|------|------|------|
| `interface/` | 主 UI | 1485+ 文件 |
| `src/` | 现代 PHP 命名空间 | 2108+ 文件 |
| `library/` | 遗留共享库 | FeeSheet/HL7/billing |
| `apis/` | REST/FHIR/Portal 路由分发 | `_rest_routes_*.inc.php` |
| `portal/` | 患者门户 SPA | account/messaging/payment |
| `templates/` | Twig 模板 | 241 文件 |
| `sql/` | database.sql | 281 表 |

---

## 2. 设计方案

### 2.1 核心设计模式

1. **Form Registry** — `registry` 表 + `interface/forms/` 可插拔临床表单，就诊时动态加载
2. **LBF 布局引擎** — `layout_options` + `layout_group_properties` 表驱动 demographics，无需改代码
3. **Service 层抽象** — 80+ `OpenEMR\Services\*` 桥接 REST/FHIR 与遗留 ADODB 代码
4. **JSON 驱动菜单** — `interface/main/tabs/menu/menus/standard.json` + GACL + globals 开关
5. **Fragment Dashboard** — `patient_file/summary/` Ajax Widget 化患者面板
6. **UUID 标准化** — `uuid_registry` + `uuid_mapping` 支持 FHIR 互操作
7. **后台服务调度** — `background_services` 表驱动定时任务（非消息队列）

### 2.2 互操作设计

- FHIR R4：30 Resources，US Core 8.0，Bulk Data Export（`FhirOperationExportRestController`）
- SMART on FHIR v2.2：EHR Launch、Granular Scopes、JWKS（`oauth_clients`）
- C-CDA/CCDA：Carecoordination 模块 + Node.js ccdaservice + `ccda_*` 表
- HL7 v2：LabCorp/Quest 订单（`procedure_order` + `library/hl7_*`）
- X12 EDI：270/271 资格验证、837 索赔、835 ERA（`interface/billing/edi_*`）

### 2.3 安全设计

- GACL 细粒度 ACL（`gacl_*` 17 张表）
- OAuth2 + OIDC + MFA（`login_mfa_registrations`）
- 审计：`log` / `audit_master` / `audit_details` / `extended_log`
- 会话/IP 追踪（`session_tracker` / `ip_tracking`）

---

## 3. 功能模块详解

### 3.1 患者管理（Patient Management）

- **路径/入口**:
  - UI：`interface/new/new.php`（新建）、`interface/main/finder/`（搜索）、`interface/patient_file/summary/demographics.php`（Dashboard）
  - Service：`src/Services/PatientService.php`、`PersonService.php`、`DemographicsRelatedPersonsService.php`
  - REST：`src/RestControllers/PatientRestController.php` → `POST/GET /api/patient`
  - FHIR：`src/RestControllers/FHIR/FhirPatientRestController.php`
- **核心数据表/模型**:
  - `patient_data`（主档案：姓名/DOB/性别/地址/保险/紧急联系人）
  - `person` + `contact` + `contact_telecom` + `contact_relation` + `person_patient_link`（FHIR Person 模型）
  - `history_data`（社会史/家族史/习惯）
  - `employer_data`（雇主信息）
  - `patient_history`（历史变更）
  - `layout_options` + `lbf_data`（LBF 可配置字段）
  - `lists` + `lists_medication`（问题/过敏/用药列表，`type` 区分）
  - `patient_reminders`、`patient_birthday_alert`
- **功能点清单**:
  1. 新建患者：分步表单（demographics LBF 布局字段）
  2. 患者搜索：finder 多条件（姓名/DOB/SSN/电话/External ID）
  3. Dashboard 面板：demographics、issues、prescriptions、labs、documents、billing 等 Ajax fragments
  4. 人口统计学编辑：LBF 引擎渲染 `layout_options` 分组字段
  5. 病史录入：`patient_file/history/history.php`（社会史/SDOH）
  6. 问题列表：medical_problem / allergy / medication 类型 CRUD
  7. 免疫接种：`immunizations` 表 + `ImmunizationService`
  8. 生命体征：`forms/vitals/` 表单
  9. 患者合并：`merge_patients.php` + `manage_dup_patients.php`
  10. 记录披露：`record_disclosure.php`
  11. 修正案：`amendments` + `amendments_history`
  12. 护理团队：`care_teams` + `care_team_member`
  13. 患者追踪器：`patient_tracker/` + `patient_tracker_element`
  14. 标签/信函：`label.php`、`letter.php`、`addr_label.php`
  15. 前台收款入口：`front_payment.php`（关联 billing）
- **与其他模块的关联**:
  - → 就诊（`form_encounter.patient_id`）
  - → 计费（`billing.pid`）
  - → 预约（`openemr_postcalendar_events.pc_pid`）
  - → 门户（`patient_access_onsite`）
  - → FHIR Patient Resource

### 3.2 就诊与临床文档（Encounter & Clinical Forms）

- **路径/入口**:
  - UI：`interface/patient_file/encounter/`（就诊加载/表单列表）
  - 表单目录：`interface/forms/`（35+ 可插拔表单）
  - Service：`EncounterService.php`、`FormService.php`、`ClinicalNotesService.php`、`VitalsService.php`
  - REST：`EncounterRestController.php` → `/api/patient/{puuid}/encounter`
- **核心数据表/模型**:
  - `form_encounter`（就诊主表：日期/类别/提供者/facility/billing 状态）
  - `forms`（表单实例注册：formdir/form_id/encounter/pid）
  - `registry`（可用表单目录与 ACL）
  - 各 `form_*` 表（soap/vitals/ros/clinical_notes 等）
  - `issue_encounter`（问题-就诊关联）
- **功能点清单**:
  1. **newpatient** — 新建就诊，选择类别/提供者/facility
  2. **soap** — Subjective/Objective/Assessment/Plan 四段文档
  3. **vitals** — 血压/脉搏/体温/身高/体重/BMI/O2 Sat（`form_vitals` + `form_vital_details` + 计算表）
  4. **ros** — Review of Systems 系统回顾（`form_ros`）
  5. **reviewofs** — 简版系统回顾
  6. **fee_sheet** — 费用单（关联 billing 编码）
  7. **procedure_order** — 检验/影像医嘱（关联 lab 模块）
  8. **care_plan** — 护理计划（`form_care_plan`）
  9. **clinical_notes** — 临床笔记（`form_clinical_notes` + `clinical_notes_documents`）
  10. **clinical_instructions** — 临床指令
  11. **dictation** — 口述转录
  12. **observation** — FHIR Observation 映射表单
  13. **questionnaire_assessments** — PHQ-9/GAD-7/LForms 量表
  14. **phq9 / gad7 / sdoh** — 独立筛查量表
  15. **track_anything** — 可配置追踪指标
  16. **CAMOS** — 综合笔记模板
  17. **LBF** — Layout-Based Form 自定义表单引擎
  18. **eye_mag** — 眼科完整子系统（10+ form_eye_* 表：HPI/ROS/Vitals/Acuity/Refraction/Biometrics/AntSeg/PostSeg/Neuro/Dispense）
  19. **group_attendance / newGroupEncounter** — 团体治疗
  20. **prior_auth** — 预授权
  21. **transfer_summary / aftercare_plan / treatment_plan** — 转院/后续/治疗计划
  22. **functional_cognitive_status** — 功能/认知状态
  23. **painmap / ankleinjury / bronchitis** — 专科模板
  24. **requisition** — 申请单
  25. 表单管理：`interface/forms_admin/` 注册/排序/ACL
- **与其他模块的关联**:
  - → 计费（fee_sheet → `billing`）
  - → 检验（procedure_order → `procedure_*`）
  - → 药房（prescriptions 在就诊上下文创建）
  - → CQM/CDR（clinical_rules 基于 encounter 数据）
  - → FHIR Encounter/Observation/Condition

### 3.3 调度与日历（Scheduling）

- **路径/入口**:
  - UI：`interface/main/calendar/`（PostCalendar 引擎）
  - Service：`AppointmentService.php`、`HolidayService.php`
  - REST：`AppointmentRestController.php` → `/api/patient/{pid}/appointment`
  - FHIR：`FhirAppointmentRestController.php`
- **核心数据表/模型**:
  - `openemr_postcalendar_events`（预约事件）
  - `openemr_postcalendar_categories`（预约类别/颜色/时长）
  - `calendar_external`（外部日历同步）
  - `enc_category_map`（就诊类别映射）
- **功能点清单**:
  1. 日/周/月视图切换
  2. 预约 CRUD（患者/提供者/类别/时长/备注）
  3. 重复预约
  4. 假日管理（`interface/main/holidays/` + `HolidayService`）
  5. 预约-就诊关联（从 calendar 创建 encounter）
  6. MedEx 提醒集成（`medex_*` 表：icons/outgoing/prefs/recalls）
  7. 批量通信（`interface/batchcom/` Email/SMS）
  8. 患者流看板报表（`reports/patient_flow_board_report.php`）
  9. 预约报表（`appointments_report.php`、`appt_encounter_report.php`）
- **与其他模块的关联**:
  - → 患者（pc_pid）
  - → 就诊（encounter 从 appointment 创建）
  - → 门户（在线预约 `portal/find_appt_popup_user.php`）
  - → FHIR Appointment

### 3.4 实验室与检验（Laboratory）

- **路径/入口**:
  - UI：`interface/orders/`、`interface/procedure_tools/`
  - Service：`ProcedureService.php`、`ObservationLabService.php`、`ProcedureProviderService.php`
  - REST：`ProcedureRestController.php`
  - FHIR：`FhirServiceRequestRestController.php`、`FhirObservationRestController.php`、`FhirDiagnosticReportRestController.php`、`FhirSpecimenRestController.php`
- **核心数据表/模型**:
  - `procedure_providers`（LabCorp/Quest 等提供商配置）
  - `procedure_type`（检验项目定义）
  - `procedure_questions`（下单问卷）
  - `procedure_order` + `procedure_order_code`（医嘱）
  - `procedure_report` + `procedure_result`（结果）
  - `procedure_specimen`（标本追踪）
  - `procedure_answers`（问卷回答）
  - `procedure_order_relationships`（医嘱关联）
- **功能点清单**:
  1. 检验提供商配置（HL7 连接参数）
  2. 检验项目字典维护（`procedure_type`）
  3. 就诊内下检验医嘱（procedure_order 表单）
  4. HL7 订单发送（LabCorp/Quest，`library/hl7_*`）
  5. HL7 结果接收与解析
  6. 结果审核/签名/发布
  7. 标本采集与追踪
  8. 外部检验（`external_procedures`、`external_encounters`）
  9. 检验报表（`clinical_reports.php`）
  10. FHIR ServiceRequest/Observation/DiagnosticReport 导出
- **与其他模块的关联**:
  - → 就诊（encounter_id）
  - → 患者（patient_id）
  - → 临床笔记（`clinical_notes_procedure_results`）
  - → CQM 质量度量

### 3.5 药房与处方（Pharmacy & Prescriptions）

- **路径/入口**:
  - UI：`interface/drugs/`（库存/发药）、`interface/eRx*.php`（电子处方）
  - Service：`DrugService.php`、`PrescriptionService.php`、`DrugSalesService.php`
  - REST：`PrescriptionRestController.php`、`DrugRestController.php`
  - FHIR：`FhirMedicationRequestRestController.php`、`FhirMedicationDispenseRestController.php`、`FhirMedicationRestController.php`
  - 模块：`oe-module-weno`（Weno eRx）、Ensora/NewCrop eRx
- **核心数据表/模型**:
  - `drugs`（药品主数据）
  - `drug_inventory` + `drug_sales`（库存与销售）
  - `drug_templates`（处方模板）
  - `prescriptions`（处方：drug/dosage/route/refills/pharmacy_id）
  - `pharmacies`（药房地址）
  - `erx_rx_log` + `erx_narcotics`（eRx 日志/管制药品）
  - `product_warehouse`（仓库）
- **功能点清单**:
  1. 药品字典 CRUD（NDC/RxNorm 编码）
  2. 库存管理（lot/quantity/warehouse）
  3. 就诊内开处方（dosage/frequency/route/days_supply/refills）
  4. 处方历史与续方
  5. 院内发药/销售（drug_sales）
  6. 电子处方（eRx SOAP/XML：`eRx.php`、`eRxSOAP.php`、`eRxXMLBuilder.php`）
  7. EPCS 管制药品电子签名
  8. Weno/NewCrop 第三方 eRx 集成
  9. 销毁药品报表（`destroyed_drugs_report.php`）
  10. 处方报表（`prescriptions_report.php`）
- **与其他模块的关联**:
  - → 患者 lists（medication 类型）
  - → 就诊（encounter）
  - → 计费（dispense 费用）
  - → 门户（`portal/get_prescriptions.php`）

### 3.6 计费与财务（Billing & Finance）

- **路径/入口**:
  - UI：`interface/patient_file/front_payment*.php`（前台收款）、`interface/billing/`（计费管理）
  - 遗留库：`library/FeeSheet/`、`library/billing.inc.php`
  - Service：`src/Services/` 内 billing 相关 + `Payment*` 
  - REST：`TransactionRestController.php`、`InsuranceRestController.php`
- **核心数据表/模型**:
  - `billing`（计费行：code/fee/paid/adj/encounter）
  - `claims`（索赔）
  - `payments` + `ar_session` + `ar_activity`（收款/应收账款）
  - `insurance_data` + `insurance_companies` + `insurance_numbers`
  - `fee_schedule` + `fee_sheet_options` + `prices` + `codes`
  - `x12_partners` + `edi_sequences` + `eligibility_verification`
  - `payment_gateway_details` + `payment_processing_audit`
  - `benefit_eligibility`
- **功能点清单**:
  1. Fee Sheet 费用单（就诊内编码/收费）
  2. 前台收款（cash/check/card/terminal：`front_payment.php`）
  3. POS Checkout（`pos_checkout.php` 多种模式）
  4. 计费管理器（billing_process.php：生成索赔）
  5. ERA/EOB 处理（`era_payments.php`、`sl_eob_*.php`）
  6. X12 837 索赔生成（`get_claim_file.php`）
  7. X12 270/271 资格验证（`edi_270.php`、`edi_271.php`）
  8. UB-04 住院账单（`ub04_form.php`）
  9. HCFA-1500 门诊账单
  10. 日结报表（`print_daysheet_report_num*.php`）
  11. 应收账款（AR）管理
  12. 在线支付（Stripe/Authorize.Net/Rainforest：`portal_payment*.js`）
  13. 预授权模块（`oe-module-prior-authorizations`）
  14. 收款报表（`sl_receipts_report.php`、`receipts_by_method_report.php`）
  15. 服务码财务报表（`svc_code_financial_report.php`）
- **与其他模块的关联**:
  - → 就诊（encounter → billing lines）
  - → 保险（insurance_data → claims）
  - → 患者（pid → AR balance）
  - → 门户（在线支付）
  - → FHIR Coverage/Claim

### 3.7 文档管理（Documents）

- **路径/入口**:
  - UI：`interface/patient_file/summary/documents.php`
  - Service：`DocumentService.php`、`CDADocumentService.php`
  - REST：`DocumentRestController.php` → `/api/patient/{pid}/document`
  - FHIR：`FhirDocumentReferenceRestController.php`
- **核心数据表/模型**:
  - `documents`（文档元数据：url/mimetype/foreign_id）
  - `categories` + `categories_to_documents`（分类树）
  - `documents_legal_master` + `documents_legal_detail` + `documents_legal_categories`（法律文档）
  - `document_templates` + `document_template_profiles`
  - `esign_signatures`（电子签名）
  - `ccda` + `ccda_*`（C-CDA 组件映射）
- **功能点清单**:
  1. 文档上传/下载/分类
  2. CouchDB 远程存储（`interface/couchdb/`）
  3. 电子签名 ESign（`interface/esign/`）
  4. C-CDA 生成/导入（Carecoordination 模块 + ccdaservice）
  5. DICOM 查看（文档类型支持）
  6. 传真/扫描（`oe-module-faxsms`）
  7. 文档模板管理（`super/manage_document_templates.php`）
  8. 批量导出（`oe-module-ehi-exporter` ONC EHI Export）
- **与其他模块的关联**:
  - → 患者（foreign_id = pid）
  - → 就诊（encounter 关联）
  - → 门户（`portal/get_patient_documents.php`）
  - → FHIR DocumentReference/Binary

### 3.8 报表与临床质量（Reports & Quality）

- **路径/入口**:
  - UI：`interface/reports/`（40+ 报表脚本）
  - Service：`src/Services/Reports/`、`Qdm/`、`Qrda/`
  - 规则引擎：`interface/super/rules/`、`clinical_rules` 表
- **核心数据表/模型**:
  - `clinical_rules` + `rule_*`（CDR 临床决策规则）
  - `clinical_plans` + `clinical_plans_rules`
  - `report_results` + `report_itemized`
  - `amc_misc_data`（AMC 自动化度量）
  - `syndromic_surveillance`（综合征监测）
- **功能点清单**:
  1. 患者列表/分类报表（`patient_list.php`）
  2. 就诊报表（`encounters_report.php`）
  3. 临床报表（`clinical_reports.php`）
  4. 免疫接种报表（`immunization_report.php`）
  5. CQM 临床质量度量（`cqm.php`）
  6. AMC 自动化度量（`amc_full_report.php`、`amc_tracking.php`）
  7. CDR 临床决策规则（`cdr_log.php` + `interface/patient_file/rules/`）
  8. 审计篡改检测（`audit_log_tamper_report.php`）
  9. 库存报表（`inventory_*`）
  10. 销售报表（`sales_by_item.php`）
  11. 唯一就诊患者（`unique_seen_patients_report.php`）
  12. QRDA Category I/III 导出
- **与其他模块的关联**:
  - 聚合所有临床/计费/检验数据
  - → ONC 认证（CQM/AMC/CDR）

### 3.9 患者门户（Patient Portal）

- **路径/入口**:
  - UI：`portal/index.php`、`portal/home.php`、`portal/patient/`
  - API：`/apis/{site}/portal/*`
  - Service：`PatientPortalService.php`、`PortalMessagingSender.php`
- **核心数据表/模型**:
  - `patient_access_onsite`（门户账户）
  - `onsite_documents` + `onsite_signatures`（门户文档/签名）
  - `onsite_mail` + `onsite_messages`（安全消息）
  - `onsite_portal_activity`（活动日志）
  - `patient_portal_menu`（门户菜单配置）
  - `onetime_auth`（一次性认证）
- **功能点清单**:
  1. 患者注册/登录/onetime_auth
  2. 查看 demographics/profile（`get_profile.php`）
  3. 查看检验结果（`get_lab_results.php`）
  4. 查看处方（`get_prescriptions.php`）
  5. 查看问题/过敏（`get_problems.php`、`get_allergies.php`）
  6. 查看文档（`get_patient_documents.php`）
  7. 安全消息（`portal/messaging/`）
  8. 在线预约（`find_appt_popup_user.php`）
  9. 文档签名（`portal/sign/`）
  10. 在线支付（`portal_payment.php` + Stripe/Authorize.Net）
  11. 问卷填写（`questionnaire_render.php`）
  12. 修正案查看（`get_amendments.php`）
- **与其他模块的关联**:
  - 只读/有限写入核心临床数据
  - → 计费（在线支付）
  - → 调度（预约）

### 3.10 FHIR / SMART / REST API

- **路径/入口**:
  - 路由：`apis/routes/_rest_routes_standard.inc.php`、`_rest_routes_fhir_r4_us_core_3_1_0.inc.php`
  - OAuth：`oauth2/`、`AuthorizationController.php`
  - SMART：`src/RestControllers/SMART/`
- **核心数据表/模型**:
  - `api_token` + `api_refresh_token` + `api_log`
  - `oauth_clients` + `oauth_trusted_user`
  - `uuid_registry` + `uuid_mapping`
  - `export_job`（Bulk Export）
- **功能点清单**:
  1. Standard REST CRUD（Patient/Encounter/Condition/Allergy/Medication/Appointment/Document/Insurance/Prescription/Facility/Practitioner/Message）
  2. FHIR R4 30 Resources（Patient/Observation/Encounter/Condition/MedicationRequest/DiagnosticReport/CarePlan/Immunization/DocumentReference 等）
  3. US Core 8.0 合规
  4. Bulk Data Export（`FhirOperationExportRestController`）
  5. SMART on FHIR v2.2 Launch
  6. Swagger UI 文档
  7. Token Introspection
  8. Background Service API
- **与其他模块的关联**:
  - 所有 Service 层的 FHIR 映射出口
  - → 外部 EHR/HIE/APP 集成

### 3.11 系统管理与配置（Administration）

- **路径/入口**:
  - UI：`interface/super/`（全局配置）、`interface/usergroup/`（用户/ACL）
  - Service：`UserService.php`、`FacilityService.php`、`ListService.php`、`Globals/`
- **核心数据表/模型**:
  - `users` + `users_secure` + `users_facility` + `user_settings`
  - `gacl_*`（17 张 ACL 表）
  - `facility` + `facility_user_ids`
  - `globals`（200+ 全局配置项）
  - `list_options`（下拉字典）
  - `codes` + `code_types`（ICD/SNOMED/RxNorm/CPT）
  - `modules` + `openemr_modules` + `module_*`
- **功能点清单**:
  1. 全局配置（`super/edit_globals.php`：200+ 开关）
  2. 用户/角色/ACL 管理（GACL）
  3. 机构/设施管理
  4. 编码管理（ICD-9/10、SNOMED、RxNorm、CPT）
  5. 表单/布局/列表管理（`edit_layout.php`、`edit_list.php`）
  6. 多语言（`lang_*` 4 表）
  7. 数据库备份（`main/backup.php`）
  8. 模块管理（安装/启用/配置）
  9. 临床规则管理（`super/rules/`）
  10. 文档模板管理
  11. MFA 注册管理
  12. 产品注册（`product_registration`）
- **与其他模块的关联**:
  - 全局配置影响所有模块行为
  - ACL 控制所有 UI/API 访问

### 3.12 可插拔模块（Modules）

- **路径/入口**:
  - Laminas：`interface/modules/zend_modules/module/`（Application/Acl/Carecoordination/Ccr/Documents/FHIR/Immunization/Installer 等 14 模块）
  - Custom：`interface/modules/custom_modules/`（7 个 oe-module-*）
- **Custom 模块清单**:
  1. `oe-module-comlink-telehealth` — 远程视频会诊
  2. `oe-module-faxsms` — 传真/SMS 通信
  3. `oe-module-weno` — Weno 电子处方
  4. `oe-module-prior-authorizations` — 预授权工作流
  5. `oe-module-ehi-exporter` — ONC EHI 批量导出
  6. `oe-module-dashboard-context` — Dashboard Widget 扩展
  7. `oe-module-dorn` — 实验室集成（DORN）
- **与其他模块的关联**:
  - 通过 Laminas MVC + hooks 扩展核心功能
  - 模块 ACL（`module_acl_*` 表）

### 3.13 其他子系统

#### 团体治疗（Group Therapy）
- **路径**: `interface/therapy_groups/`
- **表**: `therapy_groups`、`therapy_groups_participants`、`therapy_groups_participant_attendance`、`form_group_attendance`
- **功能**: 团体创建/参与者/出勤/团体就诊

#### 批量通信（Batch Communication）
- **路径**: `interface/batchcom/`
- **表**: `batchcom`、`email_queue`、`automatic_notification`
- **功能**: Email/SMS 批量发送、通知模板

#### Direct 安全消息（Direct Messaging）
- **路径**: `library/direct/` 
- **表**: `direct_message_log`
- **功能**: phiMail Direct 协议安全消息

#### Chart Tracker
- **路径**: `interface/patient_file/` 相关
- **表**: `chart_tracker`
- **功能**: 纸质病历位置追踪

---

## 4. 系统优点

1. **功能完整性** — 281 表覆盖门诊全流程（患者→就诊→检验→药房→计费→报表）
2. **互操作业界领先** — FHIR R4/SMART/CQM/Bulk Export/HL7/X12 全套
3. **20+ 年活跃社区** — 全球最大开源 EHR 社区之一
4. **可配置性** — LBF 布局引擎、JSON 菜单、registry 表单、globals 200+ 开关
5. **安全机制** — GACL + OAuth2/OIDC + MFA + 多层审计
6. **现代化进展** — src/ Service 层、Symfony API Kernel、Twig 模板迁移、UUID 标准化

---

## 5. 系统不足

1. **双轨代码并存** — `library/` 遗留 ADODB + `src/` 现代 Service，维护成本高
2. **UI 老旧** — Tab + iframe 架构，非 SPA，用户体验落后
3. **ORM 不一致** — 主要仍 ADODB/raw SQL，Doctrine 仅用于 migrations
4. **安全历史** — 8.0.0.3 修复 17+ CVE，需持续跟进
5. **部署复杂** — PHP 8.2 + MySQL + Redis + Imagick + Node 24 + CouchDB(可选)
6. **美国中心** — X12/HCFA/UB-04 计费，无中国国标/医保/DRG/DIP
7. **单体架构** — background_services 轮询，非消息队列，水平扩展弱
8. **测试覆盖** — 相对 2000+ 源文件，自动化测试偏少

---

## 6. 开发参考索引

| 模块 | 关键文件 | 可复用模式 |
|------|----------|------------|
| 患者管理 | `src/Services/PatientService.php`、`interface/patient_file/summary/`、`patient_data` 表 | LBF 布局引擎、Dashboard Fragment Ajax |
| 就诊/表单 | `interface/forms/`、`registry` 表、`src/Services/EncounterService.php` | Form Registry 可插拔表单 |
| 调度 | `interface/main/calendar/`、`AppointmentService.php` | PostCalendar 事件模型 |
| 检验 | `procedure_*` 表、`ProcedureService.php` | HL7 订单/结果 + FHIR ServiceRequest |
| 药房 | `interface/drugs/`、`PrescriptionService.php`、`prescriptions` 表 | eRx 集成抽象 |
| 计费 | `interface/billing/`、`library/FeeSheet/`、`billing` 表 | Fee Sheet + X12 EDI 管道 |
| 文档 | `DocumentService.php`、`documents` 表 | 分类树 + CouchDB 存储 |
| 报表/质量 | `interface/reports/`、`clinical_rules` 表 | CQM/CDR 规则引擎 |
| 门户 | `portal/`、`PatientPortalService.php` | 患者自助 + 在线支付 |
| FHIR/API | `apis/routes/`、`src/RestControllers/FHIR/` | Service→FHIR 映射层 |
| 权限 | `gacl_*` 表、`interface/usergroup/` | GACL 细粒度 ACL |
| 模块扩展 | `interface/modules/custom_modules/` | Laminas MVC + hooks |
| UUID/互操作 | `uuid_registry` 表、`src/Services/Trait/UuidTrait.php` | UUID 标准化 |

---

## 对本项目的借鉴价值

| 可借鉴 | 需规避 |
|--------|--------|
| FHIR Service 层设计 | 双轨 legacy/modern 共存 |
| Form Registry + LBF 布局引擎 | Tab/iframe UI |
| JSON 菜单 + ACL | ADODB 直接 SQL |
| OAuth2 API 架构 | 美国计费模型(X12/HCFA) |
| CQM/质量度量框架 | 281 表无 ORM 的类型安全 |
| UUID 标准化互操作 | 单体 + 轮询后台任务 |
| 患者 Dashboard Fragment 模式 | 部署复杂度 |
