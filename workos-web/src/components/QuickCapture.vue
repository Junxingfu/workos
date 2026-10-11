<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { fail, quickCapture } from '@/api'
import { notifyCaptureCreated } from '@/composables/useCaptureRefresh'

const props = withDefaults(defineProps<{ destination?: 'INBOX' | 'TODO' | 'TODAY' }>(), { destination: 'INBOX' })
const emit = defineEmits<{ saved: [] }>()
const content = ref('')
const destination = ref(props.destination)
const saving = ref(false)
const input = ref()
const result = ref('')
const options = [
  { value: 'TODAY', label: '今天做' },
  { value: 'TODO', label: '待办，稍后安排' },
  { value: 'INBOX', label: '先记下，待整理' }
]

async function focus() {
  await nextTick()
  input.value?.focus()
}
defineExpose({ focus })

async function submit() {
  if (!content.value.trim() || saving.value) return
  saving.value = true
  result.value = ''
  const submitted = content.value
  try {
    await quickCapture(submitted.trim(), destination.value)
    if (content.value === submitted) content.value = ''
    result.value = destination.value === 'TODAY' ? '已加入今日计划' : destination.value === 'TODO' ? '已加入待办' : '已放入收件箱'
    notifyCaptureCreated()
    emit('saved')
    await focus()
  } catch (e) {
    fail(e)
  } finally {
    saving.value = false
  }
}

function onEnter(event: KeyboardEvent) {
  if (event.isComposing || event.keyCode === 229) return
  event.preventDefault()
  void submit()
}
</script>

<template>
  <div class="capture-form">
    <a-input ref="input" v-model:value="content" :maxlength="500" :disabled="saving" placeholder="要做什么？写一句就能记录" aria-label="事项标题" @keydown.enter="onEnter" />
    <div class="capture-form__actions">
      <a-select v-model:value="destination" :disabled="saving" :options="options" aria-label="记录到哪里" style="width: 168px" />
      <a-button type="primary" :loading="saving" :disabled="!content.trim()" @click="submit">记录</a-button>
      <span class="meta-text" role="status">{{ result || 'Enter 记录' }}</span>
    </div>
  </div>
</template>

<style scoped>
.capture-form { margin-bottom: 20px; }
.capture-form__actions { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 8px; }
</style>
