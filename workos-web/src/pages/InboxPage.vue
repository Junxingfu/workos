<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import QuickCapture from '@/components/QuickCapture.vue'
import { useCaptureRefresh } from '@/composables/useCaptureRefresh'
import { message } from 'ant-design-vue'
import { acceptAll, fail, fetchInbox, fetchProjects, transition, updateItem } from '@/api'
import ItemCard from '@/components/ItemCard.vue'
import ItemDrawer from '@/components/ItemDrawer.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useListActions } from '@/composables/useListActions'
import type { InboxResp, Item, ItemPriority, ItemRowAction, Project } from '@/types'

/**
 * Inbox 是处理队列，不是长期列表（见 docs/产品与交互.md、docs/开发规范.md）
 * 防黑洞的两个出口：逐条整理 / 先都收下
 */
const data = ref<InboxResp | null>(null)
const loading = ref(false)
const drawerOpen = ref(false)
const activeId = ref<string | null>(null)
const accepting = ref(false)
/** 正在行内整理的那一条：展开它的整理面板，而不是丢一个表单抽屉过去 */
const clarifyId = ref<string | null>(null)
const projects = ref<Project[]>([])

const records = computed(() => data.value?.records ?? [])
const projectOptions = computed(() => projects.value.map((p) => ({ value: p.id, label: p.name })))
const headerContext = computed(() =>
  data.value?.oldestDays ? [`最老一条已压 ${data.value.oldestDays} 天`] : []
)

async function load() {
  loading.value = true
  try {
    data.value = await fetchInbox()
  } catch (e) {
    fail(e)
  } finally {
    loading.value = false
  }
}

function open(item: Item) {
  activeId.value = item.id
  select(item)
  drawerOpen.value = true
}

/**
 * 收件箱里的行只有「打开」这一种行内动作：
 * 待整理状态的后端状态机只允许 TODO / CANCELLED，所以不会出现完成、开始这些按钮。
 */
function onAction(action: ItemRowAction, item: Item) {
  if (action === 'open') open(item)
  if (action === 'accept' || action === 'complete') return accept(item)
}

/**
 * 键盘与选中态统一走 useListActions。
 * Inbox 页面本身不滚动，所以方向键也可以用来选择（长列表里会顶掉滚动，只有这里开）。
 */
const { selectedId, run, select, pendingActionFor } = useListActions(
  () => records.value,
  load,
  {
    arrows: false,
    onOpen: (item) => open(item),
    onDigit: (key, item) => {
      if (!['1', '2', '3', '4'].includes(key)) return false
      // 键盘是扫队列：设完优先级直接到下一条
      void setPriority(item, `P${Number(key) - 1}` as ItemPriority, true)
      return true
    }
  }
)

async function loadProjects() {
  try {
    const page = await fetchProjects({ status: 'ACTIVE', pageNo: 1, pageSize: 100 })
    projects.value = page.dataList ?? []
  } catch {
    projects.value = []
  }
}

async function startClarify(item: Item) {
  clarifyId.value = item.id
  select(item)
  await nextTick()
  document
    .querySelector<HTMLElement>(`[data-item-id="${item.id}"]`)
    ?.scrollIntoView({ block: 'nearest' })
}

function stopClarify() {
  clarifyId.value = null
}

function setProject(item: Item, value?: string) {
  return run(item, 'project', () => updateItem(item.id, { projectId: value, clearProject: !value, version: item.version }), {
    tip: '已改归属',
    keepSelection: true
  })
}

/** 收下 = 承认它是正式工作：状态转为待办，之后由「还没安排」决定什么时候做（见 docs/产品与交互.md、docs/开发规范.md） */
async function accept(item: Item) {
  const accepted = await run(item, 'accept', () => transition(item.id, { toStatus: item.type === 'NOTE' ? 'DONE' : 'TODO', version: item.version }), {
    tip: item.type === 'NOTE' ? '笔记已保存' : '已收下，可在今天页安排'
  })
  if (accepted && clarifyId.value === item.id) clarifyId.value = null
}

async function dismiss(item: Item) {
  const dismissed = await run(
    item,
    'dismiss',
    () => transition(item.id, { toStatus: 'CANCELLED', version: item.version }),
    { tip: '已略过' }
  )
  if (dismissed && clarifyId.value === item.id) clarifyId.value = null
}

