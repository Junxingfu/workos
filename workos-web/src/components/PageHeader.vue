<script setup lang="ts">
/**
 * 页面头部：标题 + 必要上下文 + 最多 1 个主操作（design.md §14）
 */
defineProps<{
  title: string
  /** 轻量上下文，例如「2 项计划未完成」「1 个跟进已逾期」 */
  context?: string[]
}>()
</script>

<template>
  <header class="page-head">
    <h1 class="page-title">{{ title }}</h1>
    <div v-if="context?.length" class="page-head__ctx">
      <span v-for="line in context" :key="line">{{ line }}</span>
    </div>
    <div class="page-head__actions">
      <slot name="actions" />
    </div>
  </header>
</template>
