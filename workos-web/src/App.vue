<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import {
  CalendarOutlined,
  CarryOutOutlined,
  FolderOutlined,
  InboxOutlined,
  UnorderedListOutlined
} from '@ant-design/icons-vue'
import { fetchInbox, fetchDesktopStorage, fetchAttention } from '@/api'
import QuickCapture from '@/components/QuickCapture.vue'
import { useCaptureRefresh } from '@/composables/useCaptureRefresh'

/**
 * App Shell：Sidebar + Main（见 docs/产品与交互.md、docs/开发规范.md）
 * Sidebar 是导航，不是视觉主体；Active 用左侧色条 + 浅底，不做彩色 Pill。
 */
const route = useRoute()
const router = useRouter()
const isDemo = ref(import.meta.env.VITE_WORKOS_DEMO === 'true')
const isDesktop = import.meta.env.VITE_WORKOS_DESKTOP === 'true'
dayjs.locale('zh-cn')

const captureOpen = ref(false)
const captureRef = ref()
const inboxCount = ref(0)
const attentionCount = ref(0)
let attentionTimer: ReturnType<typeof setInterval>
async function loadAttentionCount() {
  try { attentionCount.value = (await fetchAttention()).filter(n => !n.read && (!n.snoozedUntil || Date.parse(n.snoozedUntil) <= Date.now())).length } catch { /* 下次轮询重试；保留上次数字 */ }
}

/** Ant Design 负责行为，WorkOS 负责视觉：主色与语义色在这里统一 */
const theme = {
  token: {
    colorPrimary: '#4f46e5',
    colorSuccess: '#16a34a',
    colorWarning: '#d97706',
    colorError: '#dc2626',
    colorInfo: '#2563eb',
    colorText: '#1f2329',
    colorTextSecondary: '#646a73',
    colorBorder: '#e5e6eb',
    borderRadius: 6,
    fontSize: 14,
    fontFamily:
      "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif"
  }
}

const navItems = [
  { key: 'today', label: '今天', path: '/today', icon: CarryOutOutlined },
  { key: 'inbox', label: '收件箱', path: '/inbox', icon: InboxOutlined },
  { key: 'items', label: '全部事项', path: '/items', icon: UnorderedListOutlined },
  { key: 'projects', label: '项目', path: '/projects', icon: FolderOutlined },
  { key: 'daily', label: '日报', path: '/daily', icon: CalendarOutlined },
  { key: 'notifications', label: '通知', path: '/notifications', icon: InboxOutlined },
  { key: 'exchange', label: 'Excel 批量维护', path: '/exchange', icon: UnorderedListOutlined }
]
if (isDesktop) navItems.push({ key: 'desktop-data', label: '数据与备份', path: '/desktop-data', icon: FolderOutlined })

function isActive(key: string) {
  if (key === 'projects') return String(route.name ?? '').startsWith('project')
  return route.name === key
}

async function openCapture() {
  captureOpen.value = true
  await nextTick()
  captureRef.value?.focus()
}

/** 侧边栏只展示一个真正有行动价值的数字：收件箱数量 */
async function loadInboxCount() {
  try {
    const inbox = await fetchInbox()
    inboxCount.value = inbox.total ?? 0
  } catch {
    inboxCount.value = 0
  }
}

function onShortcut(event: KeyboardEvent) {
  if (route.name === 'focus') return
  if (event.defaultPrevented || event.isComposing || event.repeat) return
  if ((event.metaKey || event.ctrlKey) && event.shiftKey && event.code === 'KeyK') {
    event.preventDefault()
    void openCapture()
  }
}

useCaptureRefresh(loadInboxCount)
watch(() => route.fullPath, loadInboxCount)
function openWidgetItem(event: Event) {
  const id = (event as CustomEvent<string>).detail
  if (/^\d+$/.test(id)) void router.push({ path: '/items', query: { itemId: id } })
}
onMounted(() => {
  window.addEventListener('workos:open-item', openWidgetItem)
  if (isDesktop) fetchDesktopStorage().then(info => { isDemo.value = info.example }).catch(() => {})
  loadInboxCount()
  loadAttentionCount()
  attentionTimer = setInterval(loadAttentionCount, 60000)
  window.addEventListener('workos:attention-changed', loadAttentionCount)
  window.addEventListener('focus', loadAttentionCount)
  window.addEventListener('keydown', onShortcut)
  window.addEventListener('workos:inbox-changed', loadInboxCount)
})
onUnmounted(() => {
  window.removeEventListener('workos:open-item', openWidgetItem)
  clearInterval(attentionTimer)
  window.removeEventListener('workos:attention-changed', loadAttentionCount)
  window.removeEventListener('focus', loadAttentionCount)
  window.removeEventListener('keydown', onShortcut)
  window.removeEventListener('workos:inbox-changed', loadInboxCount)
})
</script>

