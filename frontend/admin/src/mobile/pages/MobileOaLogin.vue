<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { saveMobileSession, wecomApi } from '../../api/wecom'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const manualCode = ref('mock:employee.wecom')

async function login(code: string) {
  loading.value = true
  error.value = ''
  try {
    const user = await wecomApi.login(code)
    saveMobileSession(user)
    ElMessage.success('企业微信登录成功')
    router.replace(String(route.query.redirect || '/m/oa'))
  } catch (e) {
    error.value = e instanceof Error ? e.message : '企业微信登录失败'
  } finally {
    loading.value = false
  }
}

async function redirectToWeCom() {
  const { url } = await wecomApi.authUrl(String(route.query.redirect || '/m/oa'))
  window.location.href = url
}

onMounted(() => {
  const code = route.query.code
  if (typeof code === 'string' && code) login(code)
})
</script>

<template>
  <main class="mobile-shell">
    <section class="mobile-main">
      <div class="mobile-card">
        <div class="mobile-card-title">企业微信登录</div>
        <p class="mobile-muted">从企业微信工作台进入时会自动携带授权 code。本地测试可使用 mock code。</p>
        <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
        <el-button class="mobile-primary" type="primary" :loading="loading" @click="redirectToWeCom">企业微信授权登录</el-button>
      </div>
      <div class="mobile-card">
        <div class="mobile-card-title">本地联调</div>
        <el-input v-model="manualCode" placeholder="mock:employee.wecom" />
        <el-button class="mobile-primary" style="margin-top: 10px" :loading="loading" @click="login(manualCode)">使用 mock code 登录</el-button>
      </div>
    </section>
  </main>
</template>
