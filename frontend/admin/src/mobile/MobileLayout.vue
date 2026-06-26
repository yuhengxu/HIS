<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { clearSession } from '../api/http'

const router = useRouter()
const currentUser = computed(() => {
  const raw = localStorage.getItem('his.currentUser')
  return raw ? JSON.parse(raw) as { displayName?: string; username?: string } : null
})
function logout() {
  clearSession()
  router.push('/m/oa/login')
}
</script>

<template>
  <div class="mobile-shell">
    <header class="mobile-topbar">
      <div>
        <div class="mobile-title">和悦医养 OA</div>
        <div class="mobile-user">{{ currentUser?.displayName ?? currentUser?.username ?? '企业微信用户' }}</div>
      </div>
      <el-button text @click="logout">退出</el-button>
    </header>
    <main class="mobile-main"><router-view /></main>
    <nav class="mobile-bottom">
      <router-link to="/m/oa">首页</router-link>
      <router-link to="/m/oa/start">发起</router-link>
      <router-link to="/m/oa/todo">待办</router-link>
      <router-link to="/m/oa/mine">我发起</router-link>
    </nav>
  </div>
</template>