<template>
  <a-config-provider :theme="theme" :locale="zhCN">
    <div class="app-shell" :class="{ 'widget-shell': route.name === 'focus' }">
      <aside v-if="route.name !== 'focus'" class="sidebar">
        <div class="sidebar__brand">WorkOS</div>

        <nav class="sidebar__nav">
          <button
            v-for="nav in navItems"
            :key="nav.key"
            type="button"
            class="nav-item"
            :class="{ 'nav-item--active': isActive(nav.key) }"
            @click="router.push(nav.path)"
          >
            <component :is="nav.icon" class="nav-item__icon" />
            <span class="nav-item__label">{{ nav.label }}</span>
            <span v-if="nav.key === 'notifications' && attentionCount" class="nav-item__count">{{ attentionCount }}</span>
            <span v-if="nav.key === 'inbox' && inboxCount" class="nav-item__count">
              {{ inboxCount }}
            </span>
          </button>
        </nav>

        <div class="sidebar__capture">
          <a-button type="primary" block @click="openCapture">记录事项</a-button>
          <a-button v-if="isDesktop" block style="margin-top: 8px" href="/desktop/window/focus">打开任务浮窗</a-button>
          <a-button v-else block style="margin-top: 8px" @click="router.push('/focus')">待办浮窗</a-button>
          <div class="sidebar__hint">⌘ / Ctrl + Shift + K</div>
        </div>
      </aside>

      <main class="app-main" :class="{ 'focus-main': route.name === 'focus' }">
        <div class="app-content">
          <p v-if="isDemo && route.name !== 'focus'" class="demo-notice">示例工作区 · 可自由体验，进度和记录会保留</p>
          <router-view />
        </div>
      </main>
    </div>
    <a-modal :open="captureOpen" title="记录事项" :footer="null" @cancel="captureOpen = false">
      <QuickCapture ref="captureRef" :destination="route.name === 'today' ? 'TODAY' : 'INBOX'" />
      <p class="meta-text">今天做：直接出现在今日计划。待办：稍后安排。待整理：放入收件箱。</p>
    </a-modal>
  </a-config-provider>
</template>

<style scoped>
.widget-shell { min-height: 0; }
.focus-main { padding: 0; min-height: 0; }
.focus-main :deep(.app-content) { padding: 0; margin: 0; max-width: none; }
.demo-notice { margin: 0 0 16px; padding-bottom: 10px; border-bottom: 1px solid var(--color-border); color: var(--color-text-secondary); font-size: 12px; }
.sidebar {
  flex: none;
  width: var(--sidebar-width);
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  padding: var(--space-4) var(--space-3);
  position: sticky;
  top: 0;
  height: 100vh;
}

.sidebar__brand {
  font-size: 14px;
  font-weight: 600;
  padding: 0 var(--space-2) var(--space-4);
}

.sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
  padding: 7px var(--space-2);
  border: none;
  border-radius: 0;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 14px;
  text-align: left;
  cursor: pointer;
}

.nav-item:hover {
  background: var(--color-hover);
}

.nav-item--active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 600;
}

.nav-item--active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 2px;
  background: var(--color-primary);
}

.nav-item__icon {
  font-size: 15px;
}

.nav-item__label {
  flex: 1;
}

.nav-item__count {
  font-size: 12px;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}

.nav-item--active .nav-item__count {
  color: var(--color-primary);
}

.sidebar__capture {
  margin-top: auto;
  padding-top: var(--space-4);
}

.sidebar__hint {
  margin-top: var(--space-1);
  padding-left: var(--space-1);
  font-size: 12px;
  color: var(--color-text-tertiary);
}

@media (max-width: 760px) {
  .app-shell { flex-direction: column; }
  .sidebar { width: 100%; height: auto; position: static; flex-direction: row; flex-wrap: wrap; gap: 8px; border-right: 0; border-bottom: 1px solid var(--color-border); }
  .sidebar__brand, .sidebar__hint { display: none; }
  .sidebar__nav { flex-direction: row; flex-wrap: wrap; }
  .nav-item { width: auto; }
  .sidebar__capture { margin: 0 0 0 auto; padding: 0; }
  .app-content { padding: 16px; }
}
</style>
