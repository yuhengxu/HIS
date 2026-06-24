<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { authApi } from '../api/auth'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '' })

async function submit() {
  loading.value = true
  try {
    const user = await authApi.login(form)
    localStorage.setItem('his.currentUserId', String(user.userId))
    localStorage.setItem('his.currentUser', JSON.stringify(user))
    ElMessage.success('登录成功')
    router.push(String(route.query.redirect || '/'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-title">
        <h1>HIS + 康养 OA</h1>
        <p>请先登录后访问 OA 与物资管理</p>
      </div>
      <el-form @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="登录名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码" :prefix-icon="Lock" @keyup.enter="submit" />
        </el-form-item>
        <el-button class="login-button" type="primary" size="large" :loading="loading" @click="submit">登录</el-button>
      </el-form>
    </section>
  </main>
</template>