async function acceptAllItems() {
  accepting.value = true
  try {
    const result = (await acceptAll()) as unknown as { accepted: number }
    message.success(`已收下 ${result.accepted} 条，原有项目和优先级已保留`)
    await load()
    window.dispatchEvent(new Event('workos:inbox-changed'))
  } catch (e) {
    fail(e)
  } finally {
    accepting.value = false
  }
}

function organizeOldest() {
  const oldest = records.value[0]
  if (oldest) startClarify(oldest)
}

/** 单行整理：只改优先级。鼠标路径留在本行，键盘路径顺手往下走 */
async function setPriority(item: Item, priority: ItemPriority, advance = false) {
  await run(item, 'priority', () => updateItem(item.id, { priority, version: item.version }), {
    tip: `已设为 ${priority}`,
    keepSelection: !advance
  })
}

useCaptureRefresh(load)
onMounted(() => {
  load()
  loadProjects()
})
</script>

<template>
  <div>
    <PageHeader
      title="收件箱"
      :sub="`${data?.total ?? 0} 个待处理`"
      :context="headerContext"
    >
      <template #actions>
        <a-button size="small" :disabled="!records.length" @click="organizeOldest">
          整理最老的一条
        </a-button>
        <a-button
          size="small"
          type="primary"
          :loading="accepting"
          :disabled="!records.length"
          @click="acceptAllItems"
        >
          先都收下
        </a-button>
      </template>
    </PageHeader>

    <!-- 低摩擦输入：只要求一句话 -->
    <QuickCapture destination="INBOX" />

    <!-- 首屏加载才整块 loading；整理动作不再让整页变灰 -->
    <a-spin :spinning="loading && !data">
      <div v-if="records.length" class="list">
        <ItemCard
          v-for="item in records"
          :key="item.id"
          :item="item"
          :selected="item.id === selectedId"
          :pending-action="pendingActionFor(item)"
          @action="onAction"
        >
          <template #rowActions>
            <a-button
              class="list-row__clarify"
              size="small"
              type="text"
              @click.stop="clarifyId === item.id ? stopClarify() : startClarify(item)"
            >
              {{ clarifyId === item.id ? '收起' : '整理' }}
            </a-button>
          </template>

          <template #rowDetail>
            <!-- 渐进式整理：只问「收下还是不做、归到哪里」，不摊开整个编辑表单 -->
            <div v-if="clarifyId === item.id" class="clarify" @click.stop>
              <div class="clarify__field">
                <span>项目</span>
                <a-select
                  :value="item.projectId ?? undefined"
                  size="small"
                  style="width: 150px"
                  placeholder="未分类"
                  allow-clear
                  :options="projectOptions"
                  @change="(value: string) => setProject(item, value)"
                />
              </div>
              <div class="clarify__field">
                <span>优先级</span>
                <a-select
                  :value="item.priority"
                  size="small"
                  style="width: 84px"
                  :options="['P0', 'P1', 'P2', 'P3'].map((v) => ({ value: v, label: v }))"
                  @change="(value: string) => setPriority(item, value as ItemPriority)"
                />
              </div>
              <div class="clarify__actions">
                <a-button
                  size="small"
                  type="primary"
                  :loading="pendingActionFor(item) === 'accept'"
                  :disabled="Boolean(pendingActionFor(item))"
                  @click="accept(item)"
                >
                  收下
                </a-button>
                <a-popconfirm
                  title="这条不做？"
                  ok-text="不做"
                  cancel-text="再想想"
                  @confirm="dismiss(item)"
                >
                  <a-button size="small" :disabled="Boolean(pendingActionFor(item))">不做</a-button>
                </a-popconfirm>
                <a-button size="small" type="text" @click="open(item)">打开详情</a-button>
              </div>
            </div>
          </template>
        </ItemCard>
      </div>
      <div v-else class="empty-state">
        <p class="empty-state__title">收件箱是空的</p>
        <p class="empty-state__hint">临时想到的事情可以直接记在这里，不用先想清楚归属。</p>
      </div>

      <p v-if="records.length" class="meta-text" style="margin-top: 12px">
        j / k 选择 · Enter 打开 · 1/2/3/4 设为 P0~P3 · Esc 取消选中 · 行内「整理」就地处理
      </p>
    </a-spin>

    <ItemDrawer v-model:open="drawerOpen" :item-id="activeId" @changed="load" />
  </div>
</template>
