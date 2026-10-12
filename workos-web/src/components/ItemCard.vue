<script setup lang="ts">
import { computed } from 'vue'
import dayjs from 'dayjs'
import type { Item, ItemRowAction, ItemStatus } from '@/types'

/**
 * ItemRow：WorkOS 最重要的组件（对齐 前端规范.md §5.1 §5.2 §5.3）
 *
 * 展示组件，不推导业务规则：
 * - 能出现哪些动作完全由后端 allowedTransitions 决定
 * - 是否逾期、今天/明天都由后端字段与 dayjs 展示，不在前端做业务判断
 *
 * 结构：状态符号 + 标题 + 右侧上下文（项目 / 日期 / 优先级）+ 行内高频动作
 */
const props = defineProps<{
  item: Item
  /** 待安排模式：给出「安排今天」入口 */
  decisionMode?: boolean
  selected?: boolean
  /** 正在提交的动作名，用于局部 loading，不阻塞整页 */
  pendingAction?: string | null
}>()

const emit = defineEmits<{
  action: [action: ItemRowAction, item: Item]
}>()

/** 状态符号：颜色只是辅助，符号本身表达状态（§60） */
const GLYPH: Record<ItemStatus, string> = {
  INBOX: '○',
  TODO: '○',
  DOING: '◐',
  WAITING: '◷',
  DONE: '✓',
  CANCELLED: '×'
}

const STATUS_LABEL: Record<ItemStatus, string> = {
  INBOX: '待整理',
  TODO: '待办',
  DOING: '进行中',
  WAITING: '等待中',
  DONE: '已完成',
  CANCELLED: '已取消'
}

const STATUS_COLOR: Record<ItemStatus, string> = {
  INBOX: 'var(--color-text-tertiary)',
  TODO: 'var(--color-text-tertiary)',
  DOING: 'var(--color-primary)',
  WAITING: 'var(--color-warning)',
  DONE: 'var(--color-success)',
  CANCELLED: 'var(--color-text-tertiary)'
}

const glyph = computed(() => GLYPH[props.item.status] ?? '○')
const statusLabel = computed(() => props.item.type === 'NOTE' && props.item.status === 'DONE' ? '已保存' : STATUS_LABEL[props.item.status] ?? '')
const statusColor = computed(() => STATUS_COLOR[props.item.status] ?? 'var(--color-text-tertiary)')
const isClosed = computed(() => props.item.status === 'DONE' || props.item.status === 'CANCELLED')
const isPending = computed(() => Boolean(props.pendingAction))

const can = (target: ItemStatus) => (props.item.allowedTransitions ?? []).includes(target)

/**
 * 行内主要动作：按状态推导，可选值全部来自后端 allowedTransitions
 * WAITING → 恢复处理；DONE → 重新打开；其余 → 完成（不行就退到开始）
 */
const primaryAction = computed<{ action: ItemRowAction; label: string } | null>(() => {
  const status = props.item.status
  if (status === 'WAITING' && can('DOING')) return { action: 'resume', label: '恢复' }
  if (status === 'DONE' || status === 'CANCELLED') {
    if (can('TODO')) return { action: 'reopen', label: '重新打开' }
    if (can('INBOX')) return { action: 'reopen', label: '回到待整理' }
    return null
  }
  if (status === 'INBOX' && can('TODO')) return { action: 'accept', label: '收下' }
  if (status === 'INBOX' && can('DONE')) return { action: 'complete', label: '保存笔记' }
  if (status === 'TODO' && can('DOING')) return { action: 'start', label: '开始' }
  if (can('DONE')) return { action: 'complete', label: '完成' }
  if (can('DOING')) return { action: 'start', label: '开始' }
  return null
})

const showWait = computed(() => props.item.status !== 'WAITING' && can('WAITING'))
const showFollowUp = computed(() => Boolean(props.item.followUpDue) || props.item.status === 'WAITING')

/** 第一行右侧的日期：优先截止时间，其次计划日期 */
const dateText = computed(() => {
  const item = props.item
  if (item.dueAt) return `${dayjs(item.dueAt).format('MM-DD HH:mm')} 前`
  if (item.plannedDate) {
    return `计划 ${dayjs(item.plannedDate).format('MM-DD')}`
  }
  // 还没整理的记录显示捕获时间（前端规范.md §7.2：行右侧显示 10:32）
  if (item.status === 'INBOX' && item.createdAt) return dayjs(item.createdAt).format('HH:mm')
  return ''
})

