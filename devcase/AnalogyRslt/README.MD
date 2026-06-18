# AnalogyRslt · 开源项目分析结果

> 本目录为 **git 跟踪的分析文档**；对应源码位于 `devcase/{项目名}/`（本地保留，已 `.gitignore` 排除）。

## 文档索引

| 项目 | 类型 | 参考价值 | 分析文档 | 本地源码 |
|------|------|----------|----------|----------|
| OpenEMR | 开源 HIS/EHR | ★★★★★ | [openemr-master.md](./openemr-master.md) | `../openemr-master/` |
| OpenMRS Core | 模块化 EMR 平台 | ★★★★★ | [openmrs-core-master.md](./openmrs-core-master.md) | `../openmrs-core-master/` |
| Danphe EMR | 中小型 HIS | ★★★☆☆ | [hospital-management-emr-master.md](./hospital-management-emr-master.md) | `../hospital-management-emr-master/` |
| ECMS | 养老护理管理 | ★★★★☆ | [ecms-main.md](./ecms-main.md) | `../ecms-main/` |
| HospitalRun | 离线优先 HIS 前端 | ★★★★☆ | [hospitalrun-frontend-master.md](./hospitalrun-frontend-master.md) | `../hospitalrun-frontend-master/` |
| AI Hospital | 多 Agent 临床诊断评测 | ★★★★★ | [AI_Hospital-main.md](./AI_Hospital-main.md) | `../AI_Hospital-main/` |
| Phlox | AI 医疗文书辅助 | ★★★★★ | [phlox-main.md](./phlox-main.md) | `../phlox-main/` |

## 分析维度

每份分析文档包含：

1. **系统架构** — 技术栈、模块划分、数据流
2. **设计方案** — 核心设计模式与抽象
3. **功能模块详解** — 路径/入口、数据表/模型、功能点清单、模块关联
4. **系统优点**
5. **系统不足**
6. **开发参考索引** — 模块 → 关键文件 → 可复用模式

## 修订说明

- 分析基于本地源码阅读；更新分析时请改本目录下 `.md` 文件，勿将源码提交 git。
- 源码缺失时，可从各项目 GitHub 重新下载至 `devcase/{项目名}/`。
