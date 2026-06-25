# future 分支是否已合并 main 检查计划（2026-06-25）

## 1. 目标

- 判断 `future` 分支的提交是否已经合并到 `main`。
- 同时检查本地与远端分支状态，避免只看本地过期引用。

## 2. 执行步骤

- 查看当前分支与本地/远端分支列表。
- 尝试更新远端引用。
- 使用 `git merge-base --is-ancestor future main` 判断提交包含关系。
- 使用 `git log main..future` 和 `git log future..main` 查看两边独有提交。
- 给出明确结论和后续建议。
