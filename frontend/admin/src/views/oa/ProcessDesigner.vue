<template>
  <section class="page-section">
    <h1>流程节点配置</h1>
    <el-table :data="[{ sort: 10, name: '汇报上级审批', mode: 'SUPERVISOR' }, { sort: 20, name: '角色审批', mode: 'ROLE' }]" border>
      <el-table-column prop="sort" label="顺序" width="100" />
      <el-table-column prop="name" label="节点名称" />
      <el-table-column prop="mode" label="处理模式" />
    </el-table>
    <div class="flow-hint">首期使用表格配置节点，后续再扩展可视化设计器。</div>
  </section>
</template>
