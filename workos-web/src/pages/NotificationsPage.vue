<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import dayjs from 'dayjs'
import { fetchAttention, respondAttention, type AttentionNotice, fail } from '@/api'
import ItemDrawer from '@/components/ItemDrawer.vue'
import PageHeader from '@/components/PageHeader.vue'
const notices = ref<AttentionNotice[]>([])
const loading = ref(false)
const error = ref('')
const filter = ref('待处理')
const pending = ref('')
const open = ref(false)
const itemId = ref<string | null>(null)
const now = ref(Date.now())
let timer: ReturnType<typeof setInterval>
const rows = computed(() => notices.value.filter(n => {
  const snoozed = !!n.snoozedUntil && Date.parse(n.snoozedUntil) > now.value
  if (filter.value === '全部') return true
  if (filter.value === '稍后提醒') return snoozed
  return !n.read && !snoozed
}))
async function load() {
  loading.value = true
  try { notices.value = await fetchAttention(); now.value = Date.now(); error.value = ''; window.dispatchEvent(new Event('workos:attention-changed')) } catch (e) { error.value = e instanceof Error ? e.message : '加载失败' } finally { loading.value = false }
}
async function respond(n: AttentionNotice, action: 'READ' | 'SNOOZE' | 'UNREAD') {
  if (pending.value) return
  pending.value = n.key
  try { await respondAttention(n.key, action); await load() } catch (e) { fail(e) } finally { pending.value = '' }
}
function refresh() { if (!open.value && !pending.value) void load() }
onMounted(() => { load(); timer = setInterval(refresh, 60000); window.addEventListener('focus', refresh) })
onUnmounted(() => { clearInterval(timer); window.removeEventListener('focus', refresh) })
</script>
<template>
  <PageHeader title="通知" sub="到期、跟进和今日计划。标为已读只处理提醒，不会完成任务。" />
  <div class="toolbar"><a-segmented v-model:value="filter" :options="['待处理', '稍后提醒', '全部']" /><a-button :loading="loading" @click="load">刷新</a-button></div>
  <a-alert v-if="error" type="error" :message="error" show-icon />
  <a-spin v-if="loading && !notices.length" />
  <a-empty v-else-if="!error && !rows.length" description="当前没有此类通知；新到期或新安排的任务会显示在这里。" />
  <article v-for="n in rows" :key="n.key">
    <div class="notice-content"><span class="notice-reason">{{ n.reason }}{{ n.read ? ' · 已读' : '' }}</span><button class="notice-title" @click="itemId = n.itemId; open = true">{{ n.title }}</button><small v-if="n.snoozedUntil && Date.parse(n.snoozedUntil) > now">稍后提醒至 {{ dayjs(n.snoozedUntil).format('M月D日 HH:mm') }}</small></div>
    <a-space wrap><a-button size="small" :disabled="!!pending" @click="respond(n, n.read ? 'UNREAD' : 'READ')">{{ n.read ? '标为未读' : '标为已读' }}</a-button><a-button size="small" :disabled="!!pending" @click="respond(n, 'SNOOZE')">1 小时后提醒</a-button></a-space>
  </article>
  <ItemDrawer v-model:open="open" :item-id="itemId" @changed="load" />
</template>
<style scoped>
.toolbar { display: flex; gap: 12px; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; }
article { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; padding: 16px 0; border-bottom: 1px solid var(--color-border); }
.notice-content { display: flex; flex-direction: column; gap: 6px; min-width: 0; flex: 1; }
.notice-reason, small { font-size: 12px; color: var(--color-text-secondary); }
.notice-title { padding: 0; border: 0; background: none; cursor: pointer; text-align: left; color: var(--color-text); overflow-wrap: anywhere; font-size: 14px; }
.notice-title:hover { color: var(--color-primary); }
</style>
