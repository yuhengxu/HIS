<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Box, DataLine, Lock, OfficeBuilding, SwitchButton, User } from '@element-plus/icons-vue'
import { clearSession } from './api/http'

const route = useRoute()
const router = useRouter()
const isLoginPage = computed(() => route.path === '/login')
const currentUser = computed(() => {
  const raw = localStorage.getItem('his.currentUser')
  return raw ? JSON.parse(raw) as { displayName: string; username: string } : null
})

const menuItems = [
  { index: '/', label: '工作台', icon: DataLine },
  { index: '/iam/users', label: '用户管理', icon: User },
  { index: '/iam/roles', label: '角色权限', icon: Lock },
  { index: '/oa/processes', label: 'OA 流程', icon: OfficeBuilding },
  { index: '/oa/todo', label: '我的待办', icon: OfficeBuilding },
  { index: '/inventory/items', label: '物资档案', icon: Box },
  { index: '/inventory/stocks', label: '库存查询', icon: Box },
]

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
      <el-menu router default-active="/" class="menu">
        <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <strong>OA 第一阶段</strong>
          <span>用户权限、审批流、物资库存闭环</span>
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
