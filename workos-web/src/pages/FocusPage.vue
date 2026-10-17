<script setup lang="ts">
import dayjs from 'dayjs'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ExportOutlined, CloseOutlined, DownOutlined, UpOutlined, HolderOutlined, SlidersOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { fetchTaskOverview, type TaskOverview } from '@/api'
import { useCaptureRefresh } from '@/composables/useCaptureRefresh'

const host = window as Window & { __WORKOS_WIDGET__?: boolean; __WORKOS_WIDGET_OPACITY__?: number }
const desktop = host.__WORKOS_WIDGET__ === true
const data = ref<TaskOverview | null>(null)
const expanded = ref(false)
const selected = ref<TaskOverview['selected']>('TODO')
const error = ref('')
const loading = ref(false)
const preferences = ref(false)
const opacity = ref(host.__WORKOS_WIDGET_OPACITY__ ?? 92)
const updated = ref('')
const categories = computed(() => [
  { key: 'TODO' as const, label: '待办', count: data.value?.todo },
  { key: 'DOING' as const, label: '进行中', count: data.value?.doing },
  { key: 'WAITING' as const, label: '等待中', count: data.value?.waiting }
])
const selectedCount = computed(() => categories.value.find(c => c.key === selected.value)?.count ?? 0)
let timer: ReturnType<typeof setInterval>
let request = 0
function native(action: string) { if (desktop) window.location.assign(`/desktop/window/focus/${action}`) }
function toggle() {
  expanded.value = !expanded.value
  preferences.value = false
  native(expanded.value ? 'expand' : 'collapse')
}
function keydown(event: KeyboardEvent) { if (event.key === 'Escape' && expanded.value) toggle() }
async function load() {
  const sequence = ++request
  loading.value = true
  try {
    const result = await fetchTaskOverview(selected.value)
    if (sequence === request) { data.value = result; error.value = ''; updated.value = dayjs().format('HH:mm') }
  } catch {
    if (sequence === request) error.value = '暂时无法更新，请重试'
  } finally { if (sequence === request) loading.value = false }
}
function choose(status: TaskOverview['selected']) { selected.value = status; void load() }
function drag(event: MouseEvent) {
  if (event.button === 0 && !(event.target as Element).closest('button, a, input')) native('drag')
}
function saveOpacity() { native(`opacity?value=${opacity.value}`) }
let resizeStart: { x: number; y: number; width: number; height: number } | null = null
let lastResize = 0
function startResize(event: PointerEvent) {
  if (event.button !== 0) return
  resizeStart = { x: event.screenX, y: event.screenY, width: window.innerWidth, height: window.innerHeight }
  ;(event.currentTarget as HTMLElement).setPointerCapture(event.pointerId)
}
function resize(event: PointerEvent) {
  if (!resizeStart || performance.now() - lastResize < 32) return
  lastResize = performance.now()
  native(`resize?width=${resizeStart.width + event.screenX - resizeStart.x}&height=${resizeStart.height + event.screenY - resizeStart.y}`)
}
function endResize() { if (resizeStart) { resizeStart = null; native('remember') } }
function resizeKey(event: KeyboardEvent) {
  const x = event.key === 'ArrowRight' ? 20 : event.key === 'ArrowLeft' ? -20 : 0
  const y = event.key === 'ArrowDown' ? 20 : event.key === 'ArrowUp' ? -20 : 0
  if (x || y) { event.preventDefault(); native(`resize?width=${window.innerWidth + x}&height=${window.innerHeight + y}`) }
}
useCaptureRefresh(load)
onMounted(() => {
  if (desktop) document.documentElement.classList.add('native-task-widget')
  void load()
  timer = setInterval(load, 30000)
  window.addEventListener('focus', load)
  window.addEventListener('keydown', keydown)
})
onUnmounted(() => {
  ++request
  document.documentElement.classList.remove('native-task-widget')
  clearInterval(timer)
  window.removeEventListener('focus', load)
  window.removeEventListener('keydown', keydown)
})
</script>
<template>
  <section class="widget-frame" :class="{ expanded, desktop }" :style="{ '--panel-opacity': opacity / 100 }" aria-label="WorkOS 桌面待办">
    <div class="task-widget">
      <header @mousedown="drag">
        <span class="brand" title="拖动浮窗"><HolderOutlined v-if="desktop" /><span class="brand-mark">W</span></span>
        <button class="counter" :aria-expanded="expanded" aria-controls="task-summary" @click="toggle">
          <span class="counter-label">{{ expanded ? '我的工作' : '待办事项' }}<small>{{ error ? '连接异常' : expanded ? '留一点空间，专注眼前' : '点击展开' }}</small></span>
          <strong v-if="!expanded && !error">{{ data?.todo ?? '—' }}</strong>
          <UpOutlined v-if="expanded" class="chevron" /><DownOutlined v-else class="chevron" />
        </button>
        <a v-if="desktop" class="icon-button" href="/desktop/window/main" title="打开 WorkOS" aria-label="打开 WorkOS"><ExportOutlined /></a>
        <router-link v-else class="icon-button" to="/today" title="打开 WorkOS" aria-label="打开 WorkOS"><ExportOutlined /></router-link>
        <button v-if="desktop" class="icon-button close" title="关闭浮窗，可从应用菜单重新打开" aria-label="关闭浮窗" @click="native('hide')"><CloseOutlined /></button>
        <router-link v-else class="icon-button close" to="/today" title="关闭预览" aria-label="关闭预览"><CloseOutlined /></router-link>
      </header>
      <div v-if="expanded" id="task-summary" class="summary">
        <nav aria-label="任务状态">
          <button v-for="category in categories" :key="category.key" :aria-pressed="selected === category.key" @click="choose(category.key)">
            {{ category.label }} <span>{{ category.count ?? '—' }}</span>
          </button>
        </nav>
        <div v-if="preferences" class="preferences">
          <label for="widget-opacity">背景不透明度 <b>{{ opacity }}%</b></label>
          <input id="widget-opacity" v-model.number="opacity" type="range" min="35" max="100" step="1" @change="saveOpacity" />
          <small>文字保持清晰 · 拖动右下角调整大小</small>
        </div>
        <div v-if="error" class="empty" role="alert">{{ error }}<button @click="load">重新连接</button></div>
        <div v-else-if="loading && data?.selected !== selected" class="empty">正在读取任务…</div>
        <ul v-else-if="data?.tasks.length" :aria-busy="loading">
          <li v-for="item in data.tasks" :key="item.id">
            <component :is="desktop ? 'a' : 'router-link'" class="task-link" :href="desktop ? `/desktop/window/main?item=${item.id}` : undefined" :to="desktop ? undefined : { path: '/items', query: { itemId: item.id } }">
              <span class="priority" :class="item.priority" :title="`优先级 ${item.priority}`">{{ item.priority }}</span>
              <div class="task-content">
                <p class="title">{{ item.title }}</p>
                <p class="project">{{ item.projectName || '未分类' }}<span v-if="item.relatedProjects?.length"> · 关联 {{ item.relatedProjects.length }} 个项目</span></p>
                <div class="facts">
                  <span v-if="item.plannedDate">计划 {{ dayjs(item.plannedDate).format('M/D') }}</span>
                  <span v-if="item.dueAt" :class="{ risk: item.overdue }">{{ item.overdue ? '已逾期' : '截止' }} {{ dayjs(item.dueAt).format('M/D HH:mm') }}</span>
                  <span v-if="item.nextFollowUpAt" :class="{ risk: item.followUpDue }">跟进 {{ dayjs(item.nextFollowUpAt).format('M/D HH:mm') }}</span>
                  <span v-if="item.waitingFor">等待 {{ item.waitingFor }}<template v-if="item.waitingDays"> · {{ item.waitingDays }} 天</template></span>
                  <span v-if="item.remainingSteps">剩余 {{ item.remainingSteps }} 步</span>
                </div>
                <p v-if="item.lastLogContent || item.description" class="context">{{ item.lastLogContent || item.description }}</p>
              </div>
              <ExportOutlined class="row-arrow" />
            </component>
          </li>
        </ul>
        <div v-else class="empty"><span class="empty-line"></span><strong>这里暂时没有任务</strong><small>去主应用记录，浮窗会自动更新</small></div>
        <footer>
          <button class="icon-button" :aria-pressed="preferences" title="外观设置" aria-label="外观设置" @click="preferences = !preferences"><SlidersOutlined /></button>
          <button class="icon-button" title="刷新任务" aria-label="刷新任务" :disabled="loading" @click="load"><ReloadOutlined :spin="loading" /></button>
          <span class="footnote">{{ selectedCount > 30 ? '显示前 30 条' : `${selectedCount} 条` }} · {{ updated || '等待更新' }}</span>
          <a v-if="desktop" href="/desktop/window/main" class="app-link">打开应用 <ExportOutlined /></a>
          <router-link v-else to="/items" class="app-link">打开应用 <ExportOutlined /></router-link>
        </footer>
        <button v-if="desktop" class="resize-grip" title="拖动调整大小" aria-label="拖动调整浮窗大小" @pointerdown.prevent="startResize" @pointermove="resize" @pointerup="endResize" @pointercancel="endResize" @keydown="resizeKey" @keyup="native('remember')">◢</button>
      </div>
    </div>
  </section>
