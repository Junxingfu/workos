<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { acceptAll, fail, fetchInbox, quickCapture, updateItem } from '@/api'
import ItemCard from '@/components/ItemCard.vue'
import ItemDrawer from '@/components/ItemDrawer.vue'
import PageHeader from '@/components/PageHeader.vue'
import type { InboxResp, Item, ItemPriority } from '@/types'

/**
 * Inbox 是处理队列，不是长期列表（design.md §29~§31）
 * 防黑洞的两个出口：逐条整理 / 先都收下
 */
const data = ref<InboxResp | null>(null)
const loading = ref(false)
const drawerOpen = ref(false)
const activeId = ref<string | null>(null)
const selectedIndex = ref(0)
const capture = ref('')
const saving = ref(false)

const records = computed(() => data.value?.records ?? [])
const headerContext = computed(() =>
  data.value?.oldestDays ? [`最老一条已压 ${data.value.oldestDays} 天`] : []
)

async function load() {
  loading.value = true
  try {
    data.value = await fetchInbox()
    if (selectedIndex.value >= records.value.length) {
      selectedIndex.value = Math.max(0, records.value.length - 1)
    }
  } catch (e) {
    fail(e)
  } finally {
    loading.value = false
  }
}

function open(item: Item) {
  activeId.value = item.id
  drawerOpen.value = true
}

async function submitCapture() {
  const content = capture.value.trim()
  if (!content || saving.value) return
  saving.value = true
  try {
    await quickCapture(content)
    capture.value = ''
    await load()
  } catch (e) {
    fail(e)
  } finally {
    saving.value = false
  }
}

async function acceptAllItems() {
  try {
    const result = (await acceptAll()) as { accepted: number }
    message.success(`已收下 ${result.accepted} 条，都是 P2`)
    await load()
  } catch (e) {
    fail(e)
  }
}

function organizeOldest() {
  const oldest = records.value[0]
  if (oldest) open(oldest)
}

/** 单行整理：只改优先级，立刻保存并跳到下一条 */
async function setPriority(item: Item, priority: ItemPriority) {
  try {
    await updateItem(item.id, { priority })
    selectedIndex.value = Math.min(selectedIndex.value + 1, Math.max(0, records.value.length - 1))
  } catch (e) {
    fail(e)
  }
}

/** 键盘是加速器，不是唯一入口（design.md §31） */
async function onKeydown(event: KeyboardEvent) {
  const target = event.target as HTMLElement | null
  if (target && ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)) return
  if (drawerOpen.value || !records.value.length) return

  if (event.key === 'ArrowDown' || event.key === 'Tab') {
    event.preventDefault()
    selectedIndex.value = (selectedIndex.value + 1) % records.value.length
    return
  }
  if (event.key === 'ArrowUp') {
    event.preventDefault()
    selectedIndex.value = (selectedIndex.value - 1 + records.value.length) % records.value.length
    return
  }
  if (event.key === 'Enter') {
    const current = records.value[selectedIndex.value]
    if (current) {
      event.preventDefault()
      open(current)
    }
    return
  }
  if (['1', '2', '3', '4'].includes(event.key)) {
    const current = records.value[selectedIndex.value]
    if (current) {
      event.preventDefault()
      await setPriority(current, `P${Number(event.key) - 1}` as ItemPriority)
      await load()
    }
  }
}

onMounted(() => {
  load()
  window.addEventListener('keydown', onKeydown)
})
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div>
    <PageHeader :title="`待整理 ${data?.total ?? 0}`" :context="headerContext">
      <template #actions>
        <a-button size="small" :disabled="!records.length" @click="organizeOldest">
          整理最老的一条
        </a-button>
        <a-button size="small" type="primary" :disabled="!records.length" @click="acceptAllItems">
          先都收下
        </a-button>
      </template>
    </PageHeader>

    <!-- 低摩擦输入：只要求一句话 -->
    <a-input
      v-model:value="capture"
      placeholder="记录一件事…（Enter 创建）"
      allow-clear
      style="margin-bottom: 16px"
      @press-enter="submitCapture"
    />

    <a-spin :spinning="loading">
      <div v-if="records.length" class="list">
        <ItemCard
          v-for="(item, index) in records"
          :key="item.id"
          :item="item"
          :selected="index === selectedIndex"
          @open="open"
        />
      </div>
      <div v-else class="empty-state">
        <p class="empty-state__title">收件箱是空的</p>
        <p class="empty-state__hint">临时想到的事情可以直接记在这里，不用先想清楚归属。</p>
      </div>

      <p v-if="records.length" class="meta-text" style="margin-top: 12px">
        ↑ ↓ 选择 · Enter 打开 · 1/2/3/4 设为 P0~P3 · Tab 下一条
      </p>
    </a-spin>

    <ItemDrawer v-model:open="drawerOpen" :item-id="activeId" @changed="load" />
  </div>
</template>
