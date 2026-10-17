<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import ItemStateFlow from '@/components/ItemStateFlow.vue'
import {
  createWorkLog,
  deleteItem,
  fail,
  fetchActivities,
  fetchItem,
  fetchProjects,
  fetchWorkLogs,
  followUpAction,
  planItem,
  transition,
  unplanItem,
  updateItem
} from '@/api'
import type { Item, ItemActivity, ItemPriority, ItemStatus, Project, WorkLog } from '@/types'

/**
 * 事项操作中心（见 docs/产品与交互.md、docs/开发规范.md）
 * 第一屏：标题 / 项目·状态·优先级 / 描述 / 现在需要做什么
 * 属性改动即时保存；标题与描述走显式保存。
 */
const props = defineProps<{
  open: boolean
  itemId?: string | null
  /** 打开时直接落到某个动作：wait → 在等别人表单，followUp → 跟进表单 */
  intent?: 'wait' | 'followUp' | null
}>()
const emit = defineEmits<{
  'update:open': [boolean]
  changed: []
  'clear-intent': []
}>()

const loading = ref(false)
const actionOnly = ref(false)
const saving = ref(false)
const transitioning = ref(false)
const noteSaving = ref(false)
let loadRequest = 0
const busy = computed(() => saving.value || transitioning.value || noteSaving.value)
const item = ref<Item | null>(null)
const projects = ref<Project[]>([])
const relatedDraft = ref<string[]>([])
const relatedEditing = ref(false)
const projectOptions = computed(() => {
  const options = new Map(projects.value.map(p => [p.id, { value: p.id, label: p.name }]))
  if (item.value?.projectId && !options.has(item.value.projectId)) options.set(item.value.projectId, { value: item.value.projectId, label: `${item.value.projectName}（已归档）` })
  for (const p of item.value?.relatedProjects ?? []) if (!options.has(p.projectId)) options.set(p.projectId, { value: p.projectId, label: `${p.projectName}（已归档）` })
  return [...options.values()]
})
const activities = ref<ItemActivity[]>([])
const workLogs = ref<WorkLog[]>([])
const note = ref('')
const title = ref('')
const description = ref('')

const waitingModal = ref({
  open: false,
  waitingFor: '',
  waitingExpectedAt: null as Dayjs | null,
  nextFollowUpAt: null as Dayjs | null
})

const followUpModal = ref({
  open: false,
  outcome: 'STILL_WAITING' as 'STILL_WAITING' | 'RESOLVED' | 'ABANDONED',
  note: '',
  nextFollowUpAt: null as Dayjs | null,
  toStatus: 'DOING' as ItemStatus
})

const planDate = ref<Dayjs | null>(null)
const followUpDate = ref<Dayjs | null>(null)

const STATUS_LABEL: Record<ItemStatus, string> = {
  INBOX: '待整理',
  TODO: '待办',
  DOING: '进行中',
  WAITING: '等待中',
  DONE: '已完成',
  CANCELLED: '已取消'
}

const ACTIVITY_LABEL: Record<string, string> = {
  CREATED: '创建事项', STATUS_CHANGED: '状态变更', FIELD_CHANGED: '信息修改',
  PLAN_CHANGED: '计划调整', WAITING_ENDED: '结束等待'
}

const ACTION_LABEL: Record<ItemStatus, string> = {
  TODO: '转为待办',
  DOING: '开始做',
  WAITING: '在等别人',
  DONE: '完成',
  CANCELLED: '取消',
  INBOX: '退回待整理'
}

const canWait = computed(() => item.value?.allowedTransitions?.includes('WAITING'))
const canCancel = computed(() => item.value?.allowedTransitions?.includes('CANCELLED'))
const isWaiting = computed(() => item.value?.status === 'WAITING')
const canFollowUp = computed(() => item.value?.followUpDue || isWaiting.value)

