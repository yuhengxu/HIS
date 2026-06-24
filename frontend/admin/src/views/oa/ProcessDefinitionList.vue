<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { oaApi, type ProcessDefinition } from '../../api/oa'

const processes = ref<ProcessDefinition[]>([])
onMounted(async () => { processes.value = await oaApi.processes() })
</script>

<template>
  <section class="page-section">
    <header class="section-head"><h1>OA 流程定义</h1></header>
    <el-table :data="processes" border>
      <el-table-column prop="code" label="流程编码" />
      <el-table-column prop="name" label="流程名称" />
      <el-table-column prop="version" label="版本" width="90" />
      <el-table-column prop="enabled" label="启用" width="90" />
    </el-table>
    <div class="flow-hint">首期采用表格配置节点，不引入复杂拖拽流程设计器。</div>
  </section>
</template>
