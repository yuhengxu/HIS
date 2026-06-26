<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { oaApi, type ProcessInstanceView } from '../../api/oa'
const rows = ref<ProcessInstanceView[]>([])
async function load() { rows.value = await oaApi.myInstances() }
async function urge(id: number) { await oaApi.urge(id); ElMessage.success('已催办'); await load() }
async function revoke(id: number) { await oaApi.revoke(id); ElMessage.success('已撤销'); await load() }
onMounted(load)
</script>
<template>
  <section>
    <div class="mobile-card-title">我发起的</div>
    <div v-for="item in rows" :key="item.id" class="mobile-card">
      <div>{{ item.title }}</div>
      <div class="mobile-muted">{{ item.stage }}｜{{ item.currentNodeName || '-' }}｜{{ item.currentHandler }}</div>
      <span class="mobile-status">{{ item.status }}</span>
      <div v-if="item.status === 'RUNNING' || item.status === 'PENDING_CONFIG'" style="margin-top: 10px; display: grid; grid-template-columns: 1fr 1fr; gap: 8px">
        <el-button @click="urge(item.id)">催办</el-button>
        <el-button type="danger" plain @click="revoke(item.id)">撤销</el-button>
      </div>
    </div>
    <div v-if="rows.length === 0" class="mobile-card mobile-muted">暂无流程</div>
  </section>
</template>
