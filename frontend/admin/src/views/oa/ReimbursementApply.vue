<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { attachmentApi } from '../../api/attachment'
import { inventoryApi, type InboundOrder } from '../../api/inventory'
import { oaApi } from '../../api/oa'

const router = useRouter()
const form = reactive({
  relatedInboundOrderId: undefined as number | undefined,
  amount: '100.00',
  reason: '',
  voucherAttachmentIds: [] as number[],
})
const uploading = ref(false)
const voucherNames = ref<string[]>([])
const inboundOrders = ref<InboundOrder[]>([])

async function loadInboundOrders() {
  inboundOrders.value = await inventoryApi.unlinkedInboundOrdersForReimbursement()
  if (!form.relatedInboundOrderId && inboundOrders.value.length > 0) {
    selectInbound(inboundOrders.value[0].id)
  }
}

function selectInbound(id: number | string) {
  const numericId = Number(id)
  form.relatedInboundOrderId = numericId
  const order = inboundOrders.value.find((item) => item.id === numericId)
  if (order) form.amount = String(order.amount ?? '0.00')
}

async function uploadVoucher(file: File) {
  uploading.value = true
  try {
    const attachment = await attachmentApi.uploadFile(file, 'reimbursement_voucher')
    form.voucherAttachmentIds.push(attachment.id)
    voucherNames.value.push(attachment.originalName)
  } finally {
    uploading.value = false
  }
}

async function submit() {
  if (form.voucherAttachmentIds.length === 0) {
    ElMessage.warning('请上传至少一个报销凭证')
    return
  }
  if (!form.reason) {
    ElMessage.warning('请填写报销事由')
    return
  }
  if (!form.relatedInboundOrderId) {
    ElMessage.warning('请选择未关联的入库单')
    return
  }
  await oaApi.startReimbursement({ ...form, amount: form.amount })
  ElMessage.success('报销 OA 已发起')
  router.push('/')
}

onMounted(loadInboundOrders)
</script>

<template>
  <section class="page-section">
    <h1>发起报销 OA</h1>
    <el-form label-width="140px">
      <el-form-item label="关联入库单">
        <el-select v-model="form.relatedInboundOrderId" class="form-control" placeholder="选择未关联入库单" @change="selectInbound">
          <el-option
            v-for="order in inboundOrders"
            :key="order.id"
            :label="`${order.orderNo}｜物资 ${order.itemId}｜金额 ${order.amount}`"
            :value="order.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="报销金额"><el-input v-model="form.amount" /></el-form-item>
      <el-form-item label="报销事由"><el-input v-model="form.reason" type="textarea" /></el-form-item>
      <el-form-item label="报销凭证" required>
        <el-upload :auto-upload="false" :show-file-list="false" accept=".jpg,.jpeg,.png,.webp,.pdf" @change="(file: any) => uploadVoucher(file.raw)">
          <el-button :loading="uploading">上传凭证</el-button>
        </el-upload>
        <div v-for="name in voucherNames" :key="name" class="file-tag">{{ name }}</div>
      </el-form-item>
      <el-button type="primary" @click="submit">提交</el-button>
    </el-form>
  </section>
</template>

<style scoped>
.file-tag { margin-top: 8px; color: #334155; }
</style>
