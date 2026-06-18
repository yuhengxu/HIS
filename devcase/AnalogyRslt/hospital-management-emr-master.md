# Danphe EMR 项目深度分析

> 分析文档：`devcase/AnalogyRslt/hospital-management-emr-master.md`（上传 git）  
> 源码路径（本地）：`devcase/hospital-management-emr-master/`（不上传 git）
> 项目：[opensource-emr/hospital-management-emr](https://github.com/opensource-emr/hospital-management-emr)  
> 定位：**C#/.NET + Angular 7 企业级 HIS/EMR**（50+ 亚洲医院部署）

**重要说明**：非 PHP 项目，技术栈为 **C# ASP.NET Core (net461) + Angular 7 + SQL Server**。

---

## 1. 系统架构

### 1.1 三层 + 模块化单体

```
Angular 7 SPA (DanpheApp) + Razor 视图
    ↓ REST API / JWT (24h)
DanpheEMR Web (ASP.NET Core, 137 Controllers)
    ↓ Entity Framework DbContext
DanpheEMR.DalLayer (45 领域 DbContext)
    ↓
SQL Server 多库：主业务库 + DanpheAdmin + Danphe_PACS(可选)
```

### 1.2 解决方案结构

| 项目 | 职责 |
|------|------|
| DanpheEMR | Web 主站：Controllers、Services、Angular 静态资源 |
| DanpheEMR.DalLayer | 按领域拆分的 EF DbContext（45 个） |
| DanpheEMR.ServerModel | 实体/DTO/ViewModel（602 文件） |
| DanpheEMR.Security | RBAC：RbacUser/Role/Permission/Route |
| DanpheEMR.Core | 缓存、参数、Lookup |
| DanpheEMR.Sync / Jobs / AccTransfer | 同步、后台任务、账单→会计 |

### 1.3 数据库

仓库内仅含 **DanpheAdmin_CompleteDB.sql** + **CleanUpScript.sql**（主库建表 SQL 不在仓库）

主库表前缀推断：

| 前缀 | 领域 |
|------|------|
| PAT_ | 患者/就诊/预约 |
| BIL_ | 计费 |
| ACC_ | 会计 |
| PHRM_ | 药房 |
| LAB_ | 检验 |
| RAD_ | 放射 |
| ADT_ | 入出院 |
| CLN_ | 临床/EMR |
| INV_ | 库存 |
| WARD_ | 病区供应 |
| ER_ / OT_ / MAT_ / VACC_ / PROLL_ | 急诊/手术/产科/疫苗/薪酬 |

---

## 2. 设计方案

### 2.1 领域驱动模块化

- 每业务域独立 DbContext（BillingDbContext、LabDbContext 等 45 个）
- Controller + BL + Service 三层（新旧混用）
- Angular 懒加载模块（52 路由文件）+ 患者上下文 Guard 贯穿多模块

### 2.2 计费与会计联动

计费交易 → BIL_SYNC_BillingAccounting → 会计凭证自动同步

### 2.3 打印方案

HTML 模板 + 存储过程 + ServerSidePrinter/浏览器打印

### 2.4 集成能力

LIS 仪器接口、DICOM/PACS、Google Drive 报告、SSF/Medicare/Gov Insurance、SMS、队列管理

### 2.5 安全/RBAC

- 四层模型：User → Role → Permission → DanpheRoute（前端路由权限表）
- 全量缓存于 DanpheCache
- 前端 AuthGuard + 后端 DanpheDataFilter（JWT 认证）

---

## 3. 功能模块详解

### 3.1 患者与预约（Patient & Appointment）

- **路径/入口**:
  - 前端：`Patient` 模块 → `patients-routing.constant.ts`
  - 后端：`PatientController.cs`、`AppointmentController.cs`、`VisitController.cs`
- **核心数据表/模型**: `PatientModel`、`AddressModel`、`VisitModel`、`AppointmentModel`、`Guarantor*`、`Insurance*`、`PatientSchemeMapModel`
- **功能点清单**:
  1. 分步注册：`RegisterPatient/BasicInfo` → `Address` → `Guarantor` → `Insurance` → `KinEmergencyContact` → `ProfilePic`
  2. 患者搜索：`SearchPatient`（Dashboard + 全局搜索）
  3. 患者编辑：`PUT PutPatient`
  4. 患者文档：`PatientDocuments` / `PatientFiles`
  5. 重复检测：`MatchingPatients`
  6. 新建就诊：`POST NewVisit` / `VisitFromBilling` / `DefaultVisitCreate`
  7. 就诊历史：`PatientVisitHistory` / `ListVisits` / `VisitsByStatus`
  8. 预约 CRUD：`AddAppointment` / `UpdateAppointment` / `AppointmentStatus`
  9. 预约冲突检测：`CheckClashingAppointment`
  10. 在线预约（Scheduling 模块关联）
  11. SSF Claim 提交
- **与其他模块的关联**: → 计费（Visit→Bill）、临床（Patient Overview）、ADT（Admission）

### 3.2 计费（Billing）

- **路径/入口**:
  - 前端：`Billing` 模块 → `billing-routing.module.ts`（15+ 子路由）
  - 后端：`BillingController.cs`、`BillingMasterController.cs`、`IpBillingController.cs`、`BillSettlementController.cs`、`BillingDepositController.cs`
- **核心数据表/模型**: `BillingTransactionModel`、`BillingTransactionItemModel`、`DepositHeadModel`、`BillServiceItems`、`Schemes`、`PriceCategories`
- **功能点清单**:
  1. Dashboard 统计
  2. 搜索患者 → 创建账单：`BillingTransaction`
  3. 正式开票 / 暂记账单（Provisional）：`PayProvisional`
  4. 暂存 clearance：`ProvisionalClearance/Outpatient-Er`、`ProvisionalDischargeList`
  5. 账单订单请求：`BillOrderRequest`
  6. 押金管理：`BillingDeposit`
  7. 账单取消/退货：`BillCancellationRequest`、`BillReturnRequest`
  8. 编辑医生：`EditDoctor`
  9. 结算：`Settlements` / `POST NewSettlement`
  10. 住院计费：`IpBilling` / `PostBillTransactionAndDischarge`
  11. 面额交班：`Denomination`（Counter/Accounts/Reports）
  12. QR 码计费：`QrBilling`
  13. 保险计费：`BillInsuranceController`
  14. Credit Note：`BillReturnController`
  15. 出院计费：`DischargeBillingController`
- **与其他模块的关联**: → 会计（BIL_SYNC）、药房（联动发药计费）、检验/影像（WardBilling）

### 3.3 会计（Accounting）

- **路径/入口**:
  - 前端：`Accounting` 模块 → `accounting-routing.module.ts`
  - 后端：`AccountingController.cs`、`AccountingSettingsController.cs`、`AccountingReportController.cs`
- **核心数据表/模型**: `LedgerModel`、`VoucherModel`、`FiscalYearModel`、`ChartOfAccount*`
- **功能点清单**:
  1. 凭证录入/审核：`VoucherEntry`
  2. 转账：`TransferToACC`
  3. 关账：`AccountClosure`
  4. 激活医院：`ActivateHospital`
  5. 付款：`Payment`
  6. 银行对账：`BankReconciliation`
  7. 凭证验证：`VoucherVerification`
  8. 库存同步：`accounting-sync-routing/InventorySync`
  9. 报表（10+）：资产负债表、损益表、现金流量、总账、试算平衡、凭证报表等
  10. Medicare 注册：`Insurance/Member`
- **与其他模块的关联**: ← 计费自动同步凭证

### 3.4 库存与采购（Inventory）

- **路径/入口**:
  - 前端：`Inventory` 模块 → `inventory-routing.module.ts`
  - 后端：`InventoryController.cs`、`InventorySettingsController.cs`、`InventoryGoodReceiptController.cs`
- **核心数据表/模型**: `ItemMasterModel`、`RequisitionModel`、`PurchaseOrderModel`、`GoodsReceiptModel`、`MAP_*`
- **功能点清单**:
  1. Dashboard
  2. 内部请领：`InternalMain/Requisition`
  3. 采购申请：`PurchaseRequest`
  4. 子库退回：`ReturnFromSubstore`
  5. 报废：`WriteOffItems`
  6. 退供应商：`ReturnToVendor`
  7. 库存管理：`StockMain`
  8. 捐赠：`Donation`
  9. 报表：Purchase/Stock/Supplier 三大类
  10. 设置：vendor/item/UOM/currency
  11. 审批：`VerificationController` 多级审批
- **与其他模块的关联**: → 会计同步、病区供应（Dispatch）

### 3.5 药房（Pharmacy）

- **路径/入口**:
  - 前端：`Pharmacy` 模块 → `pharmacy-routing.module.ts`（Dashboard/Order/Store/Billing/Reports/Setting）
  - 后端：15+ Pharmacy Controllers
- **核心数据表/模型**: `PHRMItemMasterModel`、`PHRMInvoiceModel`、`PHRMStockTransactionModel`、`PHRM_MAP_ItemToRack`
- **功能点清单**:
  1. Dashboard 统计
  2. 主数据：药品/供应商/分类/税率/UOM/Generics/Counters
  3. 采购：PO（`PharmacyPOController`）/ GR（`PharmacyPurchaseController`）
  4. 库存：`PharmacyStockController`（WardRequisitions/NarcoticsStock/TransferToDispensary）
  5. Store/Sub-store 销售发药：`PharmacySalesController`
  6. 处方：`PharmacyPrescriptionController`（NewPrescription）
  7. 销售退回：`PharmacySalesReturnController`
  8. 采购退回：`PharmacyPurchaseReturnController`
  9. 病区请药：`WardRequisition`
  10. 货架管理：`PharmacyRackController`
  11. 信贷/贷项：`PharmacyCreditController` / `PharmacyCreditNoteController`
  12. 结算：`PharmacySettlementController`
  13. 患者消耗：`PatientConsumptionController`
  14. 30+ 报表：ABC-VED/批号/过期/麻醉品/PO/Stock Summary 等
- **与其他模块的关联**: → 计费、配药站（TransferToDispensary）、护理（DrugRequest）

### 3.6 配药站（Dispensary）

- **路径/入口**:
  - 前端：`Dispensary` 模块 → `dispensary-routing.module.ts`
  - 后端：`DispensaryController.cs`、`DispensaryTransferController.cs`、`DispensaryRequisitionController.cs`
- **功能点清单**:
  1. 激活配药站：`ActivateDispensary`
  2. 患者/处方：`Patient`、`Prescription`
  3. 销售：`Sale/New`、`Return`、`Settlement`、`CreditBills`、`ProvisionalReturn`
  4. 库存：`Stock/Requisition`、`Transfer`
  5. 患者消耗：`PatientConsumptionMain`
  6. 激活柜台：`ActivateCounter`
  7. 报表
- **与其他模块的关联**: ← 药房调拨、→ 计费

### 3.7 检验（Lab/LIS）

- **路径/入口**:
  - 前端：`Lab` 模块 → `labs-routing.module.ts` + `lab-settings-routing` + `lis-routing-module`
  - 后端：`LabController.cs`、`LabSettingController.cs`、`LISController.cs`、`IMUController.cs`
- **核心数据表/模型**: `LabRequisitionModel`、`LabTestModel`、`LabReportModel`、`LabReportTemplateModel`
- **功能点清单**:
  1. Dashboard
  2. 请检：`Requisition`
  3. 采样：`CollectSample`
  4. 结果录入：`AddResult`
  5. 待审报告：`PendingReports` / `PendingLabResults`
  6. 最终报告：`FinalReports`
  7. 病区计费：`WardBilling`
  8. 条码：`BarCode`
  9. 报告分发：`ReportDispatch`
  10. 通知：SMS / IMU Upload
  11. 设置：LabTest/ReportTemplate/Signatories/Vendors/LookUps/Categories/MapGovernmentItems
  12. 外部实验室：`ExternalLabs`
  13. LIS 仪器：`LISComponentMapping` / `LISMachineResult` / `MachineResultSync`
  14. 报告导出：`LabReportExportController`
- **与其他模块的关联**: → 计费（WardBilling）、医生医嘱（Orders）

### 3.8 放射（Radiology）

- **路径/入口**:
  - 前端：`Radiology` 模块 → `radiology-routing.module.ts`
  - 后端：`RadiologyController.cs`、`RadiologyReportController.cs`、`DicomController.cs`
- **核心数据表/模型**: `ImagingRequisitionModel`
- **功能点清单**:
  1. 影像申请列表：`ImagingRequisitionList`
  2. 报告列表：`ImagingReportsList`
  3. 住院列表：`InpatientList`
  4. 病区计费：`WardBilling`
  5. 编辑医生：`EditDoctors`
  6. DICOM 查看器
- **与其他模块的关联**: → 计费、医嘱

### 3.9 入出院（ADT）

- **路径/入口**:
  - 前端：`ADTMain` 模块 → `adt-routing.module.ts`
  - 后端：`AdmissionController.cs`、`AdmissionMasterController.cs`、`DischargeSummaryController.cs`
- **核心数据表/模型**: `AdmissionModel`、`ADTBedReservation`、`PatientBedInfo`、`WardModel`、`BedFeature*`
- **功能点清单**:
  1. 搜索患者：`AdmissionSearchPatient`
  2. 创建入院：`CreateAdmission`
  3. 在院列表：`AdmittedList`
  4. 出院列表：`DischargedList`
  5. 出院：`Discharge` / 取消：`CancelAdmission`
  6. 转科：`Transfer`
  7. 床位预留：`AvailableBeds` / `BedFeatures`
  8. 出院小结：`DischargeSummary`
  9. 出生证明：`BirthCertificate`
  10. 婴儿/死亡登记
- **与其他模块的关联**: → 计费（IpBilling）、护理（InPatient）、急诊（Admitted）

### 3.10 护理（Nursing）

- **路径/入口**:
  - 前端：`Nursing` 模块 → `nursing-routing.module.ts`
  - 后端：`NursingController.cs`（复用 Clinical/Admission API）
- **功能点清单**:
  1. 门诊护理：`OutPatient`
  2. 住院护理：`InPatient/InPatientList`
  3. 病区激活：`ActivateWard`
  4. 患者概览：`PatientOverview`
  5. 病区计费：`WardBilling`
  6. 药品请领：`DrugRequest`
  7. 转科：`Transfer`
  8. 出院小结：`DischargeSummary`
  9. 检验结果：`InvestigationResults`
  10. 会诊请求：`ConsultationRequests`
  11. 肾内科：`Nephrology`
- **与其他模块的关联**: → ADT、药房、检验、临床

### 3.11 急诊（Emergency）

- **路径/入口**:
  - 前端：`Emergency` 模块 → `emergency-routing.module.ts`
  - 后端：`EmergencyController.cs`
- **核心数据表/模型**: `EmergencyPatientModel`、`EmergencyDischargeSummaryModel`
- **功能点清单**:
  1. Dashboard
  2. 新患者：`NewPatients`
  3. 分诊：`TriagePatients`
  4. 已完成：`FinalizedPatients`（LAMA/Discharged/Transferred/Admitted/Death/DOR）
  5. 床位信息：`BedInformations`
  6. 出院小结
  7. ER 病区计费：`ERWardBilling`
  8. 患者概览
- **与其他模块的关联**: → ADT（Admitted）、计费

### 3.12 医生工作站（Doctors）

- **路径/入口**:
  - 前端：`Doctors` 模块 → `doctors-routing.constant.ts`
  - 后端：`DoctorsController.cs`、`VisitSummaryController.cs`
- **功能点清单**:
  1. 门诊：`OutPatientDoctor/NewPatient`、`OPDRecord`
  2. 住院：`InPatientDepartment`
  3. 患者记录：`PatientRecord`
  4. Patient Overview 12 Tab：
     - `PatientOverview`、`Clinical`、`Orders`
     - `PatientVisitHistory`、`NotesSummary`、`VisitSummary`
     - `ProblemsMain`、`CurrentMedications`
     - `ClinicalDocuments`、`ScannedImages`、`DischargeSummary`
  5. 动态模板问卷（DynTemplates）
- **与其他模块的关联**: → 临床 EMR、检验/影像/药房 Orders

### 3.13 临床 EMR（Clinical）

- **路径/入口**:
  - 前端：`clinical-routing.module.ts`、`notes-routing.constant.ts`
  - 后端：`ClinicalController.cs`
- **核心数据表/模型**: `VitalsModel`、`AllergyModel`、`MedicationPrescriptionModel`、`NotesModel`；眼科专用表（Lasik/Pachymetry/Wavefront 等）
- **功能点清单**:
  1. 生命体征：`Vitals` CRUD
  2. 过敏：`Allergy` CRUD
  3. 居家用药：`HomeMedication`
  4. 出入量：`InputOutput`
  5. 医生笔记：`DoctorsNotes` / `FreeNotes`
  6. 血糖监测：`BloodSugarMonitoring`
  7. **眼科专项 EMR**：
     - `EyeExamination/NewEMR`、`EMRHistory`
     - `Prescriptionslip`、`PrescriptionslipHistory`
     - `ScanUpload`
  8. 会诊请求、饮食管理
- **与其他模块的关联**: → 医生工作站、检验/影像 Orders

### 3.14 调度（Scheduling）

- **路径/入口**: `Scheduling` 模块 → `scheduling-routing.module.ts`；`SchedulingController.cs`
- **功能点清单**: 班次管理、排班、可用性、RegistrationScheme

### 3.15 员工与薪酬（Employee & Payroll）

- **路径/入口**: `Employee` 模块、`PayrollMain` 模块
- **功能点清单**:
  1. 员工：`ProfileMain/UserProfile`、`ChangePassword`
  2. 考勤：`Attendance`
  3. 薪资：`Payroll`
  4. 请假：`Leave/Holiday/LeaveRuleList/LeaveRequest/EmployeeLeaves`

### 3.16 激励分成（Incentive）

- **路径/入口**: `Incentive` 模块 → `incentive-routing.module.ts`
- **功能点清单**: `Transactions`、`Setting/ProfileManage/EmployeeItemsSetup`

### 3.17 病区供应（Ward Supply）

- **路径/入口**: `WardSupply` 模块 → `wardsupply-routing.module.ts`；`WardSupplyController.cs`
- **核心数据表/模型**: `WARDRequisitionModel`、`WARDDispatchModel`、`WARDStockModel`、`WARDConsumptionModel`
- **功能点清单**: 病区请领、发运、消耗、资产

### 3.18 手术（Operation Theatre）

- **路径/入口**: `OperationTheatre` 模块 → `ot-routing.module.ts`；`OperationTheatreController.cs`
- **功能点清单**: OT 预约、团队、清单

### 3.19 病案（Medical Records）

- **路径/入口**: `Medical-records` 模块；`MedicalRecordsController.cs`
- **功能点清单**: 出生/死亡证明、ICD10、诊断

### 3.20 产科（Maternity）

- **路径/入口**: `Maternity` 模块 → `maternity-routing.module.ts` + reports
- **功能点清单**: 产前检查、分娩记录

### 3.21 疫苗（Vaccination）

- **路径/入口**: `Vaccination` 模块；`VaccinationController.cs`
- **功能点清单**: 疫苗登记、接种记录

### 3.22 医保与理赔（Insurance & Claims）

- **路径/入口**: `GovInsurance`、`ClaimManagement`、`SSU` 模块
- **后端**: `GovInsuranceController.cs`、`ClaimManagementController.cs`、`MedicareController.cs`、`SSFController.cs`
- **功能点清单**:
  1. 政府医保：患者/就诊/IPD 计费/Reports/InsNewVisit/BillingRequest
  2. Medicare：Member/Dependent 注册
  3. SSF：CheckSSFEligibility/SubmitClaim/BookClaim
  4. 理赔管理：ClaimManagement

### 3.23 其他模块

| 模块 | 前端路由 | 后端 Controller | 功能 |
|------|----------|----------------|------|
| 固定资产 | FixedAssets | AssetManagement/Maintenance/Depreciation | 登记/折旧/报废 |
| CSSD | CSSD | CssdSterilization/Report | 消毒/灭菌 |
| 采购 | ProcurementMain | — | 采购申请/招标 |
| 队列 | QueueManagement | QueueManagementController | OPD 排队 |
| 服务台 | Helpdesk | HelpdeskDbContext | 床位/员工/病区/队列信息 |
| 营销转诊 | MktReferral | MarketingReferralController | 转诊方案/发票 |
| 验证/审批 | Verification | VerificationController | 多级审批流 |
| 系统管理 | SystemAdmin | SystemAdminController | DB 备份/审计/销售账簿 |
| 设置 | Settings | SettingsController 等 | 15+ 管理页面 |
| 报表 | Reports | ReportingController 等 | 40+ 子报表 |
| 工具 | Utilities | UtilitiesController | 方案变更/退款 |

### 3.24 安全/RBAC

- **路径/入口**: `DanpheEMR.Security/RbacDbContext`；`SecurityController.cs`、`SecuritySettingsController.cs`
- **核心数据表/模型**: `RbacApplication`、`RbacRole`、`RbacPermission`、`RbacUser`、`UserRoleMap`、`RolePermissionMap`、`DanpheRoute`
- **功能点清单**:
  1. 登录 → JWT（24h）→ Session `currentuser`
  2. `NavigationRoutes` — 用户可导航路由树
  3. `ValidRoutes` — 全部合法路由
  4. `UserPermissions` — 权限列表
  5. 前端 `AuthGuardService.checkIsAuthorizedURL`
  6. 组件级 `HasPermission(permissionName)`
  7. 上下文 Guard：BillingCounter/Lab/Ward/Dispensary 激活
  8. 后端 `DanpheDataFilter` — JWT 认证（API 级 RBAC 注释未启用）
  9. Settings/SecurityManage — 应用/路由/权限/角色/用户 CRUD

---

## 4. 系统优点

1. **功能覆盖面极广** — 40 模块真正端到端 HIS
2. **50+ 医院生产验证**
3. **RBAC 成熟** — 路由级权限 + Guard 链 + 缓存
4. **审计能力强** — Audit.NET + SQL Server Audit
5. **模块化可扩展** — 45 DbContext + 52 Angular 路由模块
6. **专科扩展示范** — 眼科 EMR 全套
7. **MIT 许可**

---

## 5. 系统不足

1. **技术栈严重老化** — net461 + Angular 7 + EF6
2. **主数据库 Schema 不在仓库**
3. **Controller 内大量业务逻辑**，新旧代码混用
4. **Windows/SQL Server 强绑定**
5. **JWT Key/密码 Salt 硬编码** — 生产需全面加固
6. **无中国 HIS 标准**（HL7 FHIR/国标/医保 DIP/DRG）
7. **单体架构**，横向扩展弱
8. **API 级 RBAC 未启用** — 仅 JWT 认证

---

## 6. 开发参考索引

| 模块 | 关键文件 | 可复用模式 |
|------|----------|------------|
| 患者 | `PatientController.cs`、`patients-routing.constant.ts` | 分步注册 + 患者上下文 Guard |
| 计费 | `BillingController.cs`、`billing-routing.module.ts` | 正式/暂存/结算/押金全流程 |
| 会计 | `AccountingController.cs` | 计费→凭证自动同步 |
| 药房 | `PharmacyController.cs`、`PHRM*` 实体 | 表前缀领域划分 |
| 检验 | `LabController.cs`、`LISController.cs` | LIS 仪器接口 |
| ADT | `AdmissionController.cs` | 入院/转科/出院/床位 |
| 临床 | `ClinicalController.cs` | Vitals/Allergy/Notes + 专科扩展 |
| 安全 | `DanpheRBAC.cs`、`SecurityController.cs` | 路由级 RBAC 四层模型 |
| 报表 | `ReportingController.cs` | 存储过程驱动报表 |
| 前端 | `app-routing.constant.ts` | 40+ 懒加载模块 |

---

## 对本项目的借鉴价值

| 可借鉴 | 需规避 |
|--------|--------|
| 完整 HIS 模块清单（40 模块） | 直接复用 net461/Angular7 技术栈 |
| 计费→会计联动设计 | Controller 内嵌业务逻辑 |
| 患者上下文 Guard 模式 | 硬编码安全配置 |
| 表前缀领域划分（PAT_/BIL_/PHRM_） | 南亚本地化假设 |
| 眼科等专科定制思路 | 无 Schema 的仓库依赖 |
