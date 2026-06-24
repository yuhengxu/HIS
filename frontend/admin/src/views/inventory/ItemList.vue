<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { inventoryApi, type Item } from '../../api/inventory'

const rows = ref<Item[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = reactive({ code: '', name: '', itemType: 'non_medical', unit: '', latestPrice: 0 })

const canWrite = computed(() => {
  const raw = localStorage.getItem('his.currentUser')
  if (!raw) return false
  const roles = (JSON.parse(raw) as { roleCodes?: string[] }).roleCodes ?? []
  return roles.includes('SYSTEM_ADMIN') || roles.includes('INVENTORY_ADMIN')
})

async function load() {
  loading.value = true
  try {
    rows.value = await inventoryApi.items()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { code: '', name: '', itemType: 'non_medical', unit: '', latestPrice: 0 })
  dialogVisible.value = true
}

async function submit() {
  if (!form.code || !form.name || !form.unit) {
    ElMessage.warning('请填写编码、名称和单位')
    return
  }
  await inventoryApi.createItem({ ...form, latestPrice: Number(form.latestPrice) })
  ElMessage.success('物资档案已新增')
  dialogVisible.value = false
  await load()
}

load()
</script>

<template>
  <section class="page-section">
    <header class="section-head">
      <h1>物资档案</h1>
      <el-button v-if="canWrite" type="primary" @click="openCreate">新增物资</el-button>
    </header>
    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column label="图片" width="90">
        <template #default="scope">
          <img v-if="scope.row.primaryImageUrl" :src="scope.row.primaryImageUrl" alt="" class="item-thumb" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="code" label="编码" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="itemType" label="类型" />
      <el-table-column prop="unit" label="单位" width="100" />
      <el-table-column prop="latestPrice" label="最新价" width="120" />
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增物资档案" width="520px">
      <el-form label-width="90px">
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.itemType" class="form-control">
            <el-option label="非医疗物资" value="non_medical" />
            <el-option label="医疗物资" value="medical" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item label="最新价"><el-input-number v-model="form.latestPrice" :min="0" :precision="2" class="form-control" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.item-thumb { width: 48px; height: 48px; object-fit: cover; border-radius: 4px; }
</style>
