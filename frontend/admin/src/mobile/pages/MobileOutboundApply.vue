<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { oaApi } from '../../api/oa'
import type { Item } from '../../api/inventory'
const router = useRouter()
const warehouseOptions = [{ label: '医疗物资', value: 1 }, { label: '非医疗物资', value: 2 }]
const form = reactive({ warehouseId: 2, itemId: undefined as number | undefined, quantity: 1 })
const items = ref<Item[]>([])
const loading = ref(false)
const selectedItem = computed(() => items.value.find(i => i.id === form.itemId))
const unitPrice = computed(() => Number(selectedItem.value?.latestPrice ?? 0))
const amount = computed(() => Number(form.quantity || 0) * unitPrice.value)
async function loadItems(keyword = '') { loading.value = true; try { items.value = await oaApi.searchClaimableMaterials(form.warehouseId, keyword) } finally { loading.value = false } }
async function submit() {
  if (!form.itemId) return ElMessage.warning('请选择物资')
  await oaApi.startOutbound({ warehouseId: form.warehouseId, itemId: form.itemId, quantity: form.quantity, unitPrice: unitPrice.value, amount: amount.value })
  ElMessage.success('物品领用已发起')
  router.push('/m/oa/mine')
}
watch(() => form.warehouseId, () => { form.itemId = undefined; loadItems() })
onMounted(() => loadItems())
</script>
<template>
  <section class="mobile-card mobile-form">
    <div class="mobile-card-title">发起物品领用</div>
    <el-form label-position="top">
      <el-form-item label="仓库"><el-select v-model="form.warehouseId"><el-option v-for="item in warehouseOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item label="选择物资"><el-select v-model="form.itemId" filterable remote reserve-keyword :remote-method="loadItems" :loading="loading"><el-option v-for="item in items" :key="item.id" :label="`${item.name} (${item.code})`" :value="item.id" /></el-select></el-form-item>
      <el-form-item label="数量"><el-input-number v-model="form.quantity" :min="1" :precision="2" /></el-form-item>
      <div class="mobile-card"><div>单价：{{ unitPrice.toFixed(2) }}</div><div>总价：{{ amount.toFixed(2) }}</div></div>
      <el-button class="mobile-primary" type="primary" @click="submit">提交领用 OA</el-button>
    </el-form>
  </section>
</template>
