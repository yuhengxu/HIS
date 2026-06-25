<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { inventoryApi, type Item } from '../../api/inventory'
import { oaApi } from '../../api/oa'

const router = useRouter()
const warehouseOptions = [
  { label: '医疗物资', value: 1 },
  { label: '非医疗物资', value: 2 },
]

const form = reactive({
  materialMode: 'existing',
  warehouseId: 1,
  itemId: undefined as number | undefined,
  quantity: 1,
  newMaterial: {
    code: '',
    name: '',
    itemType: 'non_medical',
    category: '',
    specification: '',
    unit: '',
    supplier: '',
    defaultPrice: 0,
    createReason: '',
  },
})

const items = ref<Item[]>([])
const itemLoading = ref(false)
const selectedItem = computed(() => items.value.find((item) => item.id === form.itemId))
const unitPrice = computed(() => {
  if (form.materialMode === 'new') {
    return Number(form.newMaterial.defaultPrice || 0)
  }
  return Number(selectedItem.value?.latestPrice ?? 0)
})
const totalAmount = computed(() => (Number(form.quantity || 0) * unitPrice.value).toFixed(2))

async function loadItems(keyword = '') {
  itemLoading.value = true
  try {
    items.value = await inventoryApi.items(keyword)
  } finally {
    itemLoading.value = false
  }
}

async function submit() {
  if (form.materialMode === 'existing' && !form.itemId) {
    ElMessage.warning('请选择物资')
    return
  }
  if (form.materialMode === 'new' && (!form.newMaterial.name || !form.newMaterial.unit)) {
    ElMessage.warning('请填写新增物资名称和单位')
    return
  }
  const payload: Record<string, unknown> = {
    warehouseId: form.warehouseId,
    quantity: form.quantity,
    unitPrice: unitPrice.value,
    amount: Number(totalAmount.value),
  }
  if (form.materialMode === 'existing') {
    payload.itemId = form.itemId
  } else {
    payload.newMaterial = {
      code: form.newMaterial.code || undefined,
      name: form.newMaterial.name,
      itemType: form.newMaterial.itemType,
      category: form.newMaterial.category,
      specification: form.newMaterial.specification,
      unit: form.newMaterial.unit,
      supplier: form.newMaterial.supplier,
      defaultPrice: Number(form.newMaterial.defaultPrice || 0),
      createReason: form.newMaterial.createReason,
    }
  }
  await oaApi.startInbound(payload)
  ElMessage.success('入库 OA 已发起')
  router.push('/')
}

watch(() => form.itemId, (id) => {
  if (id && !selectedItem.value) loadItems(String(id))
})

onMounted(() => loadItems())
</script>

<template>
  <section class="page-section">
    <h1>发起入库 OA</h1>
    <el-form label-width="120px">
      <el-form-item label="仓库">
        <el-select v-model="form.warehouseId" class="form-control">
          <el-option v-for="item in warehouseOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="物资来源">
        <el-radio-group v-model="form.materialMode">
          <el-radio-button label="existing">选择已有物资</el-radio-button>
          <el-radio-button label="new">新增物资</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.materialMode === 'existing'" label="选择物资">
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
      <template v-else>
        <el-form-item label="物资编码"><el-input v-model="form.newMaterial.code" class="form-control" placeholder="不填则审批后自动生成" /></el-form-item>
        <el-form-item label="物资名称"><el-input v-model="form.newMaterial.name" class="form-control" /></el-form-item>
        <el-form-item label="物资类型">
          <el-select v-model="form.newMaterial.itemType" class="form-control">
            <el-option label="医疗物资" value="medical" />
            <el-option label="非医疗物资" value="non_medical" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类"><el-input v-model="form.newMaterial.category" class="form-control" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="form.newMaterial.specification" class="form-control" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.newMaterial.unit" class="form-control" /></el-form-item>
        <el-form-item label="供应商"><el-input v-model="form.newMaterial.supplier" class="form-control" /></el-form-item>
        <el-form-item label="新增原因"><el-input v-model="form.newMaterial.createReason" type="textarea" class="form-control" /></el-form-item>
      </template>
      <el-form-item label="数量"><el-input-number v-model="form.quantity" :min="1" :precision="2" /></el-form-item>
      <el-form-item :label="form.materialMode === 'new' ? '预估单价' : '单价'">
        <el-input-number v-if="form.materialMode === 'new'" v-model="form.newMaterial.defaultPrice" :min="0" :precision="2" class="form-control" />
        <span v-else>{{ unitPrice.toFixed(2) }}</span>
      </el-form-item>
      <el-form-item label="总价"><strong>{{ totalAmount }}</strong></el-form-item>
      <el-button type="primary" @click="submit">提交</el-button>
    </el-form>
  </section>
</template>