</template>
<style>
html.native-task-widget, .native-task-widget body, .native-task-widget #app,
.native-task-widget .app-shell, .native-task-widget .app-main { background: transparent !important; min-height: 0 !important; overflow: hidden; }
</style>
<style scoped>
.widget-frame { --ink: #27313d; --muted: #697583; width: 280px; height: 76px; padding: 6px; color: var(--ink); }
.widget-frame.expanded { width: 380px; height: 520px; }
.widget-frame.desktop { width: 100vw; height: 100vh; }
.task-widget { height: 100%; overflow: hidden; display: flex; flex-direction: column; background: rgb(250 252 255 / var(--panel-opacity)); border: 1px solid rgb(116 130 151 / 25%); border-radius: 16px; box-shadow: 0 2px 5px rgb(27 37 55 / 12%); }
header { display: flex; flex: 0 0 62px; align-items: center; padding: 0 10px; gap: 3px; user-select: none; cursor: grab; }
button, a { -webkit-app-region: no-drag; }
button { font: inherit; cursor: pointer; border: 0; color: inherit; background: transparent; }
button:focus-visible, a:focus-visible, input:focus-visible { outline: 2px solid #7064c4; outline-offset: -2px; }
.brand { display: flex; gap: 3px; align-items: center; color: #8a929e; font-size: 10px; }
.brand-mark { display: grid; place-items: center; width: 26px; height: 26px; color: #6552b3; background: rgb(113 88 192 / 9%); border-radius: 8px; font-size: 14px; font-weight: 700; }
.counter { display: flex; align-items: center; gap: 8px; flex: 1; padding: 8px 5px; text-align: left; min-width: 0; }
.counter-label { font-size: 12px; font-weight: 600; flex: 1; white-space: nowrap; }
.counter-label small { display: block; color: var(--muted); font-size: 10px; font-weight: 400; margin-top: 2px; }
.counter strong { font-size: 24px; font-weight: 600; font-variant-numeric: tabular-nums; }
.chevron { color: var(--muted); font-size: 10px; }
.icon-button { flex-shrink: 0; display: inline-grid; place-items: center; width: 28px; height: 28px; border-radius: 7px; color: var(--muted); font-size: 13px; }
.icon-button:hover, .icon-button[aria-pressed="true"] { color: #6552b3; background: rgb(113 88 192 / 10%); }
.close:hover { color: #a43d43; background: rgb(164 61 67 / 10%); }
.summary { flex: 1; min-height: 0; position: relative; display: flex; flex-direction: column; padding: 0 14px; }
nav { display: flex; padding: 4px; gap: 3px; background: rgb(93 108 132 / 7%); border-radius: 10px; }
nav button { flex: 1; padding: 7px 4px; border-radius: 7px; font-size: 12px; white-space: nowrap; }
nav button span { font-size: 11px; margin-left: 4px; opacity: .75; font-variant-numeric: tabular-nums; }
nav button[aria-pressed="true"] { background: rgb(255 255 255 / 70%); box-shadow: 0 1px 3px rgb(20 35 60 / 8%); color: #6552b3; font-weight: 600; }
ul { margin: 8px 0 0; padding: 0; list-style: none; flex: 1; overflow: auto; scrollbar-width: thin; }
li + li { border-top: 1px solid rgb(105 117 131 / 12%); }
.task-link { display: flex; gap: 9px; padding: 13px 2px; color: inherit; text-decoration: none; }
.task-link:hover { background: rgb(113 88 192 / 5%); }
.priority { flex: 0 0 23px; height: 19px; border: 1px solid rgb(105 117 131 / 20%); border-radius: 5px; color: var(--muted); font-size: 10px; text-align: center; line-height: 17px; margin-top: 2px; }
.priority.P0, .priority.P1 { color: #b25835; border-color: rgb(178 88 53 / 25%); }
.task-content { flex: 1; min-width: 0; }
p { margin: 0; }
.title { font-size: 13px; line-height: 1.55; font-weight: 500; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; overflow-wrap: anywhere; }
.project { margin-top: 4px; font-size: 10px; color: var(--muted); }
.facts { display: flex; flex-wrap: wrap; gap: 3px 8px; margin-top: 4px; font-size: 10px; color: #596c83; }
.risk { color: #ab4f38; }
.context { margin-top: 6px; color: var(--muted); font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.row-arrow { color: var(--muted); font-size: 11px; margin-top: 5px; opacity: .5; }
footer { display: flex; flex-shrink: 0; align-items: center; gap: 2px; border-top: 1px solid rgb(105 117 131 / 15%); min-height: 43px; padding-bottom: 3px; }
.footnote { color: var(--muted); font-size: 10px; flex: 1; margin-left: 4px; }
.app-link { font-size: 11px; color: #6552b3; white-space: nowrap; }
.preferences { padding: 10px 5px; border-bottom: 1px solid rgb(105 117 131 / 12%); font-size: 11px; }
.preferences label { display: flex; justify-content: space-between; }
.preferences input { width: 100%; accent-color: #7766bb; margin: 7px 0; }
.preferences small { color: var(--muted); font-size: 10px; }
.empty { flex: 1; display: flex; align-items: center; justify-content: center; flex-direction: column; gap: 8px; color: var(--muted); font-size: 12px; min-height: 70px; }
.empty button { color: #6552b3; }.empty strong { font-weight: 500; }.empty small { font-size: 11px; }
.empty-line { width: 28px; height: 2px; background: #b7aecf; margin-bottom: 6px; }
.resize-grip { position: absolute; bottom: -1px; right: 1px; color: #a0a8b5; cursor: nwse-resize; font-size: 10px; padding: 0; width: 14px; height: 14px; }
</style>
