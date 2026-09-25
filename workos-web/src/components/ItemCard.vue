<script setup lang="ts">
import { computed } from 'vue'
import dayjs from 'dayjs'
import {
  CheckCircleFilled,
  CheckOutlined,
  ClockCircleOutlined,
  HourglassOutlined,
  InboxOutlined,
  MinusCircleOutlined,
  PlayCircleOutlined
} from '@ant-design/icons-vue'
import type { Component } from 'vue'
import type { Item, ItemStatus } from '@/types'

/**
 * 事项行：扫描优先（design.md §7 §16 §17）
 * 第一行 = 状态 + 标题 + 优先级
 * 第二行 = 项目 + 时间
 * 第三行 = 最多 2 个有语义价值的次级标签
 */
const props = defineProps<{
  item: Item
  /** 待安排模式：给出「今天安排」等决策入口 */
  decisionMode?: boolean
  selected?: boolean
  /** 正在提交的动作名，用于局部 loading，不阻塞整页 */
  pendingAction?: string | null
}>()

const emit = defineEmits<{
  open: [item: Item]
  complete: [item: Item]
  planToday: [item: Item]
  planTomorrow: [item: Item]
  postpone: [item: Item]
}>()

const STATUS_ICON: Record<ItemStatus, Component> = {
  INBOX: InboxOutlined,
  TODO: ClockCircleOutlined,
  DOING: PlayCircleOutlined,
  WAITING: HourglassOutlined,
  DONE: CheckCircleFilled,
  CANCELLED: MinusCircleOutlined
}

const STATUS_COLOR: Record<ItemStatus, string> = {
  INBOX: 'var(--text-3)',
  TODO: 'var(--text-2)',
  DOING: 'var(--primary)',
  WAITING: 'var(--warning)',
  DONE: 'var(--success)',
  CANCELLED: 'var(--text-3)'
}

const statusIcon = computed(() => STATUS_ICON[props.item.status] ?? ClockCircleOutlined)
const statusColor = computed(() => STATUS_COLOR[props.item.status] ?? 'var(--text-3)')
const isClosed = computed(() => props.item.status === 'DONE' || props.item.status === 'CANCELLED')

/**
 * 行内完成入口：是否可完成由后端 allowedTransitions 决定，前端不复制状态机
 */
const canComplete = computed(
  () => !isClosed.value && (props.item.allowedTransitions ?? []).includes('DONE')
)
const isPending = computed(() => Boolean(props.pendingAction))

/** 第二行的时间语义：优先截止时间，其次计划日期 */
const timeText = computed(() => {
  const item = props.item
  if (item.dueAt) {
    return `${dayjs(item.dueAt).format('MM-DD HH:mm')} 前`
  }
  if (item.plannedDate) {
    return `计划 ${dayjs(item.plannedDate).format('MM-DD')}`
  }
  return ''
})

/**
 * 次级标签：全部来自后端事实字段，前端只做组合，最多 2 个（design.md §18）
 * 优先级固定：等待 → 跟进 → 逾期/到期 → 剩余步数
 */
const labels = computed(() => {
  const item = props.item
  const result: Array<{ text: string; tone: string }> = []
  if (item.waitingFor) {
    const days = item.waitingDays ?? 0
    result.push({
      text: days > 0 ? `等 ${item.waitingFor} · ${days} 天` : `等 ${item.waitingFor}`,
      tone: 'label--waiting'
    })
  }
  if (item.followUpDue && item.nextFollowUpAt) {
    result.push({ text: `跟进 ${dayjs(item.nextFollowUpAt).format('MM-DD HH:mm')}`, tone: 'label--primary' })
  }
  if (item.overdue) {
    const days = item.dueAt ? dayjs().startOf('day').diff(dayjs(item.dueAt).startOf('day'), 'day') : 0
    result.push({ text: days > 0 ? `逾期 ${days} 天` : '今天到期', tone: 'label--danger' })
  }
  if (item.remainingSteps) {
    result.push({ text: `剩 ${item.remainingSteps} 步`, tone: 'label--primary' })
  }
  return result.slice(0, 2)
})
</script>

<template>
  <div
    class="list-row"
    :class="{ 'list-row--selected': selected, 'list-row--muted': isClosed }"
    :data-item-id="item.id"
    tabindex="-1"
    @click="emit('open', item)"
  >
    <div class="list-row__line">
      <span class="list-row__state" :style="{ color: statusColor }">
        <component :is="statusIcon" />
      </span>
      <span class="list-row__title">{{ item.title }}</span>
      <span class="prio" :class="`prio--${item.priority}`">{{ item.priority }}</span>
      <a-button
        v-if="canComplete"
        class="list-row__complete"
        type="text"
        size="small"
        title="完成"
        aria-label="完成"
        :loading="pendingAction === 'complete'"
        :disabled="isPending && pendingAction !== 'complete'"
        @click.stop="emit('complete', item)"
      >
        <CheckOutlined v-if="pendingAction !== 'complete'" />
      </a-button>
    </div>

    <div v-if="item.projectName || timeText" class="list-row__sub">
      <span v-if="item.projectName">{{ item.projectName }}</span>
      <span v-if="timeText">{{ timeText }}</span>
    </div>

    <div v-if="labels.length" class="list-row__sub">
      <span v-for="label in labels" :key="label.text" class="label" :class="label.tone">
        {{ label.text }}
      </span>
    </div>

    <div v-if="decisionMode" class="list-row__actions" @click.stop>
      <a-button
        size="small"
        type="primary"
        :loading="pendingAction === 'planToday'"
        :disabled="isPending && pendingAction !== 'planToday'"
        @click="emit('planToday', item)"
      >
        今天安排
      </a-button>
      <a-button
        size="small"
        type="text"
        :loading="pendingAction === 'planTomorrow'"
        :disabled="isPending && pendingAction !== 'planTomorrow'"
        @click="emit('planTomorrow', item)"
      >
        排到明天
      </a-button>
      <a-button
        size="small"
        type="text"
        :loading="pendingAction === 'postpone'"
        :disabled="isPending && pendingAction !== 'postpone'"
        @click="emit('postpone', item)"
      >
        暂不安排
      </a-button>
    </div>
  </div>
</template>
