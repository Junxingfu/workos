<script setup lang="ts">
import { computed } from 'vue'
import type { Item, ItemStatus } from '@/types'

const props = defineProps<{ item: Item; disabled?: boolean }>()
const emit = defineEmits<{ transition: [status: ItemStatus] }>()
const labels: Record<ItemStatus, string> = {
  INBOX: '待整理', TODO: '待办', DOING: '进行中', WAITING: '等待中', DONE: '已完成', CANCELLED: '已取消'
}
function label(status: ItemStatus) { return status === 'DONE' && props.item.type === 'NOTE' ? '已保存' : labels[status] }
const order: ItemStatus[] = ['TODO', 'DOING', 'WAITING', 'DONE', 'INBOX', 'CANCELLED']
// 排序只影响展示；每个目标是否可达由后端 allowedTransitions 决定。
const targets = computed(() => order.filter(status => props.item.allowedTransitions?.includes(status)))
const explanation = computed(() => {
  if (props.item.type === 'NOTE') return '笔记保存后仍可编辑，恢复到待整理可重新处理。'
  if (props.item.status === 'WAITING') return '等到结果后恢复处理；已经做完也可以直接完成。'
  if (['DONE', 'CANCELLED'].includes(props.item.status)) return '重新打开会回到待办，历史记录会保留。'
  if (props.item.status === 'INBOX') return '收下进入待办，再按需要安排日期。'
  return '可以直接完成，也可以进入等待。退回待办不会删除记录。'
})
</script>

<template>
  <details class="state-flow" open>
    <summary>状态流转 <span class="meta-text">当前：{{ label(item.status) }}</span></summary>
    <div class="state-flow__graph" role="group" aria-label="事项状态流转图">
      <span class="state-flow__current" aria-current="step">{{ label(item.status) }}<small>当前位置</small></span>
      <span class="state-flow__arrow" aria-hidden="true">→</span>
      <div class="state-flow__targets">
        <template v-for="target in targets" :key="target">
          <a-popconfirm v-if="target === 'CANCELLED'" title="取消这件事？记录会保留，可以重新打开。" ok-text="取消事项" cancel-text="继续处理" @confirm="emit('transition', target)">
            <a-button size="small" :disabled="disabled" :aria-label="`转为${label(target)}`">{{ label(target) }}</a-button>
          </a-popconfirm>
          <a-button v-else size="small" :disabled="disabled" :aria-label="`转为${label(target)}`" @click="emit('transition', target)">{{ label(target) }}</a-button>
        </template>
        <span v-if="!targets.length" class="meta-text">暂无可用转换</span>
      </div>
    </div>
    <p class="meta-text">{{ explanation }}点击右侧状态即可操作。</p>
    <p v-if="item.type === 'TASK'" class="meta-text">“今天做”是计划安排，不是状态；安排今天后仍可处于待办、进行中或等待中。</p>
  </details>
</template>

<style scoped>
.state-flow { margin: 16px 0; border-block: 1px solid var(--color-border); padding: 12px 0; }
.state-flow summary { cursor: pointer; font-weight: 500; }
.state-flow summary .meta-text { margin-left: 12px; font-weight: 400; }
.state-flow__graph { display: flex; align-items: center; gap: 12px; margin: 14px 0 10px; }
.state-flow__current { flex-shrink: 0; border: 1px solid var(--color-primary); color: var(--color-primary); padding: 8px 12px; text-align: center; }
.state-flow__current small { display: block; font-size: 11px; margin-top: 2px; color: var(--color-text-secondary); }
.state-flow__arrow { color: var(--color-text-tertiary); }
.state-flow__targets { display: flex; flex-wrap: wrap; gap: 8px; }
.state-flow p { margin: 6px 0 0; }
</style>
