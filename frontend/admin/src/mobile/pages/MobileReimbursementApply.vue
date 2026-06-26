<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { attachmentApi } from '../../api/attachment'
import { inventoryApi, type InboundOrder } from '../../api/inventory'
import { oaApi } from '../../api/oa'
const router = useRouter()
const form = reactive({ relatedInboundOrderId: undefined as number | undefined, amount: '0.00', reason: '', voucherAttachmentIds: [] as number[] })
const inboundOrders = ref<InboundOrder[]>([])
const voucherNames = ref<string[]>([])
const uploading = ref(false)
async function loadOrders() { inboundOrders.value = await inventoryApi.unlinkedInboundOrdersForReimbursement() }
function selectInbound(id: number | string) { const numeric = Number(id); form.relatedInboundOrderId = numeric; const order = inboundOrders.value.find(o => o.id === numeric); if (order) form.amount = String(order.amount ?? '0.00') }
async function uploadVoucher(file: File) { uploading.value = true; try { const att = await attachmentApi.uploadFile(file, 'reimbursement_voucher'); form.voucherAttachmentIds.push(att.id); voucherNames.value.push(att.originalName) } finally { uploading.value = false } }
async function submit() {
  if (!form.relatedInboundOrderId) return ElMessage.warning('请选择入库单')
  if (!form.reason) return ElMessage.warning('请填写报销事由')
  if (form.voucherAttachmentIds.length === 0) return ElMessage.warning('请上传凭证')
  await oaApi.startReimbursement({ ...form, amount: form.amount })
  ElMessage.success('报销 OA 已发起')
  router.push('/m/oa/mine')
}
onMounted(loadOrders)
</script>
<template>
  <section class="mobile-card mobile-form">
    <div class="mobile-card-title">发起报销 OA</div>
    <el-form label-position="top">
      <el-form-item label="关联入库单"><el-select v-model="form.relatedInboundOrderId" @change="selectInbound"><el-option v-for="order in inboundOrders" :key="order.id" :label="`${order.orderNo}｜金额 ${order.amount}`" :value="order.id" /></el-select></el-form-item>
      <el-form-item label="报销金额"><el-input v-model="form.amount" /></el-form-item>
      <el-form-item label="报销事由"><el-input v-model="form.reason" type="textarea" /></el-form-item>
      <el-form-item label="报销凭证"><el-upload :auto-upload="false" :show-file-list="false" accept=".jpg,.jpeg,.png,.webp,.pdf" @change="(f: any) => uploadVoucher(f.raw)"><el-button :loading="uploading">上传凭证</el-button></el-upload><div v-for="name in voucherNames" :key="name" class="mobile-muted">{{ name }}</div></el-form-item>
      <el-button class="mobile-primary" type="primary" @click="submit">提交报销 OA</el-button>
    </el-form>
  </section>
</template>
