# 数据字典编写规范

## 1. 命名规范

- 表名使用小写蛇形命名，按业务域加前缀：`iam_`、`oa_`、`inv_`、`audit_`、`ai_`。
- 字段名使用小写蛇形命名。
- 主键统一为 `id`，类型首期使用 `bigserial` 或 `bigint`。
- 时间字段统一使用 `created_at`、`updated_at`、`deleted_at`。
- 软删除字段使用 `deleted_at`，为空表示未删除。

## 2. 索引命名

| 类型 | 命名 |
|---|---|
| 主键 | `pk_<table>` |
| 唯一索引 | `uk_<table>_<columns>` |
| 普通索引 | `idx_<table>_<columns>` |
| 外键 | `fk_<table>_<ref_table>` |

## 3. 表设计必须包含

- 表用途
- 字段说明
- 主键
- 唯一索引
- 普通索引
- 外键关系
- 数据生命周期
