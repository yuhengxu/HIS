<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { oaApi } from '../../api/oa'
import type { Item } from '../../api/inventory'
const router = useRouter()
const warehouseOptions = [{ label: '医疗物资', value: 1 }, { label: '非医疗物资', value: 2 }]
const form = reactive({ materialMode: 'existing', warehouseId: 2, itemId: undefined as number | undefined, quantity: 1, newMaterial: { name: '', itemType: 'non_medical', unit: '', defaultPrice: 0 } })
const items = ref<Item[]>([])
const loading = ref(false)
const selectedItem = computed(() => items.value.find(i => i.id === form.itemId))
const unitPrice = computed(() => form.materialMode === 'new' ? Number(form.newMaterial.defaultPrice || 0) : Number(selectedItem.value?.latestPrice ?? 0))
const amount = computed(() => Number(form.quantity || 0) * unitPrice.value)
async function loadItems(keyword = '') { loading.value = true; try { items.value = await oaApi.searchInboundMaterials(form.warehouseId, keyword) } finally { loading.value = false } }
async function submit() {
  if (form.materialMode === 'existing' && !form.itemId) return ElMessage.warning('请选择物资')
  if (form.materialMode === 'new' && (!form.newMaterial.name || !form.newMaterial.unit)) return ElMessage.warning('请填写新增物资')
  const payload: Record<string, unknown> = { warehouseId: form.warehouseId, quantity: form.quantity, unitPrice: unitPrice.value, amount: amount.value }
  if (form.materialMode === 'existing') payload.itemId = form.itemId
  else payload.newMaterial = { ...form.newMaterial, defaultPrice: Number(form.newMaterial.defaultPrice || 0) }
  await oaApi.startInbound(payload)
  ElMessage.success('入库 OA 已发起')
  router.push('/m/oa/mine')
}
watch(() => form.warehouseId, () => { form.itemId = undefined; form.newMaterial.itemType = form.warehouseId === 1 ? 'medical' : 'non_medical'; loadItems() })
onMounted(() => loadItems())
</script>
<template>
  <section class="mobile-card mobile-form">
    <div class="mobile-card-title">发起物资入库</div>
    <el-form label-position="top">
      <el-form-item label="仓库"><el-select v-model="form.warehouseId"><el-option v-for="item in warehouseOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item label="物资来源"><el-radio-group v-model="form.materialMode"><el-radio-button label="existing">选择已有</el-radio-button><el-radio-button label="new">新增物资</el-radio-button></el-radio-group></el-form-item>
      <el-form-item v-if="form.materialMode === 'existing'" label="选择物资"><el-select v-model="form.itemId" filterable remote reserve-keyword :remote-method="loadItems" :loading="loading"><el-option v-for="item in items" :key="item.id" :label="`${item.name} (${item.code})`" :value="item.id" /></el-select></el-form-item>
      <template v-else>
        <el-form-item label="物资名称"><el-input v-model="form.newMaterial.name" /></el-form-item>
        <el-form-item label="物资类型"><el-select v-model="form.newMaterial.itemType" disabled><el-option label="医疗物资" value="medical" /><el-option label="非医疗物资" value="non_medical" /></el-select></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.newMaterial.unit" placeholder="例如：个、张、克、千克" /></el-form-item>
        <el-form-item label="单价"><el-input-number v-model="form.newMaterial.defaultPrice" :min="0" :precision="2" /></el-form-item>
      </template>
      <el-form-item label="数量"><el-input-number v-model="form.quantity" :min="1" :precision="2" /></el-form-item>
      <div class="mobile-card"><div>单价：{{ unitPrice.toFixed(2) }}</div><div>总价：{{ amount.toFixed(2) }}</div></div>
      <el-button class="mobile-primary" type="primary" @click="submit">提交入库 OA</el-button>
    </el-form>
  </section>
</template>
