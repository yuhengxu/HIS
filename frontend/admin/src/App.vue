<script setup lang="ts">
import { Box, DataLine, Lock, OfficeBuilding, User } from '@element-plus/icons-vue'

type ModuleCard = {
  code: string
  name: string
  summary: string
  icon: typeof User
  permissions: string[]
}

const modules: ModuleCard[] = [
  {
    code: 'iam',
    name: '用户管理',
    summary: '统一账户、角色、权限点、用户单独授权',
    icon: User,
    permissions: ['iam:user:read', 'iam:role:read', 'iam:user-permission:write'],
  },
  {
    code: 'oa',
    name: 'OA 基座',
    summary: '基础 OA 访问权限，预留审批、通知、公告能力',
    icon: OfficeBuilding,
    permissions: ['oa:access', 'oa:notice:read'],
  },
  {
    code: 'inventory',
    name: '物资管理',
    summary: '物资档案、库存、入库、出库、盘点',
    icon: Box,
    permissions: ['inventory:item:read', 'inventory:stock:read', 'inventory:inbound:write'],
  },
  {
    code: 'audit',
    name: '审计与 AI 边界',
    summary: '操作日志、权限变更日志、AI 调用白名单',
    icon: Lock,
    permissions: ['audit:read', 'ai-access:read'],
  },
]

const inventoryStats = [
  { label: '物资分类', value: '待接入' },
  { label: '库存查询', value: '只读接口' },
  { label: 'AI 调用', value: '白名单' },
]
</script>

<template>
  <el-container class="shell">
    <el-aside width="232px" class="sidebar">
      <div class="brand">HIS + 康养</div>
      <el-menu default-active="dashboard" class="menu">
        <el-menu-item index="dashboard">
          <el-icon><DataLine /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="iam">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="inventory">
          <el-icon><Box /></el-icon>
          <span>物资管理</span>
        </el-menu-item>
        <el-menu-item index="audit">
          <el-icon><Lock /></el-icon>
          <span>审计</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <strong>OA 基座与物资管理</strong>
          <span>模块化单体首期工程骨架</span>
        </div>
        <el-tag type="success">PostgreSQL 16 / Redis 7 / Spring Boot 3</el-tag>
      </el-header>

      <el-main class="content">
        <section class="module-grid">
          <article v-for="item in modules" :key="item.code" class="module-card">
            <div class="module-head">
              <el-icon size="24"><component :is="item.icon" /></el-icon>
              <h2>{{ item.name }}</h2>
            </div>
            <p>{{ item.summary }}</p>
            <div class="permission-list">
              <el-tag v-for="permission in item.permissions" :key="permission" effect="plain">
                {{ permission }}
              </el-tag>
            </div>
          </article>
        </section>

        <section class="inventory-panel">
          <div>
            <h2>物资管理首期范围</h2>
            <p>物资分类、档案、供应商、仓库、库存、入库、出库、盘点与库存流水。</p>
          </div>
          <div class="stats">
            <div v-for="stat in inventoryStats" :key="stat.label" class="stat-cell">
              <span>{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
            </div>
          </div>
        </section>
      </el-main>
    </el-container>
  </el-container>
</template>
