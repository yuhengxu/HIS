<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { inventoryApi, type Stock } from '../../api/inventory'

const rows = ref<Stock[]>([])
const loading = ref(false)
const keyword = ref('')
const dialogVisible = ref(false)
const form = reactive({ warehouseId: 1, itemId: 1, quantity: 0 })

async function load() {
  loading.value = true
  try {
    rows.value = await inventoryApi.stocks(keyword.value)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { warehouseId: 1, itemId: 1, quantity: 0 })
  dialogVisible.value = true
}

async function submit() {
  if (!form.warehouseId || !form.itemId) {
    ElMessage.warning('请填写仓库 ID 和物资 ID')
    return
  }
  await inventoryApi.createStock({
    warehouseId: Number(form.warehouseId),
    itemId: Number(form.itemId),
    quantity: Number(form.quantity),
  })
  ElMessage.success('库存已新增')
  dialogVisible.value = false
  await load()
}

load()
</script>

<template>
  <section class="page-section">
    <header class="section-head">
      <h1>库存余额</h1>
      <div class="section-actions">
        <el-input v-model="keyword" placeholder="按库存/仓库/物资模糊查询" clearable :prefix-icon="Search" @keyup.enter="load" @clear="load" />
        <el-button @click="load">查询</el-button>
        <el-button type="primary" @click="openCreate">新增库存</el-button>
      </div>
    </header>
    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="id" label="库存 ID" width="100" />
      <el-table-column prop="warehouseId" label="仓库 ID" />
      <el-table-column prop="itemId" label="物资 ID" />
      <el-table-column prop="quantity" label="数量" />
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增库存" width="520px">
      <el-form label-width="90px">
        <el-form-item label="仓库 ID"><el-input-number v-model="form.warehouseId" :min="1" :step="1" class="form-control" /></el-form-item>
        <el-form-item label="物资 ID"><el-input-number v-model="form.itemId" :min="1" :step="1" class="form-control" /></el-form-item>
        <el-form-item label="数量"><el-input-number v-model="form.quantity" :min="0" :precision="2" class="form-control" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
