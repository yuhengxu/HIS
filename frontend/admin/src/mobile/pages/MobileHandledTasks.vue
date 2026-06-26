<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { oaApi, type OaTask } from '../../api/oa'
const rows = ref<OaTask[]>([])
async function load() { rows.value = await oaApi.handled() }
onMounted(load)
</script>
<template>
  <section>
    <div class="mobile-card-title">我的已办</div>
    <div v-for="task in rows" :key="task.id" class="mobile-card">
      <div>任务 #{{ task.id }}</div>
      <div class="mobile-muted">流程 {{ task.processInstanceId }}｜{{ task.status }}</div>
    </div>
    <div v-if="rows.length === 0" class="mobile-card mobile-muted">暂无已办</div>
  </section>
</template>
