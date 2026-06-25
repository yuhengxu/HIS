<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { type Item } from '../../api/inventory'
import { oaApi } from '../../api/oa'

const router = useRouter()
const warehouseOptions = [
  { label: '医疗物资', value: 1 },
  { label: '非医疗物资', value: 2 },
]

const form = reactive({
  warehouseId: 2,
  itemId: undefined as number | undefined,
  quantity: 1,
})

const items = ref<Item[]>([])
const itemLoading = ref(false)
const selectedItem = computed(() => items.value.find((item) => item.id === form.itemId))
const unitPrice = computed(() => Number(selectedItem.value?.latestPrice ?? 0))
const totalAmount = computed(() => (Number(form.quantity || 0) * unitPrice.value).toFixed(2))

async function loadItems(keyword = '') {
  itemLoading.value = true
  try {
    items.value = await oaApi.searchClaimableMaterials(form.warehouseId, keyword)
  } finally {
    itemLoading.value = false
  }
}

async function submit() {
  if (!form.itemId) {
    ElMessage.warning('请选择物资')
    return
  }
  await oaApi.startOutbound({
    warehouseId: form.warehouseId,
    itemId: form.itemId,
    quantity: form.quantity,
    unitPrice: unitPrice.value,
    amount: Number(totalAmount.value),
  })
  ElMessage.success('物品领用已发起')
  router.push('/')
}

watch(() => form.warehouseId, () => {
  form.itemId = undefined
  loadItems()
})

onMounted(() => loadItems())
</script>

<template>
  <section class="page-section">
    <h1>物品领用</h1>
    <el-form label-width="120px">
      <el-form-item label="仓库">
        <el-select v-model="form.warehouseId" class="form-control">
          <el-option v-for="item in warehouseOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="选择物资">
        <el-select
          v-model="form.itemId"
          class="form-control"
          filterable
          remote
          reserve-keyword
          :remote-method="loadItems"
          :loading="itemLoading"
          placeholder="输入编码或名称查询"
        >
          <el-option v-for="item in items" :key="item.id" :label="`${item.name} (${item.code})`" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="数量"><el-input-number v-model="form.quantity" :min="1" :precision="2" /></el-form-item>
      <el-form-item label="单价"><span>{{ unitPrice.toFixed(2) }}</span></el-form-item>
      <el-form-item label="总价"><strong>{{ totalAmount }}</strong></el-form-item>
      <el-button type="primary" @click="submit">提交</el-button>
    </el-form>
  </section>
</template>