/**
 * Drawer 的主要动作随状态变化（见 docs/产品与交互.md、docs/开发规范.md）：
 * 用户真正想做的是「推进这件事」，不是「保存」。
 */
const primary = computed<{ status: ItemStatus; label: string } | null>(() => {
  const current = item.value
  if (!current) return null
  const can = (target: ItemStatus) => (current.allowedTransitions ?? []).includes(target)
  if (current.status === 'WAITING' && can('DOING')) return { status: 'DOING', label: '恢复处理' }
  if (current.status === 'CANCELLED' && can('TODO')) return { status: 'TODO', label: '重新打开' }
  if (current.type === 'NOTE' && can('INBOX')) return { status: 'INBOX', label: '恢复记录' }
  if (current.status === 'DONE') return can('TODO') ? { status: 'TODO', label: '重新打开' } : null
  if (current.status === 'INBOX' && can('TODO')) return { status: 'TODO', label: '收下为待办' }
  if (current.status === 'INBOX' && can('DONE')) return { status: 'DONE', label: '保存笔记' }
  if (current.status === 'TODO' && can('DOING')) return { status: 'DOING', label: '开始处理' }
  if (can('DONE')) return { status: 'DONE', label: '完成事项' }
  if (can('DOING')) return { status: 'DOING', label: '开始' }
  return null
})

/** 抽屉头部第二行：项目 · 优先级 · 状态（不显示技术字段） */
const drawerMeta = computed(() => {
  const current = item.value
  if (!current) return ''
  return [current.projectName, current.priority, current.type === 'NOTE' && current.status === 'DONE' ? '已保存' : STATUS_LABEL[current.status]]
    .filter(Boolean)
    .join(' · ')
})

/** 用户正在编辑的文本不能被刷新覆盖，否则会出现「刚打的字自己没了」 */
const titleDirty = computed(() => !!item.value && title.value !== item.value.title)
const descriptionDirty = computed(
  () => !!item.value && description.value !== (item.value.description ?? '')
)

/** 用服务端事实刷新本地字段，但不覆盖正在编辑的文本 */
function applyDetail(detail: Item) {
  const keepTitle = titleDirty.value
  const keepDescription = descriptionDirty.value
  item.value = detail
  if (!keepTitle) title.value = detail.title
  if (!keepDescription) description.value = detail.description ?? ''
  if (!relatedEditing.value) relatedDraft.value = detail.relatedProjects?.map(p => p.projectId) ?? []
  planDate.value = detail.plannedDate ? dayjs(detail.plannedDate) : null
  followUpDate.value = detail.nextFollowUpAt ? dayjs(detail.nextFollowUpAt) : null
}

/** 打开抽屉：详情 + 历史 + 记录，三个请求并行取 */
async function load() {
  if (!props.itemId) return
  const id = props.itemId
  const request = ++loadRequest
  loading.value = true
  try {
    const detail = await fetchItem(id)
    if (request !== loadRequest || id !== props.itemId) return
    applyDetail(detail)
    const [acts, logs] = await Promise.all([
      fetchActivities(id),
      fetchWorkLogs({ itemId: id })
    ])
    if (request !== loadRequest || id !== props.itemId) return
    activities.value = acts
    workLogs.value = logs
  } catch (e) {
    fail(e)
  } finally {
    if (request === loadRequest) loading.value = false
  }
}

/**
 * 动作之后只刷新事实：状态/属性变了才需要重拉历史，
 * 不再每次操作都把详情、历史、记录全部拉一遍。
 */
