<script setup lang="ts">
/**
 * 页面头部：标题 + 副标题/上下文 + 最多 1 个主要操作（前端规范.md §4.4）
 * 不堆「刷新 / 导出 / 新增 / 筛选 / 更多」，操作靠近它影响的对象。
 */
defineProps<{
  title: string
  /** 副标题，例如「9月26日 · 星期六」 */
  sub?: string
  /** 需要用户处理的事实，例如「2 项已逾期」 */
  context?: string[]
}>()
</script>

<template>
  <header class="page-head">
    <div class="page-head__text">
      <h1 class="page-title">{{ title }}</h1>
      <div v-if="sub || context?.length" class="page-head__sub">
        <span v-if="sub">{{ sub }}</span>
        <span v-for="line in context" :key="line" class="page-head__ctx">{{ line }}</span>
      </div>
    </div>
    <div class="page-head__actions">
      <slot name="actions" />
    </div>
  </header>
</template>
