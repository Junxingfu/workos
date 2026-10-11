<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  archiveProject,
  fail,
  fetchItems,
  fetchProject,
  restoreProject,
  transition,
  updateProjectContext
} from '@/api'
import ItemCard from '@/components/ItemCard.vue'
import ItemDrawer from '@/components/ItemDrawer.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useListActions } from '@/composables/useListActions'
import type { Item, ItemRowAction, Project } from '@/types'

/**
 * 项目详情：10 秒恢复项目上下文（design.md §33~§35）
 * 默认结构化阅读视图，编辑是显式动作；不做 Markdown 大编辑器。
 */
const route = useRoute()
const router = useRouter()
const project = ref<Project | null>(null)
const loading = ref(false)
const saving = ref(false)
const editing = ref(false)
const archiving = ref(false)
const doingItems = ref<Item[]>([])
const todoItems = ref<Item[]>([])
const waitingItems = ref<Item[]>([])
const drawerOpen = ref(false)
const activeId = ref<string | null>(null)
const drawerIntent = ref<'wait' | 'followUp' | null>(null)

const form = ref({
  goal: '',
  currentFocus: '',
  progressNote: '',
  blocker: '',
  nextAction: '',
  keyPeople: '',
  keyLinks: '',
  notesMarkdown: ''
})

/** 视觉优先级：当前重点 / 阻塞 / 下一步 最先看到 */
const primaryFields = computed(() => [
  { key: 'currentFocus', label: '当前重点', value: form.value.currentFocus },
  { key: 'progressNote', label: '当前进展', value: form.value.progressNote },
  { key: 'nextAction', label: '下一步', value: form.value.nextAction }
])

const secondaryFields = computed(() => [
  { key: 'blocker', label: '当前阻塞', value: form.value.blocker },
  { key: 'goal', label: '当前目标', value: form.value.goal }
])

const tertiaryFields = computed(() => [
  { key: 'keyPeople', label: '关键联系人', value: form.value.keyPeople },
  { key: 'keyLinks', label: '关键链接', value: form.value.keyLinks },
  { key: 'notesMarkdown', label: '备注', value: form.value.notesMarkdown }
])

const staleText = computed(() => {
  const context = project.value?.context
  if (!context?.updatedAt) return null
  return `现状更新于 ${dayjs(context.updatedAt).format('MM-DD HH:mm')} · ${context.staleDays ?? 0} 天前`
})

async function load() {
  loading.value = true
  try {
    const detail = await fetchProject(String(route.params.id))
    project.value = detail
    const context = detail.context
    if (!editing.value) form.value = {
      goal: context?.goal ?? '',
      currentFocus: context?.currentFocus ?? '',
      progressNote: context?.progressNote ?? '',
      blocker: context?.blocker ?? '',
      nextAction: context?.nextAction ?? '',
      keyPeople: context?.keyPeople ?? '',
      keyLinks: context?.keyLinks ?? '',
      notesMarkdown: context?.notesMarkdown ?? ''
    }

    const projectId = String(route.params.id)
    const [doing, waiting, todo] = await Promise.all([
      fetchItems({ projectId, status: 'DOING', pageNo: 1, pageSize: 50 }),
      fetchItems({ projectId, status: 'WAITING', pageNo: 1, pageSize: 50 }),
      fetchItems({ projectId, status: 'TODO', pageNo: 1, pageSize: 50 })
    ])
    todoItems.value = todo.dataList ?? []
    doingItems.value = doing.dataList ?? []
    waitingItems.value = waiting.dataList ?? []
  } catch (e) {
    fail(e)
  } finally {
    loading.value = false
  }
}

/** 项目内的进行中 / 等待中事项，可以直接在行内推进 */
const { run, pendingActionFor } = useListActions(
  () => [...todoItems.value, ...doingItems.value, ...waitingItems.value],
  load,
  { onOpen: (item) => openItem(item, null) }
)

function openItem(item: Item, intent: 'wait' | 'followUp' | null = null) {
  activeId.value = item.id
  drawerIntent.value = intent
  drawerOpen.value = true
}

function onItemAction(action: ItemRowAction, item: Item) {
  switch (action) {
    case 'open':
      return openItem(item, null)
    case 'complete':
      return run(
        item,
        'complete',
        () => transition(item.id, { toStatus: 'DONE', version: item.version }),
        { tip: '已完成' }
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
        () => transition(item.id, { toStatus: 'TODO', version: item.version }),
        { tip: '已重新打开' }
      )
    case 'wait':
      return openItem(item, 'wait')
    case 'followUp':
      return openItem(item, 'followUp')
  }
}

async function save() {
  if (!project.value) return
  saving.value = true
  try {
    await updateProjectContext(project.value.id, { ...form.value })
    message.success('已保存')
    editing.value = false
    await load()
  } catch (e) {
    fail(e)
  } finally {
    saving.value = false
  }
}

