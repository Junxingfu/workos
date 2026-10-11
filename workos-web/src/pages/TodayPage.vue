<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import QuickCapture from '@/components/QuickCapture.vue'
import { useCaptureRefresh } from '@/composables/useCaptureRefresh'
import { fail, fetchItem, fetchToday, fetchWorkLogs, planItem, transition, updateItem } from '@/api'
import ItemCard from '@/components/ItemCard.vue'
import ItemDrawer from '@/components/ItemDrawer.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useListActions } from '@/composables/useListActions'
import type { Item, ItemRowAction, TodayResp, WorkLog } from '@/types'

/**
 * 今天 = 工作驾驶舱（对齐 前端规范.md §7.1）
 * 左主区回答「现在做什么」，右栏回答「还有什么上下文不能忘」。
 * 固定顺序：今日计划 → 今天别忘了（逾期→今日到期→待跟进）→ 还没安排。
 */
const router = useRouter()
const data = ref<TodayResp | null>(null)
const loading = ref(false)
const loadError = ref('')
let loadRequest = 0
const drawerOpen = ref(false)
const activeId = ref<string | null>(null)
/** 打开抽屉时直接落到某个动作上（等待 / 跟进），避免用户再找一次入口 */
const drawerIntent = ref<'wait' | 'followUp' | null>(null)
const recentLogs = ref<WorkLog[]>([])

/** 软上限 3（PRD）；这里只显示事实与进度 */
const PLAN_CAPACITY = 3

const dateText = computed(() => {
  const date = data.value?.date
  return date ? `今天` : '今天'
})

const dateSub = computed(() => {
  const date = data.value?.date
  if (!date) return ''
  return `${dayjs(date).format('M月D日')} · 星期${'日一二三四五六'[dayjs(date).day()]}`
})

const headerContext = computed(() => {
  const summary = data.value?.summary
  if (!summary) return []
  const result: string[] = []
  if (summary.overdue) result.push(`${summary.overdue} 项已逾期`)
  if (summary.followUp) result.push(`${summary.followUp} 个跟进到点`)
  if (summary.inbox) result.push(`${summary.inbox} 条待整理`)
  return result
})

const forgetGroups = computed(() => {
  const sections = data.value?.sections
  return [
    { label: '逾期', items: sections?.overdue ?? [] },
    { label: '今日到期', items: sections?.today ?? [] },
    { label: '待跟进', items: sections?.followUp ?? [] }
  ].filter((group) => group.items.length)
})

const carriedOverItems = computed(() => data.value?.sections.carriedOverPlan ?? [])

const waitingItems = computed(() => data.value?.sections.waiting ?? [])

/** 继续上次：最近一次记过工作的事项，一键回到被打断的地方 */
const lastWorkedItemId = computed(
  () => recentLogs.value.find((log) => log.itemId)?.itemId ?? null
)

async function load() {
  const request = ++loadRequest
  loading.value = true
  loadError.value = ''
  try {
    const result = await fetchToday()
    if (request !== loadRequest) return
    data.value = result
    const logs = await fetchWorkLogs({})
    if (request === loadRequest) recentLogs.value = logs
  } catch (e) {
    if (request === loadRequest) loadError.value = e instanceof Error ? e.message : '加载失败，请重试'
  } finally {
    if (request === loadRequest) loading.value = false
  }
}

/** 按页面渲染顺序拍平，交给 useListActions 找邻居 */
function flatRows(): Item[] {
  const sections = data.value?.sections
  if (!sections) return []
  return [
    ...sections.plan,
    ...sections.overdue,
    ...sections.today,
    ...sections.followUp,
    ...sections.waiting,
    ...(sections.carriedOverPlan ?? []),
    ...sections.suggested
  ]
}

const { selectedId, run, select, pendingActionFor } = useListActions(flatRows, load, {
  onOpen: (item) => open(item, null)
})

function open(item: Item, intent: 'wait' | 'followUp' | null = null) {
  activeId.value = item.id
  drawerIntent.value = intent
  select(item)
  drawerOpen.value = true
}

