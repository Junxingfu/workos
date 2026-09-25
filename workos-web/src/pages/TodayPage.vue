<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { fail, fetchToday, planItem, transition, updateItem } from '@/api'
import ItemCard from '@/components/ItemCard.vue'
import ItemDrawer from '@/components/ItemDrawer.vue'
import PageHeader from '@/components/PageHeader.vue'
import type { Item, TodayResp } from '@/types'

/**
 * Today 是决策面（design.md §24~§28）
 * 固定顺序：今天做什么 → 今天别忘了 → 我在等谁 / 还没安排
 */
const router = useRouter()
const data = ref<TodayResp | null>(null)
const loading = ref(false)
const drawerOpen = ref(false)
const activeId = ref<string | null>(null)
/** 行选中：动作完成后把「你在这里」交给下一条，视线不用重新找位置 */
const selectedId = ref<string | null>(null)
/** 局部 loading：只让被点的那个控件转，不整页变灰 */
const pending = ref<{ id: string; action: string } | null>(null)

/** 软上限 3，硬上限 5（PRD），这里只显示事实与进度 */
const PLAN_CAPACITY = 3

const dateText = computed(() => {
  const date = data.value?.date
  return date ? `今天 · ${dayjs(date).format('M月D日')}` : '今天'
})

/** 头部只给需要用户处理的事实，不做 Summary Card */
const headerContext = computed(() => {
  const summary = data.value?.summary
  if (!summary) return []
  const result: string[] = []
  if (summary.overdue) result.push(`${summary.overdue} 项已逾期`)
  if (summary.followUp) result.push(`${summary.followUp} 个跟进到点`)
  if (summary.carriedOverPlan) result.push(`${summary.carriedOverPlan} 项计划欠账`)
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

async function load() {
  loading.value = true
  try {
    data.value = await fetchToday()
  } catch (e) {
    fail(e)
  } finally {
    loading.value = false
  }
}

function open(item: Item) {
  activeId.value = item.id
  selectedId.value = item.id
  drawerOpen.value = true
}

function pendingActionFor(item: Item): string | null {
  return pending.value?.id === item.id ? pending.value.action : null
}

/** 按页面渲染顺序拍平，用于把选中态交给邻居 */
function flatIds(): string[] {
  const sections = data.value?.sections
  if (!sections) return []
  return [
    ...sections.plan,
    ...sections.overdue,
    ...sections.today,
    ...sections.followUp,
    ...sections.waiting,
    ...sections.suggested
  ].map((item) => item.id)
}

function neighborOf(id: string): string | null {
  const ids = flatIds()
  const index = ids.indexOf(id)
  if (index < 0) return null
  return ids[index + 1] ?? ids[index - 1] ?? null
}

/** 动作结束后把选中态和焦点落到邻居上，用户不需要重新找刚才的位置 */
async function keepPlace(nextId: string | null) {
  selectedId.value = nextId
  await nextTick()
  if (nextId) document.querySelector<HTMLElement>(`[data-item-id="${nextId}"]`)?.focus()
}

/**
 * 统一动作包装：写操作后仍然重新 GET（不做乐观更新），
 * 但 loading 只挂在触发点，刷新后保持用户上下文。
 */
async function run(item: Item, action: string, task: () => Promise<unknown>, tip: string) {
  const nextId = neighborOf(item.id)
  pending.value = { id: item.id, action }
  try {
    await task()
    message.success(tip)
    await load()
    await keepPlace(nextId)
  } catch (e) {
    fail(e)
  } finally {
    pending.value = null
  }
}

function complete(item: Item) {
  return run(
    item,
    'complete',
    () => transition(item.id, { toStatus: 'DONE', version: item.version }),
    '已完成'
  )
}

function planTo(item: Item, action: string, date: string, label: string) {
  return run(item, action, () => planItem(item.id, date, item.version), label)
}

/** 暂不安排 = 显式降为 P2，不是隐藏（PRD V1.3.5） */
function postpone(item: Item) {
  return run(item, 'postpone', () => updateItem(item.id, { priority: 'P2' }), '已降为 P2')
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader :title="dateText" :context="headerContext">
      <template #actions>
        <a-button size="small" type="text" @click="router.push('/inbox')">去整理</a-button>
      </template>
    </PageHeader>

    <!-- 只有首屏加载才整块 loading；后续刷新不清空、不变灰 -->
    <a-spin :spinning="loading && !data">
      <!-- ① 今天做什么：最高视觉优先级 -->
      <section class="section">
        <div class="section__head">
          <h2 class="section-title">今天做什么</h2>
          <span class="section__count">
            计划 · {{ data?.summary.plan ?? 0 }}/{{ PLAN_CAPACITY }}
          </span>
          <span v-if="(data?.summary.plan ?? 0) > PLAN_CAPACITY" class="section__hint">
            已超过建议上限，注意取舍
          </span>
        </div>

        <div v-if="data?.sections.plan?.length" class="list">
          <ItemCard
            v-for="item in data.sections.plan"
            :key="item.id"
            :item="item"
            :selected="item.id === selectedId"
            :pending-action="pendingActionFor(item)"
            @open="open"
            @complete="complete"
          />
        </div>
        <div v-else class="empty-state">
          <p class="empty-state__title">今天还没有计划事项</p>
          <p class="empty-state__hint">从下面「还没安排」里挑一件今天要推进的。</p>
        </div>
      </section>

      <!-- ② 今天别忘了 -->
      <section class="section">
        <div class="section__head">
          <h2 class="section-title">今天别忘了</h2>
          <span class="section__count">{{ data?.summary.overdue ?? 0 }} 逾期 /
            {{ data?.summary.today ?? 0 }} 到期 / {{ data?.summary.followUp ?? 0 }} 跟进</span>
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
                @open="open"
                @complete="complete"
              />
            </div>
          </div>
        </template>
        <div v-else class="empty-state">
          <p class="empty-state__title">没有要盯着的事</p>
          <p class="empty-state__hint">逾期、今日到期、到点跟进的都会出现在这里。</p>
        </div>
      </section>

      <!-- ③ 我在等谁 -->
      <section class="section">
        <div class="section__head">
          <h2 class="section-title">我在等谁</h2>
          <span class="section__count">{{ data?.summary.waiting ?? 0 }}</span>
        </div>

        <div v-if="data?.sections.waiting?.length" class="list">
          <ItemCard
            v-for="item in data.sections.waiting"
            :key="item.id"
            :item="item"
            :selected="item.id === selectedId"
            :pending-action="pendingActionFor(item)"
            @open="open"
            @complete="complete"
          />
        </div>
        <div v-else class="empty-state">
          <p class="empty-state__title">当前没有等待中的事项</p>
          <p class="empty-state__hint">在事项里点「在等别人」，它就会出现在这里。</p>
        </div>
      </section>

      <!-- ④ 还没安排：捕漏区，不是推荐区 -->
      <section class="section">
        <div class="section__head">
          <h2 class="section-title">还没安排</h2>
          <span class="section__hint">你已认可它是正式工作，只是还没决定什么时候做</span>
          <span class="section__count">最多显示 5 条</span>
        </div>

        <div v-if="data?.sections.suggested?.length" class="list">
          <ItemCard
            v-for="item in data.sections.suggested"
            :key="item.id"
            :item="item"
            decision-mode
            :selected="item.id === selectedId"
            :pending-action="pendingActionFor(item)"
            @open="open"
            @complete="complete"
            @plan-today="planTo(item, 'planToday', dayjs().format('YYYY-MM-DD'), '已排到今天')"
            @plan-tomorrow="
              planTo(item, 'planTomorrow', dayjs().add(1, 'day').format('YYYY-MM-DD'), '已排到明天')
            "
            @postpone="postpone(item)"
          />
        </div>
        <div v-else class="empty-state">
          <p class="empty-state__title">没有等着你决定的事</p>
          <p class="empty-state__hint">P0 / P1 且没有计划日期的事项会出现在这里。</p>
        </div>
      </section>
    </a-spin>

    <ItemDrawer v-model:open="drawerOpen" :item-id="activeId" @changed="load" />
  </div>
</template>
