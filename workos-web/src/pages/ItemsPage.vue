<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { fail, fetchItems } from '@/api'
import ItemCard from '@/components/ItemCard.vue'
import ItemDrawer from '@/components/ItemDrawer.vue'
import PageHeader from '@/components/PageHeader.vue'
import type { BasePageResult, Item, ItemStatus } from '@/types'

/**
 * 全部事项：Items 的预设筛选视图（design.md §13 一级导航）
 * 默认「未完成」，因为已完成的事不需要占视线。
 */
const FILTERS: Array<{ key: string; label: string; status?: ItemStatus }> = [
  { key: 'open', label: '未完成' },
  { key: 'TODO', label: '待办', status: 'TODO' },
  { key: 'DOING', label: '进行中', status: 'DOING' },
  { key: 'WAITING', label: '等待中', status: 'WAITING' },
  { key: 'DONE', label: '已完成', status: 'DONE' }
]

const activeFilter = ref('open')
const data = ref<BasePageResult<Item> | null>(null)
const loading = ref(false)
const drawerOpen = ref(false)
const activeId = ref<string | null>(null)
const keyword = ref('')

async function load() {
  loading.value = true
  try {
    const filter = FILTERS.find((item) => item.key === activeFilter.value)
    data.value = await fetchItems({
      status: filter?.status,
      keyword: keyword.value.trim() || undefined,
      pageNo: 1,
      pageSize: 50
    })
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

watch(activeFilter, load)
watch(keyword, (() => {
  let timer: ReturnType<typeof setTimeout>
  return () => {
    clearTimeout(timer)
    timer = setTimeout(load, 300)
  }
})())
onMounted(load)
</script>

<template>
  <div>
    <PageHeader :title="`全部事项 ${data?.total ?? 0}`">
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

    <div class="section__head">
      <a-segmented
        v-model:value="activeFilter"
        size="small"
        :options="FILTERS.map((item) => ({ value: item.key, label: item.label }))"
      />
    </div>

    <a-spin :spinning="loading">
      <div v-if="data?.dataList?.length" class="list">
        <ItemCard v-for="item in data.dataList" :key="item.id" :item="item" @open="open" />
      </div>
      <div v-else class="empty-state">
        <p class="empty-state__title">这里没有符合条件的事项</p>
        <p class="empty-state__hint">换个筛选，或者用侧边栏的「记录一件事」先记下来。</p>
      </div>
    </a-spin>

    <ItemDrawer v-model:open="drawerOpen" :item-id="activeId" @changed="load" />
  </div>
</template>
