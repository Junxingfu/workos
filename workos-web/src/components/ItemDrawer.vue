<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
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
import type { Item, ItemPriority, ItemStatus, Project, WorkLog } from '@/types'

/**
 * 事项操作中心（design.md §19 §36 §37）
 * 第一屏：标题 / 项目·状态·优先级 / 描述 / 现在需要做什么
 * 属性改动即时保存；标题与描述走显式保存。
 */
const props = defineProps<{ open: boolean; itemId?: string | null }>()
const emit = defineEmits<{ 'update:open': [boolean]; changed: [] }>()

const loading = ref(false)
const saving = ref(false)
const item = ref<Item | null>(null)
const projects = ref<Project[]>([])
const activities = ref<Array<{ id: string; activityType: string; remark?: string | null; occurredAt: string }>>([])
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

const ACTION_LABEL: Record<ItemStatus, string> = {
  TODO: '转为待办',
  DOING: '开始做',
  WAITING: '在等别人',
  DONE: '完成',
  CANCELLED: '取消',
  INBOX: '退回待整理'
}

/** 主要动作里排除「在等别人」，它有专属入口（一次收齐等待信息） */
const stateActions = computed(
  () => (item.value?.allowedTransitions ?? []).filter((target) => target !== 'WAITING')
)
const canWait = computed(() => item.value?.allowedTransitions?.includes('WAITING'))
const isWaiting = computed(() => item.value?.status === 'WAITING')
const canFollowUp = computed(() => item.value?.followUpDue || isWaiting.value)

async function load() {
  if (!props.itemId) return
  loading.value = true
  try {
    const detail = await fetchItem(props.itemId)
    item.value = detail
    title.value = detail.title
    description.value = detail.description ?? ''
    planDate.value = detail.plannedDate ? dayjs(detail.plannedDate) : null
    followUpDate.value = detail.nextFollowUpAt ? dayjs(detail.nextFollowUpAt) : null
    activities.value = await fetchActivities(props.itemId)
    workLogs.value = await fetchWorkLogs({ itemId: props.itemId })
  } catch (e) {
    fail(e)
  } finally {
    loading.value = false
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
  if (!item.value) return
  saving.value = true
  try {
    await updateItem(item.value.id, { title: title.value, description: description.value })
    message.success('已保存')
    emit('changed')
    await load()
  } catch (e) {
    fail(e)
  } finally {
    saving.value = false
  }
}

/** 单选类属性即时保存：少一次点击，路径更短 */
async function patch(payload: Parameters<typeof updateItem>[1], tip = '已更新') {
  if (!item.value) return
  try {
    await updateItem(item.value.id, payload)
    message.success(tip)
    emit('changed')
    await load()
  } catch (e) {
    fail(e)
  }
}

async function onTransition(to: ItemStatus) {
  if (!item.value) return
  if (to === 'WAITING') return openWaitingForm()
  try {
    await transition(item.value.id, { toStatus: to, version: item.value.version })
    message.success(ACTION_LABEL[to] ?? '已更新')
    emit('changed')
    await load()
  } catch (e) {
    fail(e)
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
  if (!item.value) return
  if (!waitingModal.value.waitingFor.trim()) {
    message.warning('在等谁必填')
    return
  }
  try {
    await transition(item.value.id, {
      toStatus: 'WAITING',
      waitingFor: waitingModal.value.waitingFor.trim(),
      waitingExpectedAt: waitingModal.value.waitingExpectedAt?.toISOString(),
      nextFollowUpAt: waitingModal.value.nextFollowUpAt?.toISOString(),
      version: item.value.version
    })
    waitingModal.value.open = false
    message.success('已进入等待')
    emit('changed')
    await load()
  } catch (e) {
    fail(e)
  }
}

async function submitFollowUp() {
  if (!item.value) return
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
    followUpModal.value.note = ''
    message.success('已记录跟进')
    emit('changed')
    await load()
  } catch (e) {
    fail(e)
  }
}

function openFollowUp(outcome: 'STILL_WAITING' | 'RESOLVED' | 'ABANDONED') {
  followUpModal.value = {
    open: true,
    outcome,
    note: '',
    nextFollowUpAt: dayjs().add(2, 'day').hour(10).minute(0),
    toStatus: 'DOING'
  }
}

async function submitNote() {
  if (!item.value || !note.value.trim()) return
  try {
    await createWorkLog({ itemId: item.value.id, content: note.value.trim() })
    note.value = ''
    workLogs.value = await fetchWorkLogs({ itemId: item.value.id })
    emit('changed')
  } catch (e) {
    fail(e)
  }
}

async function applyPlan(value: Dayjs | null) {
  if (!item.value) return
  if (!value) {
    await unplanItem(item.value.id)
    emit('changed')
    await load()
    return
  }
  try {
    await planItem(item.value.id, value.format('YYYY-MM-DD'), item.value.version)
    emit('changed')
    await load()
  } catch (e) {
    fail(e)
  }
}

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
  ([open]) => {
    if (open) {
      loadProjects()
      load()
    }
  }
)
</script>