async function refresh(alsoActivities = false) {
  if (!props.itemId) return
  try {
    const detail = await fetchItem(props.itemId)
    applyDetail(detail)
    if (alsoActivities) activities.value = await fetchActivities(props.itemId)
  } catch (e) {
    fail(e)
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

async function saveBasics() {
  if (!item.value || busy.value) return
  if (!title.value.trim()) { message.warning('标题不能为空'); return }
  saving.value = true
  try {
    await updateItem(item.value.id, { version: item.value.version, title: title.value.trim(), description: description.value })
    message.success('已保存')
    emit('changed')
    await refresh()
  } catch (e) {
    fail(e)
  } finally {
    saving.value = false
  }
}

/** 单选类属性即时保存：少一次点击，路径更短 */
async function patch(payload: Parameters<typeof updateItem>[1], tip = '已更新') {
  if (!item.value || busy.value) return false
  saving.value = true
  try {
    await updateItem(item.value.id, { ...payload, version: item.value.version })
    message.success(tip)
    emit('changed')
    await refresh()
    return true
  } catch (e) {
    fail(e)
    return false
  } finally {
    saving.value = false
  }
}

function cancelRelatedEdit() {
  relatedDraft.value = item.value?.relatedProjects?.map(p => p.projectId) ?? []
  relatedEditing.value = false
}
async function saveRelatedProjects() {
  if (await patch({ relatedProjectIds: relatedDraft.value }, '已更新关联项目')) cancelRelatedEdit()
}

async function onTransition(to: ItemStatus) {
  if (!item.value || busy.value) return
  if (to === 'WAITING') return openWaitingForm()
  transitioning.value = true
  try {
    await transition(item.value.id, { toStatus: to, version: item.value.version })
    message.success(ACTION_LABEL[to] ?? '已更新')
    emit('changed')
    await refresh(true)
  } catch (e) {
    fail(e)
  } finally {
    transitioning.value = false
  }
}

function openWaitingForm() {
  waitingModal.value = {
    open: true,
    waitingFor: item.value?.waitingFor ?? '',
    waitingExpectedAt: item.value?.waitingExpectedAt ? dayjs(item.value.waitingExpectedAt) : null,
    nextFollowUpAt: item.value?.nextFollowUpAt
      ? dayjs(item.value.nextFollowUpAt)
      : dayjs().add(2, 'day').hour(10).minute(0)
  }
}

async function submitWaiting() {
  if (!item.value || busy.value) return
  if (!waitingModal.value.waitingFor.trim()) {
    message.warning('在等谁必填')
    return
  }
  transitioning.value = true
  try {
    await transition(item.value.id, {
      toStatus: 'WAITING',
      waitingFor: waitingModal.value.waitingFor.trim(),
      waitingExpectedAt: waitingModal.value.waitingExpectedAt?.toISOString(),
      nextFollowUpAt: waitingModal.value.nextFollowUpAt?.toISOString(),
      version: item.value.version
    })
    waitingModal.value.open = false
    if (actionOnly.value) emit('update:open', false)
    message.success('已进入等待')
    emit('changed')
    await refresh(true)
  } catch (e) {
    fail(e)
  } finally {
    transitioning.value = false
  }
}

async function submitFollowUp() {
  if (!item.value || busy.value) return
  transitioning.value = true
  try {
    await followUpAction(item.value.id, {
      outcome: followUpModal.value.outcome,
      note: followUpModal.value.note || undefined,
      nextFollowUpAt:
        followUpModal.value.outcome === 'STILL_WAITING'
          ? followUpModal.value.nextFollowUpAt?.toISOString()
          : undefined,
      toStatus: followUpModal.value.outcome === 'RESOLVED' ? followUpModal.value.toStatus : undefined,
      version: item.value.version
    })
    followUpModal.value.open = false
    if (actionOnly.value) emit('update:open', false)
    followUpModal.value.note = ''
    message.success('已记录跟进')
    emit('changed')
    await refresh(true)
  } catch (e) {
    fail(e)
  } finally {
    transitioning.value = false
  }
}

function openFollowUp(outcome: 'STILL_WAITING' | 'RESOLVED' | 'ABANDONED') {
  followUpModal.value = {
    open: true,
    outcome,
    note: '',
    nextFollowUpAt: dayjs().add(2, 'day').hour(10).minute(0),
    toStatus: item.value?.status === 'WAITING' ? 'DOING' : item.value?.status ?? 'DOING'
  }
}

async function submitNote() {
  if (!item.value || !note.value.trim() || busy.value) return
  noteSaving.value = true
  try {
    await createWorkLog({ itemId: item.value.id, content: note.value.trim() })
    note.value = ''
    workLogs.value = await fetchWorkLogs({ itemId: item.value.id })
    emit('changed')
  } catch (e) {
    fail(e)
  } finally {
    noteSaving.value = false
  }
}

async function applyPlan(value: Dayjs | null) {
  if (!item.value || busy.value) return
  saving.value = true
  try {
    if (value) await planItem(item.value.id, value.format('YYYY-MM-DD'), item.value.version)
    else await unplanItem(item.value.id)
    emit('changed')
    await refresh()
  } catch (e) { fail(e) }
  finally { saving.value = false }
}

async function canLeave(): Promise<boolean> {
  if (busy.value) { message.info('正在保存，请稍候'); return false }
  if (!titleDirty.value && !descriptionDirty.value && !note.value.trim() && !relatedEditing.value) return true
  return new Promise((resolve) => Modal.confirm({
    title: '还有未保存的内容', content: '关闭会丢失修改或尚未提交的工作记录。',
    okText: '放弃并关闭', cancelText: '继续编辑',
    onOk: () => resolve(true), onCancel: () => resolve(false)
  }))
}
async function closeDrawer() {
  if (!(await canLeave())) return
  title.value = item.value?.title ?? ''
  description.value = item.value?.description ?? ''
  note.value = ''
  cancelRelatedEdit()
  emit('update:open', false)
}
onBeforeRouteLeave(() => props.open ? canLeave() : true)
onBeforeRouteUpdate((to, from) => props.open && to.query.itemId !== from.query.itemId ? canLeave() : true)

async function removeItem() {
  if (!item.value) return
  try {
    await deleteItem(item.value.id)
    message.success('已删除')
    emit('changed')
    emit('update:open', false)
  } catch (e) {
    fail(e)
  }
}

watch(
  () => [props.open, props.itemId],
  async ([open]) => {
    if (open) {
      const requestedIntent = props.intent
      actionOnly.value = Boolean(requestedIntent)
      if (item.value?.id !== props.itemId) {
        relatedEditing.value = false; relatedDraft.value = []
        item.value = null; title.value = ''; description.value = ''; note.value = ''; activities.value = []; workLogs.value = []
      }
      loadProjects()
      await load()
      // 从列表行内点「等待 / 跟进」进来时，直接落到对应表单，不让用户再找一次入口
      if (requestedIntent === 'wait') openWaitingForm()
      if (requestedIntent === 'followUp') openFollowUp('STILL_WAITING')
      if (requestedIntent) emit('clear-intent')
    }
  }
)
</script>

<template>
  <a-drawer
    :open="open && !actionOnly"
    :width="540"
    :body-style="{ paddingBottom: '0' }"
    @close="closeDrawer"
  >
    <template #title>
      <div class="drawer-head">
        <div class="drawer-head__title">{{ item?.title ?? '事项' }}</div>
        <div class="meta-text">{{ drawerMeta }}</div>
      </div>
    </template>

    <!-- 只有首次打开才整块 loading；后续动作只刷新事实，不打断阅读 -->
    <a-spin :spinning="loading && !item">
      <template v-if="item">
        <!-- 第一屏：标题、归属、描述、当前要做什么 -->
        <a-textarea
          aria-label="编辑事项标题"
          v-model:value="title"
          :auto-size="{ minRows: 1, maxRows: 3 }"
          class="drawer-title"
          @blur="title !== item.title && saveBasics()"
        />

        <ItemStateFlow :item="item" :disabled="busy" @transition="onTransition" />

        <div class="drawer-projects" @keydown.esc.stop>
          <label for="item-main-project">主项目</label>
          <a-select id="item-main-project" aria-label="主项目" :value="item.projectId ?? undefined" :disabled="busy || relatedEditing" show-search option-filter-prop="label" placeholder="未分类（可选）" :allow-clear="!item.relatedProjects?.length" :options="projectOptions" @change="(value: string) => patch({ projectId: value, clearProject: !value }, '已修改主项目')" />
          <label for="item-related-projects">关联项目</label>
          <a-select id="item-related-projects" aria-label="关联项目" mode="multiple" v-model:value="relatedDraft" :disabled="busy || !item.projectId" show-search option-filter-prop="label" placeholder="其他项目也需要跟踪时再选" :options="projectOptions.filter(p => p.value !== item?.projectId)" @change="relatedEditing = true" />
          <div v-if="relatedEditing" class="drawer-projects__save"><a-button size="small" type="primary" :loading="saving" :disabled="busy" @click="saveRelatedProjects">保存关联</a-button> <a-button size="small" :disabled="busy" @click="cancelRelatedEdit">取消修改</a-button></div>
          <p class="meta-text">关联项目共享同一份进度和记录。日报按主项目归集，不重复生成。</p>
        </div>

        <div class="drawer-facts">
          <a-select
            :value="item.priority"
            :disabled="busy"
            size="small"
            style="width: 84px"
            :options="['P0', 'P1', 'P2', 'P3'].map((v) => ({ value: v, label: v }))"
            @change="(value: string) => patch({ priority: value as ItemPriority }, '已改优先级')"
          />
          <a-date-picker
            :value="planDate"
            size="small"
            placeholder="计划日期"
            :disabled="busy || item.type !== 'TASK' || !['TODO', 'DOING', 'WAITING'].includes(item.status)"
            @change="applyPlan"
          />
          <a-date-picker :value="item.dueAt ? dayjs(item.dueAt) : null" size="small" show-time placeholder="截止时间（可选）" :disabled="busy" @change="(value: Dayjs | null) => patch({ dueAt: value?.toISOString(), clearDueAt: !value })" />
          <span v-if="item.waitingDays" class="meta-text">已等 {{ item.waitingDays }} 天</span>
        </div>

        <!-- 先给工作语义，再给数据库属性：打开抽屉第一个要回答的是「我现在该对它做什么」（见 docs/产品与交互.md、docs/开发规范.md） -->
        <section v-if="item.type === 'TASK' && item.status !== 'INBOX' && item.status !== 'DONE' && item.status !== 'CANCELLED'" class="drawer-block">
          <div class="section__head">
            <h3 class="section-title">等待与跟进</h3>
            <span v-if="saving" class="section__hint">保存中…</span>
          </div>
          <div class="drawer-actions">
            <a-button v-if="canWait" size="small" :disabled="busy" @click="openWaitingForm">
              在等别人
            </a-button>
            <a-button size="small" type="text" @click="openFollowUp('STILL_WAITING')">
              设下次跟进
            </a-button>
            <!-- 取消是高代价动作，误点的代价高于多一次确认 -->
            <a-popconfirm
              v-if="canCancel"
              title="取消这件事？"
              ok-text="取消事项"
              cancel-text="再想想"
              @confirm="onTransition('CANCELLED')"
            >
              <a-button size="small" type="text" danger :disabled="busy">取消</a-button>
            </a-popconfirm>
          </div>
        </section>

        <a-textarea
          aria-label="事项描述"
          v-model:value="description"
          :auto-size="{ minRows: 2, maxRows: 10 }"
          placeholder="描述 / 清单（- [ ] 待办项）"
          class="drawer-desc"
          @blur="description !== (item.description ?? '') && saveBasics()"
        />

        <!-- 等待：回答我等谁、等什么、什么时候该有结果 -->
        <section v-if="isWaiting" class="drawer-block">
          <div class="section__head">
            <h3 class="section-title">我在等谁</h3>
          </div>
          <div class="field">
            <div class="field__label">等谁</div>
            <div class="field__value">{{ item.waitingFor || '未填写' }}</div>
          </div>
          <div class="field">
            <div class="field__label">预计什么时候给</div>
            <div class="field__value">
              {{ item.waitingExpectedAt ? dayjs(item.waitingExpectedAt).format('YYYY-MM-DD HH:mm') : '未填写' }}
            </div>
          </div>
          <div class="field">
            <div class="field__label">下次跟进</div>
            <div class="field__value">
              <a-date-picker
                :value="followUpDate"
                size="small"
                show-time
                @change="
                  (value: Dayjs) =>
                    patch({ nextFollowUpAt: value ? value.toISOString() : undefined }, '已改跟进时间')
                "
              />
              <a-button
                size="small"
                type="text"
                @click="patch({ clearNextFollowUp: true }, '已清除跟进')"
              >
                清除
              </a-button>
            </div>
          </div>
        </section>

        <!-- 到点跟进：三选一，不是打开编辑页 -->
        <section v-if="canFollowUp" class="drawer-block">
          <div class="section__head">
            <h3 class="section-title">到点跟进</h3>
          </div>
          <div class="drawer-actions">
            <a-button size="small" type="primary" @click="openFollowUp('STILL_WAITING')">
              还没回
            </a-button>
            <a-button size="small" @click="openFollowUp('RESOLVED')">对方给了</a-button>
            <a-button size="small" type="text" @click="openFollowUp('ABANDONED')">
              暂时不跟
            </a-button>
          </div>
        </section>

        <section class="drawer-block">
          <div class="section__head">
            <h3 class="section-title">记一句</h3>
            <span class="section__hint">只看系统看不出来的信息</span>
          </div>
          <a-textarea
            aria-label="工作记录"
            v-model:value="note"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            placeholder="做了什么 / 为什么这么决定？"
          />
          <div style="margin-top: 8px">
            <a-button size="small" :loading="noteSaving" :disabled="!note.trim() || busy" @click="submitNote">记录</a-button>
          </div>
          <div v-if="workLogs.length" class="drawer-logs">
            <div v-for="log in workLogs" :key="log.id" class="meta-text">
              {{ dayjs(log.occurredAt).format('MM-DD HH:mm') }} · {{ log.content }}
            </div>
          </div>
        </section>

        <a-collapse ghost>
          <a-collapse-panel key="history" header="历史">
            <div v-for="act in activities" :key="act.id" class="meta-text" style="padding: 2px 0">
              {{ dayjs(act.occurredAt).format('MM-DD HH:mm') }} · {{ ACTIVITY_LABEL[act.activityType] ?? act.activityType }}
              <template v-if="act.fromStatus && act.toStatus"> · {{ STATUS_LABEL[act.fromStatus as ItemStatus] }} → {{ STATUS_LABEL[act.toStatus as ItemStatus] }}</template>
              <template v-if="act.remark"> · {{ act.remark }}</template>
            </div>
          </a-collapse-panel>
        </a-collapse>

        <!-- 危险动作：明显降低视觉权重 -->
        <div v-if="item.status === 'INBOX'" class="drawer-danger">
          <a-popconfirm
            title="删除后无法恢复"
            ok-text="删除"
            cancel-text="不删"
            @confirm="removeItem"
          >
            <a-button size="small" type="text" danger>删除这条记录</a-button>
          </a-popconfirm>
        </div>
      </template>
    </a-spin>

    <!-- 主要动作固定在底部：打开就知道「这件事下一步是什么」（见 docs/产品与交互.md、docs/开发规范.md） -->
    <div v-if="primary" class="drawer-footer">
      <span v-if="item?.status === 'INBOX' && item.type === 'TASK'" class="meta-text">收下后，在今天或全部事项里安排计划</span>
      <a-button v-if="primary.status !== 'DONE' && item?.allowedTransitions?.includes('DONE')" :disabled="busy" @click="onTransition('DONE')">完成事项</a-button>
      <a-button
        type="primary"
        :loading="transitioning"
        :disabled="busy"
        @click="onTransition(primary.status)"
      >
        {{ primary.label }}
      </a-button>
    </div>

  </a-drawer>

    <a-modal
      :open="open && waitingModal.open"
      title="在等别人"
      ok-text="确认"
      :confirm-loading="transitioning"
      @ok="submitWaiting"
      @cancel="waitingModal.open = false; actionOnly && emit('update:open', false)"
    >
      <a-form layout="vertical" size="small">
        <a-form-item label="在等谁（必填）">
          <a-input v-model:value="waitingModal.waitingFor" placeholder="例如：厂商张工" />
        </a-form-item>
        <a-form-item label="对方预计什么时候给">
          <a-date-picker v-model:value="waitingModal.waitingExpectedAt" show-time style="width: 100%" />
        </a-form-item>
        <a-form-item label="我什么时候再推动">
          <a-date-picker v-model:value="waitingModal.nextFollowUpAt" show-time style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      :open="open && followUpModal.open"
      title="跟进"
      ok-text="提交"
      :confirm-loading="transitioning"
      @ok="submitFollowUp"
      @cancel="followUpModal.open = false; actionOnly && emit('update:open', false)"
    >
      <a-radio-group v-model:value="followUpModal.outcome" button-style="solid" size="small">
        <a-radio-button value="STILL_WAITING">还没回</a-radio-button>
        <a-radio-button value="RESOLVED">对方给了</a-radio-button>
        <a-radio-button value="ABANDONED">暂时不跟</a-radio-button>
      </a-radio-group>

      <a-form layout="vertical" size="small" style="margin-top: 16px">
        <a-form-item label="记一句（可留空）">
          <a-textarea v-model:value="followUpModal.note" :rows="2" />
        </a-form-item>
        <a-form-item v-if="followUpModal.outcome === 'STILL_WAITING'" label="下次跟进">
          <a-date-picker v-model:value="followUpModal.nextFollowUpAt" show-time style="width: 100%" />
        </a-form-item>
        <a-form-item v-if="followUpModal.outcome === 'RESOLVED'" label="回到哪个状态">
          <a-select
            v-model:value="followUpModal.toStatus"
            :options="['TODO', 'DOING', 'DONE'].filter((status) => status === item?.status || item?.allowedTransitions?.includes(status as ItemStatus)).map((status) => ({ value: status, label: STATUS_LABEL[status as ItemStatus] }))"
          />
        </a-form-item>
      </a-form>
    </a-modal>
</template>

<style scoped>
.drawer-title {
  font-size: 16px;
  font-weight: 500;
  padding: 0;
  border: none;
  box-shadow: none;
  resize: none;
}

.drawer-title:focus {
  box-shadow: none;
}

.drawer-facts {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 12px 0;
}

.drawer-projects { display: grid; grid-template-columns: 64px minmax(0, 1fr); gap: 8px; align-items: start; }
.drawer-projects label { padding-top: 5px; color: var(--color-text-secondary); }
.drawer-projects p { grid-column: 2; margin: 0; }
.drawer-projects__save { grid-column: 2; }

.drawer-desc {
  margin-bottom: 4px;
}

.drawer-block {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.drawer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.drawer-logs {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.drawer-danger {
  margin-top: 32px;
  text-align: right;
}

.drawer-head__title {
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
}

/* 底部主要动作：吸底但不做浮夸阴影，只用一条分隔线 */
.drawer-footer {
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  margin: var(--space-6) calc(var(--space-6) * -1) 0;
  padding: var(--space-3) var(--space-6);
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
}
</style>
