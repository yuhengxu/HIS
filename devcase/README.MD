# devcase · 开源项目调研

> 基于 [doc/02 HIS + 康养平台开源项目调研报告.md](../doc/02%20HIS%20%2B%20%E5%BA%B7%E5%85%BB%E5%B9%B3%E5%8F%B0%E5%BC%80%E6%BA%90%E9%A1%B9%E7%9B%AE%E8%B0%83%E7%A0%94%E6%8A%A5%E5%91%8A.md) 下载源码并在本地阅读后的分析产出。

## 目录说明

| 目录 | 是否上传 git | 说明 |
|------|--------------|------|
| **[AnalogyRslt/](./AnalogyRslt/)** | ✅ 是 | 分析结果（架构、模块功能、优缺点、开发参考索引） |
| `{项目名}/` | ❌ 否 | 开源项目源码，仅本地保留（见 [.gitignore](./.gitignore)） |
| `zip/` | ❌ 否 | 源码压缩包，仅本地保留 |

## 分析文档索引

详见 [AnalogyRslt/README.MD](./AnalogyRslt/README.MD)。

| 项目 | 分析文档 |
|------|----------|
| OpenEMR | [AnalogyRslt/openemr-master.md](./AnalogyRslt/openemr-master.md) |
| OpenMRS Core | [AnalogyRslt/openmrs-core-master.md](./AnalogyRslt/openmrs-core-master.md) |
| Danphe EMR | [AnalogyRslt/hospital-management-emr-master.md](./AnalogyRslt/hospital-management-emr-master.md) |
| ECMS | [AnalogyRslt/ecms-main.md](./AnalogyRslt/ecms-main.md) |
| HospitalRun | [AnalogyRslt/hospitalrun-frontend-master.md](./AnalogyRslt/hospitalrun-frontend-master.md) |
| AI Hospital | [AnalogyRslt/AI_Hospital-main.md](./AnalogyRslt/AI_Hospital-main.md) |
| Phlox | [AnalogyRslt/phlox-main.md](./AnalogyRslt/phlox-main.md) |

## 对本项目的借鉴建议

| 层次 | 优先参考 | 借鉴要点 |
|------|----------|----------|
| HIS 业务模型 | OpenEMR | 患者档案、计费流程、FHIR 接口、权限体系 |
| 平台架构 | OpenMRS | 模块化扩展、Patient/Obs/Encounter 领域模型 |
| 康养业务 | ECMS | 老人/护理员/合同/排班实体设计 |
| 离线部署 | HospitalRun | PouchDB/CouchDB Offline First |
| AI Agent | AI Hospital | 多角色 Agent 协作、MVME 评测 |
| AI 文书 | Phlox | 模板引擎、分字段 LLM 流水线、MCP 工具 |
| 快速原型 | Danphe EMR | 完整 HIS 模块清单（需技术栈升级） |

## 本地源码

克隆或下载后置于 `devcase/{项目名}/`，例如 `devcase/openemr-master/`。缺失时可从各项目 GitHub 重新获取，**勿提交至 git**。
