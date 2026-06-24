<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { inventoryApi, type Price } from '../../api/inventory'
const rows = ref<Price[]>([])
onMounted(async () => { rows.value = await inventoryApi.prices() })
</script>
<template><section class="page-section"><h1>价格记录</h1><el-table :data="rows" border><el-table-column prop="itemId" label="物资 ID" /><el-table-column prop="priceType" label="价格类型" /><el-table-column prop="price" label="价格" /><el-table-column prop="effectiveAt" label="生效时间" /></el-table></section></template>
