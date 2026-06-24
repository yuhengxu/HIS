<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { reactive } from 'vue'
import { oaApi } from '../../api/oa'

const form = reactive({ relatedInboundOrderId: 1, amount: '100.00' })
async function submit() { await oaApi.startReimbursement(form); ElMessage.success('报销 OA 已发起') }
</script>

<template><section class="page-section"><h1>发起报销 OA</h1><el-form label-width="140px"><el-form-item label="关联入库单 ID"><el-input-number v-model="form.relatedInboundOrderId" /></el-form-item><el-form-item label="金额"><el-input v-model="form.amount" /></el-form-item><el-button type="primary" @click="submit">提交</el-button></el-form></section></template>
