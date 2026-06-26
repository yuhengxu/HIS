<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { SwitchButton } from '@element-plus/icons-vue'
import { clearSession } from './api/http'
import { systemApi, type MenuTreeNode } from './api/system'
import SidebarMenuNode from './components/SidebarMenuNode.vue'

const route = useRoute()
const router = useRouter()
const isLoginPage = computed(() => route.path === '/login')
const isMobilePage = computed(() => route.path.startsWith('/m/'))

type SessionUser = { displayName: string; username: string; roleCodes?: string[]; permissions?: string[] }

function readCurrentUser() {
  const raw = localStorage.getItem('his.currentUser')
  return raw ? JSON.parse(raw) as SessionUser : null
}

const currentUser = ref<SessionUser | null>(readCurrentUser())
const serverMenus = ref<MenuTreeNode[]>([])

watch(() => route.fullPath, async () => {
  currentUser.value = readCurrentUser()
  if (!isLoginPage.value && currentUser.value) {
    await loadMenus()
  }
})

async function loadMenus() {
  try {
    serverMenus.value = await systemApi.myMenus()
  } catch {
    serverMenus.value = []
  }
}

function logout() {
  clearSession()
  currentUser.value = null
  serverMenus.value = []
  router.push('/login')
}

onMounted(() => {
  if (!isLoginPage.value && currentUser.value) {
    loadMenus()
  }
})
</script>

<template>
  <router-view v-if="isLoginPage || isMobilePage" />
  <el-container v-else class="shell">
    <el-aside width="232px" class="sidebar">
      <div class="brand">和悦医养</div>
      <el-menu router :default-active="route.path" class="menu">
        <SidebarMenuNode v-for="node in serverMenus" :key="node.menu.id" :node="node" />
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <strong>OA 管理</strong>
          <span>用户权限、流程发起、物资图片与报销凭证</span>
        </div>
        <div class="user-box">
          <el-tag type="success">{{ currentUser?.displayName ?? currentUser?.username }}</el-tag>
          <el-button :icon="SwitchButton" text @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
