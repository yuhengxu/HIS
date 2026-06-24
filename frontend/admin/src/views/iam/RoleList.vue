<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { iamApi, type RoleRecord } from '../../api/iam'

const roles = ref<RoleRecord[]>([])
onMounted(async () => { roles.value = await iamApi.roles() })
</script>

<template>
  <section class="page-section">
    <header class="section-head"><h1>角色权限</h1></header>
    <el-table :data="roles" border>
      <el-table-column prop="code" label="角色编码" width="190" />
      <el-table-column prop="name" label="角色名称" width="160" />
      <el-table-column prop="description" label="说明" />
      <el-table-column label="权限"><template #default="scope"><el-tag v-for="p in scope.row.permissionCodes" :key="p" class="tag">{{ p }}</el-tag></template></el-table-column>
    </el-table>
  </section>
</template>
