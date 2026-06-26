<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { oaApi, type OaTask } from '../../api/oa'
const rows = ref<OaTask[]>([])
async function load() { rows.value = await oaApi.todo() }
onMounted(load)
</script>
<template>
  <section>
    <div class="mobile-card-title">我的待办</div>
    <router-link v-for="task in rows" :key="task.id" class="mobile-card mobile-list-item" :to="`/m/oa/tasks/${task.id}`">
      <div>待办 #{{ task.id }}</div>
      <div class="mobile-muted">流程 {{ task.processInstanceId }}｜{{ task.assigneeMode }}｜{{ task.status }}</div>
    </router-link>
    <div v-if="rows.length === 0" class="mobile-card mobile-muted">暂无待办</div>
  </section>
</template>
