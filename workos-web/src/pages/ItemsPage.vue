<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fail, fetchItem, fetchItems, fetchProjects, planToday, transition } from '@/api'
import { useCaptureRefresh } from '@/composables/useCaptureRefresh'
import ItemCard from '@/components/ItemCard.vue'
import ItemDrawer from '@/components/ItemDrawer.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useListActions } from '@/composables/useListActions'
import type { BasePageResult, Item, ItemPriority, ItemRowAction, ItemStatus, Project } from '@/types'

/**
 * 全部事项：查询与管理，不是 Dashboard（对齐 前端规范.md §7.3）
 * 默认「未完成」，已完成的事不占视线。
 */
const FILTERS: Array<{ key: string; label: string; status?: ItemStatus }> = [
  { key: 'open', label: '未完成' },
  { key: 'INBOX', label: '待整理', status: 'INBOX' },
  { key: 'TODO', label: '待办', status: 'TODO' },
  { key: 'DOING', label: '进行中', status: 'DOING' },
  { key: 'WAITING', label: '等待中', status: 'WAITING' },
  { key: 'DONE', label: '已完成', status: 'DONE' },
  { key: 'CANCELLED', label: '已取消', status: 'CANCELLED' }
]

const activeFilter = ref('open')
const priority = ref<string | undefined>(undefined)
const route = useRoute()
const projectId = ref<string | undefined>(typeof route.query.projectId === 'string' ? route.query.projectId : undefined)
const unclassified = ref(false)
const data = ref<BasePageResult<Item> | null>(null)
const projects = ref<Project[]>([])
const loading = ref(false)
const loadError = ref(false)
const drawerOpen = ref(false)
const activeId = ref<string | null>(null)
const drawerIntent = ref<'wait' | 'followUp' | null>(null)
const keyword = ref('')
const pageNo = ref(1)
const pageSize = ref(30)
let requestId = 0
let searchTimer: ReturnType<typeof setTimeout> | undefined

async function load() {
  const request = ++requestId
  loading.value = true
  loadError.value = false
  try {
    const filter = FILTERS.find((item) => item.key === activeFilter.value)
    const result = await fetchItems({
      status: filter?.status,
      priority: priority.value as ItemPriority | undefined,
      projectId: unclassified.value ? undefined : projectId.value,
      unclassified: unclassified.value || undefined,
      keyword: keyword.value.trim() || undefined,
      pageNo: pageNo.value,
      pageSize: pageSize.value
    })
    if (request !== requestId) return
    data.value = result
    if (!result.dataList.length && pageNo.value > 1) {
      pageNo.value -= 1
      await load()
    }
  } catch (e) {
    if (request === requestId) { loadError.value = true; fail(e) }
  } finally {
    if (request === requestId) loading.value = false
  }
}

async function loadProjects() {
  try {
    const page = await fetchProjects({ status: 'ACTIVE', pageNo: 1, pageSize: 100 })
    projects.value = page.dataList ?? []
  } catch {
    projects.value = []
  }
}

const { selectedId, run, select, pendingActionFor } = useListActions(
  () => data.value?.dataList ?? [],
  load,
  { onOpen: (item) => open(item, null) }
)

function open(item: Item, intent: 'wait' | 'followUp' | null = null) {
  activeId.value = item.id
  drawerIntent.value = intent
  select(item)
  drawerOpen.value = true
}

async function openById(id: string) {
  const item = await fetchItem(id)
  open(item, null)
}

async function undoComplete(id: string) {
  const fresh = await fetchItem(id)
  await transition(id, { toStatus: fresh.type === 'NOTE' ? 'INBOX' : 'TODO', version: fresh.version })
}

function onAction(action: ItemRowAction, item: Item) {
  switch (action) {
    case 'open':
      return open(item, null)
    case 'accept':
      return run(item, 'accept', () => transition(item.id, { toStatus: 'TODO', version: item.version }), { tip: '已收下' })
    case 'planToday':
      return run(item, 'planToday', () => planToday(item.id, item.version), { tip: '已加入今日计划', keepSelection: true })
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
        () => transition(item.id, { toStatus: item.type === 'NOTE' ? 'INBOX' : 'TODO', version: item.version }),
        { tip: '已重新打开' }
      )
    case 'wait':
      return open(item, 'wait')
    case 'followUp':
      return open(item, 'followUp')
  }
}

function resetAndLoad() {
  clearTimeout(searchTimer)
  pageNo.value = 1
  void load()
}
function changePage(page: number, size: number) {
  pageNo.value = page
  pageSize.value = size
  void load()
}
watch([activeFilter, priority, projectId, unclassified], resetAndLoad)
watch(keyword, () => {
  clearTimeout(searchTimer)
  requestId += 1
  searchTimer = setTimeout(resetAndLoad, 300)
})
useCaptureRefresh(resetAndLoad)
onUnmounted(() => { clearTimeout(searchTimer); requestId += 1 })
onMounted(() => {
  load()
  loadProjects()
})
</script>

<template>
  <div>
    <PageHeader :title="data ? `全部事项 ${data.total}` : '全部事项'">
      <template #actions>
        <a-input
          v-model:value="keyword"
          size="small"
          placeholder="搜索标题或描述"
          allow-clear
          style="width: 200px"
        />
      </template>
    </PageHeader>

    <div class="section__head" style="flex-wrap: wrap">
      <a-segmented
        v-model:value="activeFilter"
        size="small"
        :options="FILTERS.map((item) => ({ value: item.key, label: item.label }))"
      />
      <a-select
        v-model:value="priority"
        size="small"
        style="width: 96px"
        placeholder="优先级"
        allow-clear
        :options="['P0', 'P1', 'P2', 'P3'].map((v) => ({ value: v, label: v }))"
      />
      <a-select
        v-model:value="projectId"
        :disabled="unclassified"
        size="small"
        style="width: 160px"
        placeholder="项目"
        allow-clear
        :options="projects.map((p) => ({ value: p.id, label: p.name }))"
      />
      <a-checkbox v-model:checked="unclassified">未分类</a-checkbox>
    </div>

    <!-- 首屏加载才整块 loading；切筛选和行内动作都不再让列表变灰 -->
    <a-spin :spinning="loading && !data">
      <a-alert v-if="loadError" type="error" message="事项未加载成功，请重试。" style="margin-bottom: 12px">
        <template #action><a-button size="small" @click="load">重试</a-button></template>
      </a-alert>
      <a-skeleton v-if="loading && !data" active />
      <div v-if="data?.dataList?.length" class="list">
        <ItemCard
          v-for="item in data.dataList"
          :key="item.id"
          :item="item"
          decision-mode
          :selected="item.id === selectedId"
          :pending-action="pendingActionFor(item)"
          @action="onAction"
        />
      </div>
      <div v-else-if="data && !loading && !loadError" class="empty-state">
        <p class="empty-state__title">这里没有符合条件的事项</p>
        <p class="empty-state__hint">换个筛选，或者用侧边栏的「记录事项」先记下来。</p>
      </div>

      <a-pagination v-if="data && data.total > pageSize" :current="pageNo" :page-size="pageSize" :total="data.total" :show-size-changer="false" size="small" style="margin-top: 16px" @change="changePage" />
      <p v-if="data?.dataList?.length" class="meta-text" style="margin-top: 12px">
        j / k 选择 · Enter 打开 · Esc 取消选中
      </p>
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
