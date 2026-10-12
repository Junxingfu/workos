<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { createProject, fail, fetchProjects } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import type { BasePageResult, Project } from '@/types'

/**
 * 项目列表回答：我有哪些项目、各自现在什么状态（design.md §32.1）
 * 用列表，不用卡片网格。
 */
const router = useRouter()
const data = ref<BasePageResult<Project> | null>(null)
const loading = ref(false)
const creating = ref(false)
const createOpen = ref(false)
const status = ref('ACTIVE')
const newName = ref('')
const newDesc = ref('')

async function load() {
  loading.value = true
  try {
    data.value = await fetchProjects({ status: status.value, pageNo: 1, pageSize: 100 })
  } catch (e) {
    fail(e)
  } finally {
    loading.value = false
  }
}

async function onCreate() {
  if (creating.value) return
  if (!newName.value.trim()) {
    message.warning('项目名不能为空')
    return
  }
  creating.value = true
  try {
    await createProject({ name: newName.value.trim(), description: newDesc.value || undefined })
    newName.value = ''
    newDesc.value = ''
    createOpen.value = false
    message.success('已创建')
    await load()
  } catch (e) {
    fail(e)
  } finally {
    creating.value = false
  }
}

watch(status, load)
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="项目" :sub="`${data?.total ?? 0} 个${status === 'ACTIVE' ? '进行中' : '已归档'}`">
      <template #actions>
        <a-popover v-model:open="createOpen" trigger="click" placement="bottomRight">
          <template #content>
            <div style="width: 260px">
              <a-input v-model:value="newName" placeholder="项目名" style="margin-bottom: 8px" />
              <a-input v-model:value="newDesc" placeholder="一句话说明（可选）" style="margin-bottom: 8px" />
              <a-button type="primary" size="small" block :loading="creating" @click="onCreate">
                新建
              </a-button>
            </div>
          </template>
          <a-button size="small" type="primary">新建项目</a-button>
        </a-popover>
      </template>
    </PageHeader>

    <a-segmented v-model:value="status" :options="[{ value: 'ACTIVE', label: '进行中' }, { value: 'ARCHIVED', label: '已归档' }]" style="margin-bottom: 16px" />

    <!-- 首屏加载才整块 loading；新建和刷新不再让列表变灰 -->
    <a-spin :spinning="loading && !data">
      <div v-if="data?.dataList?.length" class="list">
        <div
          v-for="project in data.dataList"
          :key="project.id"
          class="list-row"
          role="link"
          tabindex="0"
          @keydown.enter="router.push(`/projects/${project.id}`)"
          @click="router.push(`/projects/${project.id}`)"
        >
          <div class="list-row__line">
            <span class="list-row__title">{{ project.name }}</span>
            <span class="meta-text">{{ project.status === 'ACTIVE' ? '进行中' : '已归档' }}</span>
          </div>
          <div v-if="project.currentFocus || project.description" class="list-row__sub">
            <span>当前：{{ project.currentFocus || project.description }}</span>
          </div>
          <div class="list-row__sub">
            <span>未完成 {{ project.openCount ?? 0 }} · 进行中 {{ project.doingCount ?? 0 }} · 等待 {{ project.waitingCount ?? 0 }}</span>
            <span v-if="project.lastActivityAt">
              最近活动 {{ dayjs(project.lastActivityAt).format('MM-DD') }}
            </span>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <p class="empty-state__title">{{ status === 'ACTIVE' ? '还没有进行中的项目' : '没有已归档的项目' }}</p>
        <p class="empty-state__hint">{{ status === 'ACTIVE' ? '项目是可选的；不建项目也能用事项和今天。' : '归档不会删除事项，可以从项目详情恢复。' }}</p>
      </div>
    </a-spin>
  </div>
</template>
