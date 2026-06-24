<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { oaApi, type StartableProcess } from '../../api/oa'

const router = useRouter()
const processes = ref<StartableProcess[]>([])

onMounted(async () => {
  processes.value = await oaApi.startable()
})

function openProcess(item: StartableProcess) {
  if (item.typeCode === 'MATERIAL_INBOUND') router.push('/oa/inbound')
  else if (item.typeCode === 'MATERIAL_OUTBOUND') router.push('/oa/outbound')
  else if (item.typeCode === 'REIMBURSEMENT') router.push('/oa/reimbursement')
}
</script>

<template>
  <section class="page-section">
    <header class="section-head"><h1>发起 OA 流程</h1></header>
    <el-row :gutter="16">
      <el-col v-for="item in processes" :key="item.typeCode" :span="8">
        <el-card shadow="hover" class="process-card" @click="openProcess(item)">
          <h3>{{ item.title }}</h3>
          <p>{{ item.typeCode }}</p>
          <el-button type="primary" link>立即发起</el-button>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<style scoped>
.process-card { cursor: pointer; margin-bottom: 16px; }
.process-card h3 { margin: 0 0 8px; }
.process-card p { color: #64748b; margin: 0 0 12px; }
</style>
