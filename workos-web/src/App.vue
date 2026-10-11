<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CalendarOutlined,
  CarryOutOutlined,
  FolderOutlined,
  InboxOutlined,
  UnorderedListOutlined
} from '@ant-design/icons-vue'
import { fetchInbox } from '@/api'
import QuickCapture from '@/components/QuickCapture.vue'
import { useCaptureRefresh } from '@/composables/useCaptureRefresh'

/**
 * App Shell：Sidebar + Main（对齐 前端规范.md §4.1 §4.2）
 * Sidebar 是导航，不是视觉主体；Active 用左侧色条 + 浅底，不做彩色 Pill。
 */
const route = useRoute()
const router = useRouter()

const captureOpen = ref(false)
const captureRef = ref()
const inboxCount = ref(0)

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
  { key: 'daily', label: '日报', path: '/daily', icon: CalendarOutlined }
]

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
  if (event.defaultPrevented || event.isComposing || event.repeat) return
  if ((event.metaKey || event.ctrlKey) && event.shiftKey && event.code === 'KeyK') {
    event.preventDefault()
    void openCapture()
  }
}

useCaptureRefresh(loadInboxCount)
watch(() => route.fullPath, loadInboxCount)
onMounted(() => {
  loadInboxCount()
  window.addEventListener('keydown', onShortcut)
  window.addEventListener('workos:inbox-changed', loadInboxCount)
})
onUnmounted(() => {
  window.removeEventListener('keydown', onShortcut)
  window.removeEventListener('workos:inbox-changed', loadInboxCount)
})
</script>

<template>
  <a-config-provider :theme="theme">
    <div class="app-shell">
      <aside class="sidebar">
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
            <span v-if="nav.key === 'inbox' && inboxCount" class="nav-item__count">
              {{ inboxCount }}
            </span>
          </button>
        </nav>

        <div class="sidebar__capture">
          <a-button type="primary" block @click="openCapture">记录事项</a-button>
          <div class="sidebar__hint">⌘ / Ctrl + Shift + K</div>
        </div>
      </aside>

      <main class="app-main">
        <div class="app-content">
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
</style>
