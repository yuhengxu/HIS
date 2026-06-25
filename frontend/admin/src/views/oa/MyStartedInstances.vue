<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { oaApi, type ProcessInstance } from '../../api/oa'
const rows = ref<ProcessInstance[]>([])
onMounted(async () => { rows.value = await oaApi.instances() })
function statusText(status: string) {
  const map: Record<string, string> = { RUNNING: '审批中', APPROVED: '已结束', REJECTED: '已驳回', PENDING_CONFIG: '待配置' }
  return map[status] ?? status
}
function businessTypeText(type: string) {
  const map: Record<string, string> = { inventory_inbound: '物资入库', inventory_outbound: '物品领用', reimbursement: '报销' }
  return map[type] ?? type
}
</script>
<template><section class="page-section"><h1>我发起的流程</h1><el-table :data="rows" border><el-table-column prop="id" label="ID" /><el-table-column prop="title" label="流程" /><el-table-column label="业务类型"><template #default="scope">{{ businessTypeText(scope.row.businessType) }}</template></el-table-column><el-table-column label="状态"><template #default="scope">{{ statusText(scope.row.status) }}</template></el-table-column></el-table></section></template>
