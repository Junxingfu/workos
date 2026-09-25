<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CalendarOutlined,
  CarryOutOutlined,
  FolderOutlined,
  InboxOutlined,
  UnorderedListOutlined
} from '@ant-design/icons-vue'
import { fetchInbox, quickCapture } from '@/api'

/**
 * 应用外壳：Sidebar + Main（design.md §12 §13）
 * 侧边栏只放一级导航与一个低摩擦的快速记录入口，不堆统计。
 */
const route = useRoute()
const router = useRouter()

const capture = ref('')
const captureRef = ref()
const saving = ref(false)
const inboxCount = ref(0)

const navItems = [
  { key: 'today', label: '今天', path: '/today', icon: CarryOutOutlined },
  { key: 'inbox', label: '待整理', path: '/inbox', icon: InboxOutlined },
  { key: 'items', label: '全部事项', path: '/items', icon: UnorderedListOutlined },
  { key: 'projects', label: '项目', path: '/projects', icon: FolderOutlined },
  { key: 'daily', label: '日报', path: '/daily', icon: CalendarOutlined }
]

function isActive(key: string) {
  if (key === 'projects') return String(route.name ?? '').startsWith('project')
  return route.name === key
}

async function submitCapture() {
  const content = capture.value.trim()
  if (!content || saving.value) return
  saving.value = true
  try {
    await quickCapture(content)
    capture.value = ''
    message.success('已进待整理')
    await loadInboxCount()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '记录失败')
  } finally {
    saving.value = false
  }
}

/** 侧边栏只展示一个真正有行动价值的数字：待整理数量 */
async function loadInboxCount() {
  try {
    const inbox = await fetchInbox()
    inboxCount.value = inbox.total ?? 0
  } catch {
    inboxCount.value = 0
  }
}

function onShortcut(event: KeyboardEvent) {
  if (event.altKey && event.code === 'Space') {
    event.preventDefault()
    captureRef.value?.focus()
  }
}

watch(() => route.fullPath, loadInboxCount)
onMounted(() => {
  loadInboxCount()
  window.addEventListener('keydown', onShortcut)
})
onUnmounted(() => window.removeEventListener('keydown', onShortcut))
</script>

<template>
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
          <span v-if="nav.key === 'inbox' && inboxCount" class="nav-item__count">{{ inboxCount }}</span>
        </button>
      </nav>

      <div class="sidebar__capture">
        <a-input
          ref="captureRef"
          v-model:value="capture"
          size="small"
          placeholder="记录一件事…"
          allow-clear
          @press-enter="submitCapture"
        />
        <div class="sidebar__hint">Alt + Space</div>
      </div>
    </aside>

    <main class="app-main">
      <div class="app-content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<style scoped>
.sidebar {
  flex: none;
  width: var(--sidebar-width);
  background: var(--bg-surface);
  border-right: 1px solid var(--border);
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
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
  padding: 7px var(--space-2);
  border: none;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-2);
  font-size: 14px;
  text-align: left;
  cursor: pointer;
}

.nav-item:hover {
  background: var(--bg-subtle);
}

.nav-item--active {
  background: #f0f7ff;
  color: var(--primary);
  font-weight: 500;
}

.nav-item__icon {
  font-size: 16px;
}

.nav-item__label {
  flex: 1;
}

.nav-item__count {
  font-size: 12px;
  color: var(--text-3);
  font-variant-numeric: tabular-nums;
}

.nav-item--active .nav-item__count {
  color: var(--primary);
}

.sidebar__capture {
  margin-top: auto;
  padding-top: var(--space-4);
}

.sidebar__hint {
  margin-top: var(--space-1);
  padding-left: var(--space-1);
  font-size: 12px;
  color: var(--text-3);
}
</style>
