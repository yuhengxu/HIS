<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { attachmentApi } from '../../api/attachment'
import { inventoryApi, type Item, type StockImage, type StockSummary, type StockView } from '../../api/inventory'

const rows = ref<StockView[]>([])
const summaryRows = ref<StockSummary[]>([])
const loading = ref(false)
const activeTab = ref('summary')
const keyword = ref('')
const dialogVisible = ref(false)
const adjustDialogVisible = ref(false)
const imageDialogVisible = ref(false)
const currentStock = ref<StockView | null>(null)
const stockImages = ref<StockImage[]>([])
const uploading = ref(false)
const form = reactive({ warehouseId: 1, itemId: undefined as number | undefined, quantity: 0 })
const adjustForm = reactive({ warehouseId: 1, itemId: undefined as number | undefined, quantity: 0, reason: '' })
const items = ref<Item[]>([])
const itemLoading = ref(false)

const warehouseOptions = [
  { label: '医疗物资', value: 1 },
  { label: '非医疗物资', value: 2 },
]

const canUpload = computed(() => {
  const raw = localStorage.getItem('his.currentUser')
  if (!raw) return false
  const roles = (JSON.parse(raw) as { roleCodes?: string[] }).roleCodes ?? []
  return roles.includes('SYSTEM_ADMIN') || roles.includes('INVENTORY_ADMIN')
})
const canAdjust = computed(() => {
  const raw = localStorage.getItem('his.currentUser')
  if (!raw) return false
  const roles = (JSON.parse(raw) as { roleCodes?: string[] }).roleCodes ?? []
  return roles.includes('SYSTEM_ADMIN')
})

async function load() {
  loading.value = true
  try {
    const [summary, detail] = await Promise.all([
      inventoryApi.stockSummary(keyword.value),
      inventoryApi.stocks(keyword.value),
    ])
    summaryRows.value = summary
    rows.value = detail
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { warehouseId: 1, itemId: undefined, quantity: 0 })
  dialogVisible.value = true
  loadItems()
}

function openAdjust() {
  Object.assign(adjustForm, { warehouseId: 1, itemId: undefined, quantity: 0, reason: '' })
  adjustDialogVisible.value = true
  loadItems()
}

async function loadItems(keyword = '') {
  itemLoading.value = true
  try {
    items.value = await inventoryApi.items(keyword)
  } finally {
    itemLoading.value = false
  }
}

async function submit() {
  if (!form.warehouseId || !form.itemId) {
    ElMessage.warning('请选择仓库和物资')
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

async function submitAdjust() {
  if (!adjustForm.warehouseId || !adjustForm.itemId) {
    ElMessage.warning('请选择仓库和物资')
    return
  }
  await inventoryApi.adjustStock({
    warehouseId: Number(adjustForm.warehouseId),
    itemId: Number(adjustForm.itemId),
    quantity: Number(adjustForm.quantity),
    reason: adjustForm.reason || '管理员直接调整',
  })
  ElMessage.success('库存已调整，流水已生成')
  adjustDialogVisible.value = false
  await load()
}

async function openImages(row: StockView) {
  currentStock.value = row
  stockImages.value = await inventoryApi.stockImages(row.id)
  imageDialogVisible.value = true
}

async function uploadStockImage(file: File) {
  if (!currentStock.value) return
  uploading.value = true
  try {
    const attachment = await attachmentApi.uploadFile(file, 'material_image')
    await inventoryApi.addStockImage(currentStock.value.id, { attachmentId: attachment.id, usageType: 'stock_scene' })
    stockImages.value = await inventoryApi.stockImages(currentStock.value.id)
    await load()
    ElMessage.success('库存现场图已上传')
  } finally {
    uploading.value = false
  }
}

async function removeStockImage(image: StockImage) {
  if (!currentStock.value) return
  await inventoryApi.deleteStockImage(currentStock.value.id, image.id)
  stockImages.value = await inventoryApi.stockImages(currentStock.value.id)
  await load()
}

function imageUsageText(usageType: string) {
  const map: Record<string, string> = {
    stock_scene: '现场图',
    main_image: '主图',
    gallery: '附图',
  }
  return map[usageType] ?? usageType
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
        <el-button v-if="canUpload" type="primary" @click="openCreate">新增库存</el-button>
        <el-button v-if="canAdjust" type="warning" @click="openAdjust">调整库存</el-button>
      </div>
    </header>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="物资库" name="summary">
        <el-table :data="summaryRows" v-loading="loading" border empty-text="暂无总库存">
          <el-table-column prop="warehouseName" label="仓库" width="160" />
          <el-table-column prop="itemName" label="物资名称" />
          <el-table-column prop="itemCode" label="物资编码" width="130" />
          <el-table-column prop="quantity" label="总数量" width="120" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="物资明细库" name="detail">
        <el-table :data="rows" v-loading="loading" border empty-text="暂无库存明细">
          <el-table-column prop="id" label="库存 ID" width="100" />
          <el-table-column label="物资图" width="90">
            <template #default="scope">
              <img v-if="scope.row.primaryImageUrl" :src="scope.row.primaryImageUrl" alt="" class="item-thumb" />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="itemName" label="物资名称" />
          <el-table-column prop="itemCode" label="物资编码" width="120" />
          <el-table-column prop="warehouseName" label="仓库" width="140" />
          <el-table-column prop="ownerName" label="库存录入人" width="130" />
          <el-table-column prop="quantity" label="数量" />
          <el-table-column label="现场图" width="90">
            <template #default="scope">{{ scope.row.stockImages?.length ?? 0 }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button link type="primary" @click="openImages(scope.row)">图片</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" title="新增库存" width="520px">
      <el-form label-width="90px">
        <el-form-item label="仓库">
          <el-select v-model="form.warehouseId" class="form-control">
            <el-option v-for="item in warehouseOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="物资">
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
        <el-form-item label="数量"><el-input-number v-model="form.quantity" :min="0" :precision="2" class="form-control" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adjustDialogVisible" title="调整库存" width="520px">
      <el-form label-width="90px">
        <el-form-item label="仓库">
          <el-select v-model="adjustForm.warehouseId" class="form-control">
            <el-option v-for="item in warehouseOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="物资">
          <el-select
            v-model="adjustForm.itemId"
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
        <el-form-item label="调整后数量"><el-input-number v-model="adjustForm.quantity" :min="0" :precision="2" class="form-control" /></el-form-item>
        <el-form-item label="原因"><el-input v-model="adjustForm.reason" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdjust">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="imageDialogVisible" :title="`库存图片 - ${currentStock?.itemName ?? ''}`" width="640px">
      <div v-if="canUpload" style="margin-bottom: 12px">
        <el-upload :auto-upload="false" :show-file-list="false" accept=".jpg,.jpeg,.png,.webp" @change="(f: any) => uploadStockImage(f.raw)">
          <el-button :loading="uploading">上传现场图</el-button>
        </el-upload>
      </div>
      <div class="image-grid">
        <div v-for="img in stockImages" :key="img.id" class="image-card">
          <img :src="img.url" alt="" />
          <div class="image-meta">
            <el-tag size="small">{{ imageUsageText(img.usageType) }}</el-tag>
            <el-button v-if="canUpload" link type="danger" @click="removeStockImage(img)">删除</el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<style scoped>
.item-thumb { width: 48px; height: 48px; object-fit: cover; border-radius: 4px; }
.image-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.image-card img { width: 100%; height: 120px; object-fit: cover; border-radius: 6px; }
.image-meta { margin-top: 6px; display: flex; justify-content: space-between; align-items: center; }
</style>
