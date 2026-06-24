<script setup lang="ts">
const cards = [
  { title: '用户权限', text: 'RBAC + 用户单独授权，权限变更进入审计。' },
  { title: 'OA 流程', text: '支持指定用户、指定角色、汇报上级三种节点模式。' },
  { title: '物资库存', text: '入库/出库经 OA 审批后才影响库存。' },
  { title: 'AI 边界', text: '仅开放物资、库存、价格和流程状态只读接口。' },
]
</script>

<template>
  <section class="page-grid">
    <article v-for="card in cards" :key="card.title" class="module-card">
      <h2>{{ card.title }}</h2>
      <p>{{ card.text }}</p>
    </article>
  </section>
</template>