async function openById(id: string) {
  try {
    const item = await fetchItem(id)
    open(item, null)
  } catch (e) {
    fail(e)
  }
}

/** 完成后误点只能退回待办（后端只允许 DONE→TODO），所以给一个反悔窗口 */
async function undoComplete(id: string) {
  const fresh = await fetchItem(id)
  await transition(id, { toStatus: 'TODO', version: fresh.version })
}

/** 行内高频动作：全部按后端 allowedTransitions 走 transitions 接口 */
function onAction(action: ItemRowAction, item: Item) {
  switch (action) {
    case 'open':
      return open(item, null)
    case 'complete':
      return run(
        item,
        'complete',
        () => transition(item.id, { toStatus: 'DONE', version: item.version }),
        { tip: '已完成', undo: () => undoComplete(item.id) }
      )
    case 'start':
    case 'resume':
      return run(
        item,
        action,
        () => transition(item.id, { toStatus: 'DOING', version: item.version }),
        { tip: action === 'resume' ? '已恢复处理' : '已开始' }
      )
    case 'reopen':
      return run(
        item,
        'reopen',
        () => transition(item.id, { toStatus: 'TODO', version: item.version }),
        { tip: '已重新打开' }
      )
    case 'wait':
      return open(item, 'wait')
    case 'followUp':
      return open(item, 'followUp')
    case 'planToday':
      return run(item, 'planToday', () => planItem(item.id, data.value!.date, item.version), {
        tip: '已排到今天'
      })
    case 'planTomorrow':
      return run(
        item,
        'planTomorrow',
        () => planItem(item.id, dayjs(data.value!.date).add(1, 'day').format('YYYY-MM-DD'), item.version),
        { tip: '已排到明天' }
      )
    case 'postpone':
      return run(item, 'postpone', () => updateItem(item.id, { priority: 'P2', version: item.version }), {
        tip: '已降为 P2'
      })
  }
}

useCaptureRefresh(load)
onMounted(load)
</script>

