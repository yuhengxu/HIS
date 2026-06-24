<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { iamApi, type PermissionRecord } from '../../api/iam'

const permissions = ref<PermissionRecord[]>([])
onMounted(async () => { permissions.value = await iamApi.permissions() })
</script>

<template>
  <section class="page-section">
    <header class="section-head"><h1>权限点矩阵</h1></header>
    <el-table :data="permissions" border>
      <el-table-column prop="domain" label="权限域" width="140" />
      <el-table-column prop="code" label="权限点" width="260" />
      <el-table-column prop="name" label="说明" />
    </el-table>
  </section>
</template>
