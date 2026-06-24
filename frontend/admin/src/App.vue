<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Box, DataLine, Document, OfficeBuilding, SwitchButton, User } from '@element-plus/icons-vue'
import { clearSession } from './api/http'

const route = useRoute()
const router = useRouter()
const isLoginPage = computed(() => route.path === '/login')

type SessionUser = { displayName: string; username: string; roleCodes?: string[] }

const currentUser = computed<SessionUser | null>(() => {
  const raw = localStorage.getItem('his.currentUser')
  return raw ? JSON.parse(raw) as SessionUser : null
})

const allMenuItems = [
  { index: '/', label: '工作台', icon: DataLine },
  { index: '/iam/users', label: '用户管理', icon: User, roles: ['SYSTEM_ADMIN'] },
  { index: '/iam/roles', label: '角色权限', icon: User, roles: ['SYSTEM_ADMIN'] },
  { index: '/oa/start', label: '发起流程', icon: Document },
  { index: '/oa/processes', label: 'OA 流程定义', icon: OfficeBuilding, roles: ['SYSTEM_ADMIN', 'OA_ADMIN'] },
  { index: '/oa/todo', label: '我的待办', icon: OfficeBuilding },
  { index: '/inventory/items', label: '物资档案', icon: Box },
  { index: '/inventory/stocks', label: '库存查询', icon: Box },
]

const menuItems = computed(() => allMenuItems.filter((item) => {
  if (!item.roles) return true
  return item.roles.some((role) => currentUser.value?.roleCodes?.includes(role))
}))

function logout() {
  clearSession()
  router.push('/login')
}
</script>

<template>
  <router-view v-if="isLoginPage" />
  <el-container v-else class="shell">
    <el-aside width="232px" class="sidebar">
      <div class="brand">HIS + 康养</div>
      <el-menu router :default-active="route.path" class="menu">
        <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <strong>OA 增强版</strong>
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
