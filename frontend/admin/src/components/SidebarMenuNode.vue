<script setup lang="ts">
import { Box, DataLine, Document, Menu as MenuIcon, OfficeBuilding, Setting, Tickets, User } from '@element-plus/icons-vue'
import type { MenuTreeNode } from '../api/system'

defineOptions({ name: 'SidebarMenuNode' })
defineProps<{ node: MenuTreeNode }>()

const iconMap: Record<string, unknown> = {
  Box,
  DataLine,
  Document,
  Menu: MenuIcon,
  OfficeBuilding,
  Setting,
  Tickets,
  User,
}

function menuIndex(node: MenuTreeNode) {
  return node.menu.path || `menu-${node.menu.id}`
}

function menuIcon(node: MenuTreeNode) {
  return iconMap[node.menu.icon ?? ''] ?? Document
}
</script>

<template>
  <el-sub-menu v-if="node.children.length" :index="menuIndex(node)">
    <template #title>
      <el-icon><component :is="menuIcon(node)" /></el-icon>
      <span>{{ node.menu.name }}</span>
    </template>
    <SidebarMenuNode v-for="child in node.children" :key="child.menu.id" :node="child" />
  </el-sub-menu>
  <el-menu-item v-else :index="node.menu.path">
    <el-icon><component :is="menuIcon(node)" /></el-icon>
    <span>{{ node.menu.name }}</span>
  </el-menu-item>
</template>
