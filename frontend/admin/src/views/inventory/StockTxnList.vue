<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { inventoryApi, type StockTxn } from '../../api/inventory'

const rows = ref<StockTxn[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    rows.value = await inventoryApi.transactions()
  } finally {
    loading.value = false
  }
}

function formatTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(load)
</script>

<template>
  <section class="page-section">
    <header class="section-head">
      <h1>库存流水</h1>
      <el-button @click="load">刷新</el-button>
    </header>
    <el-table :data="rows" v-loading="loading" border empty-text="暂无库存流水">
      <el-table-column prop="id" label="流水 ID" width="100" />
      <el-table-column prop="txnTypeName" label="类型" width="90" />
      <el-table-column prop="bizNo" label="业务单号" width="140" />
      <el-table-column prop="warehouseName" label="仓库" width="140" />
      <el-table-column prop="itemName" label="物资名称" />
      <el-table-column prop="itemCode" label="物资编码" width="120" />
      <el-table-column prop="quantityDelta" label="变动数量" width="120" />
      <el-table-column prop="operatorName" label="录入人" width="130" />
      <el-table-column label="发生时间" width="180">
        <template #default="scope">{{ formatTime(scope.row.occurredAt) }}</template>
      </el-table-column>
    </el-table>
  </section>
</template>
