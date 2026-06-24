<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { reactive } from 'vue'
import { oaApi } from '../../api/oa'

const form = reactive({ warehouseId: 1, itemId: 1, quantity: 1, unitPrice: '0.50' })
async function submit() { await oaApi.startOutbound(form); ElMessage.success('出库 OA 已发起') }
</script>

<template><section class="page-section"><h1>发起出库 OA</h1><el-form label-width="120px"><el-form-item label="仓库 ID"><el-input-number v-model="form.warehouseId" /></el-form-item><el-form-item label="物资 ID"><el-input-number v-model="form.itemId" /></el-form-item><el-form-item label="数量"><el-input-number v-model="form.quantity" /></el-form-item><el-form-item label="单价"><el-input v-model="form.unitPrice" /></el-form-item><el-button type="primary" @click="submit">提交</el-button></el-form></section></template>
