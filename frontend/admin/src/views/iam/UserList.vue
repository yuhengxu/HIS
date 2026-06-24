<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { iamApi, type UserRecord } from '../../api/iam'

const users = ref<UserRecord[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try { users.value = await iamApi.users() } finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <section class="page-section">
    <header class="section-head"><h1>用户管理</h1><el-button type="primary">新建用户</el-button></header>
    <el-table :data="users" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="登录名" />
      <el-table-column prop="displayName" label="姓名" />
      <el-table-column prop="enabled" label="状态" width="100" />
      <el-table-column label="角色"><template #default="scope">{{ scope.row.roleCodes?.join(', ') }}</template></el-table-column>
    </el-table>
  </section>
</template>
