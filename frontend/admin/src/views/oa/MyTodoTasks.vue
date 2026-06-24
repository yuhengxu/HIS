<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { oaApi, type OaTask } from '../../api/oa'

const tasks = ref<OaTask[]>([])
async function load() { tasks.value = await oaApi.todo() }
async function approve(id: number) { await oaApi.approve(id); ElMessage.success('已审批'); await load() }
async function reject(id: number) { await oaApi.reject(id); ElMessage.warning('已驳回'); await load() }
onMounted(load)
</script>

<template>
  <section class="page-section">
    <header class="section-head"><h1>我的待办</h1><el-button @click="load">刷新</el-button></header>
    <el-table :data="tasks" border>
      <el-table-column prop="id" label="任务 ID" width="100" />
      <el-table-column prop="processInstanceId" label="流程实例" />
      <el-table-column prop="assigneeMode" label="处理模式" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="180"><template #default="scope"><el-button size="small" type="primary" @click="approve(scope.row.id)">同意</el-button><el-button size="small" @click="reject(scope.row.id)">驳回</el-button></template></el-table-column>
    </el-table>
  </section>
</template>
