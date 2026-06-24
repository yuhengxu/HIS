<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { oaApi, type ProcessInstance } from '../../api/oa'
const rows = ref<ProcessInstance[]>([])
onMounted(async () => { rows.value = await oaApi.instances() })
</script>
<template><section class="page-section"><h1>我发起的流程</h1><el-table :data="rows" border><el-table-column prop="id" label="ID" /><el-table-column prop="processCode" label="流程" /><el-table-column prop="businessType" label="业务类型" /><el-table-column prop="status" label="状态" /></el-table></section></template>
