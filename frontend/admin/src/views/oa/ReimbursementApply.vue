<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { attachmentApi } from '../../api/attachment'
import { oaApi } from '../../api/oa'
import type { Item } from '../../api/inventory'

const form = reactive({
  relatedInboundOrderId: 1,
  amount: '100.00',
  reason: '',
  voucherAttachmentIds: [] as number[],
})
const uploading = ref(false)
const voucherNames = ref<string[]>([])

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
  await oaApi.startReimbursement({ ...form, amount: form.amount })
  ElMessage.success('报销 OA 已发起')
}
</script>

<template>
  <section class="page-section">
    <h1>发起报销 OA</h1>
    <el-form label-width="140px">
      <el-form-item label="关联入库单 ID"><el-input-number v-model="form.relatedInboundOrderId" /></el-form-item>
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
