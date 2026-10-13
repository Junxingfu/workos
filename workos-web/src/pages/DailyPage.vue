<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { onBeforeRouteLeave } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { applyGenerated, fail, fetchDaily, fetchWorkLogs, generateDaily, updateDaily } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import type { Daily, WorkLog } from '@/types'

const date = ref<Dayjs>(dayjs())
const daily = ref<Daily | null>(null)
const workLogs = ref<WorkLog[]>([])
const content = ref('')
const loading = ref(false)
const saving = ref(false)
const previewOpen = ref(false)
const preview = ref('')
const sourceCount = ref(0)
const dirty = ref(false)

/** 未保存的正文不能被静默丢弃：切换日期或离开页面前先问一句（见 docs/产品与交互.md、docs/开发规范.md） */
function confirmDiscard(text: string): Promise<boolean> {
  return new Promise((resolve) => {
    Modal.confirm({
      title: '有未保存的修改',
      content: text,
      okText: '放弃修改',
      cancelText: '继续编辑',
      onOk: () => resolve(true),
      onCancel: () => resolve(false)
    })
  })
}

async function onDateChange(value: Dayjs | null) {
  if (!value) return
  if (value.format('YYYY-MM-DD') === date.value.format('YYYY-MM-DD')) return
  await applyDate(value)
}

/** 切换日期前保护未保存正文（见 docs/产品与交互.md、docs/开发规范.md） */
async function applyDate(next: Dayjs) {
  if (dirty.value) {
    const discard = await confirmDiscard(
      `「${date.value.format('M月D日')}」的正文还没保存，切换日期会丢失。`
    )
    if (!discard) return
  }
  date.value = next
  await load()
}

function shiftDate(delta: number) {
  return applyDate(date.value.add(delta, 'day'))
}

onBeforeRouteLeave(async () => {
  if (!dirty.value) return true
  return confirmDiscard('日报正文还没保存，离开会丢失。')
})

const headerContext = computed(() => [
  `${workLogs.value.length} 条记录`,
  daily.value?.generated ? '已生成过快照' : '未生成'
])

async function load() {
  const day = date.value.format('YYYY-MM-DD')
  loading.value = true
  try {
    daily.value = await fetchDaily(day)
    content.value = daily.value.content ?? ''
    workLogs.value = await fetchWorkLogs({ workDate: day })
    dirty.value = false
  } catch (e) {
    fail(e)
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    daily.value = await updateDaily(
      date.value.format('YYYY-MM-DD'),
      content.value,
      daily.value?.version
    )
    dirty.value = false
    message.success('已保存')
  } catch (e) {
    fail(e)
  } finally {
    saving.value = false
  }
}

/** 生成 = 快照，只写 generatedContent，不覆盖用户内容 */
async function generate() {
  try {
    const result = await generateDaily(date.value.format('YYYY-MM-DD'))
    preview.value = result.generatedContent
    sourceCount.value = result.sourceWorkLogCount
    if (daily.value) daily.value.version = result.version
    previewOpen.value = true
  } catch (e) {
    fail(e)
  }
}

async function applyPreview() {
  try {
    daily.value = await applyGenerated(
      date.value.format('YYYY-MM-DD'),
      preview.value,
      daily.value?.version
    )
    content.value = daily.value.content
    dirty.value = false
    previewOpen.value = false
    message.success('已应用')
  } catch (e) {
    fail(e)
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader :title="`日报 · ${date.format('M月D日')}`" :context="headerContext">
      <template #actions>
        <a-button size="small" type="text" aria-label="前一天" @click="shiftDate(-1)">←</a-button>
        <a-date-picker
          :value="date"
          size="small"
          :allow-clear="false"
          @change="onDateChange"
        />
        <a-button size="small" type="text" aria-label="后一天" @click="shiftDate(1)">→</a-button>
        <a-button size="small" type="text" @click="generate">按记录生成</a-button>
        <a-button size="small" type="primary" :loading="saving" @click="save">保存</a-button>
      </template>
    </PageHeader>

    <!-- 首屏加载才整块 loading；换日期和保存不再让正文变灰 -->
    <a-spin :spinning="loading && !daily">
      <a-textarea
        v-model:value="content"
        :rows="16"
        placeholder="# 今日工作&#10;&#10;## 项目名&#10;- 做了什么"
        @input="dirty = true"
      />
      <p class="meta-text" style="margin-top: 8px">
        {{ dirty ? '有未保存的修改' : '已与后端一致' }} · 生成只产生快照，点「应用」才会覆盖正文
      </p>

      <section class="section" style="margin-top: 24px">
        <div class="section__head">
          <h2 class="section-title">这一天的记录</h2>
          <span class="section__hint">只有 Item 表达不了的事实，才值得记一句</span>
        </div>
        <div v-if="workLogs.length" class="list">
          <div v-for="log in workLogs" :key="log.id" class="list-row" style="cursor: default">
            <div class="list-row__line">
              <span class="list-row__title">{{ log.content }}</span>
              <span class="meta-text">{{ dayjs(log.occurredAt).format('HH:mm') }}</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <p class="empty-state__title">这一天还没记过东西</p>
          <p class="empty-state__hint">在事项里用「记一句」补充那些系统看不出来的信息。</p>
        </div>
      </section>
    </a-spin>

    <a-modal
      :open="previewOpen"
      title="生成结果（预览）"
      width="720px"
      ok-text="应用并覆盖正文"
      @ok="applyPreview"
      @cancel="previewOpen = false"
    >
      <p class="meta-text">来源记录 {{ sourceCount }} 条。生成只是快照，点「应用」才会覆盖正文。</p>
      <a-textarea v-model:value="preview" :rows="16" />
    </a-modal>
  </div>
</template>
