<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { oaApi, type OaTask, type ProcessInstanceView } from '../../api/oa'

const todos = ref<OaTask[]>([])
const mine = ref<ProcessInstanceView[]>([])
const handled = ref<OaTask[]>([])
async function load() {
  const [todoRows, myRows, handledRows] = await Promise.all([oaApi.todo(), oaApi.myInstances(), oaApi.handled()])
  todos.value = todoRows
  mine.value = myRows
  handled.value = handledRows
}
onMounted(load)
</script>

<template>
  <section>
    <div class="mobile-grid">
      <router-link class="mobile-action" to="/m/oa/start/inbound">物资入库</router-link>
      <router-link class="mobile-action" to="/m/oa/start/outbound">物品领用</router-link>
      <router-link class="mobile-action" to="/m/oa/start/reimbursement">报销 OA</router-link>
      <router-link class="mobile-action" to="/m/oa/todo">我的待办 {{ todos.length }}</router-link>
    </div>
    <div class="mobile-card">
      <div class="mobile-card-title">我发起的流程</div>
      <router-link v-for="item in mine.slice(0, 5)" :key="item.id" class="mobile-list-item" :to="`/m/oa/mine`">
        <div>{{ item.title }}</div>
        <div class="mobile-muted">{{ item.stage }}｜{{ item.currentHandler }}｜{{ item.status }}</div>
      </router-link>
      <div v-if="mine.length === 0" class="mobile-muted">暂无流程</div>
    </div>
    <div class="mobile-card">
      <div class="mobile-card-title">已办记录</div>
      <div class="mobile-muted">已处理 {{ handled.length }} 条</div>
    </div>
  </section>
</template>
