# 后端包结构与文档边界重构计划

## 目标

- 将后端 Java 包边界调整为：
  - `com.health.platform`：共享平台能力，包括 IAM、认证、审计、AI 接入、网关与公共基础设施。
  - `com.health.his`：HIS 医院信息系统独立业务域。
  - `com.health.kangyang`：康养服务系统独立业务域。
- 重规划文档中的模块边界，明确 HIS 与康养并列独立，共享能力归入 platform。
- 文档和业务目录不新增品牌目录层；Java 包路径按 `com.health.*` 落地。

## 范围

- 后端 Java 包名与源码路径调整。
- 测试包名与 import 调整。
- 后端 Maven artifact/name 调整为中性平台命名。
- 设计文档、开发文档、API/权限/数据库说明中涉及模块边界的文字调整。
- 不修改业务行为、数据库 DDL、API 路径和权限点。

## 执行顺序

1. 盘点当前后端包、测试包、文档中的旧组合包名、旧组合 artifact 与单一业务模块后端命名。
2. 将共享能力迁移到 `com.health.platform`，保留 `com.health.his` 与 `com.health.kangyang` 作为独立业务域包。
3. 修正 Spring Boot 启动类、测试类、import 与包扫描。
4. 调整 Maven artifact/name 为中性平台后端命名。
5. 更新文档中关于后端包结构、模块边界、部署边界与开发目录的说明。
6. 运行后端测试，检查旧包名/旧模块命名残留。

## 验收标准

- 后端源码不再使用旧组合包名。
- 共享能力位于 `com.health.platform` 包下。
- `com.health.his` 与 `com.health.kangyang` 作为并列业务域存在，不互相包含。
- 文档明确 platform / HIS / Kangyang 三层边界。
- 文档和业务目录路径不新增 `health` 目录层。
- `mvn test` 通过。

## 风险

- Java 包路径必须遵循包名，因此源码物理路径会包含 `com/health`。
- 本轮只做包边界与文档规划调整，不拆分 Maven 多模块，也不改变运行时部署单元。
