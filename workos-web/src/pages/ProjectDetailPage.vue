<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { archiveProject, fail, fetchProject, restoreProject, updateProjectContext } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import type { Project } from '@/types'

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
  { key: 'blocker', label: '当前阻塞', value: form.value.blocker },
  { key: 'nextAction', label: '下一步', value: form.value.nextAction }
])

const secondaryFields = computed(() => [
  { key: 'progressNote', label: '当前进展', value: form.value.progressNote },
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
    form.value = {
      goal: context?.goal ?? '',
      currentFocus: context?.currentFocus ?? '',
      progressNote: context?.progressNote ?? '',
      blocker: context?.blocker ?? '',
      nextAction: context?.nextAction ?? '',
      keyPeople: context?.keyPeople ?? '',
      keyLinks: context?.keyLinks ?? '',
      notesMarkdown: context?.notesMarkdown ?? ''
    }
  } catch (e) {
    fail(e)
  } finally {
    loading.value = false
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
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader
      :title="project?.name ?? ''"
      :context="[
        project?.status === 'ARCHIVED' ? '已归档' : '进行中',
        ...(staleText ? [staleText] : [])
      ]"
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

    <a-spin :spinning="loading">
      <!-- 单调事实行，不做 KPI 卡片 -->
      <div class="facts" style="margin-bottom: 24px">
        <span>待办 {{ project?.statistics?.todo ?? 0 }}</span>
        <span>进行中 {{ project?.statistics?.doing ?? 0 }}</span>
        <span>等待 {{ project?.statistics?.waiting ?? 0 }}</span>
        <span>已完成 {{ project?.statistics?.done ?? 0 }}</span>
      </div>

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
          <a-button size="small" type="text" @click="toggleArchive">
            {{ project?.status === 'ARCHIVED' ? '恢复项目' : '归档项目' }}
          </a-button>
          <span class="section__hint">归档是业务状态，不做物理删除</span>
        </div>
      </section>
    </a-spin>
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
