<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { inventoryApi, type Warehouse } from '../../api/inventory'
const rows = ref<Warehouse[]>([])
onMounted(async () => { rows.value = await inventoryApi.warehouses() })
</script>
<template><section class="page-section"><h1>仓库</h1><el-table :data="rows" border><el-table-column prop="id" label="ID" /><el-table-column prop="code" label="编码" /><el-table-column prop="name" label="名称" /><el-table-column prop="warehouseType" label="类型" /></el-table></section></template>
