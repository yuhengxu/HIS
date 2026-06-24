<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { inventoryApi } from '../../api/inventory'
const rows = ref<Record<string, unknown>[]>([])
onMounted(async () => { rows.value = await inventoryApi.outboundOrders() })
</script>
<template><section class="page-section"><h1>出库单</h1><el-table :data="rows" border><el-table-column prop="orderNo" label="单号" /><el-table-column prop="oaInstanceId" label="OA 实例" /><el-table-column prop="amount" label="金额" /><el-table-column prop="status" label="状态" /></el-table></section></template>