async function toggleArchive() {
  if (!project.value) return
  archiving.value = true
  try {
    if (project.value.status === 'ARCHIVED') {
      await restoreProject(project.value.id)
      message.success('已恢复')
    } else {
      await archiveProject(project.value.id)
      message.success('已归档')
    }
    await load()
  } catch (e) {
    fail(e)
  } finally {
    archiving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader
      :title="project?.name ?? ''"
      :sub="[project?.status === 'ARCHIVED' ? '已归档' : '进行中', staleText].filter(Boolean).join(' · ')"
    >
      <template #actions>
        <a-button size="small" type="text" @click="router.push('/projects')">返回项目</a-button>
        <a-button v-if="!editing" size="small" @click="editing = true">编辑现状</a-button>
        <template v-else>
          <a-button size="small" @click="editing = false">取消</a-button>
          <a-button size="small" type="primary" :loading="saving" @click="save">保存</a-button>
        </template>
      </template>
    </PageHeader>

    <!-- 首屏加载才整块 loading；保存现状和归档不再让页面变灰 -->
    <a-spin :spinning="loading && !project">
      <section class="section">
        <div class="section__head">
          <h2 class="section-title">项目现状</h2>
        </div>

        <!-- 阅读视图 -->
        <template v-if="!editing">
          <div class="context-grid">
            <div
              v-for="field in primaryFields"
              :key="field.key"
              class="field field--focus"
              :class="{ 'field--empty': !field.value }"
            >
              <div class="field__label">{{ field.label }}</div>
              <div class="field__value">{{ field.value || '未填写' }}</div>
            </div>
          </div>
          <div class="context-grid">
            <div
              v-for="field in secondaryFields"
              :key="field.key"
              class="field"
              :class="{ 'field--empty': !field.value }"
            >
              <div class="field__label">{{ field.label }}</div>
              <div class="field__value">{{ field.value || '未填写' }}</div>
            </div>
          </div>
          <div class="context-grid">
            <div
              v-for="field in tertiaryFields"
              :key="field.key"
              class="field"
              :class="{ 'field--empty': !field.value }"
            >
              <div class="field__label">{{ field.label }}</div>
              <div class="field__value">{{ field.value || '未填写' }}</div>
            </div>
          </div>
        </template>

        <!-- 编辑视图 -->
        <a-form v-else layout="vertical" size="small">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="当前重点">
                <a-textarea v-model:value="form.currentFocus" :rows="2" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="当前阻塞">
                <a-textarea v-model:value="form.blocker" :rows="2" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="下一步">
                <a-textarea v-model:value="form.nextAction" :rows="2" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="当前进展">
                <a-textarea v-model:value="form.progressNote" :rows="2" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="当前目标">
                <a-textarea v-model:value="form.goal" :rows="2" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="关键联系人">
                <a-input v-model:value="form.keyPeople" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="关键链接">
                <a-input v-model:value="form.keyLinks" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="备注">
            <a-textarea v-model:value="form.notesMarkdown" :rows="4" />
          </a-form-item>
        </a-form>

      </section>

      <section class="section">
        <div class="section__head">
          <h2 class="section-title">待办</h2>
          <a-button class="section__count" size="small" type="link" @click="router.push({ path: '/items', query: { projectId: project?.id } })">查看项目全部事项</a-button>
        </div>
        <div v-if="todoItems.length" class="list">
          <ItemCard v-for="item in todoItems" :key="item.id" :item="item" :pending-action="pendingActionFor(item)" @action="onItemAction" />
        </div>
        <p v-else class="meta-text">当前没有待办；收下并归属到本项目的事项会出现在这里。</p>
      </section>

      <!-- 现状之后直接进入这个项目正在做的事（前端规范.md §7.5） -->
      <section v-if="doingItems.length" class="section">
        <div class="section__head">
          <h2 class="section-title">进行中</h2>
          <span class="section__count">{{ doingItems.length }}</span>
        </div>
        <div class="list">
          <ItemCard
            v-for="item in doingItems"
            :key="item.id"
            :item="item"
            :pending-action="pendingActionFor(item)"
            @action="onItemAction"
          />
        </div>
      </section>

      <section v-if="waitingItems.length" class="section">
        <div class="section__head">
          <h2 class="section-title">等待中</h2>
          <span class="section__count">{{ waitingItems.length }}</span>
        </div>
        <div class="list">
          <ItemCard
            v-for="item in waitingItems"
            :key="item.id"
            :item="item"
            :pending-action="pendingActionFor(item)"
            @action="onItemAction"
          />
        </div>
      </section>

      <section class="section">
        <div class="section__head">
          <a-button size="small" type="text" :loading="archiving" @click="toggleArchive">
            {{ project?.status === 'ARCHIVED' ? '恢复项目' : '归档项目' }}
          </a-button>
          <span class="section__hint">归档是业务状态，不做物理删除</span>
        </div>
      </section>
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

<style scoped>
.context-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0 24px;
  margin-bottom: 8px;
}

@media (max-width: 1280px) {
  .context-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