<template>
  <a-drawer
    :open="open"
    :width="600"
    :body-style="{ paddingBottom: '32px' }"
    :title="item ? STATUS_LABEL[item.status] : '事项'"
    @close="emit('update:open', false)"
  >
    <a-spin :spinning="loading">
      <template v-if="item">
        <!-- 第一屏：标题、归属、描述、当前要做什么 -->
        <a-textarea
          v-model:value="title"
          :auto-size="{ minRows: 1, maxRows: 3 }"
          class="drawer-title"
          @blur="title !== item.title && saveBasics()"
        />

        <div class="drawer-facts">
          <a-select
            :value="item.projectId ?? undefined"
            size="small"
            style="width: 160px"
            placeholder="未分类"
            allow-clear
            :options="projects.map((p) => ({ value: p.id, label: p.name }))"
            @change="(value: string) => patch({ projectId: value ?? null }, '已改归属')"
          />
          <a-select
            :value="item.priority"
            size="small"
            style="width: 84px"
            :options="['P0', 'P1', 'P2', 'P3'].map((v) => ({ value: v, label: v }))"
            @change="(value: string) => patch({ priority: value as ItemPriority }, '已改优先级')"
          />
          <a-date-picker
            :value="planDate"
            size="small"
            placeholder="计划日期"
            @change="applyPlan"
          />
          <span v-if="item.waitingDays" class="meta-text">已等 {{ item.waitingDays }} 天</span>
        </div>

        <a-textarea
          v-model:value="description"
          :auto-size="{ minRows: 2, maxRows: 10 }"
          placeholder="描述 / 清单（- [ ] 待办项）"
          class="drawer-desc"
          @blur="description !== (item.description ?? '') && saveBasics()"
        />

        <section class="drawer-block">
          <div class="section__head">
            <h3 class="section-title">这件事现在要做什么</h3>
            <span v-if="saving" class="section__hint">保存中…</span>
          </div>
          <div class="drawer-actions">
            <a-button
              v-for="target in stateActions"
              :key="target"
              size="small"
              :type="target === 'DONE' ? 'primary' : 'default'"
              @click="onTransition(target)"
            >
              {{ ACTION_LABEL[target] }}
            </a-button>
            <a-button v-if="canWait" size="small" @click="openWaitingForm">在等别人</a-button>
            <a-button size="small" type="text" @click="openFollowUp('STILL_WAITING')">
              设下次跟进
            </a-button>
          </div>
        </section>

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
            v-model:value="note"
            :auto-size="{ minRows: 2, maxRows: 4 }"
            placeholder="做了什么 / 为什么这么决定？"
          />
          <div style="margin-top: 8px">
            <a-button size="small" :disabled="!note.trim()" @click="submitNote">记录</a-button>
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
              {{ dayjs(act.occurredAt).format('MM-DD HH:mm') }} · {{ act.activityType }}
              <template v-if="act.remark"> · {{ act.remark }}</template>
            </div>
          </a-collapse-panel>
        </a-collapse>

        <!-- 危险动作：明显降低视觉权重 -->
        <div v-if="item.status === 'INBOX'" class="drawer-danger">
          <a-button size="small" type="text" danger @click="removeItem">删除这条记录</a-button>
        </div>
      </template>
    </a-spin>

    <a-modal
      :open="waitingModal.open"
      title="在等别人"
      ok-text="确认"
      @ok="submitWaiting"
      @cancel="waitingModal.open = false"
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
      :open="followUpModal.open"
      title="跟进"
      ok-text="提交"
      @ok="submitFollowUp"
      @cancel="followUpModal.open = false"
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
            :options="[
              { value: 'DOING', label: '继续做' },
              { value: 'DONE', label: '直接完成' }
            ]"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-drawer>
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
</style>
