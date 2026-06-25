<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { inventoryApi } from '../../api/inventory'
const rows = ref<Record<string, unknown>[]>([])
onMounted(async () => { rows.value = await inventoryApi.inboundOrders() })
function statusText(status: unknown) {
  const map: Record<string, string> = { approved: '已通过', pending: '待处理', rejected: '已驳回' }
  return map[String(status)] ?? String(status ?? '-')
}
</script>
<template><section class="page-section"><h1>入库单</h1><el-table :data="rows" border><el-table-column prop="orderNo" label="单号" /><el-table-column prop="oaInstanceId" label="OA 实例" /><el-table-column prop="amount" label="金额" /><el-table-column label="状态"><template #default="scope">{{ statusText(scope.row.status) }}</template></el-table-column></el-table></section></template>
