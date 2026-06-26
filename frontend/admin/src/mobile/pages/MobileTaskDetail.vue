<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { oaApi, type OaTaskDetail } from '../../api/oa'

const route = useRoute()
const router = useRouter()
const detail = ref<OaTaskDetail | null>(null)
async function load() { detail.value = await oaApi.taskDetail(Number(route.params.taskId)) }
async function approve() { await oaApi.approve(Number(route.params.taskId)); ElMessage.success('已通过'); router.push('/m/oa/todo') }
async function reject() { await oaApi.reject(Number(route.params.taskId)); ElMessage.success('已驳回'); router.push('/m/oa/todo') }
onMounted(load)
</script>
<template>
  <section v-if="detail">
    <div class="mobile-card">
      <div class="mobile-card-title">{{ detail.title }}</div>
      <div class="mobile-muted">发起人：{{ detail.initiatorName }}</div>
      <div class="mobile-muted">节点：{{ detail.nodeName }}</div>
      <div class="mobile-muted">状态：{{ detail.instanceStatus }}</div>
    </div>
    <div class="mobile-card">
      <div class="mobile-card-title">申请内容</div>
      <div v-for="(value, key) in detail.displayData" :key="key" style="margin-bottom: 8px">
        <strong>{{ key }}：</strong><span>{{ value }}</span>
      </div>
    </div>
    <div class="mobile-fixed-actions">
      <el-button class="mobile-primary" type="primary" @click="approve">通过</el-button>
      <el-button class="mobile-danger" type="danger" plain @click="reject">驳回</el-button>
    </div>
  </section>
</template>