<template>
  <div>
    <PageHeader :title="dateText" :sub="dateSub" :context="headerContext">
      <template #actions>
        <a-button v-if="lastWorkedItemId" size="small" @click="openById(lastWorkedItemId)">
          继续上次
        </a-button>
      </template>
    </PageHeader>

    <QuickCapture destination="TODAY" />

    <a-button v-if="data?.summary.inbox" type="link" size="small" @click="router.push('/inbox')">收件箱有 {{ data.summary.inbox }} 条待整理，收下后可安排今天</a-button>
    <a-alert v-if="loadError" type="error" :message="loadError" show-icon style="margin-bottom: 16px">
      <template #action><a-button size="small" @click="load">重试</a-button></template>
    </a-alert>
    <a-skeleton v-if="loading && !data" active />
    <a-spin v-if="data" :spinning="false">
      <div class="today-layout">
        <!-- 左：现在要做什么 -->
        <div>
          <section class="section">
            <div class="section__head">
              <h2 class="section-title">今日计划</h2>
              <span class="section__count">{{ data?.summary.plan ?? 0 }} 件 · 建议 {{ PLAN_CAPACITY }} 件</span>
            </div>

            <div v-if="data?.sections.plan?.length" class="list">
              <ItemCard
                v-for="item in data.sections.plan"
                :key="item.id"
                :item="item"
                :selected="item.id === selectedId"
                :pending-action="pendingActionFor(item)"
                @action="onAction"
              />
            </div>
            <div v-else class="empty-state">
              <p class="empty-state__title">今天还没有计划事项</p>
              <p class="empty-state__hint">在上方记下今天要做的事，或从待办中选择。</p>
            </div>
          </section>

          <section class="section">
            <div class="section__head">
              <h2 class="section-title">今天别忘了</h2>
              <span class="section__count">
                {{ data?.summary.overdue ?? 0 }} 逾期 / {{ data?.summary.today ?? 0 }} 到期 /
                {{ data?.summary.followUp ?? 0 }} 跟进
              </span>
            </div>

            <template v-if="forgetGroups.length">
              <div v-for="group in forgetGroups" :key="group.label">
                <div class="group-label">{{ group.label }}</div>
                <div class="list">
                  <ItemCard
                    v-for="item in group.items"
                    :key="item.id"
                    :item="item"
                    :selected="item.id === selectedId"
                    :pending-action="pendingActionFor(item)"
                    @action="onAction"
                  />
                </div>
              </div>
            </template>
            <div v-else class="empty-state">
              <p class="empty-state__title">没有要盯着的事</p>
              <p class="empty-state__hint">逾期、今日到期、到点跟进的都会出现在这里。</p>
            </div>
          </section>

          <section class="section">
            <div class="section__head">
              <h2 class="section-title">还没安排</h2>
              <a-button class="section__count" type="link" size="small" @click="router.push('/items')">从全部待办选择</a-button>
            </div>

            <template v-if="carriedOverItems.length">
              <div class="group-label">之前计划，还未完成 · 可重新安排今天</div>
              <div class="list" style="margin-bottom: 12px">
                <ItemCard v-for="item in carriedOverItems" :key="item.id" :item="item" decision-mode :selected="item.id === selectedId" :pending-action="pendingActionFor(item)" @action="onAction" />
              </div>
            </template>
            <div v-if="data?.sections.suggested?.length" class="list">
              <ItemCard
                v-for="item in data.sections.suggested"
                :key="item.id"
                :item="item"
                decision-mode
                :selected="item.id === selectedId"
                :pending-action="pendingActionFor(item)"
                @action="onAction"
              />
            </div>
            <div v-else class="empty-state">
              <p class="empty-state__title">没有等着你决定的事</p>
              <p class="empty-state__hint">没有计划日期的待办会显示在这里，按优先级展示前 5 条。</p>
            </div>
          </section>

          <p v-if="flatRows().length" class="meta-text">j / k 选择 · Enter 打开 · Esc 取消选中</p>
        </div>

        <!-- 右：还有什么上下文不能忘 -->
        <aside>
          <div class="rail" style="margin-bottom: 16px">
            <div class="rail__head">其他等待事项 {{ waitingItems.length }}</div>
            <div class="rail__body">
              <template v-if="waitingItems.length">
                <button
                  v-for="item in waitingItems"
                  :key="item.id"
                  type="button"
                  class="rail__item"
                  @click="open(item, null)"
                >
                  <div class="rail__title">{{ item.title }}</div>
                  <div class="meta-text">
                    {{ item.waitingFor ? `等 ${item.waitingFor}` : '等待中' }}
                    <template v-if="item.waitingDays"> · {{ item.waitingDays }} 天</template>
                  </div>
                </button>
              </template>
              <p v-else class="meta-text">没有其他等待事项。今日计划和提醒区里的等待事项不在这里重复显示。</p>
            </div>
          </div>

          <div class="rail">
            <div class="rail__head">最近工作</div>
            <div class="rail__body">
              <template v-if="recentLogs.length">
                <button
                  v-for="log in recentLogs.slice(0, 6)"
                  :key="log.id"
                  type="button"
                  class="rail__item"
                  @click="log.itemId && openById(log.itemId)"
                >
                  <div class="rail__title">{{ log.content }}</div>
                  <div class="meta-text">{{ dayjs(log.occurredAt).format('MM-DD HH:mm') }}</div>
                </button>
              </template>
              <p v-else class="meta-text">
                还没有记录。在事项里用「记一句」记下系统看不出来的信息。
              </p>
            </div>
          </div>
        </aside>
      </div>
    </a-spin>

    <ItemDrawer
      v-model:open="drawerOpen"
      :item-id="activeId"
      :intent="drawerIntent"
      @changed="load"
      @clear-intent="drawerIntent = null"
    />
  </div>
</template>
