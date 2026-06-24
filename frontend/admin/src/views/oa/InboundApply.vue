<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import type { Item } from '../../api/inventory'
import { oaApi } from '../../api/oa'

const form = reactive({
  warehouseId: 1,
  itemId: undefined as number | undefined,
  quantity: 1,
  unitPrice: '0.60',
  newMaterial: null as Record<string, unknown> | null,
})
const keyword = ref('')
const items = ref<Item[]>([])
const useNewMaterial = ref(false)
const newMaterial = reactive({ name: '', unit: '', itemType: 'non_medical', defaultPrice: '0.00', code: '' })

async function searchItems() {
  items.value = await oaApi.searchMaterials(keyword.value)
}

async function submit() {
  const payload: Record<string, unknown> = {
    warehouseId: form.warehouseId,
    quantity: form.quantity,
    unitPrice: form.unitPrice,
  }
  if (useNewMaterial.value) {
    payload.newMaterial = { ...newMaterial }
  } else {
    payload.itemId = form.itemId
  }
  await oaApi.startInbound(payload)
  ElMessage.success('入库 OA 已发起')
}

onMounted(searchItems)
</script>

<template>
  <section class="page-section">
    <h1>发起入库 OA</h1>
    <el-form label-width="120px">
      <el-form-item label="仓库 ID"><el-input-number v-model="form.warehouseId" /></el-form-item>
      <el-form-item label="物资来源">
        <el-radio-group v-model="useNewMaterial">
          <el-radio :value="false">选择已有物资</el-radio>
          <el-radio :value="true">录入新物资</el-radio>
        </el-radio-group>
      </el-form-item>
      <template v-if="!useNewMaterial">
        <el-form-item label="搜索物资">
          <el-input v-model="keyword" placeholder="编码或名称" @keyup.enter="searchItems">
            <template #append><el-button @click="searchItems">搜索</el-button></template>
          </el-input>
        </el-form-item>
        <el-form-item label="选择物资">
          <el-select v-model="form.itemId" class="form-control">
            <el-option v-for="item in items" :key="item.id" :label="`${item.name} (${item.code})`" :value="item.id" />
          </el-select>
        </el-form-item>
      </template>
      <template v-else>
        <el-form-item label="物资名称"><el-input v-model="newMaterial.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="newMaterial.code" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="newMaterial.unit" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="newMaterial.itemType" class="form-control">
            <el-option label="非医疗物资" value="non_medical" />
            <el-option label="医疗物资" value="medical" />
          </el-select>
        </el-form-item>
        <el-form-item label="参考价"><el-input v-model="newMaterial.defaultPrice" /></el-form-item>
      </template>
      <el-form-item label="数量"><el-input-number v-model="form.quantity" /></el-form-item>
      <el-form-item label="单价"><el-input v-model="form.unitPrice" /></el-form-item>
      <el-button type="primary" @click="submit">提交</el-button>
    </el-form>
  </section>
</template>
