<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { attachmentApi } from '../../api/attachment'
import { inventoryApi, type Item, type ItemImage } from '../../api/inventory'

const rows = ref<Item[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const tagDialogVisible = ref(false)
const imageDialogVisible = ref(false)
const currentItem = ref<Item | null>(null)
const itemImages = ref<ItemImage[]>([])
const uploading = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const form = reactive({ id: undefined as number | undefined, code: '', name: '', itemType: 'non_medical', unit: '', latestPrice: 0, materialTag: 'NORMAL' })
const tagForm = reactive({ materialTag: 'NORMAL' })

const canWrite = computed(() => {
  const raw = localStorage.getItem('his.currentUser')
  if (!raw) return false
  const roles = (JSON.parse(raw) as { roleCodes?: string[] }).roleCodes ?? []
  return roles.includes('SYSTEM_ADMIN') || roles.includes('INVENTORY_ADMIN')
})
const canManageTag = computed(() => {
  const raw = localStorage.getItem('his.currentUser')
  if (!raw) return false
  const roles = (JSON.parse(raw) as { roleCodes?: string[] }).roleCodes ?? []
  return roles.includes('SYSTEM_ADMIN')
})
const dialogTitle = computed(() => formMode.value === 'edit' ? '编辑物资档案' : '新增物资档案')

async function load() {
  loading.value = true
  try {
    rows.value = await inventoryApi.items()
  } finally {
    loading.value = false
  }
}

function itemTypeText(type: string) {
  const map: Record<string, string> = {
    medical: '医疗物资',
    non_medical: '非医疗物资',
  }
  return map[type] ?? type
}

function resetForm() {
  Object.assign(form, { id: undefined, code: '', name: '', itemType: 'non_medical', unit: '', latestPrice: 0, materialTag: 'NORMAL' })
}

function openCreate() {
  formMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(item: Item) {
  formMode.value = 'edit'
  Object.assign(form, {
    id: item.id,
    code: item.code,
    name: item.name,
    itemType: item.itemType,
    unit: item.unit,
    latestPrice: item.latestPrice,
    materialTag: item.materialTag ?? 'NORMAL',
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.code || !form.name || !form.unit) {
    ElMessage.warning('请填写编码、名称和单位')
    return
  }
  const payload = {
    code: form.code,
    name: form.name,
    itemType: form.itemType,
    unit: form.unit,
    latestPrice: Number(form.latestPrice),
    ...(canManageTag.value ? { materialTag: form.materialTag } : {}),
  }
  if (formMode.value === 'edit') {
    if (!form.id) return
    await inventoryApi.updateItem(form.id, payload)
    ElMessage.success('物资档案已修改')
  } else {
    await inventoryApi.createItem(payload)
    ElMessage.success('物资档案已新增')
  }
  dialogVisible.value = false
  await load()
}

async function removeItem(item: Item) {
  await ElMessageBox.confirm(`确定删除物资档案「${item.name}」吗？已有库存或流水引用的物资不能删除。`, '删除物资档案', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await inventoryApi.deleteItem(item.id)
  ElMessage.success('物资档案已删除')
  await load()
}

function materialTagText(tag?: string) {
  return tag === 'SPECIAL' ? '特殊物资' : '普通物资'
}

function openTagDialog(item: Item) {
  currentItem.value = item
  tagForm.materialTag = item.materialTag ?? 'NORMAL'
  tagDialogVisible.value = true
}

async function saveTag() {
  if (!currentItem.value) return
  await inventoryApi.updateItem(currentItem.value.id, {
    code: currentItem.value.code,
    name: currentItem.value.name,
    itemType: currentItem.value.itemType,
    unit: currentItem.value.unit,
    latestPrice: currentItem.value.latestPrice,
    materialTag: tagForm.materialTag,
  })
  ElMessage.success('物资标签已更新')
  tagDialogVisible.value = false
  await load()
}

async function openImages(item: Item) {
  currentItem.value = item
  itemImages.value = await inventoryApi.itemImages(item.id)
  imageDialogVisible.value = true
}

async function uploadImage(file: File, primary: boolean) {
  if (!currentItem.value) return
  uploading.value = true
  try {
    const attachment = await attachmentApi.uploadFile(file, 'material_image', 'INVENTORY_ITEM', currentItem.value.id)
    await inventoryApi.addItemImage(currentItem.value.id, { attachmentId: attachment.id, primary })
    itemImages.value = await inventoryApi.itemImages(currentItem.value.id)
    await load()
    ElMessage.success('图片已上传')
  } finally {
    uploading.value = false
  }
}

async function setMain(image: ItemImage) {
  if (!currentItem.value) return
  await inventoryApi.setMainItemImage(currentItem.value.id, image.id)
  itemImages.value = await inventoryApi.itemImages(currentItem.value.id)
  await load()
}

async function removeImage(image: ItemImage) {
  if (!currentItem.value) return
  await inventoryApi.deleteItemImage(currentItem.value.id, image.id)
  itemImages.value = await inventoryApi.itemImages(currentItem.value.id)
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
      <el-table-column label="类型">
        <template #default="scope">{{ itemTypeText(scope.row.itemType) }}</template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="100" />
      <el-table-column prop="latestPrice" label="单价" width="120" />
      <el-table-column v-if="canManageTag" label="标签" width="110">
        <template #default="scope">{{ materialTagText(scope.row.materialTag) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="scope">
          <el-button link type="primary" @click="openImages(scope.row)">图片</el-button>
          <el-button v-if="canWrite" link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button v-if="canManageTag" link @click="openTagDialog(scope.row)">标签</el-button>
          <el-button v-if="canWrite" link type="danger" @click="removeItem(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form label-width="90px">
        <el-form-item v-if="formMode === 'edit'" label="ID"><el-input :model-value="form.id" disabled /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.itemType" class="form-control">
            <el-option label="非医疗物资" value="non_medical" />
            <el-option label="医疗物资" value="medical" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" placeholder="例如：个、张、克、千克" /></el-form-item>
        <el-form-item label="单价"><el-input-number v-model="form.latestPrice" :min="0" :precision="2" class="form-control" /></el-form-item>
        <el-form-item v-if="canManageTag" label="标签">
          <el-select v-model="form.materialTag" class="form-control">
            <el-option label="普通物资" value="NORMAL" />
            <el-option label="特殊物资" value="SPECIAL" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tagDialogVisible" title="修改物资标签" width="420px">
      <el-form label-width="90px">
        <el-form-item label="物资">{{ currentItem?.name }}</el-form-item>
        <el-form-item label="标签">
          <el-select v-model="tagForm.materialTag" class="form-control">
            <el-option label="普通物资" value="NORMAL" />
            <el-option label="特殊物资" value="SPECIAL" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTag">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="imageDialogVisible" :title="`物资图片 - ${currentItem?.name ?? ''}`" width="640px">
      <div v-if="canWrite" class="upload-row">
        <el-upload :auto-upload="false" :show-file-list="false" accept=".jpg,.jpeg,.png,.webp" @change="(f: any) => uploadImage(f.raw, false)">
          <el-button :loading="uploading">上传附图</el-button>
        </el-upload>
        <el-upload :auto-upload="false" :show-file-list="false" accept=".jpg,.jpeg,.png,.webp" @change="(f: any) => uploadImage(f.raw, true)">
          <el-button :loading="uploading">上传主图</el-button>
        </el-upload>
      </div>
      <div class="image-grid">
        <div v-for="img in itemImages" :key="img.id" class="image-card">
          <img :src="img.url" alt="" />
          <div class="image-meta">
            <el-tag size="small">{{ img.usageType }}</el-tag>
            <div v-if="canWrite" class="image-actions">
              <el-button link @click="setMain(img)">设主图</el-button>
              <el-button link type="danger" @click="removeImage(img)">删除</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<style scoped>
.item-thumb { width: 48px; height: 48px; object-fit: cover; border-radius: 4px; }
.upload-row { display: flex; gap: 8px; margin-bottom: 12px; }
.image-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.image-card img { width: 100%; height: 120px; object-fit: cover; border-radius: 6px; }
.image-meta { margin-top: 6px; display: flex; justify-content: space-between; align-items: center; }
</style>