/** 第二行只放最有价值的 1~2 条事实，其余进 Drawer */
const facts = computed(() => {
  const item = props.item
  const result: Array<{ text: string; tone?: string }> = []

  if (item.waitingFor) {
    const days = item.waitingDays ?? 0
    result.push({ text: days > 0 ? `等待 ${item.waitingFor} · ${days} 天` : `等待 ${item.waitingFor}`, tone: 'label--waiting' })
  }
  if (item.followUpDue && item.nextFollowUpAt) {
    result.push({ text: `跟进 ${dayjs(item.nextFollowUpAt).format('MM-DD HH:mm')}`, tone: 'label--primary' })
  }
  if (item.overdue) {
    result.push({ text: '已逾期', tone: 'label--danger' })
  }
  if (item.remainingSteps) {
    result.push({ text: `剩 ${item.remainingSteps} 步`, tone: 'label--primary' })
  }

  if (result.length < 2 && item.lastLogContent) result.push({ text: `最近：${item.lastLogContent}` })

  // 没有更有价值的语义事实时，才用描述摘要填充第二行
  if (!result.length && item.description) {
    const first = item.description.split('\n')[0].trim()
    if (first) result.push({ text: first })
  }

  return result.slice(0, 2)
})

function onPrimary() {
  const action = primaryAction.value
  if (action) emit('action', action.action, props.item)
}
</script>

<template>
  <div
    class="item-row"
    :class="{ 'item-row--selected': selected, 'item-row--muted': isClosed }"
    :data-item-id="item.id"
    tabindex="0"
    @keydown.enter.self.stop.prevent="emit('action', 'open', item)"
    @click="emit('action', 'open', item)"
  >
    <span
      class="item-row__glyph"
      :style="{ color: statusColor }"
      :title="statusLabel"
      :aria-label="statusLabel"
    >
      {{ glyph }}
    </span>

    <div class="item-row__body">
      <div class="item-row__head">
        <span class="meta-text">{{ statusLabel }}</span>
        <span class="item-row__title">{{ item.title }}</span>
        <span v-if="item.projectName" class="item-row__project" :title="`主项目：${item.projectName}`">{{ item.projectName }}</span>
        <span v-if="item.relatedProjects?.length" class="meta-text" :title="`关联项目：${item.relatedProjects.map(p => p.projectName).join('、')}`">跨 {{ item.relatedProjects.length + 1 }} 项目</span>
        <span v-if="dateText" class="item-row__date">{{ dateText }}</span>
        <span
          class="item-row__priority"
          :class="{
            'item-row__priority--strong': item.priority === 'P0' || item.priority === 'P1',
            'item-row__priority--p0': item.priority === 'P0',
            'item-row__priority--p1': item.priority === 'P1'
          }"
        >
          {{ item.priority === 'P0' || item.priority === 'P1' ? '● ' : '' }}{{ item.priority }}
        </span>
      </div>

      <div v-if="facts.length" class="item-row__sub">
        <span v-for="fact in facts" :key="fact.text" :class="fact.tone">{{ fact.text }}</span>
      </div>

      <slot name="rowDetail" />
    </div>

    <!-- 高频动作直接在行内完成，不为改一个状态打开 Drawer -->
    <div class="item-row__actions" @click.stop>
      <a-button
        v-if="primaryAction"
        size="small"
        :type="primaryAction.action === 'complete' ? 'primary' : 'default'"
        :loading="pendingAction === primaryAction.action"
        :disabled="isPending"
        @click="onPrimary"
      >
        {{ primaryAction.label }}
      </a-button>
      <a-button v-if="primaryAction?.action === 'start' && can('DONE')" size="small" :loading="pendingAction === 'complete'" :disabled="isPending" @click="emit('action', 'complete', item)">完成</a-button>
      <a-button
        v-if="decisionMode && item.type === 'TASK' && item.status !== 'INBOX' && !isClosed"
        size="small"
        :loading="pendingAction === 'planToday'"
        :disabled="isPending"
        @click="emit('action', 'planToday', item)"
      >
        安排今天
      </a-button>
      <a-button
        v-if="showWait"
        size="small"
        :loading="pendingAction === 'wait'"
        :disabled="isPending"
        @click="emit('action', 'wait', item)"
      >
        等待
      </a-button>
      <a-button
        v-if="showFollowUp"
        size="small"
        :loading="pendingAction === 'followUp'"
        :disabled="isPending"
        @click="emit('action', 'followUp', item)"
      >
        跟进
      </a-button>
      <slot name="rowActions" />
    </div>
  </div>
</template>
